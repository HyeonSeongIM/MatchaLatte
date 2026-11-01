// 재색인 배치 + alias 교체 동안 products alias 로 검색을 계속 보내 성공률을 측정한다.
// 쿼리는 core-elasticsearch SearchService.searchProducts 와 동일 (match name, size 10).
// 성공 = HTTP 200 && hits > 0 (빈 인덱스로 교체되는 것도 장애로 본다)
import http from 'k6/http';
import { check } from 'k6';

const ES = __ENV.ES || 'http://localhost:9220';
const WORDS = ['말차', '라떼', '녹차', '우유', '쿠키', '케이크', '텀블러', '원두'];

export const options = {
  scenarios: {
    search: { executor: 'constant-arrival-rate', rate: 50, timeUnit: '1s', duration: '60m', preAllocatedVUs: 20, maxVUs: 100 },
  },
  thresholds: { checks: ['rate==1.0'] },
};

export default function () {
  const q = WORDS[Math.floor(Math.random() * WORDS.length)];
  const res = http.post(`${ES}/products/_search`,
    JSON.stringify({ size: 10, query: { match: { name: q } } }),
    { headers: { 'Content-Type': 'application/json' }, timeout: '5s' });
  check(res, {
    'status 200': (r) => r.status === 200,
    'hits > 0': (r) => r.status === 200 && r.json('hits.total.value') > 0,
  });
}
