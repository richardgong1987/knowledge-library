# Run from programming-languages/python/labs with: python3 -I memory-and-garbage-collection.py
# Reference counts, sizes and immortal objects are CPython details. The numbers
# checked here are those of CPython 3.14.5 or later on a 64-bit machine.
import gc
import sys
import weakref


class Node:
    def __init__(self, name):
        self.name = name
        self.partner = None


# Level 1: every reference adds one to the count; reaching 0 frees the object at once.
def count_references():
    node = Node("a")
    alone = sys.getrefcount(node)
    alias = node
    with_alias = sys.getrefcount(node)
    del alias
    pair = [node, node]
    in_list = sys.getrefcount(node)
    return alone, with_alias, in_list, len(pair)


def freed_at_once():
    node = Node("b")
    probe = weakref.ref(node)
    del node  # the count drops to 0: freed here, no collector needed
    return probe() is None


# Level 2: a cycle keeps every count above 0, so only the cycle collector frees it.
def leak_a_cycle():
    a, b = Node("a"), Node("b")
    a.partner, b.partner = b, a  # each keeps the other's count at 1
    return weakref.ref(a)


# Level 3: survivors move to an older generation, which is scanned less often.
def in_generation(obj, generation):
    return any(o is obj for o in gc.get_objects(generation=generation))


# Level 4: a weak reference does not keep its object alive.
class Image:
    def __init__(self, name):
        self.name = name


cache = weakref.WeakValueDictionary()


def load(name):
    image = cache.get(name)
    if image is None:
        image = Image(name)
        cache[name] = image
    return image


# Level 5: __del__ runs when the count reaches 0, or later if the object is in a cycle.
class Noisy:
    def __init__(self, log):
        self.log = log

    def __del__(self):
        self.log.append("del")


# Level 6: sizes, shared small objects and immortal objects.
def int_size(n):
    digits = max(1, -(-n.bit_length() // 30))  # 30-bit digits, rounded up
    return 24 + 4 * digits


def check_counting():
    assert count_references() == (1, 2, 3, 2)
    assert freed_at_once()


def check_cycles():
    gc.disable()  # no automatic collection may run between the steps below
    try:
        gc.collect()
        probe = leak_a_cycle()
        assert probe() is not None  # unreachable, but both counts are still 1
        assert gc.collect() == 2  # the two Node objects
        assert probe() is None

        probe = leak_a_cycle()
        kept = probe()  # an outside reference makes the cycle reachable
        assert gc.collect() == 0 and probe() is kept
        del kept
        assert gc.collect() == 2

        log = []
        noisy = Noisy(log)
        del noisy
        assert log == ["del"]  # immediately, by reference counting
        noisy = Noisy(log)
        noisy.me = noisy  # a cycle through itself
        del noisy
        assert log == ["del"]  # not yet
        assert gc.collect() == 1 and log == ["del", "del"]

        node = Node("young")
        assert in_generation(node, 0)
        gc.collect(0)
        assert in_generation(node, 1)
        gc.collect(1)
        assert in_generation(node, 2)
    finally:
        gc.enable()
    assert gc.get_threshold() == (2000, 10, 10) and len(gc.get_stats()) == 3


def check_weak_references():
    first = load("logo")
    second = load("logo")
    assert first is second and len(cache) == 1
    del first, second
    assert len(cache) == 0  # the cache did not keep the image alive

    log = []
    image = Image("banner")
    weakref.finalize(image, log.append, "closed")
    del image
    assert log == ["closed"]
    try:
        weakref.ref(3)
    except TypeError as error:
        assert str(error) == "cannot create weak reference to 'int' object"


def check_sizes_and_sharing():
    for n in (0, 1, 2**30 - 1, 2**30, 2**60, 2**90):
        assert sys.getsizeof(n) == int_size(n)
    assert [sys.getsizeof(n) for n in (0, 2**30, 2**60)] == [28, 32, 36]
    assert sys.getsizeof(object()) == 16  # reference count + type pointer
    assert sys.getsizeof([]) == 56 and sys.getsizeof(["b"]) == 64
    assert sys.getsizeof(["b" * 1000]) == 64  # shallow: the string is not counted

    assert int("256") is int("256")  # -5 to 256 are cached
    assert int("257") is not int("257")
    joined = "".join(["py", "thon"])
    other = "".join(["py", "thon"])
    assert joined == other and joined is not other
    assert sys.intern(joined) is sys.intern(other)

    assert sys.getrefcount(None) == 3 << 30 == 3221225472
    nones = [None] * 1000
    assert sys.getrefcount(None) == 3221225472 and len(nones) == 1000  # never counted
    assert sys._is_immortal(None) and sys._is_immortal(256)
    assert not sys._is_immortal(int("1000")) and not sys._is_immortal(Node("x"))


def main():
    check_counting()
    check_cycles()
    check_weak_references()
    check_sizes_and_sharing()
    print("OK")


if __name__ == "__main__":
    main()
