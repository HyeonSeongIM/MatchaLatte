# 원인 분리 실험 — RefreshPolicy.NONE으로 동기 버전 재실행

[es-sync-refresh-latency.md](./es-sync-refresh-latency.md)에서 ES 자체의 refresh 옵션별
지연을 확인했다면, 이 실험은 실제 애플리케이션 코드(스프링 부트 + JPA + Spring Data
Elasticsearch)를 통해 "동기 저장 로직에서 refresh만 껐을 때 얼마나 빨라지는가"를 확인해서
병목이 정확히 refresh인지, 동기 호출 자체인지, 아니면 제3의 원인인지 분리한다.

## 배경

이 레포 히스토리에는 `RefreshPolicy`를 쓴 실제 커밋이나 k6 스크립트가 없었다(전체 브랜치
기준 확인 완료). 따라서 "동기 버전"을 실제 커밋에서 체크아웃하는 대신, 운영 코드와 동일한
스택(Spring Boot 3.3.4 + Spring Data JPA + Spring Data Elasticsearch)으로 그 시절의 구현을
재구성한 독립 하네스(`docs/experiments/sync-harness/`)를 새로 만들어 실험했다.

## 실험 설계

- MySQL insert(JPA `save()`) 후 ES 색인까지 **요청 스레드에서 순차 대기**하는 동일한 컨트롤러
  로직에서, `ElasticsearchOperations.withRefreshPolicy(...)`만 다르게 설정한 엔드포인트 2개:
  - `POST /sync-immediate` — `RefreshPolicy.IMMEDIATE` (매 요청마다 강제 refresh, 가설상
    "느렸던 동기 버전"과 동일 조건)
  - `POST /sync-none` — `RefreshPolicy.NONE` (refresh만 끔, 나머지 동기 흐름은 동일)
- 로컬 격리 환경: MySQL 8.0(Docker), Elasticsearch 8.12.0(Docker, refresh_interval 기본값 1s)
- 부하: k6 `constant-vus`, VU 20, 30초 (스크립트: [`scripts/k6-sync-refresh.js`](./scripts/k6-sync-refresh.js))

## 결과

| 엔드포인트 | RefreshPolicy | avg | p90 | p95 | max | 처리량 |
|---|---|---|---|---|---|---|
| `/sync-immediate` | IMMEDIATE | 103.3ms | 137.7ms | 166.5ms | 406.9ms | 193 req/s |
| `/sync-none` | NONE | 13.3ms | 16.5ms | 19.1ms | 76.1ms | 1500.8 req/s |

**refresh만 끄니 평균 응답시간이 약 7.8배(103ms → 13ms) 빨라지고, 처리량은 약 7.8배
(193 → 1501 req/s) 늘었다.** 같은 동기 흐름(요청 스레드에서 MySQL → ES 순차 대기)인데도
refresh 유무만으로 이 정도 차이가 난다는 것은, 동기 호출 자체의 오버헤드(스레드 점유,
커넥션 등)보다 **"요청마다 강제 refresh를 기다리는 것"이 지배적인 병목**이었다는 뜻이다.

## 3가지 분류 중 판정

이미지의 분류 기준에 따르면:

> **refresh 끄니까 동기인데도 크게 빨라짐 → 병목은 "요청당 refresh"**

이 결과가 해당한다. 동기 호출 구조(요청 스레드 점유) 자체도 처리량 상한을 만들긴 하지만,
같은 동기 구조에서 refresh만 뺐을 때 7.8배 개선된 것을 보면 지연의 대부분은 refresh 대기가
차지하고 있었다.

## 이력서 문장에 반영할 근거

- "요청마다 refresh가 발생해 지연이 누적되는 것을 원인으로 확인" — RefreshPolicy만 바꾼
  대조 실험으로 검증(동일 동기 흐름에서 avg 103ms → 13ms, 약 7.8배)
- 이 결과는 비동기 전환(이벤트 발행 + 배치 bulk 색인, refresh=false 기본값 사용)이
  올바른 해결 방향이었다는 근거를 보강한다 — 실제로 현재 구조는 이 실험의 `/sync-none`과
  동일한 "refresh를 기다리지 않는" 조건에 더해, ES 호출 자체를 요청 스레드 밖으로 뺐다.

## 재현 방법

```bash
# 1. 로컬 MySQL / ES 컨테이너
docker run -d --name mysql-experiment -e MYSQL_ROOT_PASSWORD=experiment \
  -e MYSQL_DATABASE=sync_harness -p 3310:3306 mysql:8.0 --default-authentication-plugin=mysql_native_password
docker run -d --name es-experiment -p 9210:9200 \
  -e "discovery.type=single-node" -e "xpack.security.enabled=false" \
  docker.elastic.co/elasticsearch/elasticsearch:8.12.0

# 2. settings.gradle에 일시적으로 모듈 등록
#    include 'docs:experiments:sync-harness'  <- 추가

# 3. 하네스 실행
./gradlew :docs:experiments:sync-harness:bootRun

# 4. k6 실행 (각각)
k6 run -e TARGET_URL=http://localhost:18080/sync-immediate docs/experiments/scripts/k6-sync-refresh.js
k6 run -e TARGET_URL=http://localhost:18080/sync-none docs/experiments/scripts/k6-sync-refresh.js
# 비동기(운영 ProductEventer/@Async 재현) 버전과 비교하려면:
k6 run -e TARGET_URL=http://localhost:18080/async docs/experiments/scripts/k6-sync-refresh.js

# 5. 정리
docker rm -f mysql-experiment es-experiment
#    settings.gradle에서 추가한 줄 제거
```
