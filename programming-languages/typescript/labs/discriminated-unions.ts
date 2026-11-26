// Run from programming-languages/typescript/labs with: npx tsc -p . && node discriminated-unions.ts
// tsc proves the compile-time claims (every @ts-expect-error must hit an error); node checks the runtime ones.
import assert from "node:assert/strict";

// Equal<A, B> is true only when A and B are exactly the same type.
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Expect<T extends true> = T;

// The problem: three independent fields allow states that make no sense.
type LooseState = { isLoading: boolean; data?: string; error?: string };
const nonsense: LooseState = { isLoading: true, data: "Ada", error: "timeout" }; // compiles

// Level 1: one tag, one shape per case.
type RequestState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; data: string }
  | { status: "failure"; error: string };

// @ts-expect-error: a success has no error property
const confused: RequestState = { status: "success", data: "Ada", error: "timeout" };

function render(state: RequestState): string {
  if (state.status === "success") return `Hello, ${state.data}`; // data exists only here
  if (state.status === "failure") return `Error: ${state.error}`;
  type Here = Expect<Equal<typeof state, { status: "idle" } | { status: "loading" }>>;
  return state.status;
}

const halfBuilt = { status: "success" } as RequestState; // "as" lets the missing data through

function dataOf(state: RequestState): string {
  // @ts-expect-error: only the success case has data
  return state.data;
}

// Level 2: a switch with a never check proves every case is handled.
function assertNever(value: never): never {
  throw new Error(`Unexpected value: ${JSON.stringify(value)}`);
}

function label(state: RequestState): string {
  switch (state.status) {
    case "idle":
      return "Not started";
    case "loading":
      return "Loading";
    case "success":
      return `Got ${state.data}`;
    case "failure":
      return `Failed: ${state.error}`;
    default:
      return assertNever(state); // state: never, because every case returned
  }
}

type WithCancelled = RequestState | { status: "cancelled" };

function isFinished(state: WithCancelled): boolean {
  switch (state.status) {
    case "idle":
    case "loading":
      return false;
    case "success":
    case "failure":
      return true;
    default:
      // @ts-expect-error: "cancelled" was forgotten, so state is not never here
      return assertNever(state);
  }
}

// Level 3: a state machine. Each transition builds a whole new state.
type RequestEvent =
  | { type: "fetch" }
  | { type: "resolve"; data: string }
  | { type: "reject"; error: string };

function transition(state: RequestState, event: RequestEvent): RequestState {
  switch (state.status) {
    case "idle":
    case "failure":
      return event.type === "fetch" ? { status: "loading" } : state;
    case "loading":
      if (event.type === "resolve") return { status: "success", data: event.data };
      if (event.type === "reject") return { status: "failure", error: event.error };
      return state;
    case "success":
      return state; // finished: every event is ignored
  }
}

function run(events: RequestEvent[]): RequestState {
  const start: RequestState = { status: "idle" };
  return events.reduce(transition, start);
}

// @ts-expect-error: without a case for "cancelled" the end of the function is reachable
function stepCount(state: WithCancelled): number {
  switch (state.status) {
    case "idle":
      return 0;
    case "loading":
      return 1;
    case "success":
    case "failure":
      return 2;
  }
}

// Level 4: a Result type puts the failure in the return type.
type Result<T, E> = { ok: true; value: T } | { ok: false; error: E };

function parseAge(text: string): Result<number, string> {
  const age = Number(text);
  if (!Number.isInteger(age) || age < 0) {
    return { ok: false, error: `not an age: "${text}"` };
  }
  return { ok: true, value: age };
}

function nextYear(text: string): string {
  const parsed = parseAge(text);
  if (!parsed.ok) return parsed.error; // the caller cannot forget this branch and read value
  return `next year: ${parsed.value + 1}`;
}

function valueOf(parsed: Result<number, string>): number {
  // @ts-expect-error: value exists only when ok is true
  return parsed.value;
}

// Interview: the tag must be a literal type; a plain string tag does not narrow.
type Untagged = { kind: string; radius: number } | { kind: string; side: number };

function radiusOf(shape: Untagged): number {
  if (shape.kind === "circle") {
    // @ts-expect-error: kind is string in both members, so nothing was narrowed
    return shape.radius;
  }
  return 0;
}

// Runtime facts.
assert.deepEqual(nonsense, { isLoading: true, data: "Ada", error: "timeout" });
assert.equal(confused.status, "success");
assert.equal(render({ status: "success", data: "Ada" }), "Hello, Ada");
assert.equal(render({ status: "failure", error: "timeout" }), "Error: timeout");
assert.equal(render({ status: "idle" }), "idle");
assert.equal(render(halfBuilt), "Hello, undefined");
assert.equal(dataOf({ status: "loading" }), undefined);
assert.equal(label({ status: "success", data: "Ada" }), "Got Ada");
assert.equal(label({ status: "loading" }), "Loading");
assert.equal(isFinished({ status: "failure", error: "timeout" }), true);
assert.throws(() => isFinished({ status: "cancelled" }), { message: 'Unexpected value: {"status":"cancelled"}' });
assert.deepEqual(transition({ status: "idle" }, { type: "fetch" }), { status: "loading" });
assert.deepEqual(run([{ type: "fetch" }, { type: "resolve", data: "Ada" }]), { status: "success", data: "Ada" });
assert.deepEqual(run([{ type: "fetch" }, { type: "reject", error: "timeout" }, { type: "fetch" }]), { status: "loading" });
assert.deepEqual(run([{ type: "resolve", data: "Ada" }]), { status: "idle" }); // resolve before fetch is ignored
assert.equal(stepCount({ status: "cancelled" }), undefined);
assert.deepEqual(parseAge("42"), { ok: true, value: 42 });
assert.deepEqual(parseAge("-1"), { ok: false, error: 'not an age: "-1"' });
assert.equal(nextYear("42"), "next year: 43");
assert.equal(nextYear("abc"), 'not an age: "abc"');
assert.equal(valueOf(parseAge("abc")), undefined);
assert.equal(radiusOf({ kind: "circle", radius: 2 }), 2);
console.log("OK");
