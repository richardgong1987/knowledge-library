// Run from programming-languages/typescript/labs with: npx tsc -p . && node any-unknown-and-runtime-validation.ts
import assert from 'node:assert/strict';

// Type-level test helpers: Equal is true only when A and B are the same type.
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Expect<T extends true> = T;
type Assignable<From, To> = [From] extends [To] ? true : false;

// The problem: the compiler trusts JSON it has never seen.
type User = { id: number; name: string };
const body = '{"id": "42", "name": "Ada"}';

const user: User = JSON.parse(body); // compiles: JSON.parse returns any
assert.equal(typeof user.id, 'string');
assert.throws(() => user.id.toFixed(2), { name: 'TypeError', message: 'user.id.toFixed is not a function' });

// Level 1: unknown is the top, never is the bottom, any breaks both rules.
type Level1 = [
  Expect<Equal<Assignable<string, unknown>, true>>,
  Expect<Equal<Assignable<unknown, string>, false>>,
  Expect<Equal<Assignable<never, string>, true>>,
  Expect<Equal<Assignable<string, never>, false>>,
  Expect<Equal<Assignable<any, string>, true>>,
  Expect<Equal<Assignable<string, any>, true>>,
];

type Level1Algebra = [
  Expect<Equal<string | unknown, unknown>>,
  Expect<Equal<string & unknown, string>>,
  Expect<Equal<string | never, string>>,
  Expect<Equal<string & never, never>>,
];

let parcel: unknown = 'hello';
parcel = 42; // anything fits into unknown
// @ts-expect-error Type 'unknown' is not assignable to type 'number'.
const count: number = parcel;

const anything: any = 'hello';
const flag: boolean = anything; // any flows into every type, even the wrong one
// @ts-expect-error Type 'any' is not assignable to type 'never'.
const impossible: never = anything;
assert.equal(typeof flag, 'string');

function fail(message: string): never {
  throw new Error(message);
}

function parsePort(text: string): number {
  const port = Number(text);
  return Number.isInteger(port) ? port : fail(`not a port: ${text}`);
}

assert.equal(parsePort('8080'), 8080);
assert.throws(() => parsePort('http'), { message: 'not a port: http' });

// Level 2: unknown must be narrowed before use.
function describe(value: unknown): string {
  if (typeof value === 'string') return `text of length ${value.length}`;
  if (typeof value === 'number') return `number ${value.toFixed(1)}`;
  return 'something else';
}

assert.equal(describe('Ada'), 'text of length 3');
assert.equal(describe(2), 'number 2.0');
assert.equal(describe(null), 'something else');

function shout(value: unknown): string {
  // @ts-expect-error 'value' is of type 'unknown'.
  return value.toUpperCase();
}
assert.throws(() => shout(42), TypeError);

// Level 3: `as` and `!` change the type, never the value.
const asserted = JSON.parse(body) as User;
assert.equal(typeof asserted.id, 'string');

// @ts-expect-error Conversion of type 'string' to type 'number' may be a mistake
const refused = 'hello' as number;
const forced = 'hello' as unknown as number;
assert.equal(typeof forced, 'string');

const ages = new Map([['ada', 36]]);
assert.equal(ages.get('ada')!, 36);
assert.throws(() => ages.get('alan')!.toFixed(), {
  message: "Cannot read properties of undefined (reading 'toFixed')",
});

// any is contagious; unknown stops it.
const parsed = JSON.parse(body);
const id = parsed.id;
type Level3 = [Expect<Equal<typeof parsed, any>>, Expect<Equal<typeof id, any>>];

const contained: unknown = JSON.parse(body);
// @ts-expect-error 'contained' is of type 'unknown'.
assert.equal(contained.id, '42');

// Level 4: check the value, then let the types follow.
function isUser(value: unknown): value is User {
  return typeof value === 'object' && value !== null
    && 'id' in value && typeof value.id === 'number'
    && 'name' in value && typeof value.name === 'string';
}

assert.equal(isUser({ id: 1, name: 'Ada' }), true);
assert.equal(isUser(JSON.parse(body)), false);

function hasId(value: object): boolean {
  if ('id' in value) {
    type NarrowedByIn = Expect<Equal<typeof value, object & Record<'id', unknown>>>; // TypeScript 4.9
    return true;
  }
  return false;
}
assert.equal(hasId({ id: 1 }), true);

class InvalidUserError extends Error {
  name = 'InvalidUserError';
}

function parseUser(json: string): User {
  const value: unknown = JSON.parse(json);
  if (typeof value !== 'object' || value === null) throw new InvalidUserError('not an object');
  if (!('id' in value) || typeof value.id !== 'number') throw new InvalidUserError('id must be a number');
  if (!('name' in value) || typeof value.name !== 'string') throw new InvalidUserError('name must be a string');
  return { id: value.id, name: value.name };
}

assert.deepEqual(parseUser('{"id": 42, "name": "Ada", "admin": true}'), { id: 42, name: 'Ada' });
assert.throws(() => parseUser(body), { name: 'InvalidUserError', message: 'id must be a number' });
assert.throws(() => parseUser('[1, 2]'), { message: 'id must be a number' });
assert.throws(() => parseUser('null'), { message: 'not an object' });

// Since TypeScript 5.5, a simple check like this is inferred as a predicate.
const isNumber = (x: unknown) => typeof x === 'number';
type InferredPredicate = Expect<Equal<typeof isNumber, (x: unknown) => x is number>>;
const numbers = [1, 'two', 3].filter(isNumber);
type FilteredNumbers = Expect<Equal<typeof numbers, number[]>>;
assert.deepEqual(numbers, [1, 3]);

// A type predicate is a promise the compiler does not check.
function isUserLie(value: unknown): value is User {
  return typeof value === 'object';
}
const notReallyUser: unknown = {};
assert.equal(isUserLie(notReallyUser) && notReallyUser.name, undefined);

// Level 5: a brand remembers that a check happened.
class InvalidEmailError extends Error {
  name = 'InvalidEmailError';
}

type Email = string & { readonly __brand: 'Email' };

function parseEmail(text: string): Email {
  if (!/^[^@\s]+@[^@\s]+$/.test(text)) throw new InvalidEmailError(text);
  return text as Email;
}

function sendWelcome(to: Email): string {
  return `welcome mail to ${to}`;
}

assert.equal(sendWelcome(parseEmail('ada@example.com')), 'welcome mail to ada@example.com');
assert.throws(() => parseEmail('ada.example.com'), InvalidEmailError);
const typed: string = 'ada.example.com';
// @ts-expect-error Argument of type 'string' is not assignable to parameter of type 'Email'.
assert.equal(sendWelcome(typed), 'welcome mail to ada.example.com');

// Level 6: satisfies checks a value but keeps its own type; as only overrides it.
type Config = { port: number; host: string };

const assertedConfig = { port: 8080 } as Config; // compiles: host is missing
assert.equal(assertedConfig.host, undefined);
// @ts-expect-error Property 'host' is missing in type '{ port: number; }' but required in type 'Config'.
const checkedConfig = { port: 8080 } satisfies Config;

const routes = { home: '/', user: '/users/:id' } satisfies Record<string, string>;
const annotated: Record<string, string> = { home: '/', user: '/users/:id' };
type Level6 = Expect<Equal<typeof routes, { home: string; user: string }>>;
// @ts-expect-error Property 'nope' does not exist on type '{ home: string; user: string; }'.
assert.equal(routes.nope, undefined);
assert.equal(annotated.nope, undefined); // compiles: every string key is "present"

console.log('OK');
