// Run from programming-languages/javascript/labs with: node iterators-and-generators.mjs
import assert from 'node:assert/strict';

// The problem: eager arrays do all the work before you see one answer.
let eagerSquares = 0;
const eagerBigSquares = Array.from({ length: 1_000_000 }, (_, i) => i + 1)
  .map((n) => {
    eagerSquares += 1;
    return n * n;
  })
  .filter((square) => square > 50)
  .slice(0, 3);
assert.deepEqual(eagerBigSquares, [64, 81, 100]);
assert.equal(eagerSquares, 1_000_000);

// Level 1: the iteration protocol, written by hand.
const oneToThree = {
  [Symbol.iterator]() { // the iterable: hands out a fresh iterator
    let n = 0;
    return {
      next() { // the iterator: one result object per call
        n += 1;
        return n <= 3 ? { value: n, done: false } : { value: undefined, done: true };
      },
    };
  },
};
const iterator = oneToThree[Symbol.iterator]();
assert.deepEqual(iterator.next(), { value: 1, done: false });
assert.deepEqual(iterator.next(), { value: 2, done: false });
assert.deepEqual(iterator.next(), { value: 3, done: false });
assert.deepEqual(iterator.next(), { value: undefined, done: true });

// Level 2: for...of, spread and destructuring all speak the protocol.
const seen = [];
for (const n of oneToThree) seen.push(n);
assert.deepEqual(seen, [1, 2, 3]);
assert.deepEqual([...oneToThree], [1, 2, 3]); // a fresh iterator each time
const [first, second] = oneToThree;
assert.equal(first + second, 3);
assert.equal(Math.max(...oneToThree), 3);

assert.equal('a😀'.length, 3); // UTF-16 code units
assert.deepEqual([...'a😀'], ['a', '😀']); // the string iterator yields code points
const settings = { theme: 'dark' };
assert.throws(() => [...settings], { name: 'TypeError', message: 'settings is not iterable' });
assert.deepEqual([...Object.entries(settings)], [['theme', 'dark']]);

// Level 3: a generator function writes the iterator for you.
function* range(start, end) {
  for (let n = start; n < end; n++) yield n;
}
assert.deepEqual([...range(1, 4)], [1, 2, 3]);

const steps = [];
function* traced() {
  steps.push('start');
  yield 1;
  steps.push('after 1');
  yield 2;
}
const tracer = traced(); // runs nothing yet
assert.deepEqual(steps, []);
assert.deepEqual(tracer.next(), { value: 1, done: false });
assert.deepEqual(steps, ['start']); // ran to the first yield, then paused
tracer.next();
assert.deepEqual(steps, ['start', 'after 1']);

const numbers = range(1, 3);
assert.equal(numbers[Symbol.iterator](), numbers); // a generator is its own iterator
assert.deepEqual([...numbers], [1, 2]);
assert.deepEqual([...numbers], []); // ...so it can be used up only once

// Level 4: laziness makes infinite sequences safe.
function* naturals() {
  for (let n = 1; ; n++) yield n; // never ends, and never runs ahead
}
function* take(iterable, count) {
  if (count <= 0) return;
  let taken = 0;
  for (const value of iterable) {
    yield value;
    taken += 1;
    if (taken === count) return; // stop before pulling one more value
  }
}
assert.deepEqual([...take(naturals(), 5)], [1, 2, 3, 4, 5]);

function* fibonacci() {
  let [a, b] = [0, 1];
  while (true) {
    yield a;
    [a, b] = [b, a + b];
  }
}
assert.deepEqual([...take(fibonacci(), 8)], [0, 1, 1, 2, 3, 5, 8, 13]);

// Level 5: next(value), return() and throw().
function* runningTotal() {
  let total = 0;
  while (true) {
    const amount = yield total; // pause; next(amount) resumes with a value
    total += amount;
  }
}
const till = runningTotal();
till.next(); // run to the first yield; an argument here would be ignored
assert.equal(till.next(5).value, 5);
assert.equal(till.next(10).value, 15);

const cleanups = [];
function* withCleanup() {
  try {
    yield 1;
    yield 2;
  } finally {
    cleanups.push('closed'); // runs on break, return() and throw()
  }
}
for (const value of withCleanup()) {
  if (value === 1) break; // break calls the generator's return()
}
assert.deepEqual(cleanups, ['closed']);
const manual = withCleanup();
manual.next();
assert.deepEqual(manual.return(99), { value: 99, done: true });
assert.deepEqual(cleanups, ['closed', 'closed']);

function* resilient() {
  while (true) {
    try {
      yield 'working';
    } catch (error) {
      yield `recovered from ${error.message}`;
    }
  }
}
const job = resilient();
job.next();
assert.equal(job.throw(new Error('timeout')).value, 'recovered from timeout');
const fresh = resilient(); // not started: no try block is active yet
assert.throws(() => fresh.throw(new Error('early')), { message: 'early' });

function* selfCaller() {
  yield selfCalling.next(); // a generator cannot resume itself
}
const selfCalling = selfCaller();
assert.throws(() => selfCalling.next(), { name: 'TypeError', message: 'Generator is already running' });

// Level 6: iterator helpers (ES2025) are lazy versions of the array methods.
let lazySquares = 0;
const lazyBigSquares = naturals()
  .map((n) => {
    lazySquares += 1;
    return n * n;
  })
  .filter((square) => square > 50)
  .take(3)
  .toArray();
assert.deepEqual(lazyBigSquares, [64, 81, 100]);
assert.equal(lazySquares, 10); // the eager version computed 1,000,000

const order = [];
[1, 2].values() // arrays hand out an iterator with values()
  .map((n) => {
    order.push(`map ${n}`);
    return n;
  })
  .filter((n) => {
    order.push(`filter ${n}`);
    return true;
  })
  .toArray();
assert.deepEqual(order, ['map 1', 'filter 1', 'map 2', 'filter 2']); // one item at a time

withCleanup().take(1).toArray(); // take closes its source when it has enough
assert.deepEqual(cleanups, ['closed', 'closed', 'closed']);

// Level 7: async iterators and for await.
async function fetchPage(page) { // stands in for a network call
  await null;
  return page <= 3 ? [`item${page}a`, `item${page}b`] : [];
}
async function* allItems() {
  for (let page = 1; ; page++) {
    const items = await fetchPage(page);
    if (items.length === 0) return;
    yield* items; // hand on each item, one at a time
  }
}
const collected = [];
for await (const item of allItems()) collected.push(item);
assert.equal(collected.length, 6);
assert.deepEqual(collected.slice(0, 3), ['item1a', 'item1b', 'item2a']);

const pages = allItems();
const pending = pages.next(); // an async iterator returns a promise of a result
assert.ok(pending instanceof Promise);
assert.deepEqual(await pending, { value: 'item1a', done: false });
const stream = allItems();
assert.throws(() => {
  for (const item of stream) collected.push(item); // a plain for...of needs Symbol.iterator
}, { name: 'TypeError', message: 'stream is not iterable' });

console.log('OK');
