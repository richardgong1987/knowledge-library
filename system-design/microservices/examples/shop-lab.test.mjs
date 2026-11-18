import test from 'node:test';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { setTimeout as delay } from 'node:timers/promises';
import { fileURLToPath } from 'node:url';

test('shop lab serves resources, places an order, and times out safely', { timeout: 15000 }, async t => {
  const child = spawn(process.execPath, [fileURLToPath(new URL('./shop-lab.mjs', import.meta.url))], {
    stdio: 'pipe'
  });
  let logs = '';
  child.stdout.on('data', chunk => { logs += chunk.toString(); });
  child.stderr.on('data', chunk => { logs += chunk.toString(); });
  t.after(() => { if (!child.killed) child.kill('SIGTERM'); });

  let ready = false;
  for (let i = 0; i < 100; i++) {
    if (child.exitCode !== null) throw new Error('Shop lab exited: ' + logs);
    try {
      const response = await fetch('http://127.0.0.1:4100/health', {
        signal: AbortSignal.timeout(250)
      });
      if (response.status === 200) { ready = true; break; }
    } catch { /* service still starting */ }
    await delay(50);
  }
  assert.ok(ready, 'Gateway did not start: ' + logs);

  const product = await fetch('http://127.0.0.1:4100/api/products/7');
  assert.equal(product.status, 200);
  assert.equal((await product.json()).price, 100);

  const user = await fetch('http://127.0.0.1:4100/api/users/9');
  assert.equal(user.status, 200);
  assert.equal((await user.json()).name, 'Ada');

  const created = await fetch('http://127.0.0.1:4100/api/orders?userId=9&productId=7', {
    method: 'POST'
  });
  assert.equal(created.status, 201);
  const order = await created.json();
  assert.equal(order.userId, 9);
  assert.equal(order.productId, 7);
  assert.equal(order.total, 100);
  assert.ok(order.traceId);

  const failed = await fetch('http://127.0.0.1:4100/api/orders?userId=9&productId=7&slow=1', {
    method: 'POST'
  });
  assert.equal(failed.status, 503);
  assert.equal((await failed.json()).error, 'Dependency unavailable');

  const orders = await fetch('http://127.0.0.1:4100/api/orders');
  assert.equal(orders.status, 200);
  assert.equal((await orders.json()).length, 1);
});
