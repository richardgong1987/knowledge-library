// Run from programming-languages/javascript/labs with: node memory-and-garbage-collection.mjs
import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import v8 from 'node:v8';
import vm from 'node:vm';

// Labs only: expose V8's gc() so the checks below can force a collection.
v8.setFlagsFromString('--expose-gc');
const gc = vm.runInNewContext('gc');

const nextTask = () => new Promise(resolve => setImmediate(resolve));

// A WeakRef keeps its target alive until the current job ends, so wait a task, then collect.
async function isCollected(ref) {
  for (let attempt = 0; attempt < 10; attempt++) {
    await nextTask();
    gc();
    if (ref.deref() === undefined) return true;
  }
  return false;
}

// Level 1: what is reachable from a root survives; the rest is garbage.
function makeGraph() {
  const a = { name: 'a' };
  const b = { name: 'b' };
  const c = { name: 'c' };
  a.next = b; // a -> b
  c.next = b; // c -> b, but nothing points to c
  return { root: a, weakB: new WeakRef(b), weakC: new WeakRef(c) };
}

// Level 2: a cycle that nothing outside points to is still garbage.
function makeCycle() {
  const left = {};
  const right = { left };
  left.right = right; // left <-> right: each is referenced once
  return new WeakRef(left);
}

// Level 3: three common leaks.
function startPolling(data) {
  return setInterval(() => data.length, 1000); // the callback keeps data reachable
}

function subscribe(emitter, onData) {
  emitter.on('data', onData); // the emitter now holds onData and everything it captures
  return () => emitter.off('data', onData);
}

function makeHandlers(big) {
  const usesBig = () => big.length; // never returned, but it captures big
  const small = () => 'small';
  return small; // in V8, small shares usesBig's context, so big stays alive
}

// Level 4: WeakMap holds its keys weakly.
const labels = new WeakMap();
function label(object, text) {
  labels.set(object, text); // no strong reference to object is kept
}

// Level 5: FinalizationRegistry runs a callback some time after collection.
const cleaned = [];
const registry = new FinalizationRegistry(heldValue => cleaned.push(heldValue));
function registerTemporary() {
  registry.register({ size: 1024 }, 'temporary buffer');
}

// Level 1
const graph = makeGraph();
assert.equal(await isCollected(graph.weakC), true);
assert.equal(await isCollected(graph.weakB), false); // still reachable: root -> a -> b
assert.equal(graph.root.next.name, 'b');

// Level 2
assert.equal(await isCollected(makeCycle()), true);

// Level 3
let polled = { length: 3 };
const polledRef = new WeakRef(polled);
const timer = startPolling(polled);
polled = null;
assert.equal(await isCollected(polledRef), false); // the interval still holds it
clearInterval(timer);
assert.equal(await isCollected(polledRef), true);

const emitter = new EventEmitter();
const unsubscribers = [1, 2, 3].map(() => subscribe(emitter, () => {}));
assert.equal(emitter.listenerCount('data'), 3); // one more per call, forever, unless removed
unsubscribers.forEach(unsubscribe => unsubscribe());
assert.equal(emitter.listenerCount('data'), 0);

let big = new Array(1000).fill(0);
const bigRef = new WeakRef(big);
const small = makeHandlers(big);
big = null;
assert.equal(small(), 'small');
assert.equal(await isCollected(bigRef), false); // retained through the shared context

// Level 4
const cache = new Map();
let mapKey = {};
const mapKeyRef = new WeakRef(mapKey);
cache.set(mapKey, 'cached');
mapKey = null;
assert.equal(await isCollected(mapKeyRef), false); // a Map holds its keys strongly
cache.clear();
assert.equal(await isCollected(mapKeyRef), true);

let weakKey = {};
const weakKeyRef = new WeakRef(weakKey);
label(weakKey, 'temporary');
assert.equal(labels.get(weakKey), 'temporary');
weakKey = null;
assert.equal(await isCollected(weakKeyRef), true); // the WeakMap did not keep it alive

// Level 5
registerTemporary();
for (let attempt = 0; attempt < 20 && cleaned.length === 0; attempt++) {
  await nextTask();
  gc();
}
assert.deepEqual(cleaned, ['temporary buffer']);

// Underneath: V8's heap is split into spaces; new objects start in new_space.
const spaces = v8.getHeapSpaceStatistics().map(space => space.space_name);
assert.ok(spaces.includes('new_space') && spaces.includes('old_space'));
assert.ok(process.memoryUsage().heapUsed > 0);

console.log('OK');
