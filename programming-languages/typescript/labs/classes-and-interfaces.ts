// Run from programming-languages/typescript/labs with: npx tsc -p . && node classes-and-interfaces.ts
import assert from 'node:assert/strict';

// Type-level test helpers: Equal is true only when A and B are the same type.
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Expect<T extends true> = T;

// Level 1: an interface can be reopened; a type alias cannot.
interface Settings { theme: string }
interface Settings { fontSize: number } // merges into the declaration above

const settings: Settings = { theme: 'dark', fontSize: 14 };
assert.equal(settings.fontSize, 14);

// @ts-expect-error Duplicate identifier 'Pair'.
type Pair = { left: number };
// @ts-expect-error Duplicate identifier 'Pair'.
type Pair = { right: number };

// extends checks the override; & silently intersects it.
interface Animal { name: string; id: number }
// @ts-expect-error Interface 'Pet' incorrectly extends interface 'Animal'.
interface Pet extends Animal { id: string }
type PetAlias = Animal & { id: string };
type Level1 = Expect<Equal<PetAlias['id'], never>>;

// Level 2: a class is a value and a type; implements only checks.
interface Shape { area(): number }

class Circle implements Shape {
  radius: number;
  constructor(radius: number) {
    this.radius = radius;
  }
  area(): number {
    return Math.PI * this.radius ** 2;
  }
}

// @ts-expect-error Class 'Square' incorrectly implements interface 'Shape'.
class Square implements Shape { side = 2 }

const unit: Shape = new Circle(1);
assert.equal(unit.area(), Math.PI);
assert.equal(typeof Circle, 'function'); // the value: a constructor function
assert.equal(new Square().side, 2);

interface Validator { check(input: string): boolean }
class LengthValidator implements Validator {
  // @ts-expect-error Parameter 'input' implicitly has an 'any' type.
  check(input) {
    return input.length > 3;
  }
}
assert.equal(new LengthValidator().check('Ada'), false);

// Classes are compared by shape, not by name.
const fake: Circle = { radius: 2, area: () => 12 };
assert.equal(fake instanceof Circle, false); // the checker says Circle; the runtime disagrees

// Level 3: private is checked by tsc; #private is enforced by JavaScript.
class Account {
  readonly owner: string;
  private balance = 100;
  #pin = 1234;
  constructor(owner: string) {
    this.owner = owner;
  }
  checkPin(pin: number): boolean {
    return pin === this.#pin;
  }
  static isAccount(value: object): boolean {
    return #pin in value;
  }
}

const account = new Account('Ada');
// @ts-expect-error Property 'balance' is private and only accessible within class 'Account'.
account.balance = -1000;
// @ts-expect-error Cannot assign to 'owner' because it is a read-only property.
account.owner = 'Eve';
assert.equal(account['balance'], -1000); // the write happened, and brackets are allowed to read it
assert.deepEqual(Object.keys(account), ['owner', 'balance']); // #pin is not a property
assert.equal(JSON.stringify(account), '{"owner":"Eve","balance":-1000}');
assert.throws(() => eval('account.#pin'), {
  name: 'SyntaxError',
  message: "Private field '#pin' must be declared in an enclosing class",
});
assert.equal(account.checkPin(1234), true);
assert.equal(Account.isAccount(account), true);
assert.equal(Account.isAccount({ owner: 'Ada', balance: 100 }), false);

class Ledger {
  protected entries: string[] = [];
}
class AuditedLedger extends Ledger {
  record(entry: string): number {
    return this.entries.push(entry);
  }
}
const ledger = new AuditedLedger();
assert.equal(ledger.record('opened'), 1);
// @ts-expect-error Property 'entries' is protected and only accessible within class 'Ledger' and its subclasses.
assert.deepEqual(ledger.entries, ['opened']);

// A private member makes a class nominal.
class Meters {
  private value: number;
  constructor(value: number) {
    this.value = value;
  }
}
class Feet {
  private value: number;
  constructor(value: number) {
    this.value = value;
  }
}
// @ts-expect-error Types have separate declarations of a private property 'value'.
const height: Meters = new Feet(6);
assert.equal(height instanceof Meters, false);

// Level 4: an abstract class is a base with holes to fill.
abstract class Report {
  abstract body(): string;
  render(): string {
    return `# Report\n${this.body()}`;
  }
}

class SalesReport extends Report {
  body(): string {
    return 'sales: 3';
  }
}

// @ts-expect-error Non-abstract class 'EmptyReport' does not implement inherited abstract member body from class 'Report'.
class EmptyReport extends Report {}
// @ts-expect-error Cannot create an instance of an abstract class.
const blank = new Report();
assert.equal(new SalesReport().render(), '# Report\nsales: 3');
assert.equal(typeof blank.body, 'undefined'); // abstract is erased: the runtime made the object anyway
assert.equal(new EmptyReport() instanceof Report, true);

// Level 5: erasable replacements for enum, namespace and parameter properties.
const Direction = { Up: 'UP', Down: 'DOWN' } as const;
type Direction = (typeof Direction)[keyof typeof Direction];

function move(direction: Direction): string {
  return `moving ${direction}`;
}

type Level5 = Expect<Equal<Direction, 'UP' | 'DOWN'>>;
assert.equal(move(Direction.Up), 'moving UP');
assert.equal(move('DOWN'), 'moving DOWN');
// @ts-expect-error Argument of type '"LEFT"' is not assignable to parameter of type 'Direction'.
assert.equal(move('LEFT'), 'moving LEFT');

class Money {
  readonly cents: number;
  constructor(cents: number) {
    this.cents = cents; // what `constructor(readonly cents: number) {}` would generate
  }
}
assert.deepEqual(Object.keys(new Money(250)), ['cents']);

console.log('OK');
