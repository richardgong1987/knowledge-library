// Run from programming-languages/typescript/labs with: npx tsc -p . && node unions-and-narrowing.ts
// tsc proves the compile-time claims (every @ts-expect-error must hit an error); node checks the runtime ones.
import assert from "node:assert/strict";

// Equal<A, B> is true only when A and B are exactly the same type.
// A line such as `type Here = Expect<Equal<typeof x, string>>` inside a branch proves what x is there.
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Expect<T extends true> = T;

// The problem: a price arrives as a number or as a string.
function formatUnchecked(price: number | string): string {
  // @ts-expect-error: toFixed exists on number but not on string
  return price.toFixed(2);
}

// Level 1: a union is "or", an intersection is "and".
type Named = { name: string };
type Aged = { age: number };

type Person = Named & Aged; // must have both
const ada: Person = { name: "Ada", age: 36 };

function nameOf(contact: Named | Aged): string {
  // @ts-expect-error: an Aged contact has no name
  return contact.name;
}

type NoValueIsBoth = Expect<Equal<string & number, never>>;
type ClashingProperty = Expect<Equal<({ id: string } & { id: number })["id"], never>>;

// Level 2: typeof narrows a union inside each branch.
function formatPrice(price: number | string): string {
  if (typeof price === "string") {
    type Here = Expect<Equal<typeof price, string>>;
    return Number(price).toFixed(2);
  }
  type Here = Expect<Equal<typeof price, number>>;
  return price.toFixed(2);
}

function keepsNull(tags: string[] | null): string {
  if (typeof tags === "object") {
    type Here = Expect<Equal<typeof tags, string[] | null>>; // typeof null is "object"
    return tags === null ? "null" : "array";
  }
  return "never reached";
}

// Level 3: truthiness, equality, in and instanceof are guards too.
function greet(name: string | null | undefined): string {
  if (name) {
    type Here = Expect<Equal<typeof name, string>>;
    return `Hello, ${name}`;
  }
  return "Hello, guest"; // null, undefined and also ""
}

function joinIfEqual(a: string | number, b: string | boolean): string {
  if (a === b) {
    type Here = Expect<Equal<typeof a | typeof b, string>>; // string is the only overlap
    return a + b;
  }
  return "different";
}

type Fish = { swim: () => string };
type Bird = { fly: () => string };

function move(animal: Fish | Bird): string {
  if ("swim" in animal) return animal.swim();
  return animal.fly();
}

function toDate(value: Date | string): Date {
  if (value instanceof Date) return value;
  return new Date(value);
}

// Level 4: your own guards. A predicate narrows in an if; an assertion narrows after the call.
function isFish(animal: Fish | Bird): animal is Fish {
  return "swim" in animal;
}

function assertIsString(value: unknown): asserts value is string {
  if (typeof value !== "string") {
    throw new TypeError(`expected a string, got ${typeof value}`);
  }
}

function shout(input: unknown): string {
  assertIsString(input);
  return input.toUpperCase(); // input: string from here on
}

function isFishLie(animal: Fish | Bird): animal is Fish {
  return true; // tsc trusts the predicate and does not check the body
}

const assertArrow = (value: unknown): asserts value is string => assertIsString(value);

function lengthOf(input: unknown): number {
  // @ts-expect-error: an assertion must be called through a name with an explicit type
  assertArrow(input);
  return 0;
}

// Level 5: control-flow analysis follows assignments and joins.
function pick(flag: boolean) {
  let result: string | number | boolean;
  if (flag) {
    result = "yes";
  } else {
    result = 0;
  }
  return result; // the two paths meet: string | number, never boolean
}

type PickReturns = Expect<Equal<ReturnType<typeof pick>, string | number>>;

let late: string | number = "a";
// @ts-expect-error: late is assigned again below, so the closure cannot trust "string"
const readLate = () => late.toUpperCase();
late = 1;

function withQuery(url: string | URL, names: string[]): string[] {
  if (typeof url === "string") {
    url = new URL(url);
  }
  return names.map(name => {
    url.searchParams.set("lang", name); // TypeScript 5.4+: url is URL after its last assignment
    return url.toString();
  });
}

// Level 6: TypeScript 5.5 infers a type predicate from the function body.
const scores = [90, null, 75].filter(score => score !== null);
const truthy = [90, null, 0].filter(score => !!score);

type ScoresAreNumbers = Expect<Equal<typeof scores, number[]>>;
type TruthyKeepsNull = Expect<Equal<typeof truthy, (number | null)[]>>;

const isPresent = (score: number | null) => score !== null;
type InferredPredicate = Expect<Equal<typeof isPresent, (score: number | null) => score is number>>;

// Runtime facts.
assert.throws(() => formatUnchecked("9.5"), { name: "TypeError", message: "price.toFixed is not a function" });
assert.equal(formatUnchecked(9.5), "9.50");
assert.deepEqual(ada, { name: "Ada", age: 36 });
assert.equal(nameOf({ age: 36 }), undefined);
assert.equal(formatPrice("9.5"), "9.50");
assert.equal(formatPrice(9.5), "9.50");
assert.equal(typeof null, "object");
assert.equal(keepsNull(null), "null");
assert.equal(greet("Ada"), "Hello, Ada");
assert.equal(greet(""), "Hello, guest");
assert.equal(joinIfEqual("x", "x"), "xx");
assert.equal(joinIfEqual(1, true), "different");
assert.equal(move({ swim: () => "splash" }), "splash");
assert.equal(move({ fly: () => "flap" }), "flap");
assert.equal(toDate("2026-11-26").getUTCDate(), 26);
const bird: Fish | Bird = { fly: () => "flap" };
assert.equal(isFish(bird), false);
assert.equal(shout("hi"), "HI");
assert.throws(() => shout(42), { name: "TypeError", message: "expected a string, got number" });
assert.throws(() => (isFishLie(bird) ? bird.swim() : ""), TypeError); // the lie crashes at runtime
assert.equal(lengthOf("abc"), 0);
assert.equal(pick(true), "yes");
assert.equal(pick(false), 0);
assert.throws(readLate, TypeError); // late is 1 by now, and numbers have no toUpperCase
assert.deepEqual(withQuery("https://example.com/", ["en"]), ["https://example.com/?lang=en"]);
assert.deepEqual(scores, [90, 75]);
assert.deepEqual(truthy, [90]);
assert.equal(isPresent(0), true);
console.log("OK");
