// Run from programming-languages/javascript/labs with: node objects-and-prototypes.mjs
import assert from 'node:assert/strict';

// The problem: copies of the defaults go stale; a prototype link does not.
const defaults = { theme: 'light', fontSize: 14 };
const copiedSettings = { ...defaults, fontSize: 18 }; // a snapshot
const adaSettings = Object.create(defaults); // a link: ada's settings inherit from defaults
adaSettings.fontSize = 18; // an own property shadows the default
defaults.theme = 'dark';
assert.equal(copiedSettings.theme, 'light'); // the copy missed the change
assert.equal(adaSettings.theme, 'dark'); // the link sees it
assert.equal(adaSettings.fontSize, 18);

// Level 1: an object is a map from string (or symbol) keys to properties.
const box = {};
box[1] = 'one'; // the number 1 becomes the string key '1'
assert.equal(box['1'], 'one');
const keyA = { id: 'a' };
const keyB = { id: 'b' };
box[keyA] = 'A'; // an object key becomes '[object Object]'
box[keyB] = 'B'; // ...the same string, so this overwrites A
assert.equal(box[keyA], 'B');
assert.deepEqual(Object.keys(box), ['1', '[object Object]']);
assert.deepEqual(Object.keys({ b: 1, 2: 1, a: 1, 1: 1 }), ['1', '2', 'b', 'a']);

// Level 2: every property has a descriptor.
const order = { item: 'tea' };
assert.deepEqual(Object.getOwnPropertyDescriptor(order, 'item'), {
  value: 'tea', writable: true, enumerable: true, configurable: true,
});
Object.defineProperty(order, 'id', { value: 7 }); // omitted flags default to false
assert.deepEqual(Object.getOwnPropertyDescriptor(order, 'id'), {
  value: 7, writable: false, enumerable: false, configurable: false,
});
assert.deepEqual(Object.keys(order), ['item']); // not enumerable: hidden from keys
assert.equal(JSON.stringify(order), '{"item":"tea"}'); // ...and from JSON
assert.throws(() => {
  order.id = 8;
}, { name: 'TypeError', message: "Cannot assign to read only property 'id' of object '#<Object>'" });
assert.throws(() => {
  delete order.id;
}, { name: 'TypeError', message: "Cannot delete property 'id' of #<Object>" });

const config = Object.freeze({ port: 8080, db: { host: 'localhost' } });
assert.throws(() => {
  config.port = 9090;
}, { name: 'TypeError', message: "Cannot assign to read only property 'port' of object '#<Object>'" });
assert.throws(() => {
  config.debug = true;
}, { name: 'TypeError', message: 'Cannot add property debug, object is not extensible' });
config.db.host = 'example.com'; // the inner object was never frozen
assert.equal(config.db.host, 'example.com');
assert.equal(Object.isFrozen(config.db), false);

// Level 3: accessor properties run code on read and write.
const thermometer = {
  celsius: 100,
  get fahrenheit() {
    return this.celsius * 9 / 5 + 32;
  },
  set fahrenheit(value) {
    this.celsius = (value - 32) * 5 / 9;
  },
};
assert.equal(thermometer.fahrenheit, 212);
thermometer.fahrenheit = 32;
assert.equal(thermometer.celsius, 0);
assert.equal(typeof Object.getOwnPropertyDescriptor(thermometer, 'fahrenheit').get, 'function');

// Level 4: the prototype chain.
assert.equal(Object.getPrototypeOf(adaSettings), defaults);
assert.equal(Object.getPrototypeOf(defaults), Object.prototype);
assert.equal(Object.getPrototypeOf(Object.prototype), null); // the end of every chain
assert.equal(adaSettings.missing, undefined); // searched all three objects, then gave up
assert.equal(typeof adaSettings.toString, 'function'); // found on Object.prototype

const animal = {
  describe() {
    return `${this.name} says ${this.sound}`;
  },
};
const cow = Object.create(animal);
cow.name = 'Cow';
cow.sound = 'moo';
assert.equal(cow.describe(), 'Cow says moo'); // found on animal, this is cow
assert.equal(Object.hasOwn(cow, 'describe'), false);

// Level 5: own versus inherited, and shadowing.
assert.equal('theme' in adaSettings, true); // in: own or inherited
assert.equal(Object.hasOwn(adaSettings, 'theme'), false); // hasOwn: own only
assert.deepEqual(Object.keys(adaSettings), ['fontSize']); // own and enumerable
const seen = [];
for (const key in adaSettings) seen.push(key); // for...in also walks the chain
assert.deepEqual(seen, ['fontSize', 'theme']);

adaSettings.theme = 'blue'; // creates an own property; defaults is untouched
assert.equal(defaults.theme, 'dark');
delete adaSettings.theme; // removing the shadow uncovers the inherited value
assert.equal(adaSettings.theme, 'dark');

const locked = Object.freeze({ version: 1 });
const child = Object.create(locked);
assert.throws(() => {
  child.version = 2; // an inherited read-only property blocks assignment
}, { name: 'TypeError', message: "Cannot assign to read only property 'version' of object '#<Object>'" });
Object.defineProperty(child, 'version', { value: 2 }); // defining is still allowed
assert.equal(child.version, 2);

// Level 6: spread is a shallow copy; structuredClone is a deep one.
const original = { name: 'Ada', tags: ['admin'], joined: new Date(0) };
const shallow = { ...original };
shallow.tags.push('editor'); // the array is shared
assert.deepEqual(original.tags, ['admin', 'editor']);
const deep = structuredClone(original);
deep.tags.push('owner'); // a separate array
assert.deepEqual(original.tags, ['admin', 'editor']);
assert.ok(deep.joined instanceof Date); // a JSON round trip would give a string

assert.deepEqual({ ...adaSettings }, { fontSize: 18 }); // spread copies own properties only
assert.throws(() => structuredClone({ greet: () => 'hi' }), {
  name: 'DataCloneError',
  message: "() => 'hi' could not be cloned.",
});
const loop = { name: 'loop' };
loop.self = loop;
const loopCopy = structuredClone(loop); // cycles are fine
assert.equal(loopCopy.self, loopCopy);

// Level 7: Map is the dictionary type.
const visits = new Map();
visits.set(keyA, 1); // any value can be a key, compared by identity
visits.set(keyB, 2);
visits.set(1, 'number one').set('1', 'string one'); // 1 and '1' stay apart
assert.equal(visits.get(keyA), 1);
assert.equal(visits.size, 4);
assert.equal('toString' in {}, true); // a plain object inherits keys
assert.equal(visits.has('toString'), false);
const dictionary = Object.create(null); // an object with no prototype at all
assert.equal('toString' in dictionary, false);

// Interview: F.prototype is not F's own prototype.
function Point(x, y) {
  this.x = x;
  this.y = y;
}
const p = new Point(1, 2);
assert.equal(Object.getPrototypeOf(p), Point.prototype);
assert.equal(Object.getPrototypeOf(Point), Function.prototype);

console.log('OK');
