// Run from programming-languages/typescript/labs with: npx tsc -p . && node structural-typing.ts
// tsc proves the compile-time claims (every @ts-expect-error must hit an error); node checks the runtime ones.
import assert from "node:assert/strict";

// Equal<A, B> is true only when A and B are exactly the same type.
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Expect<T extends true> = T;

// Level 1: a type is a set of values; "assignable" means "is a subset of".
const red: "red" = "red";
const colour: string = red; // {"red"} is inside the set of all strings
// @ts-expect-error: "three" is a string, and strings are not in the set of numbers
const count: number = "three";

const anything: unknown = 42; // unknown is the set of every value
// @ts-expect-error: never is the empty set, so no value fits
const nothing: never = 0;

// Level 2: the shape decides, not the name.
type Rect = { width: number; height: number };

function area(rect: Rect): number {
  return rect.width * rect.height;
}

const tile = { width: 3, height: 4, colour: "red" };
const tileArea = area(tile); // the extra colour does not matter

class Photo {
  width = 6;
  height = 4;
}
const photoArea = area(new Photo()); // Photo never mentions Rect

// @ts-expect-error: without a height the value is not a Rect
const strip = area({ width: 3 });

type Size = { width: number; height: number };
const size: Size = { width: 2, height: 5 };
const sizeAsRect: Rect = size; // different names, same shape

// Level 3: a fresh object literal gets an extra check for unknown properties.
// @ts-expect-error: the literal is fresh, so its extra colour is reported
const typo: Rect = { width: 3, height: 4, colour: "red" };
const viaVariable: Rect = tile; // tile is not fresh any more: no excess check

type Options = { title?: string; width?: number };
// @ts-expect-error: the check exists to catch typos like this one
const options: Options = { title: "Box", widht: 100 };

const colourOnly = { colour: "red" };
// @ts-expect-error: a type whose properties are all optional needs at least one in common
const weak: Options = colourOnly;

// Level 4: brands add a name that structure alone cannot see.
type UserId = number & { readonly brand: "UserId" };
type OrderId = number & { readonly brand: "OrderId" };

function toUserId(n: number): UserId {
  return n as UserId; // the one place allowed to mint a UserId
}
function toOrderId(n: number): OrderId {
  return n as OrderId;
}

function findUser(id: UserId): string {
  return id === 7 ? "Ada" : "unknown";
}

const ada = findUser(toUserId(7));
// @ts-expect-error: an OrderId is not a UserId
const wrongUser = findUser(toOrderId(7));
// @ts-expect-error: a plain number is not a UserId either
const plainNumber = findUser(7);

// Level 5: types are erased; the running program never sees them.
function double(n: number): number {
  return n * 2;
}

const missingRect = () => {
  // @ts-expect-error: Rect exists only in the type world
  return tile instanceof Rect;
};

// Interview: classes with a #private field are compared by declaration, not by shape.
class Celsius {
  #unit = "C";
  value: number;
  constructor(value: number) {
    this.value = value;
  }
}
class Fahrenheit {
  #unit = "F";
  value: number;
  constructor(value: number) {
    this.value = value;
  }
}
// @ts-expect-error: the two #unit fields are different members
const boiling: Celsius = new Fahrenheit(212);

// Interview: {} means "any value except null and undefined".
const notNullish: {} = 42;
// @ts-expect-error: null is outside {}
const nullValue: {} = null;

// Type-level facts.
type TileKeepsItsColour = Expect<Equal<typeof tile, { width: number; height: number; colour: string }>>;
type RedIsALiteral = Expect<Equal<typeof red, "red">>;
type BrandIsInTheType = Expect<Equal<UserId, number & { readonly brand: "UserId" }>>;

// Runtime facts.
assert.equal(colour, "red");
assert.equal(anything, 42);
assert.equal(tileArea, 12);
assert.equal(photoArea, 24);
assert.ok(Number.isNaN(strip)); // 3 * undefined is NaN: the bug the type check prevents
assert.equal(area(sizeAsRect), 10);
assert.equal(area(viaVariable), 12);
assert.deepEqual(Object.keys(viaVariable), ["width", "height", "colour"]); // the colour is still there
assert.equal(ada, "Ada");
assert.equal(wrongUser, "Ada"); // nothing stops the wrong id at runtime
assert.equal(plainNumber, "Ada");
assert.equal(typeof toUserId(7), "number"); // the brand left no trace
assert.equal(double.toString(), "function double(n        )         {\n  return n * 2;\n}");
assert.equal("function double(n: number): number {".length, 36);
assert.equal(double.toString().split("\n")[0].length, 36); // same columns as the source
assert.throws(missingRect, { name: "ReferenceError", message: "Rect is not defined" });
assert.equal(boiling.value, 212);
assert.equal(notNullish, 42);
assert.deepEqual([count, nothing, typo.width, options.title, weak, nullValue], ["three", 0, 3, "Box", colourOnly, null]);
console.log("OK");
