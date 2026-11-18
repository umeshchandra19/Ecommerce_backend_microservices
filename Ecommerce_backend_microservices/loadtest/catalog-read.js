// k6 load test for the cached read path:  GET /api/products/{id}
//
//   RPS=1000 DURATION=60s k6 run loadtest/catalog-read.js
//
// Run it twice and compare (see README "Load testing"):
//   A) CACHE_TYPE=none  RATE_LIMIT_RPM=100000000 docker compose up -d --force-recreate catalog-service
//   B) CACHE_TYPE=redis RATE_LIMIT_RPM=100000000 docker compose up -d --force-recreate catalog-service
import http from 'k6/http';
import { check } from 'k6';

const AUTH = __ENV.AUTH_URL || 'http://localhost:8081';
const CATALOG = __ENV.CATALOG_URL || 'http://localhost:8082';
const PRODUCTS = Number(__ENV.PRODUCTS || 200);   // ids 1..PRODUCTS exist (SEED_PRODUCTS)

export const options = {
  scenarios: {
    reads: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RPS || 1000),
      timeUnit: '1s',
      duration: __ENV.DURATION || '60s',
      preAllocatedVUs: 200,
      maxVUs: 2000,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
  const email = `load${Date.now()}@example.com`;
  const body = JSON.stringify({ email, password: 'load-test-123' });
  const h = { headers: { 'Content-Type': 'application/json' } };
  http.post(`${AUTH}/api/auth/register`, body, h);
  const res = http.post(`${AUTH}/api/auth/login`, body, h);
  return { token: res.json('token') };
}

export default function (data) {
  const id = 1 + Math.floor(Math.random() * PRODUCTS);
  const res = http.get(`${CATALOG}/api/products/${id}`, { headers: { Authorization: `Bearer ${data.token}` } });
  check(res, { 'status is 200': (r) => r.status === 200 });
}
