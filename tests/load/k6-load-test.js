import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const authLatency = new Trend('auth_latency');
const simulationLatency = new Trend('simulation_latency');
const mlLatency = new Trend('ml_latency');

export const options = {
  stages: [
    { duration: '1m', target: 50 },    // Ramp up
    { duration: '3m', target: 200 },   // Load
    { duration: '5m', target: 500 },   // Peak
    { duration: '2m', target: 200 },   // Scale down
    { duration: '1m', target: 0 },     // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000', 'p(99)<5000'],
    errors: ['rate<0.05'],
    auth_latency: ['p(95)<500'],
    simulation_latency: ['p(95)<3000'],
    ml_latency: ['p(95)<1000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    email: 'loadtest@rheosim.com',
    password: 'loadtest123',
  }), { headers: { 'Content-Type': 'application/json' } });

  return { token: loginRes.json('accessToken') };
}

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`,
  };

  group('Authentication', () => {
    const start = Date.now();
    const res = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
      email: 'loadtest@rheosim.com',
      password: 'loadtest123',
    }), { headers: { 'Content-Type': 'application/json' } });

    authLatency.add(Date.now() - start);
    check(res, { 'auth success': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
  });

  group('Project CRUD', () => {
    const res = http.get(`${BASE_URL}/api/v1/projects`, { headers });
    check(res, { 'projects listed': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
  });

  group('ML Prediction', () => {
    const start = Date.now();
    const timePoints = Array.from({ length: 100 }, (_, i) => Math.pow(10, -2 + i * 0.05));
    const values = timePoints.map(t => 1e6 * Math.exp(-t / 10));

    const res = http.post(`${BASE_URL}/api/v1/ml/predict`, JSON.stringify({
      datasetId: '00000000-0000-0000-0000-000000000001',
      timePoints,
      values,
      experimentType: 'relaxation',
    }), { headers });

    mlLatency.add(Date.now() - start);
    check(res, { 'ml prediction': (r) => r.status === 200 || r.status === 503 });
    errorRate.add(res.status >= 500 && res.status !== 503);
  });

  group('Marketplace', () => {
    const res = http.get(`${BASE_URL}/api/v1/marketplace/plugins?page=0&size=10`, { headers });
    check(res, { 'marketplace listed': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
  });

  sleep(1);
}

export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'load-test-results.json': JSON.stringify(data),
  };
}

function textSummary(data, opts) {
  return JSON.stringify(data.metrics, null, 2);
}
