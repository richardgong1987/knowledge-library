// Run from programming-languages/javascript/labs with: node the-event-loop.mjs
import assert from 'node:assert/strict';
import { readFile } from 'node:fs';

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

// Level 1: one call stack, and every piece of code runs to completion.
function runToCompletion(order) {
  setTimeout(() => order.push('timeout'), 0);
  order.push('sync 1');
  order.push('sync 2'); // the timer cannot interrupt this function
}

// Level 2: setTimeout(fn, 0) waits until the call stack is empty.
function blockFor(ms) {
  const end = performance.now() + ms;
  while (performance.now() < end) {} // busy: nothing else can run
}

function timerAfterBusyStack(busyMs) {
  return new Promise(resolve => {
    const scheduled = performance.now();
    setTimeout(() => resolve(performance.now() - scheduled), 0);
    blockFor(busyMs);
  });
}

// Level 3: microtasks run before the next task.
function classicPuzzle(log) {
  log.push('script start');
  setTimeout(() => log.push('setTimeout'), 0);
  Promise.resolve()
    .then(() => log.push('promise1'))
    .then(() => log.push('promise2'));
  log.push('script end');
}

// Level 4: the microtask queue is drained completely, even as it grows.
function microtasksBeforeNextTask(count) {
  return new Promise(resolve => {
    let ran = 0;
    setTimeout(() => resolve(ran), 0);
    function step() {
      ran += 1;
      if (ran < count) queueMicrotask(step); // joins the same queue
    }
    queueMicrotask(step);
  });
}

// Level 5: Node's phases. Inside an I/O callback, check comes before timers.
function afterIo() {
  return new Promise(resolve => {
    readFile(import.meta.filename, () => {
      const order = [];
      const done = name => { order.push(name); if (order.length === 2) resolve(order); };
      setTimeout(() => done('timeout'), 0);
      setImmediate(() => done('immediate'));
    });
  });
}

function microtasksBetweenCallbacks() {
  return new Promise(resolve => {
    const order = [];
    setImmediate(() => {
      order.push('immediate 1');
      Promise.resolve().then(() => order.push('promise'));
    });
    setImmediate(() => { order.push('immediate 2'); resolve(order); });
  });
}

function tickOrder(order) {
  Promise.resolve().then(() => order.push('promise'));
  queueMicrotask(() => order.push('microtask'));
  process.nextTick(() => order.push('nextTick'));
}

// Interview: async functions run synchronously until their first await.
async function a1(log) {
  log.push('a1 start');
  await a2(log);
  log.push('a1 end');
}

async function a2(log) {
  log.push('a2');
}

function asyncPuzzle(log) {
  log.push('script start');
  setTimeout(() => log.push('setTimeout'), 0);
  a1(log);
  new Promise(resolve => {
    log.push('executor'); // the executor runs synchronously
    resolve();
  }).then(() => log.push('then'));
  log.push('script end');
}

// Interview: split long work so other callbacks get a turn.
const yieldToLoop = () => new Promise(resolve => setImmediate(resolve));

async function sumInChunks(n, chunkSize, progress) {
  let total = 0;
  for (let start = 0; start < n; start += chunkSize) {
    for (let i = start; i < Math.min(n, start + chunkSize); i++) total += i;
    progress.chunks += 1;
    await yieldToLoop(); // other tasks can run here
  }
  return total;
}

const order = [];
runToCompletion(order);
assert.deepEqual(order, ['sync 1', 'sync 2']);
await sleep(20);
assert.deepEqual(order, ['sync 1', 'sync 2', 'timeout']);

const waited = await timerAfterBusyStack(50);
assert.ok(waited >= 50, `a 0 ms timer waited ${waited} ms behind a 50 ms busy loop`);

const log = [];
classicPuzzle(log);
assert.deepEqual(log, ['script start', 'script end']);
await sleep(20);
assert.deepEqual(log, ['script start', 'script end', 'promise1', 'promise2', 'setTimeout']);

assert.equal(await microtasksBeforeNextTask(1000), 1000);

assert.deepEqual(await afterIo(), ['immediate', 'timeout']);
assert.deepEqual(await microtasksBetweenCallbacks(), ['immediate 1', 'promise', 'immediate 2']);

const inModule = [];
tickOrder(inModule); // ES module code already runs inside the microtask queue
await sleep(5);
assert.deepEqual(inModule, ['promise', 'microtask', 'nextTick']);

const inTask = [];
setTimeout(() => tickOrder(inTask), 0); // a fresh task: nextTick queue first
await sleep(20);
assert.deepEqual(inTask, ['nextTick', 'promise', 'microtask']);

const asyncLog = [];
asyncPuzzle(asyncLog);
await sleep(20);
assert.deepEqual(asyncLog,
  ['script start', 'a1 start', 'a2', 'executor', 'script end', 'a1 end', 'then', 'setTimeout']);

const progress = { chunks: 0 };
let chunksSeenByOtherTask = -1;
const sum = sumInChunks(1_000_000, 100_000, progress);
setImmediate(() => { chunksSeenByOtherTask = progress.chunks; });
assert.equal(await sum, 499_999_500_000); // 0 + 1 + ... + 999,999 = n(n - 1) / 2
assert.ok(chunksSeenByOtherTask >= 1 && chunksSeenByOtherTask < 10, 'another task ran mid-sum');

console.log('OK');
