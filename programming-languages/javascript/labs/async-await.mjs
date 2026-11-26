// Run from programming-languages/javascript/labs with: node async-await.mjs
import assert from 'node:assert/strict';
import { setTimeout as delay } from 'node:timers/promises'; // delay(ms, value, { signal })

// Level 1: an async function always returns a promise.
async function answer() {
  return 42;
}

async function broken() {
  throw new Error('inside async');
}

// Level 2: await pauses the function, not the program.
async function pauseAndResume(log) {
  log.push('before await');
  await null; // even a plain value: the rest waits for a microtask
  log.push('after await');
}

// Level 3: three pretend network calls taking 30, 20 and 10 ms.
async function sequentialTotal() {
  const a = await delay(30, 5);
  const b = await delay(20, 7); // starts only after a has arrived
  const c = await delay(10, 9);
  return a + b + c;
}

async function parallelTotal() {
  const [a, b, c] = await Promise.all([delay(30, 5), delay(20, 7), delay(10, 9)]);
  return a + b + c;
}

// Level 4: try/catch works with await, but only for awaited promises.
async function loadOrDefault(load) {
  try {
    return await load(); // awaited here, so a rejection lands in this catch
  } catch {
    return 'default';
  }
}

async function loadOrDefaultBroken(load) {
  try {
    return load(); // returned without await: the catch never sees the rejection
  } catch {
    return 'default';
  }
}

const failingLoad = () => Promise.reject(new Error('offline'));

// Level 5: loops.
function forEachDoesNotWait(ids, seen) {
  ids.forEach(async id => {
    await delay(10);
    seen.push(id);
  }); // forEach ignores the promises the callback returns
}

async function* countdown(from) {
  for (let n = from; n > 0; n--) {
    await delay(5);
    yield n;
  }
}

async function collect(source) {
  const values = [];
  for await (const value of source) values.push(value);
  return values;
}

// Level 6: cancellation with an AbortSignal.
async function download(signal) {
  await delay(1000, undefined, { signal }); // rejects as soon as signal aborts
  return 'downloaded';
}

// Common mistake: start early, await late, and one of them fails.
async function startEarlyAwaitLate() {
  const slow = delay(30, 5);
  const failing = delay(10).then(() => { throw new Error('b failed'); });
  return (await slow) + (await failing); // failing rejects while we wait for slow
}

const elapsedSince = start => performance.now() - start;

// Level 1
const promise = answer();
assert.ok(promise instanceof Promise);
assert.equal(await promise, 42);
const rejected = broken(); // no exception escapes the call itself
await assert.rejects(rejected, { message: 'inside async' });

// Level 2
const log = [];
const resumed = pauseAndResume(log);
log.push('caller continues');
await resumed;
assert.deepEqual(log, ['before await', 'caller continues', 'after await']);

// Level 3
let start = performance.now();
assert.equal(await sequentialTotal(), 21);
const sequentialMs = elapsedSince(start);
start = performance.now();
assert.equal(await parallelTotal(), 21);
const parallelMs = elapsedSince(start);
assert.ok(sequentialMs >= 55, `sequential took ${sequentialMs} ms, about 30 + 20 + 10`);
assert.ok(parallelMs >= 25, `parallel took ${parallelMs} ms, about max(30, 20, 10)`);
assert.ok(parallelMs < sequentialMs, 'parallel beats sequential');

// Level 4
assert.equal(await loadOrDefault(failingLoad), 'default');
await assert.rejects(loadOrDefaultBroken(failingLoad), { message: 'offline' });

// Level 5
const seen = [];
forEachDoesNotWait([1, 2, 3], seen);
assert.deepEqual(seen, []); // nothing has finished yet
await delay(40);
assert.deepEqual(seen.toSorted(), [1, 2, 3]);
assert.deepEqual(await collect(countdown(3)), [3, 2, 1]);
assert.deepEqual(await Array.fromAsync(countdown(3)), [3, 2, 1]);

// Level 6
const controller = new AbortController();
const pending = download(controller.signal);
controller.abort();
await assert.rejects(pending, { name: 'AbortError', message: 'The operation was aborted' });
assert.equal(controller.signal.reason.name, 'AbortError');
const timeout = AbortSignal.timeout(20);
await assert.rejects(download(timeout), { name: 'AbortError' });
assert.equal(timeout.reason.name, 'TimeoutError');
assert.equal(timeout.reason.message, 'The operation was aborted due to timeout');
const either = AbortSignal.any([new AbortController().signal, AbortSignal.timeout(10)]);
await assert.rejects(download(either), { name: 'AbortError' });

// Common mistake: the late-awaited rejection is reported as unhandled first.
const events = [];
const onUnhandled = reason => events.push(`unhandled ${reason.message}`);
const onHandled = () => events.push('handled later');
process.on('unhandledRejection', onUnhandled);
process.on('rejectionHandled', onHandled);
await assert.rejects(startEarlyAwaitLate(), { message: 'b failed' });
await delay(10);
process.off('unhandledRejection', onUnhandled);
process.off('rejectionHandled', onHandled);
assert.deepEqual(events, ['unhandled b failed', 'handled later']);

console.log('OK');
