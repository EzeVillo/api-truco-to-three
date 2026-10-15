import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { once } from 'node:events';
import { Miniflare, convertV4MiniflareOptions } from 'miniflare';
import WebSocket, { WebSocketServer } from 'ws';

const worker = await readFile(new URL('./worker.mjs', import.meta.url), 'utf8');
const config = JSON.parse(await readFile(new URL('./wrangler.jsonc', import.meta.url), 'utf8'));
// Valor exclusivo de fixtures: nunca se consulta una clave de producción.
const fixtureKey = 'gateway-fixture-not-used-in-production';
const origin = 'https://ezevillo.com';
const connected = 'CONNECTED\nversion:1.2\n\n\0';

async function gateway(t, address) {
  const backend = `http://${address}`;
  const mf = new Miniflare(convertV4MiniflareOptions({
    modules: true,
    // Sustituye únicamente el destino fijo por el origen efímero del test.
    script: worker.replaceAll(config.vars.BACKEND_ORIGIN, backend),
    // Fecha máxima que soporta el workerd incluido en la versión fijada del SDK.
    compatibilityDate: '2026-10-13',
    bindings: { ...config.vars, BACKEND_ORIGIN: backend, GATEWAY_SECRET: fixtureKey },
    ratelimits: Object.fromEntries(config.ratelimits.map(({ name, ...value }) => [name, value])),
  }));
  t.after(() => mf.dispose());
  const url = new URL('/ws', await mf.ready);
  url.protocol = 'ws:';
  return url;
}

async function connect(t, url, token = 'fixture', direct = false) {
  const headers = { Origin: origin, 'CF-Connecting-IP': '203.0.113.7' };
  if (direct) headers['X-Truco-Gateway-Secret'] = fixtureKey;
  const socket = new WebSocket(url, ['v12.stomp', 'v11.stomp', 'v10.stomp'], { headers });
  t.after(() => socket.terminate());
  await once(socket, 'open', { signal: AbortSignal.timeout(8000) });
  const reply = once(socket, 'message', { signal: AbortSignal.timeout(8000) });
  socket.send(`CONNECT\naccept-version:1.2,1.1,1.0\nAuthorization:Bearer ${token}\nheart-beat:0,0\n\n\0`);
  const [frame] = await reply;
  assert.equal(frame.toString().split('\n')[0], 'CONNECTED');
  return socket;
}

test('El gateway transmite CONNECT, CONNECTED, SUBSCRIBE y MESSAGE con compresión', { timeout: 20000 }, async t => {
  const server = new WebSocketServer({ port: 0, host: '127.0.0.1', perMessageDeflate: true });
  await once(server, 'listening');
  t.after(() => { for (const socket of server.clients) socket.terminate(); server.close(); });
  const commands = [];
  server.on('connection', (socket, request) => {
    assert.equal(request.headers['x-truco-gateway-secret'], fixtureKey);
    socket.on('message', data => {
      const command = data.toString().split('\n')[0];
      commands.push(command);
      socket.send(command === 'CONNECT' ? connected
        : 'MESSAGE\nsubscription:match\ndestination:/user/queue/match\n\n{"eventType":"fixture"}\0');
    });
  });
  const socket = await connect(t, await gateway(t, `127.0.0.1:${server.address().port}`));
  const message = once(socket, 'message', { signal: AbortSignal.timeout(8000) });
  socket.send('SUBSCRIBE\nid:match\ndestination:/user/queue/match\n\n\0');
  assert.match((await message)[0].toString(), /^MESSAGE\n/);
  assert.deepEqual(commands, ['CONNECT', 'SUBSCRIBE']);
});

for (const throughGateway of [false, true]) {
  test(`El binario nativo completa STOMP ${throughGateway ? 'a través de Cloudflare' : 'directamente'}`,
    { skip: !process.env.TRUCO_SMOKE_URL, timeout: 20000 }, async t => {
      const backend = new URL(process.env.TRUCO_SMOKE_URL);
      assert.equal(backend.hostname, '127.0.0.1', 'El smoke sólo acepta un backend local de CI');
      const response = await fetch(new URL('/api/auth/guest', backend), {
        method: 'POST', headers: { Origin: origin, 'X-Truco-Gateway-Secret': fixtureKey,
          'Content-Type': 'application/json' }, body: '{}',
      });
      assert.equal(response.status, 200, 'El fixture debe poder emitir un token de invitado');
      const { accessToken } = await response.json();
      assert.equal(typeof accessToken, 'string');
      const url = throughGateway ? await gateway(t, backend.host) : new URL('/ws', backend);
      url.protocol = 'ws:';
      await connect(t, url, accessToken, !throughGateway);
    });
}
