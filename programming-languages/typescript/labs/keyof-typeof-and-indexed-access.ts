// Run from programming-languages/typescript/labs with: npx tsc -p . && node keyof-typeof-and-indexed-access.ts
// tsc proves the compile-time claims (every @ts-expect-error must hit an error); node checks the runtime ones.
import assert from "node:assert/strict";

// Equal<A, B> is true only when A and B are exactly the same type.
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Expect<T extends true> = T;

// The problem: an arbitrary string is not a key of the price list.
const prices = { apple: 0.5, banana: 0.25, cherry: 3 };

function priceOfLoose(name: string): number {
  // @ts-expect-error: not every string is a key of prices
  return prices[name];
}

// Level 1: typeof in a type position asks the checker for a value's type.
type Prices = typeof prices;
const runtimeTag = typeof prices; // the JavaScript operator: a string at runtime

type PricesShape = Expect<Equal<Prices, { apple: number; banana: number; cherry: number }>>;
type ApplePrice = Expect<Equal<typeof prices.apple, number>>; // a property path is allowed

// Level 2: keyof lists the property names as a union of literals.
type Fruit = keyof typeof prices;

function priceOf(name: Fruit): number {
  return prices[name];
}

// @ts-expect-error: "durian" is not on the price list
const durian = priceOf("durian");

type FruitIsUnion = Expect<Equal<Fruit, "apple" | "banana" | "cherry">>;
type StringIndexKeys = Expect<Equal<keyof { [name: string]: number }, string | number>>;

// Level 3: T[K] looks up the type of a property.
type User = { name: string; born: number; address: { city: string; zip: string } };

type City = User["address"]["city"];
type NameOrBorn = User["name" | "born"];
// @ts-expect-error: User has no email
type Email = User["email"];
// @ts-expect-error: a dot reads a namespace, not a property type
type DotName = User.name;

type CityIsString = Expect<Equal<City, string>>;
type NameOrBornIsUnion = Expect<Equal<NameOrBorn, string | number>>;

// Level 4: [number] gives the element type of an array or tuple.
const sizes = ["S", "M", "L"] as const;
type Size = (typeof sizes)[number];

function isSize(value: string): value is Size {
  const allowed: readonly string[] = sizes; // includes on the tuple itself would accept only a Size
  return allowed.includes(value);
}

type Pair = [string, number];

type SizeIsUnion = Expect<Equal<Size, "S" | "M" | "L">>;
type ScoreIsNumber = Expect<Equal<number[][number], number>>;
type PairElements = Expect<Equal<Pair[number], string | number>>;
type PairSecond = Expect<Equal<Pair[1], number>>;
type PairLength = Expect<Equal<Pair["length"], 2>>;
// @ts-expect-error: a pair has no third element
type PairThird = Pair[2];

// Level 5: a getter whose return type follows the key.
function get<T, K extends keyof T>(obj: T, key: K): T[K] {
  return obj[key];
}

const ada: User = { name: "Ada", born: 1815, address: { city: "London", zip: "W1" } };
const adaName = get(ada, "name");
const adaBorn = get(ada, "born");
// @ts-expect-error: "email" is not a key of User
const adaEmail = get(ada, "email");

type AdaNameIsString = Expect<Equal<typeof adaName, string>>;
type AdaBornIsNumber = Expect<Equal<typeof adaBorn, number>>;

// Level 6: derive types from one "as const" object instead of an enum.
const HttpStatus = { ok: 200, notFound: 404, serverError: 500 } as const;

type HttpStatusName = keyof typeof HttpStatus;
type HttpStatusCode = (typeof HttpStatus)[HttpStatusName];

function isError(code: HttpStatusCode): boolean {
  return code >= 400;
}

// @ts-expect-error: 418 is not one of the codes
const teapot = isError(418);

type NamesFromObject = Expect<Equal<HttpStatusName, "ok" | "notFound" | "serverError">>;
type CodesFromObject = Expect<Equal<HttpStatusCode, 200 | 404 | 500>>;

// Underneath: keyof turns unions into intersections and back.
type Book = { id: number; title: string };
type Film = { id: number; minutes: number };

type KeysOfEither = Expect<Equal<keyof (Book | Film), "id">>;
type KeysOfBoth = Expect<Equal<keyof (Book & Film), "id" | "title" | "minutes">>;

// Interview: Object.keys returns string[] because objects may carry extra keys.
const withDurian = { apple: 0.5, banana: 0.25, cherry: 3, durian: 9 };
const sameShape: Prices = withDurian; // allowed: extra keys are fine structurally
const keys = Object.keys(sameShape);

type KeysAreStrings = Expect<Equal<typeof keys, string[]>>;

// Interview: writing through a union key needs a value that fits every property.
function setField(user: User, key: "name" | "born", value: string): void {
  // @ts-expect-error: the target type is User["name"] & User["born"], which is never
  user[key] = value;
}

function setTyped<K extends keyof User>(user: User, key: K, value: User[K]): void {
  user[key] = value; // the value's type follows the key
}
// @ts-expect-error: "born" takes a number
const wrongSet = () => setTyped(ada, "born", "unknown");

// Runtime facts.
assert.equal(priceOfLoose("banana"), 0.25);
assert.equal(priceOfLoose("durian"), undefined);
assert.ok(Number.isNaN(priceOfLoose("apple") + priceOfLoose("durian"))); // 0.5 + undefined
assert.equal(runtimeTag, "object");
assert.equal(priceOf("cherry"), 3);
assert.equal(durian, undefined);
assert.equal(isSize("M"), true);
assert.equal(isSize("XL"), false);
assert.equal(adaName, "Ada");
assert.equal(adaBorn, 1815);
assert.equal(adaEmail, undefined);
assert.equal(isError(HttpStatus.notFound), true);
assert.equal(isError(HttpStatus.ok), false);
assert.equal(teapot, true);
assert.deepEqual(Object.values(HttpStatus), [200, 404, 500]);
assert.deepEqual(keys, ["apple", "banana", "cherry", "durian"]);
setTyped(ada, "born", 1816);
assert.equal(ada.born, 1816);
setField(ada, "born", "unknown");
assert.equal(ada.born, "unknown"); // the bug the error prevents: born is now a string
console.log("OK");
