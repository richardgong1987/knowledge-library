// Run from programming-languages/typescript/labs with: npx tsc -p . && node variance-and-soundness.ts
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

// Type-level test helpers: Equal is true only when A and B are the same type.
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Expect<T extends true> = T;
type Assignable<From, To> = [From] extends [To] ? true : false;

type Animal = { name: string };
type Dog = { name: string; bark(): string };
type Cat = { name: string; meow(): string };

const rex: Dog = { name: 'Rex', bark: () => 'woof' };
const tom: Cat = { name: 'Tom', meow: () => 'meow' };

// The problem: a list of dogs used as a list of animals.
const dogs: Dog[] = [rex];
const animals: Animal[] = dogs; // allowed: mutable arrays are covariant
animals.push(tom); // allowed: Tom is an Animal
assert.equal(dogs.length, 2); // one array, two names for it
assert.throws(() => dogs[1].bark(), { name: 'TypeError', message: 'dogs[1].bark is not a function' });

// Level 1: a subtype has at least the members of its supertype.
type Level1 = [
  Expect<Equal<Assignable<Dog, Animal>, true>>,
  Expect<Equal<Assignable<Animal, Dog>, false>>,
];

const someAnimal: Animal = rex;
// @ts-expect-error Property 'bark' is missing in type 'Animal' but required in type 'Dog'.
const someDog: Dog = someAnimal;

// Level 2: read-only positions are covariant.
type Level2 = [
  Expect<Equal<Assignable<readonly Dog[], readonly Animal[]>, true>>,
  Expect<Equal<Assignable<readonly Animal[], readonly Dog[]>, false>>,
  Expect<Equal<Assignable<() => Dog, () => Animal>, true>>,
  Expect<Equal<Assignable<() => Animal, () => Dog>, false>>,
];

function names(list: readonly Animal[]): string[] {
  return list.map(animal => animal.name);
}

const kennel: readonly Dog[] = [rex];
assert.deepEqual(names(kennel), ['Rex']);

// Level 3: parameters are contravariant under strictFunctionTypes.
type Handler<T> = (value: T) => string;

const describeAnimal: Handler<Animal> = animal => `an animal called ${animal.name}`;
const describeDog: Handler<Dog> = dog => `${dog.name} says ${dog.bark()}`;

const forDogs: Handler<Dog> = describeAnimal; // safe: it only reads name
// @ts-expect-error Type 'Handler<Dog>' is not assignable to type 'Handler<Animal>'.
const forAnimals: Handler<Animal> = describeDog;

assert.equal(forDogs(rex), 'an animal called Rex');
assert.throws(() => forAnimals(tom), TypeError); // Tom cannot bark

// Level 4: method syntax is checked bivariantly.
type MethodHandler<T> = { handle(value: T): string };
type PropertyHandler<T> = { handle: (value: T) => string };

type Level4 = [
  Expect<Equal<Assignable<MethodHandler<Dog>, MethodHandler<Animal>>, true>>,
  Expect<Equal<Assignable<MethodHandler<Animal>, MethodHandler<Dog>>, true>>,
  Expect<Equal<Assignable<PropertyHandler<Dog>, PropertyHandler<Animal>>, false>>,
  Expect<Equal<Assignable<PropertyHandler<Animal>, PropertyHandler<Dog>>, true>>,
];

const dogMethod: MethodHandler<Dog> = { handle: describeDog };
const animalMethod: MethodHandler<Animal> = dogMethod; // allowed, and unsafe
assert.throws(() => animalMethod.handle(tom), TypeError);

// Level 5: in and out state the variance instead of letting tsc measure it.
interface Producer<out T> { make(): T }
interface Consumer<in T> { take(value: T): void }
interface Cell<in out T> { get(): T; set(value: T): void }
interface MeasuredCell<T> { get(): T; set(value: T): void }

type Level5 = [
  Expect<Equal<Assignable<Producer<Dog>, Producer<Animal>>, true>>,
  Expect<Equal<Assignable<Consumer<Animal>, Consumer<Dog>>, true>>,
  Expect<Equal<Assignable<Consumer<Dog>, Consumer<Animal>>, false>>,
  Expect<Equal<Assignable<Cell<Dog>, Cell<Animal>>, false>>,
  Expect<Equal<Assignable<Cell<Animal>, Cell<Dog>>, false>>,
  Expect<Equal<Assignable<MeasuredCell<Dog>, MeasuredCell<Animal>>, true>>,
];

// @ts-expect-error Type 'Sink<sub-T>' is not assignable to type 'Sink<super-T>' as implied by variance annotation.
interface Sink<out T> { put: (value: T) => void }

// Level 6: reading past the end of an array.
const scores: number[] = [90, 85];
const third = scores[2];
type Level6 = Expect<Equal<typeof third, number>>; // the type says number...
assert.throws(() => third.toFixed(1), TypeError); // ...the value is undefined

const safer = scores.at(2);
type Level6At = Expect<Equal<typeof safer, number | undefined>>;
assert.equal(safer, undefined);

// The lab type-checks a snippet twice: as configured, then with the flag on.
function typeErrors(source: string, flags: string[]): string {
  const dir = mkdtempSync(join(tmpdir(), 'variance-lab-'));
  writeFileSync(join(dir, 'snippet.ts'), source);
  const tsc = join(import.meta.dirname, 'node_modules', '.bin', 'tsc');
  const result = spawnSync(tsc, ['--noEmit', '--strict', ...flags, 'snippet.ts'], { cwd: dir, encoding: 'utf8' });
  rmSync(dir, { recursive: true });
  return result.stdout;
}

const indexSnippet = 'const scores: number[] = [90, 85];\nconst third = scores[2];\nthird.toFixed(1);\n';
assert.equal(typeErrors(indexSnippet, []), '');
assert.match(typeErrors(indexSnippet, ['--noUncheckedIndexedAccess']), /error TS18048: 'third' is possibly 'undefined'\./);

const handlerSnippet = 'type Animal = { name: string };\nconst dogOnly = (dog: Animal & { bark(): string }) => dog.bark();\nconst anyAnimal: (animal: Animal) => string = dogOnly;\n';
assert.match(typeErrors(handlerSnippet, []), /error TS2322/);
assert.equal(typeErrors(handlerSnippet, ['--strictFunctionTypes', 'false']), '');

console.log('OK');
