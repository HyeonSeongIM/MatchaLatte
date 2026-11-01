# ES 동기 저장이 느렸던 이유 — refresh 대기 실험

## 배경 / 가설

상품 생성 로직에서 MySQL 저장 후 ES 저장까지 **동기적으로** 기다렸다면 왜 느렸을까?

**가설:** ES 색인이 refresh를 호출(또는 대기)하기 때문에 느려졌다.

지금은 `ProductEventer` → `@Async` 이벤트 리스너 → multicast-server 큐잉/배치로 ES 저장을
요청 스레드에서 완전히 분리해뒀다. 이 실험은 "만약 그 분리 없이 요청 스레드 안에서
MySQL insert 다음에 ES 색인까지 동기로 기다렸다면" 어떤 지연이 발생했을지, 그 메커니즘을
실제 ES로 재현해서 확인한다.

## 실험 환경

- 로컬 단일 노드 Elasticsearch 8.12.0 (Docker, 운영 클러스터와 완전히 분리된 독립 환경)
- 인덱스 `refresh_interval`은 건드리지 않음 → ES 기본값 1s (운영 배치 재색인 때만 쓰는
  `refresh_interval=-1` 최적화와 무관한 "평상시" 상태를 재현)
- 단건 색인 API(`POST /{index}/_doc/{id}?refresh=...`)의 응답 시간을 `curl -w "%{time_total}"`로 측정
- 스크립트: [`scripts/es_refresh_bench.sh`](./scripts/es_refresh_bench.sh)

## 결과 1 — 유휴 상태 인덱스, 40회 반복

| refresh 옵션 | avg | p50 | p95 | max |
|---|---|---|---|---|
| `false` (기본값, 현재 bulk 색인과 동일) | 11.6ms | 5.4ms | 13.0ms | 240ms (첫 요청 워밍업) |
| `wait_for` | 984.7ms | 995.3ms | 1010.9ms | 1013.1ms |
| `true` | 11.8ms | 11.0ms | 15.1ms | 29.0ms |

## 결과 2 — 동시 쓰기 부하 상황(백그라운드 8 workers 동시 색인), 25회 반복

| refresh 옵션 | avg | p50 | max |
|---|---|---|---|
| `true` (부하중) | 48.7ms | 46.7ms | 68.6ms |
| `wait_for` (부하중) | 989.6ms | 1001.7ms | 1040.8ms |

## 왜 이런 차이가 나는가

ES는 문서를 색인해도 즉시 검색 가능해지는 게 아니라, 내부 메모리 버퍼(in-memory buffer)에
쌓아두고 `refresh_interval`(기본 1s)마다 한 번씩 새 Lucene 세그먼트를 만들어 커밋해야
검색에 반영된다.

- **`refresh=false` (기본값):** 이 refresh를 기다리지 않고 write 확인만 받은 뒤 즉시 응답한다.
  매우 빠름 — 지금 `SyncItemWriter`/`SyncScheduleHelper`의 bulk 색인이 쓰는 방식과 동일.
- **`refresh=wait_for`:** "다음 refresh 주기가 돌 때까지" 응답을 미룬다. 요청이 직전 refresh
  시점과 무관하게 도착하므로, 평균적으로 `refresh_interval`(1s)의 거의 전체를 기다리게 된다.
  → **동기 저장 로직이 "ES에 저장한 게 검색에 바로 반영됐는지" 보장하려고 `wait_for`(또는
  라이브러리의 동등 옵션)를 썼다면, 상품 생성 API 한 번당 최대 ~1초가 그대로 얹힌다.**
  이게 "동기일 때 느렸다"는 현상(수백ms ~ 1초대 지연)과 가장 잘 맞아떨어지는 설명이다.
- **`refresh=true`:** 대기 없이 즉시 강제로 refresh를 수행한다. 유휴 상태에서는 세그먼트가
  작아 강제 refresh 비용 자체가 작아서(11ms) 상대적으로 안 느려 보이지만, 동시 쓰기가 몰리는
  상황에서는 비용이 4배 이상(11ms → 49ms)으로 늘어난다. 세그먼트 수가 많아지고 merge 압박이
  커질수록 이 비용은 계속 증가하는 구조라, 실제 트래픽 규모에서는 격차가 더 벌어질 수 있다.

## 결론

- "동기 저장이 느렸다"는 현상은 refresh 연산 자체의 비용보다는, **ES가 검색 가능해지는
  시점까지 클라이언트를 붙잡아두는 refresh 정책(특히 `wait_for`)을 저장 경로에 직접 얹었기
  때문**일 가능성이 가장 크다.
- `wait_for` 방식이었다면 `refresh_interval`(1s) 자체가 매 요청의 하한 지연시간이 되어버리는
  구조라서, 트래픽이 줄어도 사라지지 않는 고정 비용이었을 것이다.
- 지금처럼 이벤트를 비동기로 분리하고, 실제 색인은 `refresh=false` 기본값으로(배치 재색인은
  `refresh_interval=-1`까지 최적화) 처리하면 이 대기 시간이 상품 생성 API 응답 경로에서
  완전히 빠지게 된다 — 현재 구조가 올바른 방향이라는 근거가 된다.

## 재현 방법

```bash
docker run -d --name es-experiment -p 9210:9200 \
  -e "discovery.type=single-node" -e "xpack.security.enabled=false" \
  docker.elastic.co/elasticsearch/elasticsearch:8.12.0

bash docs/experiments/scripts/es_refresh_bench.sh

docker rm -f es-experiment
```
