# 100만 건 재색인 chunk 크기 실험 (mysqlToEsJob)

## 요약 (이력서용)

> 배치 chunk 크기 100 / 500 / 1,000 / 5,000 / 10,000 / 20,000 / 30,000 / 50,000 / 100,000을 비교해, 100만 건 재색인이 가장 빠른 **5,000**으로 설정
> (**chunk 100 기준 1분 35초 → 38초, 약 60% 단축**)

> 재색인 배치와 alias 교체가 진행되는 동안 k6로 검색을 계속 보낸 결과 **검색 성공률 100% (2,659 / 2,659 요청)**

- 기준: chunk 100(가장 느림)의 중앙값 95.2초 → chunk 5,000의 중앙값 37.6초. 둘 다 3회 측정의 중앙값이다.
- 1,000 기준으로 쓰면 "43.9초 → 37.6초 (약 14% 단축)"이다.
- 단서: 5,000 ~ 20,000은 37.6 ~ 39.8초로, 크기 간 차이가 회차 간 편차(5,000 안에서도 37.1 ~ 43.1초)보다 작다. 그래서 "5,000이 유일하게 빠르다"가 아니라 **"5,000 이상에서 수렴하며, 그중 bulk 1회 지연이 가장 작은 5,000을 택했다"** 가 정확한 서술이다.

## 측정 환경

| 항목 | 값 |
|---|---|
| 호스트 | MacBook, Apple M1 (8코어), RAM 16GB, macOS 15.6, Docker 27.5.1 |
| Elasticsearch | 8.12.0, **단일 노드**, 컨테이너 CPU 2 / 메모리 3GB (힙 1GB), analysis-nori |
| 인덱스 | `product-index-settings.json` 그대로 (샤드 1, 레플리카 0, nori, `dynamic: strict`), 배치 중 `refresh_interval=-1` |
| MySQL | 8.0.46, 컨테이너 CPU 2 / 메모리 2GB, `products` 100만 건 |
| multicast-server | 로컬 JVM 17, `-Xmx1g`, 실제 코드 그대로 (`JdbcPagingItemReader` pageSize = chunk 크기, ES `socket-timeout: 3s`) |
| 부하 도구 | k6 v2.0.0 |

상품 데이터는 이름 `"{말차|라떼|녹차|…} {세트|선물|…} {n}"`, 설명, 가격, userId로 구성했다. 문서 하나는 약 70B(인덱스 저장 크기 기준)이다.

## 방법

- 측정 구간: `POST /api/internal/sync/mysql-to-es` 호출부터 응답까지.
  - `JobLauncher`가 동기로 실행되므로, 이 구간에 새 인덱스 생성, MySQL 읽기, bulk 색인, refresh 복구, **alias 교체**가 모두 들어 있다.
  - Spring Batch `BATCH_STEP_EXECUTION`의 step 시간도 함께 기록했다.
- 회차마다 alias를 `products_base`로 되돌리고 이전 인덱스를 지운 뒤 실행했다.
- 크기마다 앱을 새로 띄워서 3회씩 측정했다.
- 스크립트: `docs/experiments/scripts/reindex_chunk_bench.sh`, `docs/experiments/scripts/k6-search-during-reindex.js`
- 원본 데이터: `docs/experiments/results/` (CSV, 앱 로그, k6 결과)

## 결과 1 — chunk 크기별 100만 건 재색인 시간

| chunk | 1회 | 2회 | 3회 | **중앙값** | bulk 횟수 | bulk took p50 | bulk took max |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 100 | 98.6 | 94.4 | 95.2 | **95.2초** | 10,000 | 1ms | 742ms |
| 500 | 57.3 | 53.5 | 58.9 | 57.3초 | 2,000 | 6ms | 989ms |
| 1,000 | 48.8 | 43.8 | 43.9 | 43.9초 | 1,000 | 10ms | 895ms |
| **5,000** | 37.6 | 37.1 | 43.1 | **37.6초** | 200 | 46ms | 828ms |
| 10,000 | 39.4 | 40.4 | 39.8 | 39.8초 | 100 | – | – |
| 20,000 | 38.6 | 38.8 | 39.9 | 38.8초 | 50 | 196ms | 1,045ms |
| 30,000 | 43.4 | 43.3 | 38.9 | 43.3초 | 34 | 303ms | 1,461ms |
| 50,000 | 41.6 | 48.3 | 43.7 | 43.7초 | 20 | 623ms | 2,166ms |
| 100,000 | 43.2 | 37.3 | 38.2 | 38.2초 | 10 | 1,337ms | **2,322ms** |
| 1,000,000 | **FAILED** (21.1초 만에 실패) | – | – | – | 1 | – | – |

모든 성공 회차는 ES 문서 100만 건과 `COMPLETED`를 확인했다.

bulk took 열은 앱 로그의 ES `took` 값이다. 앱 로그는 크기별 3회 분량이 누적된 것이고, 10,000은 k6 회차가 로그를 덮어써서 집계에서 뺐다.

### 해석

- **100 → 5,000: 처리 시간이 줄어든다.** chunk가 작으면 100만 건에 bulk 요청이 1만 번(100 기준) 생긴다. 요청마다 HTTP 왕복, 트랜잭션 커밋, `BATCH_STEP_EXECUTION` 갱신이 붙는데, 이 고정 비용이 지배적이다.
- **5,000 이상: 수렴한다.** 요청 횟수가 200번 이하로 줄면 고정 비용은 무시할 만해진다. 이때부터는 ES의 실제 색인 처리량(CPU 2개)이 병목이라 크기를 더 키워도 빨라지지 않는다.
- **30,000 이상: 편차와 위험이 커진다.**
  - bulk 1회 지연이 크기에 비례해 커진다. 100,000에서는 max 2.3초로, 앱의 ES `socket-timeout` 3초에 근접했다.
  - 실패하면 재시도할 단위도 그만큼 커진다.
- 그래서 수렴 구간 중 **bulk 1회 지연이 가장 작은(타임아웃 여유가 가장 큰) 5,000**을 기본값으로 정했다.

## 결과 2 — chunk 1,000,000 (한 번에 bulk 1회)

- 결과: `FAILED`, ES 응답 `413 Request Entity Too Large`
  - ES 기본 `http.max_content_length`는 100MB이다. 100만 건을 bulk 요청 하나에 담으면 이 한도를 넘는다.
  - write 0건으로 Step이 실패했다.
- `SyncJobListener`는 Job이 `COMPLETED`가 아니면 alias 교체를 건너뛴다. 그래서 alias는 기존 `products_base`를 그대로 가리켰고, 검색 서비스에는 영향이 없었다.
- 결론: chunk는 "클수록 좋다"가 아니다. 요청 크기 한도와 소켓 타임아웃으로 정해지는 상한이 있다.

## 결과 3 — 무중단 검증 (재색인 + alias 교체 중 검색)

- 조건:
  - chunk 10,000으로 재색인 1회를 돌렸다(43.2초).
  - 배치 시작 5초 전부터 alias 교체 5초 뒤까지, k6로 `products` alias에 초당 50건씩 검색을 보냈다.
  - 쿼리는 `SearchService.searchProducts`와 같다(`match name`, size 10).
- 성공 판정: HTTP 200 **이고 검색 결과가 1건 이상**일 때. 빈 인덱스로 교체되는 경우도 장애로 잡기 위해서다.

| 지표 | 값 |
|---|---|
| 검색 요청 | 2,659건 (50 req/s) |
| **검색 성공률** | **100%** (HTTP 실패 0건, 결과 0건 응답 0건) |
| 응답 시간 | 평균 4.03ms, p95 8.28ms, max 123ms |

alias 교체가 `_aliases`의 remove와 add를 한 요청으로 원자적으로 처리하기 때문에, 교체 순간에도 검색이 끊기거나 빈 결과가 나오지 않았다.

## 실험 중 발견해 수정한 버그 (실험 전에는 재색인 Job이 동작하지 않았음)

1. **테이블 이름 오류**
   - 증상: `BatchConfig`, `GhostCheckTasklet`, `RepairService`가 `FROM product`로 조회하는데, 실제 테이블은 `products`(`ProductEntity` 기준)이다. 실행하면 `Table 'product' doesn't exist`가 난다.
   - 조치: 세 곳 모두 `products`로 고쳤다.
2. **매핑 불일치**
   - 증상: 인덱스 매핑은 설계상 `description`을 제외하고 `dynamic: strict`인데, `ProductDocument`가 `description`을 보내고 있었다. 그래서 모든 문서가 `strict_dynamic_mapping_exception`(400)으로 거절됐다.
   - 더 큰 문제: `SyncItemWriter`는 bulk 오류를 로그만 남기고 예외를 던지지 않는다. 그 결과 Job이 `COMPLETED`로 끝나고 **빈 인덱스로 alias가 교체될 수 있는 구조**였다.
   - 조치: 매핑 설계와 테스트(`SyncAliasManagerTest`)에 맞춰 `ProductDocument`에서 `description`을 제거했다.
   - 남은 과제: `SyncItemWriter`의 오류 무시는 이번 범위에서 고치지 않았다. 별도 수정 대상이다.

## 한계

- 단일 노드에 레플리카 0이고, 노트북의 Docker 환경이다. 운영처럼 노드나 레플리카가 늘면 절대 시간과 최적 구간이 달라질 수 있다.
- 크기마다 3회만 측정했다. 수렴 구간(5,000 ~ 100,000)의 순위는 오차 범위 안이다.
- 첫 측정은 시드 데이터의 한글이 이중 인코딩된 상태로 진행해 무효 처리했다(`results/invalid_mojibake/`).
  - 데이터를 복구한 뒤 전 구간을 다시 측정했고, 위 표는 모두 재측정 값이다.
  - chunk 100은 두 측정에서 95.8초 / 95.2초로 거의 같았다.
