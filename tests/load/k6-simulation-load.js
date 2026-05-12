import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const simulationDuration = new Trend('simulation_duration');

export const options = {
  stages: [
    { duration: '2m', target: 20 },   // Ramp up to 20 users
    { duration: '5m', target: 50 },   // Ramp up to 50 users
    { duration: '10m', target: 100 }, // Sustained 100 users
    { duration: '3m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],  // P95 < 2s
    errors: ['rate<0.05'],              // Error rate < 5%
    simulation_duration: ['p(95)<600000'], // Simulation P95 < 10 min
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';

let authToken = '';

export function setup() {
  const loginRes = http.post(`${BASE_URL}/v1/auth/login`, JSON.stringify({
    email: 'loadtest@rheosim.com',
    password: 'LoadTest123!'
  }), { headers: { 'Content-Type': 'application/json' } });

  return { token: loginRes.json('accessToken') };
}

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`,
  };

  group('API Health', () => {
    const healthRes = http.get(`${BASE_URL}/actuator/health`);
    check(healthRes, { 'health endpoint ok': (r) => r.status === 200 });
  });

  group('Projects CRUD', () => {
    const listRes = http.get(`${BASE_URL}/v1/projects`, { headers });
    check(listRes, { 'projects list ok': (r) => r.status === 200 });
    errorRate.add(listRes.status !== 200);
  });

  group('Simulation Submit', () => {
    const simPayload = JSON.stringify({
      projectId: 'test-project-id',
      modelType: 'MAXWELL',
      fitTarget: 'RELAXATION_MODULUS',
      initialParameters: [1000.0, 10.0],
      lowerBounds: [100.0, 0.1],
      upperBounds: [10000.0, 1000.0]
    });

    const submitRes = http.post(`${BASE_URL}/v1/simulations`, simPayload, { headers });
    check(submitRes, { 'simulation submitted': (r) => r.status === 201 || r.status === 202 });
    errorRate.add(submitRes.status >= 400);

    if (submitRes.status === 201 || submitRes.status === 202) {
      const simId = submitRes.json('id');
      let status = 'RUNNING';
      const start = Date.now();

      while (status === 'RUNNING' && Date.now() - start < 60000) {
        sleep(2);
        const statusRes = http.get(`${BASE_URL}/v1/simulations/${simId}`, { headers });
        if (statusRes.status === 200) {
          status = statusRes.json('status');
        }
      }

      simulationDuration.add(Date.now() - start);
    }
  });

  group('Dataset Operations', () => {
    const datasetsRes = http.get(`${BASE_URL}/v1/datasets`, { headers });
    check(datasetsRes, { 'datasets list ok': (r) => r.status === 200 });
    errorRate.add(datasetsRes.status !== 200);
  });

  sleep(1);
}
