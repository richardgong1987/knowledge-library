// Run from programming-languages/javascript/labs with: node classes.mjs
import assert from 'node:assert/strict';

// The problem: the pre-2015 way, a constructor function plus its prototype.
function OldAccount(owner) {
  this.owner = owner;
  this.balance = 0; // public: anyone can write here
}
OldAccount.prototype.deposit = function (amount) {
  this.balance += amount;
  return this.balance;
};
const old = new OldAccount('Ada');
old.balance = -1000; // nothing stops this
assert.equal(old.deposit(10), -990);

// Level 1: class syntax, the same prototype machinery underneath.
class Point {
  constructor(x, y) {
    this.x = x;
    this.y = y;
  }
  distance() {
    return Math.hypot(this.x, this.y);
  }
}
const p = new Point(3, 4);
assert.equal(p.distance(), 5);
assert.equal(typeof Point, 'function'); // a class is a constructor function
assert.equal(Object.getPrototypeOf(p), Point.prototype);
assert.equal(Object.hasOwn(p, 'distance'), false); // methods live on the prototype
assert.deepEqual(Object.keys(Point.prototype), []); // ...and are not enumerable
assert.throws(() => Point(3, 4), {
  name: 'TypeError',
  message: "Class constructor Point cannot be invoked without 'new'",
});

// Level 2: fields are per instance; methods are shared.
class Counter {
  count = 0; // a field: one copy per instance
  increment() { // a method: one copy, on Counter.prototype
    this.count += 1;
    return this.count;
  }
}
const first = new Counter();
const second = new Counter();
first.increment();
assert.equal(first.count, 1);
assert.equal(second.count, 0);
assert.equal(first.increment, second.increment); // the very same function object

class Button {
  clicks = 0;
  onClick = () => { // an arrow field: a new function per instance, this fixed
    this.clicks += 1;
  };
}
const okButton = new Button();
const cancelButton = new Button();
assert.notEqual(okButton.onClick, cancelButton.onClick);
const detached = okButton.onClick;
detached(); // still works: the arrow captured this
assert.equal(okButton.clicks, 1);

// Level 3: static members and static blocks belong to the class itself.
class User {
  static count = 0; // one value, on the class
  static roles;
  static {
    User.roles = new Set(['admin', 'guest']); // runs once, when the class is evaluated
  }
  constructor(name) {
    this.name = name;
    User.count += 1;
    this.id = User.count;
  }
  static guest() { // a factory, called on the class
    return new User('guest');
  }
}
const ada = new User('Ada');
const visitor = User.guest();
assert.equal(visitor.id, 2);
assert.equal(User.count, 2);
assert.equal(ada.count, undefined); // statics are not on instances
assert.ok(User.roles.has('admin'));

// Level 4: private fields and methods, and accessors.
class Account {
  #balance = 0; // private: only code inside this class body can touch it
  constructor(owner) {
    this.owner = owner;
  }
  deposit(amount) {
    this.#check(amount);
    this.#balance += amount;
    return this.#balance;
  }
  get balance() { // an accessor: read like a property, and no setter
    return this.#balance;
  }
  #check(amount) { // a private method
    if (!(amount > 0)) throw new RangeError(`invalid amount: ${amount}`);
  }
  static isAccount(value) {
    return #balance in value; // a brand check (ES2022)
  }
}
const account = new Account('Ada');
account.deposit(100);
assert.equal(account.balance, 100);
assert.deepEqual(Object.keys(account), ['owner']); // #balance is not a property
assert.equal(JSON.stringify(account), '{"owner":"Ada"}');
assert.throws(() => {
  account.balance = 1e6;
}, { name: 'TypeError', message: 'Cannot set property balance of #<Account> which has only a getter' });
assert.throws(() => account.deposit(-5), { name: 'RangeError', message: 'invalid amount: -5' });
assert.equal(Account.isAccount(account), true);
assert.equal(Account.isAccount({ owner: 'Ada', balance: 100 }), false); // looks alike, no brand
assert.throws(() => Account.isAccount(5), { name: 'TypeError' }); // in needs an object

assert.throws(() => new Function('account', 'return account.#balance;'), {
  name: 'SyntaxError',
  message: "Private field '#balance' must be declared in an enclosing class",
});
assert.throws(() => Account.prototype.deposit.call({ owner: 'fake' }, 5), {
  name: 'TypeError',
  message: 'Receiver must be an instance of class Account',
});

// Level 5: extends and super.
class SavingsAccount extends Account {
  history = [];
  constructor(owner, rate) {
    super(owner); // Account's constructor creates this; only then can we use it
    this.rate = rate;
  }
  deposit(amount) { // override, then reuse the parent's version
    this.history.push(amount);
    return super.deposit(amount);
  }
  addInterest() {
    return this.deposit(this.balance * this.rate); // the getter is inherited too
  }
}
const savings = new SavingsAccount('Bo', 0.05);
savings.deposit(1000);
assert.equal(savings.addInterest(), 1050);
assert.deepEqual(savings.history, [1000, 50]);
assert.equal(Object.getPrototypeOf(SavingsAccount.prototype), Account.prototype); // methods
assert.equal(Object.getPrototypeOf(SavingsAccount), Account); // statics
assert.equal(SavingsAccount.isAccount(savings), true);

class BrokenSavings extends Account {
  constructor(owner) {
    this.rate = 0; // too early: this does not exist yet
    super(owner);
  }
}
assert.throws(() => new BrokenSavings('Cy'), {
  name: 'ReferenceError',
  message: "Must call super constructor in derived class before accessing 'this' or returning from derived constructor",
});

// Level 6: instanceof walks the prototype chain.
assert.ok(savings instanceof SavingsAccount); // one step up the chain
assert.ok(savings instanceof Account); // two steps
assert.ok(savings instanceof Object); // three steps
const impostor = Object.create(Account.prototype); // never ran the constructor
assert.ok(impostor instanceof Account); // instanceof only checks the chain
assert.equal(Account.isAccount(impostor), false); // the brand check is not fooled

class Even {
  static [Symbol.hasInstance](n) {
    return n % 2 === 0;
  }
}
assert.equal(4 instanceof Even, true); // instanceof asks Symbol.hasInstance first
assert.equal(5 instanceof Even, false);

// Level 7: mixins, and composition.
const Serializable = (Base) => class extends Base {
  serialize() {
    return JSON.stringify(this);
  }
};
const Timestamped = (Base) => class extends Base {
  createdAt = 0; // fixed for the lab; real code would use Date.now()
};
class Note {
  constructor(text) {
    this.text = text;
  }
}
class SmartNote extends Serializable(Timestamped(Note)) {}
assert.equal(new SmartNote('hi').serialize(), '{"text":"hi","createdAt":0}');

class Logger {
  lines = [];
  log(message) {
    this.lines.push(message);
  }
}
class Checkout {
  constructor(logger) {
    this.logger = logger; // has a logger, instead of being one
  }
  pay(amount) {
    this.logger.log(`paid ${amount}`);
  }
}
const logger = new Logger();
new Checkout(logger).pay(30);
assert.deepEqual(logger.lines, ['paid 30']);

console.log('OK');
