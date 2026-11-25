import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { priceOrder, createHandler, InvalidOrderError } from './server.mjs';

test('adds up the price of every cup', () => {
  assert.deepEqual(priceOrder({ classic: 2, pink: 1 }), { cups: 3, totalCents: 800 });
});

test('gives 10% off from the tenth cup', () => {
  assert.deepEqual(priceOrder({ classic: 6, mint: 4 }), { cups: 10, totalCents: 2610 });
});

test('rejects unknown drinks and bad quantities', () => {
  assert.throws(() => priceOrder({ cola: 1 }), InvalidOrderError);
  assert.throws(() => priceOrder({ classic: 0 }), InvalidOrderError);
});

test('serves health, prices and 404 over HTTP', async (t) => {
  const server = createServer(createHandler('test-1'));
  await new Promise((resolve) => server.listen(0, resolve));
  t.after(() => server.close());
  const base = `http://127.0.0.1:${server.address().port}`;

  assert.deepEqual(await (await fetch(`${base}/health`)).json(), { status: 'ok', version: 'test-1' });
  assert.equal((await (await fetch(`${base}/price?classic=2&pink=1`)).json()).totalCents, 800);
  assert.equal((await fetch(`${base}/price?cola=1`)).status, 400);
  assert.equal((await fetch(`${base}/secret`)).status, 404);
});
