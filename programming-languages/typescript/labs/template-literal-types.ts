// Run from programming-languages/typescript/labs with: npx tsc -p . && node template-literal-types.ts
import assert from 'node:assert/strict';

// Type-level test helpers: Equal is true only when A and B are the same type.
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Expect<T extends true> = T;

// Level 1: a template literal type is a pattern for strings.
type Greeting = `hello ${string}`;
type Pixels = `${number}px`;

const greeting: Greeting = 'hello world';
const width: Pixels = '12px';
// @ts-expect-error Type '"hi world"' is not assignable to type '`hello ${string}`'.
const rude: Greeting = 'hi world';
// @ts-expect-error Type '"twelve px"' is not assignable to type '`${number}px`'.
const vague: Pixels = 'twelve px';
assert.equal(`${greeting} ${width}`, 'hello world 12px');
assert.equal(rude + vague, 'hi worldtwelve px'); // the types are erased; any string still runs

// Level 2: unions in the holes expand into every combination.
const sizes = ['sm', 'md', 'lg'] as const;
const colors = ['red', 'blue'] as const;
type Size = (typeof sizes)[number];
type Color = (typeof colors)[number];
type ButtonClass = `btn-${Size}-${Color}`;

type Level2 = Expect<Equal<ButtonClass,
  'btn-sm-red' | 'btn-sm-blue' | 'btn-md-red' | 'btn-md-blue' | 'btn-lg-red' | 'btn-lg-blue'>>;

const allClasses: ButtonClass[] = sizes.flatMap(size => colors.map(color => `btn-${size}-${color}` as const));
assert.equal(allClasses.length, sizes.length * colors.length);
assert.deepEqual(allClasses.slice(0, 2), ['btn-sm-red', 'btn-sm-blue']);

type Digit = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9';
type FourDigits = `${Digit}${Digit}${Digit}${Digit}`; // 10 ** 4 = 10,000 members
// @ts-expect-error Expression produces a union type that is too complex to represent.
type FiveDigits = `${Digit}${Digit}${Digit}${Digit}${Digit}`; // 10 ** 5 = 100,000 members
const pin: FourDigits = '0420';
assert.equal(pin.length, 4);

// Level 3: four intrinsic types change letter case.
type Level3 = [
  Expect<Equal<Uppercase<'ada'>, 'ADA'>>,
  Expect<Equal<Lowercase<'ADA'>, 'ada'>>,
  Expect<Equal<Capitalize<'click'>, 'Click'>>,
  Expect<Equal<Uncapitalize<'Click'>, 'click'>>,
  Expect<Equal<Capitalize<'click' | 'focus'>, 'Click' | 'Focus'>>,
];

function capitalize<S extends string>(word: S): Capitalize<S> {
  return (word.charAt(0).toUpperCase() + word.slice(1)) as Capitalize<S>;
}

const handlerName = `on${capitalize('click')}` as const;
type Level3Handler = Expect<Equal<typeof handlerName, 'onClick'>>;
assert.equal(handlerName, 'onClick');

// Level 4: key remapping turns property names into method names.
type Getters<T> = { [K in keyof T as `get${Capitalize<K & string>}`]: () => T[K] };
type Person = { name: string; age: number };
type Level4 = Expect<Equal<Getters<Person>, { getName: () => string; getAge: () => number }>>;

// @ts-expect-error Type 'K' does not satisfy the constraint 'string'.
type BrokenGetters<T> = { [K in keyof T as `get${Capitalize<K>}`]: () => T[K] };

function makeGetters<T extends object>(source: T): Getters<T> {
  const entries = Object.entries(source).map(([key, value]) => [`get${capitalize(key)}`, () => value]);
  return Object.fromEntries(entries) as Getters<T>;
}

const getters = makeGetters({ name: 'Ada', age: 36 });
assert.equal(getters.getName(), 'Ada');
assert.equal(getters.getAge(), 36);

// Level 5: infer reads pieces back out of a string type.
type SplitOnce<S extends string> = S extends `${infer Head}/${infer Tail}` ? [Head, Tail] : [S];
type HeadTail<S extends string> = S extends `${infer Head}${infer Tail}` ? [Head, Tail] : never;

type Level5Split = [
  Expect<Equal<SplitOnce<'a/b/c'>, ['a', 'b/c']>>,
  Expect<Equal<SplitOnce<'abc'>, ['abc']>>,
  Expect<Equal<HeadTail<'abc'>, ['a', 'bc']>>,
  Expect<Equal<HeadTail<'😀abc'>, ['😀', 'abc']>>, // TypeScript 7: one code point, not half of one
];

assert.equal([...'😀abc'][0], '😀'); // for...of and spread also walk code points
assert.notEqual('😀abc'[0], '😀'); // indexing returns one UTF-16 code unit

type RouteParams<P extends string> =
  P extends `${string}:${infer Param}/${infer Rest}`
    ? Param | RouteParams<Rest>
    : P extends `${string}:${infer Param}`
      ? Param
      : never;

type Level5Route = [
  Expect<Equal<RouteParams<'/users/:id'>, 'id'>>,
  Expect<Equal<RouteParams<'/users/:id/posts/:postId'>, 'id' | 'postId'>>,
  Expect<Equal<RouteParams<'/about'>, never>>,
];

type Params<P extends string> = { [K in RouteParams<P>]: string };

function matchRoute<P extends string>(pattern: P, path: string): Params<P> | undefined {
  const patternParts = pattern.split('/');
  const pathParts = path.split('/');
  if (patternParts.length !== pathParts.length) return undefined;
  const params: Record<string, string> = {};
  for (const [i, part] of patternParts.entries()) {
    if (part.startsWith(':')) params[part.slice(1)] = pathParts[i];
    else if (part !== pathParts[i]) return undefined;
  }
  return params as Params<P>;
}

const params = matchRoute('/users/:id/posts/:postId', '/users/7/posts/42');
assert.deepEqual(params, { id: '7', postId: '42' });
assert.equal(matchRoute('/users/:id', '/teams/7'), undefined);
// @ts-expect-error Property 'postid' does not exist on type 'Params<"/users/:id/posts/:postId">'.
const typo = params?.postid;
assert.equal(typo, undefined);

console.log('OK');
