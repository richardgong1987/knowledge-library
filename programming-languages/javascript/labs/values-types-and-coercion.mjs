// Run from programming-languages/javascript/labs with: node values-types-and-coercion.mjs
import assert from 'node:assert/strict';

// The problem: a form field always gives a string, and decimals are not exact.
const quantityFromForm = '2';
assert.equal(quantityFromForm + 1, '21'); // string + number: concatenation
assert.equal(Number(quantityFromForm) + 1, 3); // convert first, then add
assert.equal(0.1 + 0.2, 0.30000000000000004);

// Level 1: seven primitive types, plus objects.
assert.equal(typeof undefined, 'undefined');
assert.equal(typeof true, 'boolean');
assert.equal(typeof 42, 'number');
assert.equal(typeof 42n, 'bigint');
assert.equal(typeof 'hi', 'string');
assert.equal(typeof Symbol('id'), 'symbol');
assert.equal(typeof { a: 1 }, 'object');

assert.equal(typeof null, 'object'); // a bug kept since 1995
assert.equal(typeof [1, 2], 'object'); // arrays are objects
assert.equal(typeof function () {}, 'function'); // callable objects get their own answer
assert.equal(typeof notDeclaredAnywhere, 'undefined'); // no ReferenceError

const nothing = null;
assert.equal(nothing === null, true); // the reliable null test
assert.equal(Array.isArray([1, 2]), true); // the reliable array test

// Level 2: every Number is an IEEE 754 double.
function float64Bits(x) {
  const view = new DataView(new ArrayBuffer(8));
  view.setFloat64(0, x);
  return view.getBigUint64(0).toString(16);
}
assert.equal(float64Bits(0.1), '3fb999999999999a');
assert.equal(float64Bits(0.2), '3fc999999999999a');
assert.equal(float64Bits(0.3), '3fd3333333333333');
assert.equal(float64Bits(0.1 + 0.2), '3fd3333333333334'); // one step above 0.3

assert.equal((0.1).toFixed(20), '0.10000000000000000555');
assert.equal(0x3fb, 1019); // the stored exponent of 0.1
assert.equal(0x999999999999a, 2702159776422298); // its 52-bit fraction
assert.equal((1 + 2702159776422298 / 2 ** 52) * 2 ** (1019 - 1023), 0.1);

assert.equal(Number.MAX_SAFE_INTEGER, 2 ** 53 - 1);
assert.equal(Number.MAX_SAFE_INTEGER, 9007199254740991);
assert.equal(2 ** 53 + 1, 2 ** 53); // 9007199254740993 rounds to its neighbour
assert.equal(Number.isSafeInteger(2 ** 53), false);
assert.equal(Number.EPSILON, 2 ** -52);

const totalCents = 10 + 20; // store 0.10 and 0.20 as whole cents
assert.equal(totalCents, 30);
assert.ok(Math.abs(0.1 + 0.2 - 0.3) < Number.EPSILON); // or compare with a tolerance

// Level 3: BigInt is exact for integers of any size.
assert.equal(2n ** 64n, 18446744073709551616n);
assert.equal(9007199254740993n + 1n, 9007199254740994n); // no rounding
assert.equal(7n / 2n, 3n); // division drops the fraction
assert.throws(() => 1n + 1, {
  name: 'TypeError',
  message: 'Cannot mix BigInt and other types, use explicit conversions',
});
assert.equal(1n + BigInt(1), 2n); // convert explicitly
assert.equal(Number(2n ** 64n), 18446744073709552000); // back to a double: rounded

// Level 4: coercion. + concatenates if either side becomes a string.
assert.equal('5' + 3, '53');
assert.equal('5' - 3, 2); // minus only means subtraction: ToNumber
assert.equal('5' * '2', 10);
assert.equal(true + 1, 2);
assert.equal(null + 1, 1);
assert.ok(Number.isNaN(undefined + 1));

assert.equal(Number(''), 0);
assert.equal(Number(' 12 '), 12); // surrounding spaces are ignored
assert.ok(Number.isNaN(Number('12px')));
assert.equal(Number(null), 0);
assert.ok(Number.isNaN(Number(undefined)));
assert.equal(Number([5]), 5); // [5] -> '5' -> 5

const price = {
  valueOf() {
    return 42;
  },
  toString() {
    return 'forty-two';
  },
};
assert.equal(price + 1, 43); // hint "default": valueOf first
assert.equal(`${price}`, 'forty-two'); // hint "string": toString first
assert.equal([] + [], ''); // [] -> ''
assert.equal([] + {}, '[object Object]');
assert.equal([1, 2] + [3], '1,23'); // '1,2' + '3'

const temperature = {
  [Symbol.toPrimitive](hint) {
    return hint === 'string' ? '21 degrees' : 21;
  },
};
assert.equal(+temperature, 21); // hint "number"
assert.equal(`${temperature}`, '21 degrees'); // hint "string"
assert.equal(temperature + 1, 22); // hint "default"

// Level 5: == converts, === does not.
assert.equal(null == undefined, true); // a special pair
assert.equal(null == 0, false); // null only equals undefined
assert.equal('0' == false, true); // false -> 0, '0' -> 0
assert.equal([] == false, true); // false -> 0, [] -> '' -> 0
assert.equal(NaN == NaN, false);
assert.equal('0' === false, false); // different types: never equal
assert.equal([] == ![], true); // interview question 3

assert.equal(Object.is(NaN, NaN), true);
assert.equal(Object.is(0, -0), false);
assert.equal(0 === -0, true);
assert.equal(1 / -0, -Infinity); // why -0 is a different value
assert.equal([NaN].includes(NaN), true); // SameValueZero
assert.equal([NaN].indexOf(NaN), -1); // ===

// Level 6: exactly eight falsy values.
const falsy = [false, 0, -0, 0n, '', null, undefined, NaN];
assert.equal(falsy.filter(Boolean).length, 0);
assert.equal(Boolean('0'), true);
assert.equal(Boolean('false'), true);
assert.equal(Boolean([]), true);
assert.equal(Boolean({}), true);

const savedVolume = 0;
assert.equal(savedVolume || 50, 50); // 0 is falsy: the user's choice is lost
assert.equal(savedVolume ?? 50, 0); // ?? only replaces null and undefined

// Level 7: primitives are immutable; objects are shared by reference.
const word = 'cat';
assert.throws(() => {
  word[0] = 'b';
}, {
  name: 'TypeError',
  message: "Cannot assign to read only property '0' of string 'cat'",
});
assert.equal(word.toUpperCase(), 'CAT'); // a new string
assert.equal(word, 'cat'); // the original never changes

const original = { n: 3 };
const alias = original; // copies the reference, not the object
alias.n = 7;
assert.equal(original.n, 7);
assert.equal({ n: 7 } === original, false); // same contents, different object

function rename(user) {
  user.name = 'Bo'; // changes the caller's object
  user = { name: 'Cy' }; // rebinds only the local parameter
  return user.name;
}
const person = { name: 'Ada' };
assert.equal(rename(person), 'Cy');
assert.equal(person.name, 'Bo');

console.log('OK');
