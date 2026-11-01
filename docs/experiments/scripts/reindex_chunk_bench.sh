#!/bin/bash
# mysqlToEsJob(100만 건 재색인)을 chunk 크기별로 실행해 소요 시간을 비교한다.
# 사전 조건: docs/experiments/reindex-chunk-size.md 의 환경(rx-mysql:3317, rx-es:9220) + multicast-server bootJar 빌드
# 사용: reindex_chunk_bench.sh "100 500 1000 5000" 3 [k6]
set -euo pipefail

SIZES=${1:-"100 500 1000 5000"}
REPEAT=${2:-3}
WITH_K6=${3:-}
ES=http://localhost:9220
APP=http://localhost:8082
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
JAR=${JAR:-$(ls "$ROOT"/multicast-server/build/libs/multicast-server-*-SNAPSHOT.jar | grep -v plain)}
OUT=${OUT:-$ROOT/docs/experiments/results}
mkdir -p "$OUT"
NEW_INDEX="products_$(date +%Y_%m_%d)"   # SyncBatchHelper 와 같은 규칙
RESULT="$OUT/reindex_chunk_$(date +%H%M%S).csv"
echo "chunk,run,wall_sec,step_sec,doc_count,status" > "$RESULT"

alias_to_base() {   # alias 를 products_base 로 되돌리고 이번 회차 인덱스 삭제
  curl -s -XPOST "$ES/_aliases" -H 'Content-Type: application/json' -d "{\"actions\":[
    {\"remove\":{\"index\":\"*\",\"alias\":\"products\"}},
    {\"add\":{\"index\":\"products_base\",\"alias\":\"products\",\"is_write_index\":true}}]}" > /dev/null
  curl -s -XDELETE "$ES/$NEW_INDEX" > /dev/null || true
}

start_app() {
  java -Xmx1g -jar "$JAR" --sync.batch.chunk-size="$1" --spring.profiles.active=bench \
    --spring.elasticsearch.uris=$ES \
    --spring.datasource.url="jdbc:mysql://localhost:3317/matcha?rewriteBatchedStatements=true" \
    --spring.datasource.username=root --spring.datasource.password=root \
    > "$OUT/app_chunk_$1.log" 2>&1 &
  APP_PID=$!
  until curl -s -o /dev/null "$APP/actuator/prometheus"; do sleep 1; done
}

for size in $SIZES; do
  start_app "$size"
  for run in $(seq 1 "$REPEAT"); do
    alias_to_base
    if [ -n "$WITH_K6" ]; then
      k6 run --quiet --summary-export "$OUT/k6_chunk_${size}_run${run}.json" \
        "$ROOT/docs/experiments/scripts/k6-search-during-reindex.js" > "$OUT/k6_chunk_${size}_run${run}.log" 2>&1 &
      K6_PID=$!
      sleep 5   # 배치 시작 전 baseline 검색 구간
    fi
    start=$(python3 -c 'import time;print(time.time())')
    curl -s -XPOST "$APP/api/internal/sync/mysql-to-es" > /dev/null   # JobLauncher 동기 실행 → alias 교체까지 끝나야 응답
    wall=$(python3 -c "import time;print(round(time.time()-$start,1))")
    [ -n "$WITH_K6" ] && { sleep 5; kill -INT $K6_PID; wait $K6_PID || true; }   # 교체 후 5초 더 검색
    row=$(docker exec rx-mysql mysql -uroot -proot matcha -N -e "SELECT TIMESTAMPDIFF(MICROSECOND,START_TIME,END_TIME)/1e6, STATUS FROM BATCH_STEP_EXECUTION ORDER BY STEP_EXECUTION_ID DESC LIMIT 1" 2>/dev/null)
    count=$(curl -s "$ES/products/_count" | python3 -c 'import sys,json;print(json.load(sys.stdin)["count"])')
    echo "$size,$run,$wall,$(echo "$row" | awk '{print $1","}')$count,$(echo "$row" | awk '{print $2}')" | tee -a "$RESULT"
  done
  kill "$APP_PID"; wait "$APP_PID" 2>/dev/null || true
done
alias_to_base
echo "결과: $RESULT"
