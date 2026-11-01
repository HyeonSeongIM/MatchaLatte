import http from 'k6/http';
import { check } from 'k6';

// 사용법: k6 run -e TARGET_URL=http://localhost:18080/sync-immediate docs/experiments/scripts/k6-sync-refresh.js
export const options = {
  scenarios: {
    steady: {
      executor: 'constant-vus',
      vus: 20,
      duration: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

const TARGET_URL = __ENV.TARGET_URL || 'http://localhost:18080/sync-none';

export default function () {
  const res = http.post(TARGET_URL);
  check(res, { 'status is 200': (r) => r.status === 200 });
}
