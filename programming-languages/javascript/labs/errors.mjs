// Run from programming-languages/javascript/labs with: node errors.mjs
import assert from 'node:assert/strict';
import vm from 'node:vm';

function caught(action) {
  try {
    action();
  } catch (error) {
    return error;
  }
  throw new Error('expected the action to throw');
}

// Level 1: you can throw any value, but only Error objects carry a stack.
function throwString() {
  throw 'disk full';
}

function throwError() {
  throw new Error('disk full');
}

// Level 3: custom errors that keep their cause.
class ConfigError extends Error {
  constructor(message, options) {
    super(message, options); // stores options.cause as this.cause
    this.name = 'ConfigError';
  }
}

function loadConfig(text) {
  try {
    return JSON.parse(text);
  } catch (error) {
    throw new ConfigError('config is not valid JSON', { cause: error });
  }
}

// Level 4: finally runs on every path, and an abrupt finally wins.
function finallyAlwaysRuns(log) {
  try {
    log.push('try');
    return 'from try';
  } finally {
    log.push('finally'); // runs after the return value is computed
  }
}

function finallyOverrides() {
  try {
    return 'from try';
  } finally {
    return 'from finally'; // replaces the pending return
  }
}

function finallySwallows() {
  try {
    throw new Error('lost');
  } finally {
    return 'swallowed'; // replaces the pending throw too
  }
}

// Level 5: a try block cannot catch what is thrown after it has finished.
function throwLater() {
  try {
    setTimeout(() => { throw new Error('too late to catch'); }, 0);
  } catch {
    return 'caught'; // never happens: the callback runs after this try has ended
  }
  return 'try finished';
}

// Level 7: using disposes resources at the end of the block, in reverse order.
function openFile(name, log) {
  log.push(`open ${name}`);
  return {
    name,
    [Symbol.dispose]() {
      log.push(`close ${name}`);
    },
  };
}

function readTwo(log) {
  using first = openFile('a.txt', log);
  using second = openFile('b.txt', log);
  log.push(`reading ${first.name} and ${second.name}`);
} // disposes second, then first

function failingRead() {
  using file = { [Symbol.dispose]() { throw new Error('close failed'); } };
  throw new Error('read failed');
}

async function openConnection(log) {
  log.push('connect');
  return {
    async [Symbol.asyncDispose]() {
      log.push('disconnect');
    },
  };
}

async function query(log) {
  await using connection = await openConnection(log);
  log.push('query');
}

// Level 1
assert.equal(caught(throwString), 'disk full');
assert.equal(caught(throwString).stack, undefined); // a string has no stack
const error = caught(throwError);
assert.ok(error instanceof Error);
assert.equal(error.name, 'Error');
assert.equal(error.message, 'disk full');
assert.ok(error.stack.startsWith('Error: disk full\n    at throwError'));

// Level 2: the engine throws built-in error types.
const message = action => String(caught(action));
assert.equal(message(() => null.port), "TypeError: Cannot read properties of null (reading 'port')");
assert.equal(message(() => JSON.parse('{"port": 80')),
  "SyntaxError: Expected ',' or '}' after property value in JSON at position 11 (line 1 column 12)");
assert.equal(message(() => missingVariable), 'ReferenceError: missingVariable is not defined');
assert.equal(message(() => new Array(-1)), 'RangeError: Invalid array length');
assert.equal(message(() => ({}).load()), 'TypeError: {}.load is not a function');

// Level 3
const configError = caught(() => loadConfig('{"port": 80'));
assert.ok(configError instanceof ConfigError && configError instanceof Error);
assert.equal(String(configError), 'ConfigError: config is not valid JSON');
assert.ok(configError.cause instanceof SyntaxError);
assert.deepEqual(loadConfig('{"port": 80}'), { port: 80 });

// Level 4
const log = [];
assert.equal(finallyAlwaysRuns(log), 'from try');
assert.deepEqual(log, ['try', 'finally']);
assert.equal(finallyOverrides(), 'from finally');
assert.equal(finallySwallows(), 'swallowed');

// Level 5
const uncaught = Promise.withResolvers();
process.once('uncaughtException', reason => uncaught.resolve(reason.message));
assert.equal(throwLater(), 'try finished');
assert.equal(await uncaught.promise, 'too late to catch');
const both = await Promise.any([Promise.reject(new Error('mirror 1 down')), Promise.reject(new Error('mirror 2 down'))])
  .catch(reason => reason);
assert.ok(both instanceof AggregateError);
assert.deepEqual(both.errors.map(reason => reason.message), ['mirror 1 down', 'mirror 2 down']);

// Level 6: Error.isError works across realms; instanceof does not.
const foreign = vm.runInNewContext('new Error("from another realm")');
assert.equal(foreign instanceof Error, false);
assert.equal(Error.isError(foreign), true);
assert.equal(Error.isError({ name: 'Error', message: 'a look-alike' }), false);
assert.equal(Error.isError(configError), true);

// Level 7
const files = [];
readTwo(files);
assert.deepEqual(files, ['open a.txt', 'open b.txt', 'reading a.txt and b.txt', 'close b.txt', 'close a.txt']);
const suppressed = caught(failingRead);
assert.ok(suppressed instanceof SuppressedError);
assert.equal(suppressed.message, 'An error was suppressed during disposal');
assert.equal(suppressed.error.message, 'close failed'); // the newer error
assert.equal(suppressed.suppressed.message, 'read failed'); // the one it replaced
const connections = [];
await query(connections);
assert.deepEqual(connections, ['connect', 'query', 'disconnect']);
assert.equal(message(() => { using nothing = 42; }), 'TypeError: An object is expected with `using` declarations');

// Underneath
assert.equal(Error.stackTraceLimit, 10); // V8 keeps the top 10 frames by default

console.log('OK');
