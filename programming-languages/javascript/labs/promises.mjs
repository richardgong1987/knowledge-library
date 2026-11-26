// Run from programming-languages/javascript/labs with: node promises.mjs
import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import { inspect } from 'node:util';

const delay = (ms, value) => new Promise(resolve => setTimeout(resolve, ms, value));
const fail = (ms, message) => new Promise((_, reject) => setTimeout(reject, ms, new Error(message)));

// Level 1: pending, then fulfilled or rejected, exactly once.
function settleTwice() {
  return new Promise((resolve, reject) => {
    resolve('first');
    resolve('second'); // ignored: already settled
    reject(new Error('too late')); // ignored too
  });
}

// Level 2: then returns a new promise, so calls chain.
function chain() {
  return Promise.resolve(2)
    .then(x => x * 3) // a plain value: the next promise fulfils with 6
    .then(x => delay(10, x + 1)) // a promise: the chain waits for it, then gets 7
    .then(x => `total ${x}`);
}

const thenable = { then(resolve) { resolve(42); } }; // not a Promise, but has then()

// Level 3: a rejection skips fulfilment handlers until a catch.
function errorPath(log) {
  return Promise.resolve(1)
    .then(() => { throw new Error('boom'); })
    .then(() => log.push('skipped')) // not called: the promise is rejected
    .catch(error => { log.push(`caught ${error.message}`); return 'recovered'; })
    .finally(() => log.push('finally')); // gets no value and passes 'recovered' on
}

// Level 5: withResolvers and try.
function nextMessage(emitter) {
  const { promise, resolve } = Promise.withResolvers();
  emitter.once('message', resolve);
  return promise;
}

function parsePort(text) {
  const port = Number(text);
  if (!Number.isInteger(port)) throw new TypeError(`not a port: ${text}`);
  return port;
}

// Underneath: adopting a returned promise costs two extra microtasks.
function tickPuzzle(log) {
  Promise.resolve()
    .then(() => { log.push(0); return Promise.resolve(4); })
    .then(x => log.push(x));
  Promise.resolve()
    .then(() => log.push(1))
    .then(() => log.push(2))
    .then(() => log.push(3))
    .then(() => log.push(5))
    .then(() => log.push(6));
}

function tickPuzzlePlain(log) {
  Promise.resolve()
    .then(() => { log.push(0); return 4; }) // a plain value: no extra jobs
    .then(x => log.push(x));
  Promise.resolve()
    .then(() => log.push(1))
    .then(() => log.push(2))
    .then(() => log.push(3))
    .then(() => log.push(5))
    .then(() => log.push(6));
}

// Interview: a timeout around any promise.
function withTimeout(promise, ms) {
  let timer;
  const timeout = new Promise((_, reject) => {
    timer = setTimeout(() => reject(new Error(`timed out after ${ms} ms`)), ms);
  });
  return Promise.race([promise, timeout]).finally(() => clearTimeout(timer));
}

const message = error => error.message;
const elapsedSince = start => performance.now() - start;

// Level 1
assert.equal(await settleTwice(), 'first');
const later = delay(10, 42);
assert.equal(inspect(later), 'Promise { <pending> }');
await later;
assert.equal(inspect(later), 'Promise { 42 }');

// Level 2
assert.equal(await chain(), 'total 7');
assert.equal(await Promise.resolve(thenable), 42);
const p = Promise.resolve(1);
assert.notEqual(p.then(x => x), p); // a new promise every time

// Level 3
const log = [];
assert.equal(await errorPath(log), 'recovered');
assert.deepEqual(log, ['caught boom', 'finally']);
await assert.rejects(new Promise(() => { throw new Error('in executor'); }), { message: 'in executor' });

// Level 4: inputs settle at 30, 10 and 20 ms.
let start = performance.now();
assert.deepEqual(await Promise.all([delay(30, 'a'), delay(10, 'b'), delay(20, 'c')]), ['a', 'b', 'c']);
assert.ok(elapsedSince(start) >= 25, 'all waits for the slowest');
await assert.rejects(Promise.all([delay(30, 'a'), fail(10, 'b failed'), delay(20, 'c')]), { message: 'b failed' });
const settled = await Promise.allSettled([delay(30, 'a'), fail(10, 'b failed'), delay(20, 'c')]);
assert.deepEqual(settled.map(result => result.status), ['fulfilled', 'rejected', 'fulfilled']);
assert.equal(settled[1].reason.message, 'b failed');
assert.equal(await Promise.race([delay(30, 'a'), delay(10, 'b'), delay(20, 'c')]), 'b');
await assert.rejects(Promise.race([delay(30, 'a'), fail(10, 'b failed'), delay(20, 'c')]), { message: 'b failed' });
assert.equal(await Promise.any([delay(30, 'a'), fail(10, 'b failed'), delay(20, 'c')]), 'c');
start = performance.now();
const none = await Promise.any([fail(30, 'a failed'), fail(10, 'b failed')]).catch(error => error);
assert.ok(elapsedSince(start) >= 25, 'any waits for the last rejection');
assert.ok(none instanceof AggregateError);
assert.equal(none.message, 'All promises were rejected');
assert.deepEqual(none.errors.map(message), ['a failed', 'b failed']); // input order
assert.deepEqual(await Promise.all([]), []);
assert.equal(inspect(Promise.race([])), 'Promise { <pending> }'); // never settles

// Level 5
const emitter = new EventEmitter();
const next = nextMessage(emitter);
emitter.emit('message', 'hello');
assert.equal(await next, 'hello');
assert.equal(await Promise.try(parsePort, '80'), 80);
await assert.rejects(Promise.try(parsePort, 'eighty'), { name: 'TypeError', message: 'not a port: eighty' });
const calls = [];
Promise.try(() => calls.push('called now'));
assert.deepEqual(calls, ['called now']); // the function runs synchronously

// Level 6: an unhandled rejection reaches the process.
const unhandled = Promise.withResolvers();
process.once('unhandledRejection', reason => unhandled.resolve(reason.message));
Promise.reject(new Error('nobody listens'));
assert.equal(await unhandled.promise, 'nobody listens');

// Underneath
const ticks = [];
tickPuzzle(ticks);
await delay(10);
assert.deepEqual(ticks, [0, 1, 2, 3, 4, 5, 6]);
const plainTicks = [];
tickPuzzlePlain(plainTicks);
await delay(10);
assert.deepEqual(plainTicks, [0, 1, 4, 2, 3, 5, 6]);

// Interview
assert.equal(await withTimeout(delay(5, 'fast'), 100), 'fast');
await assert.rejects(withTimeout(delay(50, 'slow'), 10), { message: 'timed out after 10 ms' });

console.log('OK');
