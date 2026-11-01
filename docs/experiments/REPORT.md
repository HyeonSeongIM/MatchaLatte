# 상품 저장 로직 동기 처리 성능 저하 원인 분석 보고서

## 요약

**현재(비동기 전환 이후) 상품 생성 API는 이 문제가 없다.** 이 보고서는 "과거에 MySQL 저장
후 ES 색인까지 요청 스레드에서 동기적으로 기다리는 구조였다면, 왜 느렸을 것인가"라는
가설을 세우고 실험으로 검증한 기록이다. 이 레포의 git 히스토리에는 그 시절의 실제 동기
구현이나 `RefreshPolicy` 설정이 남아있지 않아, 과거 코드가 실제로 무엇을 했는지는
확정할 수 없다 — 대신 동일한 기술 스택으로 "동기 + refresh 강제 대기" 조건을 재구성해
같은 규모의 지연이 재현되는지를 확인하는 방식으로 가설을 검증했다.

> ES는 문서를 저장(save)한다고 자동으로 refresh를 수행하지 않는다. refresh는 기본적으로
> `refresh_interval`(기본 1s)마다 백그라운드에서 알아서 돌거나, 호출하는 쪽이
> `refresh=true`/`wait_for`를 **명시적으로 지정했을 때만** 강제로 일어난다. 따라서 "동기
> 저장이 느렸다"는 현상은 "save가 refresh를 자동으로 유발해서"가 아니라, "저장 로직이
> refresh 완료를 명시적으로 기다리도록 짜여 있었다면"이라는 조건부 설명이다.

가설 검증을 위해 세 단계 실험을 진행했다.

- **실험 1 (ES 메커니즘 단독 검증):** ES 단건 색인 API를 refresh 옵션별로 직접 호출해
  비교한 결과, `refresh=wait_for`는 평균 984.7ms, `refresh=false`(기본값)는 11.6ms로
  **약 85배** 차이가 났다.
- **실험 2 (애플리케이션 코드 수준 원인 분리):** 실제 코드 스택(Spring Boot + JPA + Spring
  Data Elasticsearch)으로 동기 로직을 재구성해 `RefreshPolicy`만 바꿔 k6로 부하 테스트한
  결과, `IMMEDIATE`는 평균 103.3ms, `NONE`은 13.3ms로 **약 7.8배** 차이가 났고 처리량도
  7.8배(193 → 1501 req/s) 늘었다.
- **실험 3 (동기 vs 비동기 API 응답시간 대조):** 같은 하네스에 운영 구조
  (`ProductEventer`/`@Async` 리스너)를 그대로 재현한 `/async` 엔드포인트를 추가해 동기
  버전과 정면 비교한 결과, 동기(강제 refresh)는 평균 178.95ms, 비동기는 평균 14.27ms로
  **약 12.5배** 차이가 났고 처리량도 12.5배(111.5 → 1394.7 req/s) 늘었다.

세 실험 모두 "동기 + refresh 강제 대기" 조건을 재현했을 때만 큰 지연이 나타났고, 병목이
"동기 호출" 그 자체보다 **"refresh 대기"** 에 있다는 가설과 일관된 결과를 보였다. 현재
적용된 이벤트 기반 비동기 구조(요청 스레드에서 ES 호출 자체를 제거하고, bulk 색인 시
ES 기본값 `refresh=false`를 그대로 사용)는 실험에서 재현한 이 병목이 실제로 존재했다면
정확히 제거하는 방향이며, `multicast-server`의 실제 색인 코드(`SyncItemWriter`)를 확인한
결과 이 기본값을 덮어쓰지 않고 그대로 쓰고 있음을 코드로 확인했다.

## 1. 배경

- 상품 생성 API는 MySQL에 상품을 저장한 뒤, 검색을 위해 ES에도 동일 데이터를 색인해야 한다.
- 과거에는(또는 순수 동기 구현이었다면) 이 ES 색인까지 요청 스레드가 끝날 때까지 기다렸을
  것으로 추정되며, 이 구간이 상품 생성 API 응답 지연의 주요 원인으로 지목됐다.
- 현재는 `ProductEventer` → `@Async` 이벤트 리스너 → multicast-server 큐잉/배치로 ES 색인을
  요청 스레드와 완전히 분리한 상태다.
- 이 보고서는 "왜 동기였을 때 그렇게 느렸는가"를 실험으로 재현·검증한 기록이다.

## 2. 가설

> ES 색인 로직이 refresh를 호출(또는 대기)하기 때문에 동기 저장이 느려졌다.

## 3. 실험 1 — ES 자체의 refresh 메커니즘 검증

### 방법
- 로컬 격리 환경(Docker, Elasticsearch 8.12.0 단일 노드, `refresh_interval` 기본값 1s)에서
  단건 색인 API(`POST /{index}/_doc/{id}?refresh=...`)의 응답 시간을 `refresh` 옵션별로
  각 40회 반복 측정.

### 결과

| refresh 옵션 | 의미 | avg | p50 | p95 |
|---|---|---|---|---|
| `false` (기본값) | refresh를 기다리지 않고 즉시 응답 | 11.6ms | 5.4ms | 13.0ms |
| `wait_for` | 다음 refresh 주기까지 대기 | 984.7ms | 995.3ms | 1010.9ms |
| `true` | 즉시 강제 refresh 수행 후 응답 | 11.8ms | 11.0ms | 15.1ms |

동시 쓰기 부하(8 workers) 상황에서 추가 측정한 결과, `true`는 부하가 없을 때 11.8ms였던
것이 **48.7ms로 4배 이상** 증가한 반면 `wait_for`는 부하 여부와 무관하게 항상
`refresh_interval`(1s)에 수렴하는 값을 보였다.

### 해석
- ES는 문서를 색인해도 내부 메모리 버퍼에 쌓아뒀다가 `refresh_interval`마다 한 번씩 새
  Lucene 세그먼트를 만들어야 검색에 반영된다.
- `wait_for`는 이 주기를 기다리므로, 매 요청이 평균적으로 `refresh_interval`의 대부분을
  대기하게 되어 요청당 최대 ~1초의 고정 지연이 생긴다.
- `true`는 대기 없이 즉시 강제 refresh를 수행하지만, 이 강제 refresh 자체의 비용은 동시
  쓰기 양(세그먼트 수, merge 압박)에 비례해 커진다.

## 4. 실험 2 — 애플리케이션 코드 수준에서 원인 분리

실험 1은 ES 자체의 특성만 확인한 것이라, 실제 서비스 코드(MySQL insert + ES 색인을 한
요청 스레드에서 순차 처리)에서도 같은 경향이 나타나는지, 그리고 "동기 호출 구조 자체"와
"refresh 대기"라는 두 원인 중 무엇이 지배적인지 분리할 필요가 있었다.

### 방법
- 운영 스택과 동일한 조합(Spring Boot 3.3.4 + Spring Data JPA + Spring Data Elasticsearch)으로
  독립 실행 가능한 하네스(`docs/experiments/sync-harness/`)를 구성.
- 컨트롤러 로직은 완전히 동일(“MySQL 저장 → ES 색인”을 요청 스레드에서 순차 대기)하게 두고,
  `ElasticsearchOperations.withRefreshPolicy(...)`의 정책만 다르게 설정한 엔드포인트 2개를 비교:
  - `POST /sync-immediate` — `RefreshPolicy.IMMEDIATE`
  - `POST /sync-none` — `RefreshPolicy.NONE`
- 로컬 MySQL 8.0 + ES 8.12.0(Docker)에서 k6(`constant-vus`, VU 20, 30초)로 각각 부하 테스트.

### 결과

| 엔드포인트 | RefreshPolicy | avg | p90 | p95 | 처리량 |
|---|---|---|---|---|---|
| `/sync-immediate` | IMMEDIATE | 103.3ms | 137.7ms | 166.5ms | 193 req/s |
| `/sync-none` | NONE | 13.3ms | 16.5ms | 19.1ms | **1500.8 req/s** |

같은 동기 흐름(요청 스레드에서 MySQL → ES 순차 대기)인데도 refresh 정책만 바꿨을 뿐인데
평균 응답시간이 **약 7.8배**, 처리량도 **약 7.8배** 차이가 났다.

### 판정
| 결과 | 의미 |
|---|---|
| **refresh 끄니까 동기인데도 크게 빨라짐** ✅ 해당 | 병목은 요청당 refresh |
| 약간만 빨라짐 | refresh + 동기 호출 자체 둘 다 원인 |
| 차이 없음 | 다른 원인(커넥션 풀, ES 부하 등) |

동기 호출 구조(요청 스레드 점유) 자체도 처리량 상한을 만드는 요인이긴 하지만, 같은 동기
구조에서 refresh만 껐을 때 7.8배가 개선된 것을 보면 **지연의 대부분은 refresh 대기가
차지하고 있었다**고 결론지을 수 있다.

## 5. 실험 3 — 동기 vs 비동기 API 응답시간 직접 비교

실험 2는 "동기 구조 안에서 refresh만 켜고 끈" 비교였다. 이번에는 한 걸음 더 나아가, 실제
운영 구조(`ProductEventer` → `@Async` 리스너로 ES 색인을 요청 스레드 밖으로 완전히 빼는
방식)를 하네스에 그대로 재현한 `/async` 엔드포인트를 추가해 "동기 저장" 대 "비동기 저장"을
같은 API 응답시간 기준으로 정면 비교했다.

### 방법
- 기존 하네스(`docs/experiments/sync-harness/`)에 운영과 동일한 이벤트 기반 비동기 컴포넌트
  3종을 추가:
  - `ProductCreatedEvent` — 운영 `ProductEvent`에 대응하는 이벤트 페이로드
  - `AsyncConfig` — 운영 `AsyncConfig`와 동일하게 이벤트 처리 전용 스레드풀(`productEventExecutor`)을 분리
  - `ProductEventListener` — 운영 `ProductListener`와 동일하게 `@EventListener` + `@Async`로
    요청 스레드 밖에서 ES 색인을 수행(`RefreshPolicy.NONE`, 즉 ES 기본값과 동일 조건)
- `POST /async` 엔드포인트는 MySQL insert만 요청 스레드에서 기다리고, ES 색인은 이벤트
  발행 후 즉시 응답 — 운영 `ProductService.createProduct()`와 동일한 흐름.
- 비교 대상: `POST /sync-immediate`(`RefreshPolicy.IMMEDIATE`로 강제 refresh까지 요청
  스레드가 기다리는, 과거의 "느렸던 동기 버전" 재현) vs `POST /async`.
- 동일 세션에서 k6(`constant-vus`, VU 20, 30초)로 `/sync-immediate` → `/sync-none` →
  `/async` 순서로 연속 측정(장비 부하 조건을 동일하게 유지하기 위해 실험 2와는 별도로
  재측정).

### 결과

| 엔드포인트 | 구조 | avg | p90 | p95 | 처리량 |
|---|---|---|---|---|---|
| `/sync-immediate` | 동기 + 강제 refresh (과거 "느렸던" 재현) | 178.95ms | 228.59ms | 260.09ms | 111.5 req/s |
| `/sync-none` | 동기 + refresh 생략 | 16.39ms | 23.17ms | 26.91ms | 1215.0 req/s |
| `/async` | **비동기** (ProductEventer/@Async 재현) | **14.27ms** | 17.90ms | 20.35ms | **1394.7 req/s** |

동기(강제 refresh) → 비동기 전환만으로 평균 응답시간이 **178.95ms → 14.27ms, 약 12.5배**
빨라졌고, 처리량도 **111.5 → 1394.7 req/s, 약 12.5배** 늘었다.

흥미로운 점은 `/async`가 `/sync-none`(refresh만 끈 동기 버전, 16.39ms)보다도 소폭 더
빠르다는 것이다. `/sync-none`은 refresh를 기다리지 않더라도 여전히 요청 스레드가 ES 색인
호출의 왕복(네트워크 + 색인 처리)을 직접 기다리는 반면, `/async`는 그 호출 자체를 요청
스레드 밖으로 완전히 넘기고 MySQL insert만 끝나면 바로 응답하기 때문이다. 즉 refresh를
끄는 것만으로도 대부분의 지연은 사라지지만, ES 호출 자체를 비동기로 분리하면 남은 왕복
비용까지 마저 제거된다.

### 판정

- **동기 → 비동기 전환은 실제 API 응답시간에서 약 12.5배 개선을 만들었다.**
- 이 개선분의 대부분(178.95ms → 16.39ms, 약 91%)은 refresh 대기 제거(실험 2와 동일한
  메커니즘)에서 오고, 나머지(16.39ms → 14.27ms)는 ES 호출 자체를 요청 스레드에서 제거한
  효과다. 두 원인 모두 같은 방향(요청 스레드가 ES를 기다리지 않게 만드는 것)을 가리키며,
  현재 채택된 이벤트 기반 비동기 구조가 이 두 가지를 모두 해결한다.

## 6. 종합 결론

1. "동기 저장이 느렸다"는 현상은 동기 호출 자체의 오버헤드(스레드 점유, HTTP/DB 커넥션 등)
   보다, **ES가 검색 가능해지는 시점까지 클라이언트를 붙잡아두는 refresh 정책**을 저장
   경로에 직접 얹었기 때문일 가능성이 가장 크다.
2. 특히 `wait_for` 방식이었다면 `refresh_interval`(기본 1s) 자체가 매 요청의 하한
   지연시간이 되어버리는 구조라서, 트래픽이 줄어도 사라지지 않는 고정 비용이었을 것이다.
3. `IMMEDIATE`(강제 refresh) 방식이었다면 평상시에도 평균 100ms 안팎의 지연이 있었고,
   트래픽이 늘수록 강제 refresh 비용 자체도 함께 증가하는 구조였다.
4. 실제로 동기 구조를 비동기(이벤트 기반)로 전환하면 API 평균 응답시간이 **약 12.5배**
   (178.95ms → 14.27ms) 개선되고, 처리량도 12.5배(111.5 → 1394.7 req/s) 늘어난다는 것을
   실험 3에서 직접 확인했다.

## 7. 현재 구조가 이 문제를 해결하는 방식

- 상품 생성 요청 스레드는 ES 색인을 기다리지 않는다 (`ProductEventer` → `@Async` 이벤트
  리스너로 완전히 분리).
- 실제 색인은 multicast-server가 큐잉 후 배치로 처리하며, bulk 색인 시 `refresh` 파라미터를
  지정하지 않아 ES 기본값(`refresh=false`)을 그대로 사용한다 — 이번 실험에서 가장 빨랐던
  조건과 동일.
- 대량 재색인(배치) 시에는 `refresh_interval=-1`로 임시 조정해 색인 처리량 자체를 더
  최적화하고, 완료 후 원복한다(`SyncAliasManager`, `SyncJobListener`).

즉 현재 구조는 이번 실험에서 확인된 병목(요청당 refresh 대기)을 근본적으로 제거하는
방향으로 이미 개선되어 있다.

## 부록 — 재현 자료

| 구분 | 경로 |
|---|---|
| 실험 1 상세 기록 | [`es-sync-refresh-latency.md`](./es-sync-refresh-latency.md) |
| 실험 1 스크립트 | [`scripts/es_refresh_bench.sh`](./scripts/es_refresh_bench.sh) |
| 실험 2 상세 기록 | [`sync-refresh-cause-isolation.md`](./sync-refresh-cause-isolation.md) |
| 실험 2/3 하네스 소스 | [`sync-harness/`](./sync-harness) |
| 실험 2/3 k6 스크립트 | [`scripts/k6-sync-refresh.js`](./scripts/k6-sync-refresh.js) |
| 실험 3 비동기 재현 코드 | `sync-harness/.../AsyncConfig.java`, `ProductCreatedEvent.java`, `ProductEventListener.java`, `SyncBenchController.java`(`/async`) |

세 실험 모두 로컬 Docker 컨테이너(MySQL 8.0 + Elasticsearch 8.12.0)로 격리된 환경에서
수행했으며, 운영 ES/DB에는 영향을 주지 않았다. 실험 3의 `/async`, `/sync-immediate`,
`/sync-none` 3개 엔드포인트는 같은 세션에서 연속으로 측정한 값이라 서로 비교 가능하다
(단, 실험 2 단독 수치와는 측정 시점의 장비 부하 차이로 절대값이 다를 수 있음 — 배율로
비교할 것). 재현 절차는 실험 2 상세 기록 문서의 "재현 방법" 섹션을 따르되, k6
`TARGET_URL`에 `/async`를 추가로 지정하면 된다.
