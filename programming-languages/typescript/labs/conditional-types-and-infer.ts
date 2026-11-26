// Run from programming-languages/typescript/labs with: npx tsc -p . && node conditional-types-and-infer.ts
import assert from 'node:assert/strict';

// Type-level test helpers: Equal is true only when A and B are the same type.
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Expect<T extends true> = T;

// Level 1: a conditional type is an if-statement on types.
type IsString<T> = T extends string ? true : false;

type Level1 = [
  Expect<Equal<IsString<'ada'>, true>>,
  Expect<Equal<IsString<42>, false>>,
];

// Level 2: a naked type parameter distributes over a union.
type ToArray<T> = T extends unknown ? T[] : never;
type ToArrayWhole<T> = [T] extends [unknown] ? T[] : never;

type Level2 = [
  Expect<Equal<IsString<string | number>, boolean>>,
  Expect<Equal<ToArray<string | number>, string[] | number[]>>,
  Expect<Equal<ToArrayWhole<string | number>, (string | number)[]>>,
];

type MyExclude<T, U> = T extends U ? never : T;
type MyExtract<T, U> = T extends U ? T : never;

type Level2Filters = [
  Expect<Equal<MyExclude<'a' | 'b' | 'c', 'a'>, 'b' | 'c'>>,
  Expect<Equal<MyExtract<'a' | 'b' | 'c', 'a' | 'z'>, 'a'>>,
  Expect<Equal<MyExclude<'a' | 'b' | 'c', 'a'>, Exclude<'a' | 'b' | 'c', 'a'>>>,
];

// Level 3: infer names a part of the matched type.
type ElementOf<T> = T extends readonly (infer E)[] ? E : T;

type Level3Element = [
  Expect<Equal<ElementOf<string[]>, string>>,
  Expect<Equal<ElementOf<number>, number>>,
];

function firstOrSelf<T>(value: T): ElementOf<T> {
  return (Array.isArray(value) ? value[0] : value) as ElementOf<T>;
}

const fromList: string = firstOrSelf(['a', 'b']);
const fromValue: number = firstOrSelf(7);
assert.equal(fromList, 'a');
assert.equal(fromValue, 7);

function firstOrSelfUnchecked<T>(value: T): ElementOf<T> {
  // @ts-expect-error Type 'T' is not assignable to type 'ElementOf<T>'.
  return Array.isArray(value) ? value[0] : value;
}
assert.equal(firstOrSelfUnchecked([1, 2]), 1);

type MyReturnType<F> = F extends (...args: any[]) => infer R ? R : never;
type MyParameters<F> = F extends (...args: infer P) => any ? P : never;

function greet(name: string, times: number): string {
  return `hello ${name} `.repeat(times).trim();
}

type Level3 = [
  Expect<Equal<MyReturnType<typeof greet>, string>>,
  Expect<Equal<MyParameters<typeof greet>, [name: string, times: number]>>,
  Expect<Equal<MyReturnType<typeof greet>, ReturnType<typeof greet>>>,
  Expect<Equal<MyParameters<typeof greet>, Parameters<typeof greet>>>,
];

const args: MyParameters<typeof greet> = ['Ada', 2];
assert.equal(greet(...args), 'hello Ada hello Ada');

// With overloads, infer reads only the last signature.
function parse(input: string): number;
function parse(input: number): string;
function parse(input: string | number): number | string {
  return typeof input === 'string' ? Number(input) : String(input);
}

type LastOverloadWins = Expect<Equal<ReturnType<typeof parse>, string>>;
assert.equal(parse('42'), 42);

// Level 4: recursion unwraps nested types.
type MyAwaited<T> = T extends PromiseLike<infer V> ? MyAwaited<V> : T;

type Level4 = [
  Expect<Equal<MyAwaited<Promise<Promise<number>>>, number>>,
  Expect<Equal<MyAwaited<string>, string>>,
  Expect<Equal<MyAwaited<Promise<Promise<number>>>, Awaited<Promise<Promise<number>>>>>,
];

async function loadCount(): Promise<number> {
  return 3;
}

const count: MyAwaited<ReturnType<typeof loadCount>> = await loadCount();
assert.equal(count, 3);

type DeepReadonly<T> = T extends (...args: any[]) => any
  ? T
  : T extends object
    ? { readonly [K in keyof T]: DeepReadonly<T[K]> }
    : T;

type Order = { id: number; address: { city: string }; tags: string[] };
const order: DeepReadonly<Order> = { id: 1, address: { city: 'Liverpool' }, tags: ['new'] };
// @ts-expect-error Cannot assign to 'city' because it is a read-only property.
order.address.city = 'Leeds';
// @ts-expect-error Property 'push' does not exist on type 'readonly string[]'.
order.tags.push('paid');
assert.equal(order.address.city, 'Leeds'); // only the type was readonly
assert.deepEqual(order.tags, ['new', 'paid']);

// The tail-recursion limit is 1000 steps.
type BuildTuple<N extends number, Acc extends unknown[] = []> =
  Acc['length'] extends N ? Acc : BuildTuple<N, [...Acc, unknown]>;

type Level4Depth = Expect<Equal<BuildTuple<999>['length'], 999>>;
// @ts-expect-error Type instantiation is excessively deep and possibly infinite.
type TooDeep = BuildTuple<1000>;

// Level 5: never is the empty union, so distribution over it gives never.
type NaiveIsNever<T> = T extends never ? true : false;
type IsNever<T> = [T] extends [never] ? true : false;

type Level5 = [
  Expect<Equal<NaiveIsNever<never>, never>>,
  Expect<Equal<IsNever<never>, true>>,
  Expect<Equal<IsNever<string>, false>>,
  Expect<Equal<ToArray<never>, never>>,
];

// any matches both branches; boolean is the union true | false.
type Level5Surprises = [
  Expect<Equal<IsString<any>, boolean>>,
  Expect<Equal<ToArray<boolean>, false[] | true[]>>,
];

// 1 & T is never for every T except any, so 0 extends 1 & T detects any.
type IsAny<T> = 0 extends 1 & T ? true : false;
type Level5Any = [Expect<Equal<IsAny<any>, true>>, Expect<Equal<IsAny<unknown>, false>>, Expect<Equal<IsAny<string>, false>>];

console.log('OK');
