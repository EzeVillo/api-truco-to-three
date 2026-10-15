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
      Forwarded: 'host=attacker.example;proto=http', 'X-Forwarded-Host': 'attacker.example',
      'X-Truco-Gateway-Secret': 'caller-controlled' },
  }), env);
  assert.equal(response.status, 204);
  const [upstream, options] = fetchMock.mock.calls[0].arguments;
  assert.equal(upstream.url, `${env.BACKEND_ORIGIN}/api/auth/login?source=web`);
  assert.equal(upstream.headers.get('Authorization'), 'Bearer opaque');
  assert.equal(upstream.headers.get('X-Truco-Gateway-Secret'), env.GATEWAY_SECRET);
  assert.equal(upstream.headers.get('X-Forwarded-For'), null);
  assert.equal(upstream.headers.get('Forwarded'), null);
  assert.equal(upstream.headers.get('X-Forwarded-Host'), null);
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

test('Rechaza doble codificación y parámetros de path para impedir bypass de políticas', async t => {
  const { env, request, fetchMock } = fixture(t);
  for (const path of ['/api/auth/%256cogin', '/api/auth/login;param=value']) {
    assert.equal((await handleRequest(request(path, { method: 'POST' }), env)).status, 400);
  }
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

test('Activa ambos sockets, conserva STOMP y aplica la cuota sólo a la apertura', async t => {
  const { env, request, calls, fetchMock } = fixture(t);
  class Socket extends EventTarget {
    readyState = 1;
    sent = [];
    closed = [];
    accept(options) { this.accepted = options; }
    send(data) { this.sent.push(data); }
    close(code, reason) { this.readyState = 3; this.closed.push({ code, reason }); }
  }
  const upstream = new Socket();
  const downstream = new Socket();
  const client = {};
  globalThis.WebSocketPair = class { constructor() { this[0] = client; this[1] = downstream; } };
  t.after(() => { delete globalThis.WebSocketPair; });
  t.mock.method(globalThis, 'Response', function (_body, init) { return { ...init }; });
  const upgrade = { status: 101, webSocket: upstream, headers: new Headers({
    'Sec-WebSocket-Protocol': 'v12.stomp', 'Sec-WebSocket-Extensions': 'permessage-deflate',
    'Sec-WebSocket-Accept': 'upstream-key', 'X-Truco-Gateway-Secret': env.GATEWAY_SECRET,
  }) };
  fetchMock.mock.mockImplementation(async () => upgrade);
  const result = await handleRequest(request('/ws', { headers: { Upgrade: 'websocket' } }), env);
  assert.equal(result.status, 101);
  assert.equal(result.webSocket, client);
  assert.deepEqual(upstream.accepted, { allowHalfOpen: true });
  assert.deepEqual(downstream.accepted, { allowHalfOpen: true });
  assert.equal(upstream.binaryType, 'arraybuffer');
  assert.equal(result.headers.get('Sec-WebSocket-Protocol'), 'v12.stomp');
  for (const header of ['Sec-WebSocket-Extensions', 'Sec-WebSocket-Accept', 'X-Truco-Gateway-Secret']) {
    assert.equal(result.headers.get(header), null);
  }
  downstream.dispatchEvent(new MessageEvent('message', { data: 'CONNECT\n\n\0' }));
  upstream.dispatchEvent(new MessageEvent('message', { data: 'CONNECTED\n\n\0' }));
  const binary = new Uint8Array([0, 1, 2]).buffer;
  upstream.dispatchEvent(new MessageEvent('message', { data: binary }));
  assert.deepEqual(upstream.sent, ['CONNECT\n\n\0']);
  assert.deepEqual(downstream.sent, ['CONNECTED\n\n\0', binary]);
  assert.deepEqual(calls.map(value => value.name), ['API_LIMITER', 'CONNECTION_LIMITER']);
  assert.equal(fetchMock.mock.calls[0].arguments[0].headers.get('Upgrade'), 'websocket');
  const closed = new Event('close');
  Object.assign(closed, { code: 1006, reason: '' });
  upstream.dispatchEvent(closed);
  assert.equal(downstream.closed[0].code, 1011);
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
