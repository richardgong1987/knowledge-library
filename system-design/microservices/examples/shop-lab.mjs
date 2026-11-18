// Original teaching lab. Node.js 22+; no npm dependencies.
import http from 'node:http';
import { EventEmitter } from 'node:events';
import { randomUUID } from 'node:crypto';
const events = new EventEmitter();
const productInstances = ['http://127.0.0.1:4101', 'http://127.0.0.1:4104'];
let nextProduct = 0, nextOrder = 1;
const orders = [];
function json(res, status, payload) {
  res.writeHead(status, { 'content-type': 'application/json; charset=utf-8' });
  res.end(JSON.stringify(payload));
}
function listen(port, name, handler) {
  const server = http.createServer(async (req, res) => {
    try {
      const url = new URL(req.url, 'http://127.0.0.1:' + port);
      if (url.pathname === '/health') return json(res, 200, { service: name, status: 'ok' });
      await handler(req, res, url);
    } catch (error) {
      json(res, 500, { error: error.message });
    }
  });
  server.listen(port, '127.0.0.1', () => console.log(name + ': http://127.0.0.1:' + port));
  return server;
}
function productService(port) {
  return listen(port, 'product-' + port, async (_req, res, url) => {
    if (url.pathname !== '/products/7') return json(res, 404, { error: 'Unknown product' });
    if (url.searchParams.get('slow') === '1') {
      await new Promise(resolve => setTimeout(resolve, 800));
    }
    json(res, 200, { id: 7, name: 'Bond Primer', price: 100, instance: port });
  });
}
function userService() {
  return listen(4102, 'user', (_req, res, url) => {
    if (url.pathname !== '/users/9') return json(res, 404, { error: 'Unknown user' });
    json(res, 200, { id: 9, name: 'Ada' });
  });
}
async function fetchJSON(url, { timeout = 350, ...options } = {}) {
  const response = await fetch(url, { ...options, signal: AbortSignal.timeout(timeout) });
  if (!response.ok) throw new Error('HTTP ' + response.status + ' from ' + url);
  return response.json();
}
function orderService() {
  events.on('order.placed', e =>
    console.log('[event] order.placed order=' + e.orderId + ' product=' + e.productId));
  return listen(4103, 'order', async (req, res, url) => {
    if (req.method === 'GET' && url.pathname === '/orders') return json(res, 200, orders);
    if (req.method !== 'POST' || url.pathname !== '/orders') return json(res, 404, { error: 'Route not found' });
    const userId = Number(url.searchParams.get('userId'));
    const productId = Number(url.searchParams.get('productId'));
    if (userId !== 9 || productId !== 7) return json(res, 400, { error: 'Use userId=9&productId=7' });
    const instance = productInstances[nextProduct++ % productInstances.length];
    const slow = url.searchParams.get('slow') === '1' ? '?slow=1' : '';
    const traceId = req.headers['x-trace-id'] || randomUUID();
    try {
      const [user, product] = await Promise.all([
        fetchJSON('http://127.0.0.1:4102/users/9'),
        fetchJSON(instance + '/products/7' + slow)
      ]);
      const order = { id: nextOrder++, userId: user.id, productId: product.id, total: product.price, traceId };
      orders.push(order);
      events.emit('order.placed', { orderId: order.id, productId });
      json(res, 201, order);
    } catch (error) {
      json(res, 503, { error: 'Dependency unavailable', detail: error.message, traceId });
    }
  });
}
function gateway() {
  return listen(4100, 'gateway', async (req, res, url) => {
    const route = [
      ['/api/orders', 'http://127.0.0.1:4103/orders'],
      ['/api/products', 'http://127.0.0.1:4101/products'],
      ['/api/users', 'http://127.0.0.1:4102/users']
    ].find(([prefix]) => url.pathname === prefix || url.pathname.startsWith(prefix + '/'));
    if (!route) return json(res, 404, { error: 'Unknown gateway route' });
    const [prefix, target] = route;
    const destination = target + url.pathname.slice(prefix.length) + url.search;
    const traceId = req.headers['x-trace-id'] || randomUUID();
    try {
      const response = await fetch(destination, {
        method: req.method,
        headers: { 'x-trace-id': traceId },
        signal: AbortSignal.timeout(1200)
      });
      res.writeHead(response.status, {
        'content-type': response.headers.get('content-type') || 'application/json',
        'x-trace-id': traceId
      });
      res.end(Buffer.from(await response.arrayBuffer()));
    } catch (error) {
      json(res, 502, { error: 'Gateway upstream failed', detail: error.message, traceId });
    }
  });
}
productService(4101);
productService(4104);
userService();
orderService();
gateway();
