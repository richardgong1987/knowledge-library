// Run from programming-languages/javascript/labs with: node modules.mjs
// The lab writes tiny module files into a temporary folder, then loads them.
import assert from 'node:assert/strict';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';

const dir = mkdtempSync(join(tmpdir(), 'modules-lab-'));
const write = (name, source) => writeFileSync(join(dir, name), source);
const load = (name, query = '') => import(pathToFileURL(join(dir, name)).href + query); // dynamic import()
const require = createRequire(join(dir, 'main.cjs'));

// Level 1 and 2: an ES module exports live bindings.
write('counter.mjs', `
// counter.mjs
export let count = 0;
export function increment() {
  count += 1;
}
`);

write('main.mjs', `
// main.mjs
import { count, increment } from './counter.mjs';
increment();
increment();
export const seen = count; // 2: count is a live view of counter.mjs's variable
`);

write('reassign.mjs', `
import { count } from './counter.mjs';
export let message;
try {
  count = 5; // imports are read-only views
} catch (error) {
  message = error.message;
}
`);

// Level 2: CommonJS exports copies of values.
write('counter.cjs', `
// counter.cjs
let count = 0;
function increment() {
  count += 1;
}
module.exports = { count, increment }; // copies the current value, 0
`);

// Level 3: a module is evaluated once per URL.
write('logger.mjs', `
// logger.mjs
globalThis.loggerRuns = (globalThis.loggerRuns ?? 0) + 1;
export const instance = Symbol('logger');
`);

// Level 4: a cycle, a.mjs -> b.mjs -> a.mjs.
write('a.mjs', `
// a.mjs
import { b } from './b.mjs';
export const a = 'from a';
export const bSeenByA = b; // b.mjs has already run
export function greet() {
  return 'hello from a';
}
`);

write('b.mjs', `
// b.mjs
import { a, greet } from './a.mjs';
export const b = 'from b';
export let aSeenByB;
try {
  aSeenByB = a; // a.mjs has not run yet
} catch (error) {
  aSeenByB = String(error); // 'name: message'
}
export function readA() {
  return a; // fine later, once a.mjs has run
}
export const greeting = greet(); // function declarations are ready at link time
`);

write('x.cjs', `
// x.cjs
exports.early = 'early';
const y = require('./y.cjs');
exports.late = 'late';
exports.ySaw = y.saw;
`);

write('y.cjs', `
// y.cjs
const x = require('./x.cjs'); // x.cjs is half done
exports.saw = Object.keys(x); // only the exports that exist so far
`);

// Level 5: top-level await and import.meta.
write('config.mjs', `
// config.mjs
await new Promise(resolve => setTimeout(resolve, 20)); // importers wait for this
export const port = 8080;
export const file = import.meta.filename;
`);

write('uses-config.mjs', `
import { port } from './config.mjs';
export const startedWith = port; // runs only after config.mjs has finished
`);

// Level 1-2
const main = await load('main.mjs');
assert.equal(main.seen, 2);
const counter = await load('counter.mjs');
assert.equal(counter.count, 2); // the same instance main.mjs used
counter.increment();
assert.equal(counter.count, 3);
assert.equal((await load('reassign.mjs')).message, 'Assignment to constant variable.');
assert.equal(Object.prototype.toString.call(counter), '[object Module]');

const { count, increment } = require('./counter.cjs');
increment();
increment();
assert.equal(count, 0); // a copy, taken when we destructured
assert.equal(require('./counter.cjs').count, 0); // module.exports holds the old copy too

// Level 3
const first = await load('logger.mjs');
const second = await load('logger.mjs');
assert.equal(first, second);
assert.equal(globalThis.loggerRuns, 1);
const third = await load('logger.mjs', '?fresh'); // a different URL is a different module
assert.notEqual(third.instance, first.instance);
assert.equal(globalThis.loggerRuns, 2);
assert.equal(require('./counter.cjs'), require('./counter.cjs')); // require has a cache too

// Level 4
const a = await load('a.mjs');
const b = await load('b.mjs');
assert.equal(a.bSeenByA, 'from b');
assert.equal(b.aSeenByB, "ReferenceError: Cannot access 'a' before initialization");
assert.equal(b.readA(), 'from a');
assert.equal(b.greeting, 'hello from a');
assert.deepEqual(require('./x.cjs').ySaw, ['early']);

// Level 5
const start = performance.now();
const usesConfig = await load('uses-config.mjs');
assert.ok(performance.now() - start >= 15, 'the importer waited for config.mjs');
assert.equal(usesConfig.startedWith, 8080);
const config = await load('config.mjs');
assert.ok(config.file.endsWith('config.mjs'));
assert.ok(import.meta.url.startsWith('file://'));

// Level 6: require() of an ES module.
const required = require('./counter.mjs');
assert.equal(required, counter); // the same namespace object as import()
assert.equal(required.count, 3);
assert.throws(() => require('./config.mjs'), { code: 'ERR_REQUIRE_ASYNC_MODULE' });

rmSync(dir, { recursive: true });
console.log('OK');
