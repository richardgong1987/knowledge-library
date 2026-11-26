// Run from programming-languages/typescript/labs with: npx tsc -p . && node mapped-and-utility-types.ts
import assert from 'node:assert/strict';

// Type-level test helpers: Equal is true only when A and B are the same type.
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Expect<T extends true> = T;

// The problem: a patch type copied by hand from User.
type User = { id: number; name: string; email: string };
type UserPatchByHand = { id?: number; name?: string; email?: string };

// Level 1: a mapped type walks over keyof T.
type Flags<T> = { [K in keyof T]: boolean };
type UserFlags = Flags<User>;
type Level1Flags = Expect<Equal<UserFlags, { id: boolean; name: boolean; email: boolean }>>;

type Copy<T> = { [K in keyof T]: T[K] };
type Level1Copy = Expect<Equal<Copy<User>, User>>;

// Level 2: modifiers add or remove ? and readonly.
type MyPartial<T> = { [K in keyof T]?: T[K] };
type MyRequired<T> = { [K in keyof T]-?: T[K] };
type MyReadonly<T> = { readonly [K in keyof T]: T[K] };
type Mutable<T> = { -readonly [K in keyof T]: T[K] };

type Level2 = [
  Expect<Equal<MyPartial<User>, Partial<User>>>,
  Expect<Equal<MyPartial<User>, UserPatchByHand>>,
  Expect<Equal<MyRequired<Partial<User>>, User>>,
  Expect<Equal<MyReadonly<User>, Readonly<User>>>,
  Expect<Equal<Mutable<Readonly<User>>, User>>,
];
type CopyKeepsOptional = Expect<Equal<Copy<Partial<User>>, Partial<User>>>;

const patch: MyPartial<User> = { name: 'Ada' };
assert.deepEqual(patch, { name: 'Ada' });

const ada: MyReadonly<User> = { id: 1, name: 'Ada', email: 'ada@example.com' };
// @ts-expect-error Cannot assign to 'id' because it is a read-only property.
ada.id = 2;
assert.equal(ada.id, 2); // readonly is erased: the write still happens at runtime

const frozen = Object.freeze({ id: 1 }); // returns Readonly<{ id: 1 }>
// @ts-expect-error Cannot assign to 'id' because it is a read-only property.
assert.throws(() => { frozen.id = 2; }, TypeError); // freeze is enforced by the runtime

// Level 3: Pick, Omit and Record are mapped types over a chosen key set.
type MyPick<T, K extends keyof T> = { [P in K]: T[P] };
type MyRecord<K extends keyof any, V> = { [P in K]: V };
type MyOmit<T, K extends keyof any> = MyPick<T, Exclude<keyof T, K>>;

type Level3 = [
  Expect<Equal<MyPick<User, 'id' | 'name'>, { id: number; name: string }>>,
  Expect<Equal<MyPick<User, 'id' | 'name'>, Pick<User, 'id' | 'name'>>>,
  Expect<Equal<MyOmit<User, 'email'>, Omit<User, 'email'>>>,
  Expect<Equal<MyRecord<'ada' | 'alan', number>, Record<'ada' | 'alan', number>>>,
];

function pick<T, K extends keyof T>(source: T, keys: K[]): Pick<T, K> {
  const result = {} as Pick<T, K>;
  for (const key of keys) result[key] = source[key];
  return result;
}

const card = pick({ id: 1, name: 'Ada', email: 'ada@example.com' }, ['id', 'name']);
assert.deepEqual(card, { id: 1, name: 'Ada' });

const scores: Record<'ada' | 'alan', number> = { ada: 90, alan: 85 };
// @ts-expect-error Property 'alan' is missing in type '{ ada: number; }'
const missing: Record<'ada' | 'alan', number> = { ada: 90 };
assert.equal(scores.ada + scores.alan, 175);
assert.equal(missing.alan, undefined);

type WideKeys = Expect<Equal<Record<string, number>, { [x: string]: number }>>;

// Omit does not check its keys: a typo removes nothing.
type Typo = Omit<User, 'nmae'>;
type TypoKeepsEverything = Expect<Equal<Typo, { id: number; name: string; email: string }>>;

type StrictOmit<T, K extends keyof T> = Omit<T, K>;
// @ts-expect-error Type '"nmae"' does not satisfy the constraint 'keyof User'.
type CaughtTypo = StrictOmit<User, 'nmae'>;

// Level 4: key remapping with `as` renames or drops keys.
type WithoutId<T> = { [K in keyof T as Exclude<K, 'id'>]: T[K] };
type StringFields<T> = { [K in keyof T as T[K] extends string ? K : never]: T[K] };

type Level4 = [
  Expect<Equal<WithoutId<User>, { name: string; email: string }>>,
  Expect<Equal<StringFields<User>, { name: string; email: string }>>,
  Expect<Equal<StringFields<{ id: number; done: boolean }>, {}>>,
];

// Level 5: homomorphic mapped types keep modifiers and shapes.
type Profile = { readonly id: number; nickname?: string };
type FlagsOfKeys<Keys extends PropertyKey> = { [K in Keys]: boolean };

type Level5 = [
  Expect<Equal<Flags<Profile>, { readonly id: boolean; nickname?: boolean }>>,
  Expect<Equal<FlagsOfKeys<keyof Profile>, { id: boolean; nickname: boolean }>>,
  Expect<Equal<Flags<[number, string]>, [boolean, boolean]>>,
  Expect<Equal<Flags<number[]>, boolean[]>>,
  Expect<Equal<Partial<number>, number>>,
  Expect<Equal<Partial<User | Profile>, Partial<User> | Partial<Profile>>>,
];

type PickKeepsModifiers = Expect<Equal<Pick<Profile, 'id' | 'nickname'>, { readonly id: number; nickname?: string }>>;

// Partial is shallow: only the top level becomes optional.
type Order = { id: number; address: { city: string; zip: string } };
// @ts-expect-error Property 'zip' is missing in type '{ city: string; }'
const shallow: Partial<Order> = { address: { city: 'Liverpool' } };
assert.equal(shallow.address?.zip, undefined);

console.log('OK');
