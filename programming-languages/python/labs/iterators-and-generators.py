# Run from programming-languages/python/labs with: python3 -I iterators-and-generators.py
import dis
import sys
from inspect import getgeneratorstate
from itertools import accumulate, batched, chain, count, islice, takewhile


def error_text(action):
    try:
        action()
    except Exception as error:
        return f"{type(error).__name__}: {error}"


# Level 1: the iteration protocol is iter() once, then next() until StopIteration.
def walk_by_hand():
    it = iter([10, 20])
    first = next(it)
    second = next(it)
    try:
        next(it)
    except StopIteration:
        return first, second, "StopIteration"


def for_loop_by_hand(iterable):
    seen = []
    it = iter(iterable)  # for calls iter() once ...
    while True:
        try:
            item = next(it)  # ... then next() until StopIteration
        except StopIteration:
            break
        seen.append(item)
    return seen


class Countdown:
    def __init__(self, start):
        self.current = start

    def __iter__(self):
        return self  # an iterator is its own iterable

    def __next__(self):
        if self.current == 0:
            raise StopIteration
        self.current -= 1
        return self.current + 1


# Level 2: a generator function pauses at each yield.
def countdown(start):
    while start > 0:
        yield start  # pause here; resume on the next next()
        start -= 1


def two_steps(log):
    log.append("started")
    yield 1
    log.append("resumed")
    yield 2
    log.append("finished")


def run_two_steps():
    log = []
    gen = two_steps(log)
    log.append("created")  # nothing in the body has run yet
    log.append(next(gen))
    log.append(next(gen))
    log.append(next(gen, "exhausted"))
    return log


def generator_states():
    gen = countdown(2)
    states = [getgeneratorstate(gen)]
    next(gen)
    states.append(getgeneratorstate(gen))
    list(gen)
    states.append(getgeneratorstate(gen))
    return states


# Level 3: a generator expression holds a recipe, not the results.
def memory_of_squares():
    as_list = [n * n for n in range(1_000_000)]
    as_generator = (n * n for n in range(1_000_000))
    return sys.getsizeof(as_list), sys.getsizeof(as_generator), sum(as_generator)


# Level 4: pipelines and infinite sequences.
def log_lines(read):
    for n in count(1):  # an endless log
        read.append(n)
        yield f"{n} ERROR" if n % 4 == 0 else f"{n} ok"


def errors_only(lines):
    for line in lines:
        if "ERROR" in line:
            yield line


def first_errors(how_many):
    read = []
    found = list(islice(errors_only(log_lines(read)), how_many))
    return found, len(read)


def fibonacci():
    a, b = 0, 1
    while True:
        yield a
        a, b = b, a + b


def itertools_tour():
    evens = list(islice(count(0, 2), 4))  # an infinite sequence, cut to 4
    small_squares = list(takewhile(lambda n: n < 10, (n * n for n in count(1))))
    running_totals = list(accumulate([3, 1, 4]))
    joined = list(chain("ab", [1, 2]))
    pairs = list(batched("abcde", 2))
    return evens, small_squares, running_totals, joined, pairs


# Level 5: yield from delegates to another generator and receives its return value.
def inner():
    yield 1
    yield 2
    return "inner done"  # becomes the value of the yield from expression


def outer(results):
    result = yield from inner()
    results.append(result)
    yield 3


def flatten(items):
    for item in items:
        if isinstance(item, list):
            yield from flatten(item)
        else:
            yield item


# Level 6: send() resumes a generator with a value.
def running_average():
    total, n, average = 0, 0, None
    while True:
        value = yield average  # send(x) makes this yield evaluate to x
        total += value
        n += 1
        average = total / n


def averages():
    avg = running_average()
    first = next(avg)  # run to the first yield: "priming"
    return first, avg.send(10), avg.send(20), avg.send(60)


# Underneath: the frame lives inside the generator object.
def paused_frame():
    gen = countdown(3)
    next(gen)
    paused = gen.gi_suspended, dict(gen.gi_frame.f_locals)
    list(gen)
    return paused, gen.gi_frame


# Interview: StopIteration escaping a generator becomes RuntimeError (PEP 479).
def leaky():
    yield 1
    raise StopIteration  # wrong: use return to end a generator


def closing_demo(log):
    try:
        yield 1
    finally:
        log.append("cleaned up")  # close() raises GeneratorExit at the yield


def main():
    assert walk_by_hand() == (10, 20, "StopIteration")
    assert for_loop_by_hand("abc") == ["a", "b", "c"]
    numbers = [1, 2]
    assert iter(numbers) is not iter(numbers), "a list hands out a fresh iterator each time"
    once = Countdown(3)
    assert list(once) == [3, 2, 1] and list(once) == [], "an iterator is used up"

    assert list(countdown(3)) == [3, 2, 1]
    assert run_two_steps() == ["created", "started", 1, "resumed", 2, "finished", "exhausted"]
    assert generator_states() == ["GEN_CREATED", "GEN_SUSPENDED", "GEN_CLOSED"]
    first_ops = [op.opname for op in dis.get_instructions(countdown)][:3]
    assert first_ops == ["RETURN_GENERATOR", "POP_TOP", "RESUME"], "calling makes a generator"
    assert "YIELD_VALUE" in [op.opname for op in dis.get_instructions(countdown)]

    list_bytes, generator_bytes, total = memory_of_squares()
    assert list_bytes == 8_448_728, "8 bytes per slot, over-allocated"
    assert sys.getsizeof([]) == 56 and (list_bytes - 56) // 8 == 1_056_084, "header + slots"
    assert generator_bytes == 208
    assert total == 999_999 * 1_000_000 * 1_999_999 // 6 == 333_332_833_333_500_000

    assert first_errors(3) == (["4 ERROR", "8 ERROR", "12 ERROR"], 12), "only 12 lines were read"
    assert list(islice(fibonacci(), 10)) == [0, 1, 1, 2, 3, 5, 8, 13, 21, 34]
    assert itertools_tour() == (
        [0, 2, 4, 6], [1, 4, 9], [3, 4, 8], ["a", "b", 1, 2], [("a", "b"), ("c", "d"), ("e",)])

    results = []
    assert list(outer(results)) == [1, 2, 3] and results == ["inner done"]
    assert list(flatten([1, [2, [3, 4]], 5])) == [1, 2, 3, 4, 5]

    assert averages() == (None, 10.0, 15.0, 30.0)
    assert error_text(lambda: running_average().send(10)) == (
        "TypeError: can't send non-None value to a just-started generator")

    assert paused_frame() == ((True, {"start": 3}), None), "locals survive the pause"

    gen = leaky()
    assert next(gen) == 1
    assert error_text(lambda: next(gen)) == "RuntimeError: generator raised StopIteration"
    log = []
    gen = closing_demo(log)
    next(gen)
    gen.close()
    assert log == ["cleaned up"] and getgeneratorstate(gen) == "GEN_CLOSED"
    assert error_text(lambda: len(countdown(1))) == "TypeError: object of type 'generator' has no len()"
    print("OK")


if __name__ == "__main__":
    main()
