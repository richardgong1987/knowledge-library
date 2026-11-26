// Run from programming-languages/typescript/labs with: npx tsc -p . && node inference-and-literal-types.ts
// tsc proves the compile-time claims (every @ts-expect-error must hit an error); node checks the runtime ones.
import assert from "node:assert/strict";

// Equal<A, B> is true only when A and B are exactly the same type.
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Expect<T extends true> = T;

// The problem: a method read from a plain object is just a string.
type Method = "GET" | "POST";

function request(url: string, method: Method): string {
  return `${method} ${url}`;
}

const settings = { url: "/users", method: "GET" };
// @ts-expect-error: settings.method was widened to string
const failed = request(settings.url, settings.method);

// Level 1: the initialiser decides the type, and the type then stays fixed.
let count = 3;
// @ts-expect-error: count is a number for the rest of its life
count = "three";

function half(n: number) {
  return n / 2;
}

function zero() {
  return 0; // a single literal is widened: the result is number, not 0
}

function sign(n: number) {
  if (n > 0) return "positive";
  if (n < 0) return "negative";
  return "zero";
}

type CountIsNumber = Expect<Equal<typeof count, number>>;
type HalfReturnsNumber = Expect<Equal<ReturnType<typeof half>, number>>;
type ZeroReturnsNumber = Expect<Equal<ReturnType<typeof zero>, number>>;
type SignReturnsUnion = Expect<Equal<ReturnType<typeof sign>, "positive" | "negative" | "zero">>;

// Level 2: const keeps the literal; let widens it.
const fixed = "GET"; // can never change, so its type is exactly "GET"
let changeable = "GET"; // may become any string later, so its type is string
const list = [1, 2, 3]; // elements can be replaced, so number[]

type FixedIsLiteral = Expect<Equal<typeof fixed, "GET">>;
type ChangeableIsString = Expect<Equal<typeof changeable, string>>;
type SettingsAreWidened = Expect<Equal<typeof settings, { url: string; method: string }>>;
type ListIsNumberArray = Expect<Equal<typeof list, number[]>>;

// Level 3: as const freezes the types (not the object).
const frozenSettings = { url: "/users", method: "GET" } as const;
const sent = request(frozenSettings.url, frozenSettings.method); // "GET" fits Method

const point = [3, 4] as const;
// @ts-expect-error: a readonly tuple has no push
point.push(5);

type FrozenSettings = Expect<Equal<typeof frozenSettings, { readonly url: "/users"; readonly method: "GET" }>>;
type PointIsReadonlyTuple = Expect<Equal<typeof point, readonly [3, 4]>>;
type PointHasLengthTwo = Expect<Equal<(typeof point)["length"], 2>>;

const draft = { method: "GET" } as const;
// @ts-expect-error: readonly is checked by tsc only
draft.method = "POST";

// Level 4: the expected type flows into an expression (contextual typing).
type Formatter = (amount: number) => string;
const euros: Formatter = amount => `€${amount.toFixed(2)}`; // amount: number from Formatter

const names = ["Ada", "Alan"];
const lengths = names.map(name => name.length); // name: string from the array

// @ts-expect-error: with no expected type, the parameter has nothing to go on
const bare = amount => amount;

const typedSettings: { url: string; method: Method } = { url: "/users", method: "GET" };

// Level 4: an array literal gets the best common type of its elements.
class Animal {
  name = "animal";
}
class Dog extends Animal {
  bark = "woof";
}
class Cat extends Animal {
  meow = "meow";
}

const mixed = [1, "two", null];
const pets = [new Dog(), new Animal()]; // Dog is a subtype of Animal, so it is absorbed
const zoo = [new Dog(), new Cat()]; // neither absorbs the other: a union, not Animal[]
// @ts-expect-error: an Animal is neither a Dog nor a Cat
zoo.push(new Animal());

type MixedArray = Expect<Equal<typeof mixed, (string | number | null)[]>>;
type PetsAreAnimals = Expect<Equal<typeof pets, Animal[]>>;
type ZooIsAUnion = Expect<Equal<typeof zoo, (Dog | Cat)[]>>;

// Level 5: satisfies checks a value against a type but keeps the inferred type.
type Route = { path: string; method: Method };

const routes = {
  users: { path: "/users", method: "GET" },
  login: { path: "/login", method: "POST" },
} satisfies Record<string, Route>;

const annotated: Record<string, Route> = {
  users: { path: "/users", method: "GET" },
};

// @ts-expect-error: routes knows its own keys
const missingRoute = routes.missing;
const missingAnnotated = annotated.missing; // allowed: any string key is a Route here

// @ts-expect-error: satisfies still checks every value
const badRoute = { path: "/upload", method: "PUT" } satisfies Route;

type UsersMethodIsGet = Expect<Equal<typeof routes.users.method, "GET">>;
type AnnotatedMethodIsMethod = Expect<Equal<(typeof annotated)["users"]["method"], Method>>;

// Interview: a const declared without an annotation has a "widening" literal type.
const hello = "hello";
let copied = hello;
const pinned: "hello" = "hello";
let copiedPinned = pinned;

type CopiedIsString = Expect<Equal<typeof copied, string>>;
type CopiedPinnedStaysLiteral = Expect<Equal<typeof copiedPinned, "hello">>;

// Interview: an annotated return type catches a typo that inference would accept.
type Sign = "positive" | "negative" | "zero";

function signChecked(n: number): Sign {
  // @ts-expect-error: "postive" is not one of the three signs
  if (n > 0) return "postive";
  if (n < 0) return "negative";
  return "zero";
}

// Interview: "as" is an assertion, not a check.
const unsafe = { path: "/x" } as Route; // compiles although method is missing
// @ts-expect-error: as still refuses two types that do not overlap at all
const nonsense = "/x" as number;

// Runtime facts.
assert.equal(failed, "GET /users"); // the value was fine; only its type was too wide
assert.equal(count, "three"); // the rejected assignment still ran: types are erased
assert.equal(half(9), 4.5);
assert.equal(zero(), 0);
assert.deepEqual([sign(5), sign(-2), sign(0)], ["positive", "negative", "zero"]);
assert.equal(sent, "GET /users");
assert.deepEqual(point, [3, 4, 5]); // push ran at runtime: readonly is not Object.freeze
assert.equal(Object.isFrozen(frozenSettings), false);
assert.equal(draft.method, "POST");
assert.equal(euros(9.5), "€9.50");
assert.deepEqual(lengths, [3, 4]);
assert.equal(bare(7), 7);
assert.equal(typedSettings.method, "GET");
assert.equal(pets.length + zoo.length, 5); // the rejected push ran too
assert.equal(missingRoute, undefined);
assert.equal(missingAnnotated, undefined); // typed as Route, but nothing is there
assert.equal(badRoute.method, "PUT");
assert.equal(copied + copiedPinned, "hellohello");
assert.equal(signChecked(1), "postive");
assert.equal(unsafe.method, undefined);
assert.equal(nonsense, "/x");
assert.deepEqual([fixed, changeable, list, mixed], ["GET", "GET", [1, 2, 3], [1, "two", null]]);
console.log("OK");
