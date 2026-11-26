// Run from programming-languages/typescript/labs with: npx tsc -p . && node generics.ts
// tsc proves the compile-time claims (every @ts-expect-error must hit an error); node checks the runtime ones.
import assert from "node:assert/strict";

// Equal<A, B> is true only when A and B are exactly the same type.
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Expect<T extends true> = T;

// The problem: any forgets what went in.
function firstAny(items: any[]): any {
  return items[0];
}
const crash = () => firstAny(["apple"]).toFixed(2); // compiles

// Level 1: a type parameter remembers it.
function first<T>(items: T[]): T | undefined {
  return items[0];
}

const word = first(["apple", "pear"]); // T = string
const count = first([3, 7]); // T = number
const explicit = first<number>([3, 7]); // T written by hand
const mixed = first([1, "a"]); // T = string | number

// @ts-expect-error: first(["apple"]) is a string or undefined, not a number
const rounded = Math.round(first(["apple"]));

type WordIsString = Expect<Equal<typeof word, string | undefined>>;
type CountIsNumber = Expect<Equal<typeof count, number | undefined>>;
type MixedIsUnion = Expect<Equal<typeof mixed, string | number | undefined>>;

// Level 2: "for every T" means the body may assume nothing about T...
function lengthOfAnything<T>(value: T): number {
  // @ts-expect-error: not every T has a length
  return value.length;
}

function notIdentity<T>(value: T): T {
  // @ts-expect-error: the caller picks T, so a number is not necessarily a T
  return 42;
}

// ...unless a constraint narrows "every T" to "every T that has a length".
function longest<T extends { length: number }>(a: T, b: T): T {
  return b.length > a.length ? b : a;
}

const longerText = longest("ab", "abc");
const longerList = longest([1, 2], [1]);
// @ts-expect-error: numbers have no length
const longerNumber = longest(10, 20);

// Level 3: keyof constraints tie one parameter to another.
function pluck<T, K extends keyof T>(items: T[], key: K): T[K][] {
  return items.map(item => item[key]);
}

const people = [
  { name: "Ada", born: 1815 },
  { name: "Alan", born: 1912 },
];
const names = pluck(people, "name");
const years = pluck(people, "born");
// @ts-expect-error: "email" is not a key of the people
const emails = pluck(people, "email");

type NamesAreStrings = Expect<Equal<typeof names, string[]>>;
type YearsAreNumbers = Expect<Equal<typeof years, number[]>>;

// Level 4: generic interfaces, classes and defaults.
interface Box<T> {
  value: T;
}
const boxed: Box<string> = { value: "Ada" };

class Stack<T> {
  #items: T[] = [];
  push(item: T): void {
    this.#items.push(item);
  }
  pop(): T | undefined {
    return this.#items.pop();
  }
}

const stack = new Stack<number>();
stack.push(1);
stack.push(2);
// @ts-expect-error: a Stack<number> takes only numbers
stack.push("three");

type ApiResponse<T = unknown> = { status: number; body: T };
const raw: ApiResponse = { status: 200, body: "ok" }; // T defaults to unknown
const typed: ApiResponse<string[]> = { status: 200, body: ["Ada"] };

type RawBodyIsUnknown = Expect<Equal<ApiResponse, { status: number; body: unknown }>>;

// Level 5: const type parameters (TypeScript 5.0) infer as if the argument were "as const".
function routesPlain<T extends readonly string[]>(paths: T): T {
  return paths;
}
function routesExact<const T extends readonly string[]>(paths: T): T {
  return paths;
}

const plain = routesPlain(["/", "/users"]);
const exact = routesExact(["/", "/users"]);

type PlainIsStringArray = Expect<Equal<typeof plain, string[]>>;
type ExactIsTuple = Expect<Equal<typeof exact, readonly ["/", "/users"]>>;

// Level 5: NoInfer (TypeScript 5.4) stops an argument from widening T.
function createLight<C extends string>(colours: C[], start: C): C {
  return start;
}
function createLightSafe<C extends string>(colours: C[], start: NoInfer<C>): C {
  return start;
}

const blue = createLight(["red", "green"], "blue"); // C grew to include "blue"
// @ts-expect-error: "blue" is not one of the colours
const blueSafe = createLightSafe(["red", "green"], "blue");

type BlueWidenedC = Expect<Equal<typeof blue, "red" | "green" | "blue">>;

// Underneath: one candidate per argument, and the first one wins when they clash.
function pair<T>(a: T, b: T): [T, T] {
  return [a, b];
}
// @ts-expect-error: T was inferred as number from the first argument
const clash = pair(1, "a");

// Underneath: type parameters are erased; there is no T at runtime.
function make<T>(): T {
  // @ts-expect-error: T is only a type
  return new T();
}

// Interview: a type parameter used once adds nothing.
function lengthOnce<T extends { length: number }>(value: T): number {
  return value.length;
}
function lengthPlain(value: { length: number }): number {
  return value.length;
}

// Runtime facts.
assert.throws(crash, { name: "TypeError", message: "firstAny(...).toFixed is not a function" });
assert.deepEqual([word, count, explicit, mixed], ["apple", 3, 3, 1]);
assert.equal(first([]), undefined);
assert.ok(Number.isNaN(rounded));
assert.equal(lengthOfAnything(42), undefined);
assert.equal(notIdentity("x"), 42);
assert.equal(longerText, "abc");
assert.deepEqual(longerList, [1, 2]);
assert.equal(longerNumber, 10);
assert.deepEqual(names, ["Ada", "Alan"]);
assert.deepEqual(years, [1815, 1912]);
assert.deepEqual(emails, [undefined, undefined]);
assert.equal(boxed.value, "Ada");
assert.equal(stack.pop(), "three"); // the rejected push ran: types are erased
assert.equal(stack.pop(), 2);
assert.deepEqual([raw.body, typed.body], ["ok", ["Ada"]]);
assert.deepEqual([plain, exact], [["/", "/users"], ["/", "/users"]]);
assert.equal(blue, "blue");
assert.equal(blueSafe, "blue");
assert.deepEqual(clash, [1, "a"]);
assert.throws(make, { name: "ReferenceError", message: "T is not defined" });
assert.equal(lengthOnce("abc"), lengthPlain("abc"));
assert.equal(first.toString(), "function first   (items     )                {\n  return items[0];\n}");
console.log("OK");
