# ES 3노드 / replica 2 재색인 비용과 해결책 실험

## 요약 (이력서용)

> primary 1, replica 2로 바꾸자 replica마다 문서를 다시 색인하게 되어 100만 건 재색인의 **ES 색인 시간이 2.3배**로 늘었다 (bulk 21.6초 → 49.3초).

> 새 인덱스를 replica 0, refresh -1로 만들어 primary에만 색인했다. 이어서 forcemerge(세그먼트 1개)를 하고, replica를 2로 올려 세그먼트 파일을 복사하게 한 뒤, refresh를 복구하고 alias를 교체했다.
> **재색인 시간 80.3초 → 61.2초 (약 24% 단축)**.
> 이 과정 내내 검색 성공률은 **100% (3,579 / 3,579)** 였다.

- "3배"라는 표현의 정확한 의미:
  - 작업량 기준(문서를 3벌 색인)으로는 맞다.
  - 시간 기준으로는 2.3배다. replica는 다른 노드에서 **병렬로** 색인하기 때문이다.
- 재색인 시간은 Job 전체 시간이다. 즉 API 호출부터 alias 교체까지이며, 전체 동기화 lock을 잡고 있는 시간과 같다.

## 측정 환경

| 항목 | 값 |
|---|---|
| 호스트 | Apple M1 (8코어), RAM 16GB, macOS 15.6, Docker 27.5.1 (Docker에 CPU 8개, 메모리 8GB 할당) |
| ES 클러스터 | 8.12.0, **3노드 (es01 ~ es03), 모두 master + data 역할** (ingest 제외), 노드당 CPU 2개, 메모리 1.8GB, 힙 1GB, nori 설치 |
| 인덱스 | 샤드 1, **replica 2**, nori, `dynamic: strict` |
| MySQL | 8.0.46, CPU 2개, 메모리 1.5GB, `products` 100만 건 |
| 앱 | multicast-server, JVM 17, `-Xmx1g`, chunk 10,000, ES `socket-timeout: 3s` |

한계: 노드 3개가 노트북 한 대에서 CPU와 디스크를 나눠 쓴다. 실제로 서버가 분리된 환경이라면 replica 노드끼리 서로 방해하지 않으므로, 증가 폭이 더 작을 수 있다.

## 비교 대상

| | A. 기준 | B. 해결책 |
|---|---|---|
| 인덱스 생성 | replica 2, refresh -1 | replica 2로 생성 → **즉시 replica 0**, refresh -1 |
| 색인 | primary + replica 2개가 각자 색인 | primary에만 색인 |
| 색인 후 | refresh 복구 → alias 교체 | **forcemerge(max_num_segments=1)** → **replica 2** (세그먼트 파일 복사) → green 대기 → refresh 복구 → alias 교체 |

## 결과 1 — 재색인 시간 (Job 전체, alias 교체 포함)

| | 1회 | 2회 | 3회 | **중앙값** |
|---|---:|---:|---:|---:|
| A. 기준 (replica 2로 색인) | 87.2 | 77.2 | 80.3 | **80.3초** |
| B. 해결책 | 61.2 | 68.9 | 54.9 | **61.2초** |

- A는 같은 조건으로 1세트(3회)를 먼저 측정했다.
  - 그중 1회는 bulk 응답이 3초를 넘겨 `SocketTimeoutException`으로 **Job이 실패**했다.
  - 나머지 2회는 78.1초, 66.6초였다.
  - replica 2로 색인하면 bulk 1회 지연이 최대 2.9초까지 늘어나 앱의 소켓 타임아웃(3초)에 걸릴 수 있다. 해결책(B)에서는 bulk 1회 최대 지연이 1.3초였다.
- B 단계별 소요 (중앙값): forcemerge 4.1초, replica 복구 4.2초, refresh 0.1초

## 결과 2 — ES 색인 시간만 (bulk `took` 합계)

| | ES 색인 시간 | bulk 1회 p50 | bulk 1회 max |
|---|---:|---:|---:|
| A. replica 2로 색인 | **49.3초** | 376ms | 2,422ms |
| B. primary에만 색인 | **21.6초** | 153ms | 1,308ms |

- replica 2개 때문에 늘어난 ES 색인 시간은 약 27.7초다.
- B에서 같은 replica 2개를 만드는 데 걸린 시간은 forcemerge 4.1초와 replica 복구 4.2초를 합쳐 약 8.3초다.
  - replica 복구는 문서를 다시 색인하지 않는다. 병합된 세그먼트 파일을 그대로 복사한다.
  - 그래서 문서를 다시 분석하는 비용(nori 형태소 분석 포함)이 들지 않는다.

## 결과 3 — 배치 Step 구간별 분해 (B, Step 50.2초)

Spring Batch Micrometer 지표(`spring_batch_*_seconds_sum`)와 bulk `took`으로 나눴다.

| 구간 | 시간 | 비중 |
|---|---:|---:|
| ES 서버 색인 (`took`) | 21.3초 | 42% |
| 앱 쪽 bulk 처리 (write − took: JSON 직렬화, HTTP 전송, 응답 1만 건 파싱) | 18.7초 | 37% |
| MySQL 읽기 | 8.6초 | 17% |
| 커밋, Batch 메타데이터 | 1.3초 | 3% |
| Processor | 0.3초 | 1% |

replica 수를 바꾸면 이 중 ES 서버 색인 구간만 달라진다. 그래서 ES 색인 시간이 2.3배가 되어도 Job 전체 시간은 약 1.3배로만 늘어난다.

## 결과 4 — 무중단 검증

- 조건:
  - B 흐름으로 재색인을 1회 돌렸다(61.5초).
  - 배치 시작 5초 전부터 alias 교체 5초 뒤까지, k6로 `products` alias에 초당 50건씩 검색을 보냈다.
- 결과: 3,579건 중 **100% 성공**했다. 판정 기준은 HTTP 200이고 검색 결과가 1건 이상인 것이다. 응답 시간은 p95 16.7ms, max 383ms였다.
- 색인하는 동안 검색은 alias가 가리키는 기존 인덱스(replica 2)가 계속 처리했다.

## 구현 중 발견한 문제와 조치

1. **forcemerge가 소켓 타임아웃(3초)보다 오래 걸린다**
   - 증상: `wait_for_completion=false`로 task를 만든 뒤 폴링하는 방식으로 구현했다. 그런데 고수준 client(8.13)가 이 요청의 응답을 디코딩하지 못하고 실패했다(`Missing required property 'ForcemergeResponse.shards'`).
   - 조치: forcemerge만 low-level `RestClient`로 호출하도록 바꿨다.
2. **`cluster.health(wait_for_status=green)`이 타임아웃되면 408 예외가 난다**
   - 조치: 기다리는 대신 상태를 폴링하는 방식으로 바꿨다.
3. **설정 복구가 실패해도 alias가 교체되던 구조**
   - 증상: 위 1번 문제로 복구가 실패했는데도 `SyncJobListener`가 replica 0, refresh 미실행 상태(검색 결과 0건)의 인덱스로 alias를 교체했다.
   - 조치: 복구에 실패하면 alias를 교체하지 않도록 고쳤다. `SyncJobListenerTest`를 추가했다.

## 원본 데이터

- 결과 위치: `docs/experiments/results-replica/` 아래
  - `baseline/`: A 기준
  - `optimized/`: B 해결책
  - `optimized-k6/`: 무중단 검증
  - `metrics_run.prom`: 구간 분해용 지표
- `invalid_*` 폴더는 위 문제로 무효 처리한 실행이다.
  - `invalid_forcemerge_decode/`, `invalid_health_408/`: 구현 버그로 실패한 실행
  - `invalid_k6_empty_base/`: 기존 인덱스가 비어 있던 상태에서 돌린 k6 측정
