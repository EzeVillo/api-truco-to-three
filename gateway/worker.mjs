const SECRET_HEADER = 'X-Truco-Gateway-Secret';
const METHODS = new Set(['GET', 'HEAD', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS']);
const REQUEST_HEADERS = new Set(['authorization', 'content-type', 'x-request-id', 'x-requested-with']);

function corsHeaders(origin) {
  const headers = new Headers({
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff',
    Vary: 'Origin',
    'Access-Control-Expose-Headers': 'Retry-After, X-Request-Id',
  });
  if (origin) headers.set('Access-Control-Allow-Origin', origin);
  return headers;
}

function errorResponse(status, errorCode, origin, requestId, retryAfter) {
  const headers = corsHeaders(origin);
  headers.set('Content-Type', 'application/json');
  headers.set('X-Request-Id', requestId);
  if (retryAfter) headers.set('Retry-After', String(retryAfter));
  return new Response(JSON.stringify({
    errorCode,
    message: status === 429 ? 'Too many requests' : 'Request unavailable',
    timestamp: new Date().toISOString(),
    requestId,
  }), { status, headers });
}

function routeLimiter(request, pathname, env) {
  if (request.method === 'POST') {
    const limits = {
      '/api/auth/login': 'LOGIN_LIMITER',
      '/api/auth/register': 'REGISTER_LIMITER',
      '/api/auth/guest': 'GUEST_LIMITER',
      '/api/auth/refresh': 'REFRESH_LIMITER',
    };
    if (limits[pathname]) return limits[pathname];
  }
  if (request.headers.get('Upgrade')?.toLowerCase() === 'websocket'
      || pathname === '/ws-sockjs/info') return 'CONNECTION_LIMITER';
  return null;
}

function relayWebSocket(response, requestId) {
  const upstream = response.webSocket;
  if (!upstream) throw new Error('Missing upstream WebSocket');
  const [client, downstream] = Object.values(new WebSocketPair());
  upstream.binaryType = 'arraybuffer';
  downstream.binaryType = 'arraybuffer';

  const close = (socket, code, reason) => {
    if (socket.readyState === 3) return;
    try { socket.close(code, reason); } catch { /* El otro extremo ya puede estar cerrado. */ }
  };
  const fail = () => {
    close(upstream, 1011, 'Connection unavailable');
    close(downstream, 1011, 'Connection unavailable');
  };
  for (const [source, target] of [[upstream, downstream], [downstream, upstream]]) {
    source.addEventListener('message', event => {
      try { target.send(event.data); } catch { fail(); }
    });
    source.addEventListener('close', event => {
      const valid = event.code === 1000 || (event.code >= 1001 && event.code <= 1014
        && ![1004, 1005, 1006].includes(event.code)) || (event.code >= 3000 && event.code <= 4999);
      const code = valid ? event.code : 1011;
      const reason = valid ? event.reason : 'Connection unavailable';
      close(target, code, reason);
      close(source, code, reason);
    });
    source.addEventListener('error', fail);
  }
  // Aceptar ambos extremos activa la entrega de frames tras el upgrade HTTP.
  upstream.accept({ allowHalfOpen: true });
  downstream.accept({ allowHalfOpen: true });
  const headers = new Headers({ 'Cache-Control': 'no-store', 'X-Request-Id': requestId });
  const protocol = response.headers.get('Sec-WebSocket-Protocol');
  if (protocol) headers.set('Sec-WebSocket-Protocol', protocol);
  // La compresión y las claves del handshake se negocian por separado en cada tramo.
  return new Response(null, { status: 101, webSocket: client, headers });
}

export async function handleRequest(request, env) {
  const requestId = crypto.randomUUID();
  const url = new URL(request.url);
  const origin = request.headers.get('Origin');
  const allowed = new Set(env.ALLOWED_ORIGINS.split(',').map(value => value.trim()));
  if (origin && !allowed.has(origin)) {
    return errorResponse(403, 'OriginNotAllowedException', null, requestId);
  }
  const path = url.pathname;
  if (!(path.startsWith('/api/') || path === '/ws' || path.startsWith('/ws/')
      || path === '/ws-sockjs' || path.startsWith('/ws-sockjs/'))) {
    return errorResponse(404, 'ResourceNotFoundException', origin, requestId);
  }
  if (!METHODS.has(request.method)) {
    return errorResponse(405, 'MethodNotAllowedException', origin, requestId);
  }
  if (request.method === 'OPTIONS') {
    const method = request.headers.get('Access-Control-Request-Method');
    const requestedHeaders = (request.headers.get('Access-Control-Request-Headers') ?? '')
      .split(',').map(value => value.trim().toLowerCase()).filter(Boolean);
    if (!origin || !METHODS.has(method) || method === 'OPTIONS'
        || requestedHeaders.some(header => !REQUEST_HEADERS.has(header))) {
      return errorResponse(403, 'CorsRequestRejectedException', origin, requestId);
    }
    const headers = corsHeaders(origin);
    headers.set('Access-Control-Allow-Methods', 'GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS');
    headers.set('Access-Control-Allow-Headers', 'Authorization, Content-Type, X-Request-Id, X-Requested-With');
    headers.set('Access-Control-Max-Age', '600');
    return new Response(null, { status: 204, headers });
  }
  if (!env.GATEWAY_SECRET || env.GATEWAY_SECRET.length < 32) {
    return errorResponse(503, 'GatewayUnavailableException', origin, requestId);
  }
  // Cloudflare overwrites this header at the public ingress. Never use caller-supplied XFF.
  const clientIp = request.headers.get('CF-Connecting-IP');
  if (!clientIp) return errorResponse(503, 'GatewayUnavailableException', origin, requestId);
  try {
    let policyPath;
    try {
      policyPath = decodeURIComponent(path).replace(/\/+/g, '/').replace(/\/+$/, '');
      if (/[;%\\\0]/.test(policyPath)) throw new Error('Ambiguous path');
    } catch {
      return errorResponse(400, 'InvalidRequestException', origin, requestId);
    }
    const policy = routeLimiter(request, policyPath, env);
    const limiters = policy ? [env.API_LIMITER, env[policy]] : [env.API_LIMITER];
    for (const limiter of limiters) {
      if (!(await limiter.limit({ key: clientIp })).success) {
        return errorResponse(429, 'RateLimitExceededException', origin, requestId, 60);
      }
    }
    const backend = new URL(env.BACKEND_ORIGIN);
    if (backend.origin !== 'https://api-truco-to-three.onrender.com') {
      return errorResponse(503, 'GatewayUnavailableException', origin, requestId);
    }
    const upstream = new URL(path + url.search, backend);
    const headers = new Headers(request.headers);
    for (const header of ['Host', 'Forwarded', 'X-Forwarded-For', 'X-Forwarded-Host',
      'X-Forwarded-Proto', 'X-Forwarded-Port', 'X-Forwarded-Prefix', 'X-Real-IP']) {
      headers.delete(header);
    }
    headers.set(SECRET_HEADER, env.GATEWAY_SECRET);
    headers.set('X-Request-Id', requestId);
    const forwarded = new Request(new Request(upstream, request), { headers, redirect: 'manual' });
    const response = await fetch(forwarded, { cf: { cacheTtl: 0, cacheEverything: false } });
    if (response.status === 101) return relayWebSocket(response, requestId);
    const result = new Response(response.body, response);
    for (const [name, value] of corsHeaders(origin)) result.headers.set(name, value);
    if (!origin) result.headers.delete('Access-Control-Allow-Origin');
    result.headers.delete('Access-Control-Allow-Credentials');
    result.headers.delete(SECRET_HEADER);
    return result;
  } catch {
    return errorResponse(503, 'GatewayUnavailableException', origin, requestId);
  }
}

export default { fetch: handleRequest };
