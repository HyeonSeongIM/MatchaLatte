#!/bin/bash
# ES 단건 색인 API의 refresh 옵션별 응답시간을 비교하는 실험 스크립트.
# 사전 조건: localhost:9210 에 Elasticsearch가 떠 있어야 함 (docs/experiments/es-sync-refresh-latency.md 참고)
set -euo pipefail

ES="http://localhost:9210"
INDEX="products_experiment"
N=40
TMP_DIR=$(mktemp -d)

curl -s -X PUT "$ES/$INDEX" -H "Content-Type: application/json" -d '{
  "mappings": {
    "properties": {
      "id": {"type": "long"},
      "name": {"type": "text"},
      "description": {"type": "text"},
      "price": {"type": "long"},
      "userId": {"type": "long"}
    }
  }
}' > /dev/null

run_mode() {
  local refresh_param="$1"
  local times_file="$2"
  local count="$3"
  : > "$times_file"
  for i in $(seq 1 "$count"); do
    id=$((RANDOM * RANDOM))
    body="{\"id\":$id,\"name\":\"상품_$i\",\"description\":\"설명\",\"price\":10000,\"userId\":1}"
    t=$(curl -s -o /dev/null -w "%{time_total}" -X POST \
      "$ES/$INDEX/_doc/$id?refresh=$refresh_param" \
      -H "Content-Type: application/json" -d "$body")
    echo "$t" >> "$times_file"
  done
}

echo "=== [1] refresh=false (기본값, 현재 bulk 색인 방식과 동일) ==="
run_mode "false" "$TMP_DIR/refresh_false.txt" "$N"

echo "=== [2] refresh=wait_for (다음 refresh 주기까지 대기) ==="
run_mode "wait_for" "$TMP_DIR/refresh_waitfor.txt" "$N"

echo "=== [3] refresh=true (매 요청마다 즉시 강제 refresh) ==="
run_mode "true" "$TMP_DIR/refresh_true.txt" "$N"

echo ""
echo "=== 동시 쓰기 부하(8 workers) 상황에서 true / wait_for 재측정 ==="
for w in $(seq 1 8); do
  ( for i in $(seq 1 400); do
      id=$((RANDOM * RANDOM))
      curl -s -o /dev/null -X POST "$ES/$INDEX/_doc/$id?refresh=false" \
        -H "Content-Type: application/json" \
        -d "{\"id\":$id,\"name\":\"부하_${w}_${i}\",\"description\":\"lorem ipsum dolor sit amet\",\"price\":10000,\"userId\":1}"
    done ) &
done
sleep 1
run_mode "true" "$TMP_DIR/refresh_true_load.txt" 25
run_mode "wait_for" "$TMP_DIR/refresh_waitfor_load.txt" 25
wait

python3 - "$TMP_DIR" << 'EOF'
import statistics, sys, pathlib
d = pathlib.Path(sys.argv[1])
files = [
    ("refresh=false", d / "refresh_false.txt"),
    ("refresh=wait_for", d / "refresh_waitfor.txt"),
    ("refresh=true", d / "refresh_true.txt"),
    ("refresh=true (부하중)", d / "refresh_true_load.txt"),
    ("refresh=wait_for (부하중)", d / "refresh_waitfor_load.txt"),
]
print("\n=== 결과 요약 (응답 시간, ms) ===")
for label, path in files:
    vals = [float(l.strip()) * 1000 for l in path.open() if l.strip()]
    vals_sorted = sorted(vals)
    n = len(vals_sorted)
    p50 = vals_sorted[int(n * 0.5)]
    print(f"{label:24s} n={n:3d} avg={statistics.mean(vals):7.1f}ms  p50={p50:7.1f}ms  max={max(vals):7.1f}ms")
EOF
