import test from 'node:test';
import assert from 'node:assert/strict';
import { randomBytes } from 'node:crypto';
import { handleRequest } from './worker.mjs';

const origin = 'https://ezevillo.com';
const names = ['API_LIMITER', 'LOGIN_LIMITER', 'REGISTER_LIMITER', 'GUEST_LIMITER',
  'REFRESH_LIMITER', 'CONNECTION_LIMITER'];
function fixture(t) {
  const calls = [];
  const env = {
    BACKEND_ORIGIN: 'https://api-truco-to-three.onrender.com',
    ALLOWED_ORIGINS: `${origin},capacitor://localhost`,
    GATEWAY_SECRET: randomBytes(32).toString('hex'),
  };
  for (const name of names) env[name] = {
    async limit({ key }) { calls.push({ name, key }); return { success: true }; },
  };
  const fetchMock = t.mock.method(globalThis, 'fetch', async () => new Response(null, { status: 204 }));
  function request(path, options = {}) {
    const headers = new Headers({ Origin: origin, 'CF-Connecting-IP': '203.0.113.7' });
    for (const [name, value] of new Headers(options.headers)) headers.set(name, value);
    return new Request(`https://truco-api.example${path}`, { ...options, headers });
  }
  return { env, calls, fetchMock, request };
}

test('Reenvía cuerpo, JWT y query sin exponer la clave ni confiar en XFF', async t => {
  const { env, request, calls, fetchMock } = fixture(t);
  const response = await handleRequest(request('/api/auth/login?source=web', {
    method: 'POST', body: JSON.stringify({ username: 'persona' }),
    headers: { Authorization: 'Bearer opaque', 'X-Forwarded-For': '198.51.100.1',
      'X-Truco-Gateway-Secret': 'caller-controlled' },
  }), env);
  assert.equal(response.status, 204);
  const [upstream, options] = fetchMock.mock.calls[0].arguments;
  assert.equal(upstream.url, `${env.BACKEND_ORIGIN}/api/auth/login?source=web`);
  assert.equal(upstream.headers.get('Authorization'), 'Bearer opaque');
  assert.equal(upstream.headers.get('X-Truco-Gateway-Secret'), env.GATEWAY_SECRET);
  assert.equal(upstream.headers.get('X-Forwarded-For'), null);
  assert.equal(upstream.redirect, 'manual');
  assert.equal(await upstream.text(), JSON.stringify({ username: 'persona' }));
  assert.equal(options.cf.cacheTtl, 0);
  assert.deepEqual(calls.map(value => value.key), ['203.0.113.7', '203.0.113.7']);
  assert.equal(response.headers.get('Access-Control-Allow-Origin'), origin);
  assert.equal(response.headers.get('Cache-Control'), 'no-store');
  assert.equal(response.headers.get('X-Truco-Gateway-Secret'), null);
});

for (const [path, binding] of [
  ['/api/auth/login', 'LOGIN_LIMITER'], ['/api/auth/login/', 'LOGIN_LIMITER'],
  ['/api/auth/%6cogin', 'LOGIN_LIMITER'], ['/api//auth/login', 'LOGIN_LIMITER'],
  ['/api/auth/register', 'REGISTER_LIMITER'], ['/api/auth/guest', 'GUEST_LIMITER'],
  ['/api/auth/refresh', 'REFRESH_LIMITER'],
]) {
  test(`Bloquea ${path} con la política correspondiente y Retry-After`, async t => {
    const { env, request, fetchMock } = fixture(t);
    env[binding].limit = async () => ({ success: false });
    const response = await handleRequest(request(path, { method: 'POST' }), env);
    assert.equal(response.status, 429);
    assert.equal(response.headers.get('Retry-After'), '60');
    assert.equal(response.headers.get('Access-Control-Allow-Origin'), origin);
    assert.match(response.headers.get('Access-Control-Expose-Headers'), /Retry-After/);
    const body = await response.json();
    assert.equal(body.errorCode, 'RateLimitExceededException');
    assert.ok(Number.isFinite(Date.parse(body.timestamp)));
    assert.ok(body.requestId);
    assert.equal(fetchMock.mock.callCount(), 0);
  });
}

test('La cuota general limita la API y no se evade con un XFF inventado', async t => {
  const { env, request, fetchMock } = fixture(t);
  let key;
  env.API_LIMITER.limit = async input => { key = input.key; return { success: false }; };
  const result = await handleRequest(request('/api/matches', {
    headers: { 'X-Forwarded-For': '198.51.100.2' },
  }), env);
  assert.equal(result.status, 429);
  assert.equal(key, '203.0.113.7');
  assert.equal(fetchMock.mock.callCount(), 0);
});

test('Nunca expone la clave privada aunque el origen la incluya en un header de respuesta', async t => {
  const { env, request, fetchMock } = fixture(t);
  fetchMock.mock.mockImplementation(async () => new Response(null, {
    status: 204, headers: { 'X-Truco-Gateway-Secret': env.GATEWAY_SECRET },
  }));
  const result = await handleRequest(request('/api/public/wake'), env);
  assert.equal(result.headers.get('X-Truco-Gateway-Secret'), null);
});

test('El preflight permitido no consume cuota ni contacta a Render', async t => {
  const { env, request, calls, fetchMock } = fixture(t);
  const response = await handleRequest(request('/api/auth/login', {
    method: 'OPTIONS', headers: { 'Access-Control-Request-Method': 'POST',
      'Access-Control-Request-Headers': 'authorization,content-type,x-request-id' },
  }), env);
  assert.equal(response.status, 204);
  assert.equal(response.headers.get('Access-Control-Allow-Origin'), origin);
  assert.equal(calls.length, 0);
  assert.equal(fetchMock.mock.callCount(), 0);
});

test('Rechaza orígenes ajenos y no autoriza el header privado desde el navegador', async t => {
  const { env, request, fetchMock } = fixture(t);
  const forbidden = await handleRequest(request('/api/auth/login', {
    headers: { Origin: 'https://ezevillo.com.attacker.example' },
  }), env);
  assert.equal(forbidden.status, 403);
  assert.equal(forbidden.headers.get('Access-Control-Allow-Origin'), null);
  const preflight = await handleRequest(request('/api/auth/login', {
    method: 'OPTIONS', headers: { 'Access-Control-Request-Method': 'POST',
      'Access-Control-Request-Headers': 'X-Truco-Gateway-Secret' },
  }), env);
  assert.equal(preflight.status, 403);
  assert.equal(fetchMock.mock.callCount(), 0);
});

test('Preserva exactamente el upgrade WebSocket y aplica su cuota de conexión', async t => {
  const { env, request, calls, fetchMock } = fixture(t);
  const upgrade = { status: 101, webSocket: {} };
  fetchMock.mock.mockImplementation(async () => upgrade);
  const result = await handleRequest(request('/ws', { headers: { Upgrade: 'websocket' } }), env);
  assert.equal(result, upgrade);
  assert.deepEqual(calls.map(value => value.name), ['API_LIMITER', 'CONNECTION_LIMITER']);
  assert.equal(fetchMock.mock.calls[0].arguments[0].headers.get('Upgrade'), 'websocket');
});

test('SockJS limita aperturas sin tratar cada recepción XHR como una conexión nueva', async t => {
  const { env, request, fetchMock } = fixture(t);
  env.CONNECTION_LIMITER.limit = async () => ({ success: false });
  const info = await handleRequest(request('/ws-sockjs/info?t=1'), env);
  assert.equal(info.status, 429);
  const xhr = await handleRequest(request('/ws-sockjs/1/session/xhr', { method: 'POST' }), env);
  assert.equal(xhr.status, 204);
  assert.equal(fetchMock.mock.callCount(), 1);
});

for (const missing of ['GATEWAY_SECRET', 'API_LIMITER', 'LOGIN_LIMITER']) {
  test(`Falla cerrado si falta ${missing}`, async t => {
    const { env, request, fetchMock } = fixture(t);
    delete env[missing];
    const result = await handleRequest(request('/api/auth/login', { method: 'POST' }), env);
    assert.equal(result.status, 503);
    assert.equal(fetchMock.mock.callCount(), 0);
  });
}

test('Rechaza paths ajenos, metadatos de IP ausentes y errores sin filtrar detalles', async t => {
  const { env, request, fetchMock } = fixture(t);
  assert.equal((await handleRequest(request('/actuator/metrics'), env)).status, 404);
  const noIp = request('/api/auth/login');
  noIp.headers.delete('CF-Connecting-IP');
  assert.equal((await handleRequest(noIp, env)).status, 503);
  assert.equal(fetchMock.mock.callCount(), 0);
  fetchMock.mock.mockImplementation(async () => { throw new Error(env.GATEWAY_SECRET); });
  const failed = await handleRequest(request('/api/auth/login', { method: 'POST' }), env);
  assert.equal(failed.status, 503);
  assert.equal((await failed.text()).includes(env.GATEWAY_SECRET), false);
});
