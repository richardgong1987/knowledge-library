// Run from programming-languages/javascript/labs with: node this-and-function-calls.mjs
import assert from 'node:assert/strict';

// The problem: a method passed around on its own forgets its object.
const clickCounter = {
  clicks: 0,
  record() {
    this.clicks += 1;
  },
};
clickCounter.record(); // works: this is clickCounter
const handler = clickCounter.record; // the same function, detached from its object
assert.throws(handler, {
  name: 'TypeError',
  message: "Cannot read properties of undefined (reading 'clicks')",
});
assert.equal(clickCounter.clicks, 1);

// Level 1: implicit binding. The object before the dot becomes this.
function describe() {
  return `${this.name} has ${this.legs} legs`;
}
const cat = { name: 'Cat', legs: 4, describe };
const bird = { name: 'Bird', legs: 2, describe };
assert.equal(cat.describe(), 'Cat has 4 legs');
assert.equal(bird.describe(), 'Bird has 2 legs'); // same function, different this

// Level 2: default binding. No object before the call.
function whoAmI() {
  return this;
}
assert.equal(whoAmI(), undefined); // modules are strict: no receiver means undefined
const sloppyWhoAmI = new Function('return this;'); // a Function body is sloppy by default
assert.equal(sloppyWhoAmI(), globalThis);

// Level 3: explicit binding with call, apply and bind.
function introduce(greeting, punctuation) {
  return `${greeting}, I am ${this.name}${punctuation}`;
}
assert.equal(introduce.call(cat, 'Hello', '!'), 'Hello, I am Cat!');
assert.equal(introduce.apply(bird, ['Hi', '.']), 'Hi, I am Bird.');
const catIntro = introduce.bind(cat, 'Hey'); // fixes this and the first argument
assert.equal(catIntro('?'), 'Hey, I am Cat?');
assert.equal(catIntro.call(bird, '?'), 'Hey, I am Cat?'); // a bound this cannot be replaced

// Level 4: new binding, and which rule wins.
function Point(x, y) {
  this.x = x; // this is a brand-new object
  this.y = y;
}
const p = new Point(1, 2);
assert.deepEqual({ ...p }, { x: 1, y: 2 });
assert.equal(Object.getPrototypeOf(p), Point.prototype);

assert.equal(cat.describe.call(bird), 'Bird has 2 legs'); // explicit beats implicit
const BoundPoint = Point.bind({ x: 'ignored' }, 10);
const q = new BoundPoint(20); // new beats bind; the preset 10 still applies
assert.equal(q.x, 10);
assert.equal(q.y, 20);
assert.ok(q instanceof Point);

// Level 5: arrow functions have no this of their own.
const timer = {
  seconds: 0,
  tickAll(ticks) {
    ticks.forEach(() => {
      this.seconds += 1; // the arrow uses tickAll's this
    });
    return this.seconds;
  },
};
assert.equal(timer.tickAll([1, 2, 3]), 3);

const brokenTimer = {
  seconds: 0,
  tickAll(ticks) {
    ticks.forEach(function () {
      this.seconds += 1; // a plain call inside forEach: this is undefined
    });
  },
};
assert.throws(() => brokenTimer.tickAll([1]), {
  name: 'TypeError',
  message: "Cannot read properties of undefined (reading 'seconds')",
});

const arrowThis = () => this; // module top level: this is undefined
assert.equal(arrowThis.call(cat), undefined); // call cannot change an arrow's this
assert.throws(() => new arrowThis(), { name: 'TypeError', message: 'arrowThis is not a constructor' });

// Level 6: three ways to pass a method as a callback.
const tally = {
  count: 0,
  add() {
    this.count += 1;
  },
};
[1, 2].forEach(tally.add.bind(tally)); // fix 1: bind
[1, 2].forEach(() => tally.add()); // fix 2: call it as a method inside an arrow
[1, 2].forEach(tally.add, tally); // fix 3: forEach's thisArg
assert.equal(tally.count, 6);

// Underneath: the Reference Record and [[ThisMode]].
assert.equal((cat.describe)(), 'Cat has 4 legs'); // grouping keeps the reference
assert.throws(() => (0, cat.describe)(), { name: 'TypeError' }); // the comma operator returns a bare function

const sloppyTypeOfThis = new Function('return typeof this;');
function strictTypeOfThis() {
  return typeof this;
}
assert.equal(sloppyTypeOfThis.call(5), 'object'); // sloppy: 5 is boxed into a Number object
assert.equal(strictTypeOfThis.call(5), 'number'); // strict: this is exactly what was passed
assert.equal(sloppyWhoAmI.call(null), globalThis); // sloppy: null becomes globalThis
assert.equal(whoAmI.call(null), null); // strict: null stays null

// Interview: a simplified bind.
function simpleBind(fn, boundThis, ...preset) {
  return function (...args) {
    return fn.apply(boundThis, [...preset, ...args]);
  };
}
assert.equal(simpleBind(introduce, cat, 'Yo')('!'), 'Yo, I am Cat!');
const realBound = introduce.bind(cat);
assert.equal(realBound.name, 'bound introduce');
assert.equal(realBound.length, 2);
assert.equal(catIntro.length, 1); // one of introduce's two parameters is preset

console.log('OK');
