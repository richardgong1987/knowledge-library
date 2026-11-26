# Run from programming-languages/python/labs with: python3 -I built-in-collections.py
import sys
from collections import Counter, defaultdict, deque


# The problem: a list compares item by item; a set jumps straight to a slot.
class StudentId:
    comparisons = 0  # counts every == that a container performs

    def __init__(self, number):
        self.number = number

    def __eq__(self, other):
        StudentId.comparisons += 1
        return self.number == other.number

    def __hash__(self):
        return hash(self.number)


def comparisons_to_find(container, wanted):
    StudentId.comparisons = 0
    found = wanted in container
    return found, StudentId.comparisons


# Level 1: a list is a dynamic array that over-allocates.
def list_capacities(count):
    empty = sys.getsizeof([])
    items, capacities = [], []
    for value in range(count):
        items.append(value)
        slots = (sys.getsizeof(items) - empty) // 8  # one 8-byte pointer per slot
        if not capacities or capacities[-1] != slots:
            capacities.append(slots)
    return capacities


def new_allocated(new_size):
    # list_resize in Objects/listobject.c: about 1/8 extra, plus 6, rounded down to a multiple of 4
    return (new_size + (new_size >> 3) + 6) & ~3


def copies_for_appends(count):
    allocated = size = copies = 0
    for _ in range(count):
        if size == allocated:  # full: the resize may move every item
            copies += size
            allocated = new_allocated(size + 1)
        size += 1
    return copies


# Level 2: a tuple is a fixed record, and a tuple of immutable items is hashable.
def tuple_as_key():
    distance = {("York", "Leeds"): 40}
    origin, destination = ("York", "Leeds")  # unpacking
    return distance[(origin, destination)]


# Level 3: a dict finds a slot from the hash, and probes on a collision.
def probe_order(h, size, count):
    # the recurrence in Objects/dictobject.c, for a hash h >= 0
    mask = size - 1
    perturb = h
    i = h & mask
    order = [i]
    while len(order) < count:
        perturb >>= 5  # PERTURB_SHIFT
        i = (i * 5 + perturb + 1) & mask
        order.append(i)
    return order


def build_index(keys, size):
    index = [None] * size  # slot -> position of the key in the entries list
    for position, key in enumerate(keys):
        mask, perturb = size - 1, hash(key)
        i = perturb & mask
        while index[i] is not None:  # occupied: probe the next slot
            perturb >>= 5
            i = (i * 5 + perturb + 1) & mask
        index[i] = position
    return index


# Level 4: the table grows when it is two-thirds full.
def dict_resize_points(count):
    table, points = {}, []
    size = sys.getsizeof(table)
    for key in range(1, count + 1):
        table[key] = key
        if sys.getsizeof(table) != size:
            points.append(key)
            size = sys.getsizeof(table)
    return points


# Level 5: equal objects must have equal hashes.
def equal_keys_collide():
    return {1: "a", 1.0: "b", True: "c"}


class Seat:
    def __init__(self, row):
        self.row = row

    def __eq__(self, other):
        return self.row == other.row

    def __hash__(self):
        return hash(self.row)  # depends on a field that can change


def lost_key():
    seat = Seat(1)
    booked = {seat}
    seat.row = 2  # the hash changes, but the set filed the seat under the old one
    return seat in booked, len(booked)


class EqOnly:
    def __eq__(self, other):
        return True  # defining __eq__ alone sets __hash__ to None


def error_text(action):
    try:
        action()
    except Exception as error:
        return f"{type(error).__name__}: {error}"


# Level 6: comprehensions build a collection from a rule.
def comprehensions():
    squares = [n * n for n in range(5)]
    even_squares = [n * n for n in range(5) if n % 2 == 0]
    lengths = {word: len(word) for word in ("ox", "cat")}
    initials = {name[0] for name in ("ann", "amy", "bob")}
    return squares, even_squares, lengths, initials


# Level 7: collections.deque, Counter and defaultdict.
def queue_with_deque():
    line = deque(["ann", "bob"])
    line.append("cy")  # join at the back: O(1)
    first = line.popleft()  # leave at the front: O(1), unlike list.pop(0)
    return first, list(line)


def recent_pages():
    last_three = deque(maxlen=3)
    for page in [1, 2, 3, 4, 5]:
        last_three.append(page)  # the oldest page falls off the front
    return list(last_three)


def tally_votes():
    votes = Counter("abcab")  # counts each item
    return votes.most_common(1), votes["z"]  # a missing key counts as 0


def group_by_initial():
    groups = defaultdict(list)  # a missing key gets a fresh empty list
    for name in ["ann", "bob", "amy"]:
        groups[name[0]].append(name)
    return dict(groups)


# Interview: when every key has the same hash, a dict lookup becomes a scan.
class ClashingId(StudentId):
    def __hash__(self):
        return 0


def mutate_while_iterating():
    marks = {"ann": 90}
    for name in marks:
        marks[name + "!"] = 0


def main():
    ids = [StudentId(n) for n in range(1000)]
    assert comparisons_to_find(ids, StudentId(999)) == (True, 1000), "a list scans all 1000"
    assert comparisons_to_find(set(ids), StudentId(999)) == (True, 1), "a set compares once"

    capacities = [4, 8, 16, 24, 32, 40, 52, 64, 76, 92]
    assert list_capacities(80) == capacities, "CPython 3.14 growth pattern"
    assert [new_allocated(n) for n in (1, 5, 9, 17, 25, 33, 41, 53, 65, 77)] == capacities
    assert copies_for_appends(1_000_000) == 8_445_096, "about 8.4 moves per append"
    assert copies_for_appends(1_000_000) <= 9 * 1_000_000, "the geometric bound 9n"

    assert tuple_as_key() == 40
    assert error_text(lambda: hash((1, [2]))) == "TypeError: unhashable type: 'list'"

    assert probe_order(0, 8, 9) == [0, 1, 6, 7, 4, 5, 2, 3, 0], "the order in the source comment"
    assert probe_order(40, 8, 2) == [0, 2], "perturb brings in the high bits"
    assert build_index([8, 16, 24, 40], 8) == [0, 1, 3, None, None, None, 2, None]

    assert dict_resize_points(24) == [1, 6, 11, 22], "8 -> 16 -> 32 -> 64 slots"
    assert sys.getsizeof({1: 1}) - sys.getsizeof({}) == 32 + 8 * 1 + 5 * 24, "header + indices + entries"
    assert sys.getsizeof({"a": 1}) - sys.getsizeof({}) == 32 + 8 * 1 + 5 * 16, "str keys omit the hash"
    ordered = {"b": 1, "a": 2, "c": 3}
    assert list(ordered) == ["b", "a", "c"], "insertion order"
    ordered["b"] = 9
    assert list(ordered) == ["b", "a", "c"], "re-assigning keeps the position"
    del ordered["b"]
    ordered["b"] = 1
    assert list(ordered) == ["a", "c", "b"], "delete and re-insert moves it to the end"

    assert equal_keys_collide() == {1: "c"} and hash(1) == hash(1.0) == hash(True)
    assert type(next(iter(equal_keys_collide()))) is int, "the first key object stays"
    assert hash(-1) == -2, "-1 is reserved for errors in C"
    assert lost_key() == (False, 1), "a mutated key cannot be found"
    assert EqOnly.__hash__ is None
    assert error_text(lambda: {EqOnly()}) == (
        "TypeError: cannot use 'EqOnly' as a set element (unhashable type: 'EqOnly')")
    assert error_text(lambda: {[1]: 0}) == (
        "TypeError: cannot use 'list' as a dict key (unhashable type: 'list')")

    assert comprehensions() == ([0, 1, 4, 9, 16], [0, 4, 16], {"ox": 2, "cat": 3}, {"a", "b"})
    assert {1, 2, 3} & {2, 3, 4} == {2, 3} and {1, 2} | {2, 3} == {1, 2, 3}
    assert {1, 2, 3} - {2} == {1, 3} and {1, 2} ^ {2, 3} == {1, 3}
    assert type({}) is dict, "{} is an empty dict, not an empty set"

    assert queue_with_deque() == ("ann", ["bob", "cy"])
    assert recent_pages() == [3, 4, 5]
    assert tally_votes() == ([("a", 2)], 0)
    assert group_by_initial() == {"a": ["ann", "amy"], "b": ["bob"]}

    clashing = dict.fromkeys(ClashingId(n) for n in range(100))
    assert comparisons_to_find(clashing, ClashingId(99)) == (True, 100), "one hash for all: a scan"
    assert error_text(mutate_while_iterating) == "RuntimeError: dictionary changed size during iteration"
    print("OK")


if __name__ == "__main__":
    main()
