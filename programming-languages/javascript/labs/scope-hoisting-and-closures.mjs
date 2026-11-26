// Run from programming-languages/javascript/labs with: node scope-hoisting-and-closures.mjs
import assert from 'node:assert/strict';
import { runInNewContext } from 'node:vm';

// The problem: callbacks made in a var loop all see the final value.
function makeCallbacksWithVar() {
  const callbacks = [];
  for (var i = 0; i < 3; i++) {
    callbacks.push(() => i);
  }
  return callbacks.map((callback) => callback());
}

function makeCallbacksWithLet() {
  const callbacks = [];
  for (let i = 0; i < 3; i++) {
    callbacks.push(() => i);
  }
  return callbacks.map((callback) => callback());
}

assert.deepEqual(makeCallbacksWithVar(), [3, 3, 3]);
assert.deepEqual(makeCallbacksWithLet(), [0, 1, 2]);

// Level 1: lexical scope. A name is looked up from the inside out.
const planet = 'Earth'; // module scope
function country() {
  const name = 'Japan';
  function city() {
    const name = 'Tokyo'; // shadows country's name
    return `${name}, ${planet}`; // name found here, planet two scopes out
  }
  return city();
}
assert.equal(country(), 'Tokyo, Earth');
assert.throws(() => missingName, { name: 'ReferenceError', message: 'missingName is not defined' });

// Level 2: var is function-scoped; let and const are block-scoped.
function blockScopes() {
  if (true) {
    var fromVar = 'function-scoped';
    let fromLet = 'block-scoped';
  }
  return [typeof fromVar, typeof fromLet];
}
assert.deepEqual(blockScopes(), ['string', 'undefined']);

const scores = [90];
scores.push(85); // const fixes the binding, not the array
assert.deepEqual(scores, [90, 85]);
assert.throws(() => {
  scores = [];
}, { name: 'TypeError', message: 'Assignment to constant variable.' });

assert.throws(() => new Function('let x = 1; let x = 2;'), {
  name: 'SyntaxError',
  message: "Identifier 'x' has already been declared",
});
assert.equal(runInNewContext('var visits = 1; globalThis.visits'), 1); // a script's top-level var
assert.equal(runInNewContext('let views = 1; globalThis.views'), undefined); // a top-level let

// Level 3: hoisting. Every declaration exists before its line runs.
function hoisting() {
  const early = [hoistedVar, hoistedFunction()];
  var hoistedVar = 'set later';
  function hoistedFunction() {
    return 'ready';
  }
  return early;
}
assert.deepEqual(hoisting(), [undefined, 'ready']);

function callTooEarly() {
  const result = greet(); // greet exists, but holds undefined
  var greet = function () {
    return 'hi';
  };
  return result;
}
assert.throws(callTooEarly, { name: 'TypeError', message: 'greet is not a function' });

function useBeforeLet() {
  const before = total; // total exists but is not initialised yet
  let total = 10;
  return before;
}
assert.throws(useBeforeLet, {
  name: 'ReferenceError',
  message: "Cannot access 'total' before initialization",
});

function timeNotPlace() {
  const readTotal = () => total; // written above the declaration...
  let total = 10;
  return readTotal(); // ...but runs after it
}
assert.equal(timeNotPlace(), 10);

assert.throws(() => {
  typeof later; // typeof does not protect a name in its dead zone
  let later;
}, { name: 'ReferenceError' });

// Level 4: a closure keeps its birth scope alive.
function makeCounter() {
  let count = 0; // lives on after makeCounter returns
  return () => {
    count += 1;
    return count;
  };
}
const counterA = makeCounter();
const counterB = makeCounter();
counterA();
counterA();
assert.equal(counterA(), 3);
assert.equal(counterB(), 1); // each call made a new count

function liveLink() {
  let message = 'before';
  const read = () => message;
  message = 'after'; // the closure sees the variable, not a snapshot
  return read();
}
assert.equal(liveLink(), 'after');

// Level 5: the loop puzzle, fixed the pre-2015 way.
function makeCallbacksWithIIFE() {
  const callbacks = [];
  for (var i = 0; i < 3; i++) {
    (function (copy) {
      callbacks.push(() => copy); // each call made a new copy
    })(i);
  }
  return callbacks.map((callback) => callback());
}
assert.deepEqual(makeCallbacksWithIIFE(), [0, 1, 2]);

// Level 6: private state. Only the returned functions can reach balance.
function makeAccount(opening) {
  let balance = opening;
  return {
    deposit(amount) {
      if (amount <= 0) throw new RangeError('deposit must be positive');
      balance += amount;
      return balance;
    },
    getBalance: () => balance,
  };
}
const account = makeAccount(100);
account.deposit(50);
assert.equal(account.getBalance(), 150);
assert.equal(account.balance, undefined); // not a property: no way in from outside
assert.deepEqual(Object.keys(account), ['deposit', 'getBalance']);
assert.throws(() => account.deposit(-5), { name: 'RangeError' });

// Level 7: a closure keeps everything it uses alive.
function makeSummary(readings) {
  return () => readings.length; // keeps the whole array alive
}
function makeLeanSummary(readings) {
  const count = readings.length; // copy the one number we need
  return () => count; // keeps one number alive
}
const readings = new Array(1_000_000).fill(20.5);
assert.equal(makeSummary(readings)(), 1_000_000);
assert.equal(makeLeanSummary(readings)(), 1_000_000);

// Interview: once(fn) runs fn the first time only.
function once(fn) {
  let called = false;
  let result;
  return (...args) => {
    if (!called) {
      called = true;
      result = fn(...args);
    }
    return result;
  };
}
let runs = 0;
const init = once(() => {
  runs += 1;
  return 'initialised';
});
assert.equal(init(), 'initialised');
assert.equal(init(), 'initialised');
assert.equal(runs, 1);

console.log('OK');
