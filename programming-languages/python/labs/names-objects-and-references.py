# Run from programming-languages/python/labs with: python3 -I names-objects-and-references.py
import copy
import dis
import sys
import warnings


# Level 1: every object has an identity, a type and a value.
def identity_type_value():
    x = [1, 2]
    before = id(x)
    x.append(3)  # the value changes ...
    return id(x) == before, type(x) is list, x  # ... the identity and the type do not


def equal_but_not_identical():
    first = [1, 2]
    second = [1, 2]  # a new list that happens to hold the same value
    return first == second, first is second


# Level 2: assignment binds a second name to the same object.
def alias_a_list():
    scores = [90, 85]
    backup = scores  # a second name, not a second list
    scores.append(70)
    return backup, backup is scores


def rebind_a_name():
    scores = [90, 85]
    backup = scores
    scores = scores + [70]  # + builds a new list; only `scores` moves to it
    return backup, scores


# Level 3: += mutates a mutable object but rebinds a name to an immutable one.
def augmented_on_list():
    a = [1]
    b = a
    a += [2]  # list.__iadd__ extends the same list
    return b, a is b


def augmented_on_str():
    s = "hi"
    t = s
    s += "!"  # str has no __iadd__: Python builds "hi!" and rebinds s
    return t, s, t is s


# Level 4: call by sharing. The parameter is one more name for the caller's object.
def add_bonus(marks):
    marks.append(5)  # mutates the caller's list


def replace_marks(marks):
    marks = [0]  # rebinds the local name only
    return marks


def call_by_sharing():
    marks = [7]
    add_bonus(marks)
    replace_marks(marks)
    return marks


# Level 5: the += puzzle on a list inside a tuple.
def tuple_puzzle():
    t = (1, [2, 3])
    try:
        t[1] += [4]
    except TypeError as error:
        return t, str(error)


# Level 6: shallow and deep copies.
def shared_rows():
    grid = [[0] * 3] * 3  # three references to ONE row
    grid[0][0] = 1
    return grid


def separate_rows():
    grid = [[0] * 3 for _ in range(3)]  # three different rows
    grid[0][0] = 1
    return grid


def shallow_vs_deep():
    original = [[1, 2], [3]]
    shallow = copy.copy(original)  # new outer list, same inner lists
    deep = copy.deepcopy(original)  # new lists all the way down
    original[0].append(99)
    return shallow, deep


# Level 7: the small-int cache is a CPython detail, not a language rule.
def small_int_cache():
    # int() runs at run time, so the compiler cannot share one constant object
    return int("256") is int("256"), int("257") is int("257")


def equal_constants_shared():
    a = 257
    b = 257  # the compiler stores the constant 257 once for this function
    return a is b


def is_with_literal_message():
    with warnings.catch_warnings(record=True) as caught:
        warnings.simplefilter("always")
        compile("x is 1", "<lab>", "eval")
    return caught[0].category.__name__, str(caught[0].message)


# Underneath: a name holds a reference; binding a name adds one reference.
def one_more_reference():
    obj = []
    before = sys.getrefcount(obj)
    alias = obj  # binding a name copies a pointer, not the list
    return sys.getrefcount(obj) - before, alias is obj


# Interview: is versus ==.
def nan_identity():
    nan = float("nan")
    return nan == nan, nan is nan, [nan] == [nan]


class Anything:
    def __eq__(self, other):
        return True  # claims to equal everything


def deepcopy_keeps_sharing():
    row = [0]
    pair = [row, row]  # one row, two references
    clone = copy.deepcopy(pair)
    return clone[0] is clone[1], clone[0] is row


def deepcopy_a_cycle():
    loop = [1]
    loop.append(loop)  # the list contains itself
    clone = copy.deepcopy(loop)
    return clone[1] is clone, clone is loop


def instructions(code):
    return [(op.opname, op.argrepr) for op in dis.get_instructions(code)]


def appears_in_order(wanted, found):
    remaining = iter(found)
    return all(step in remaining for step in wanted)


def main():
    same_id, is_list, value = identity_type_value()
    assert same_id and is_list and value == [1, 2, 3], "mutation keeps identity and type"
    assert equal_but_not_identical() == (True, False), "== compares values, is compares identity"

    assert alias_a_list() == ([90, 85, 70], True), "assignment never copies"
    assert rebind_a_name() == ([90, 85], [90, 85, 70]), "rebinding moves one name only"

    assert augmented_on_list() == ([1, 2], True), "list += mutates in place"
    assert augmented_on_str() == ("hi", "hi!", False), "str += rebinds to a new object"

    assert call_by_sharing() == [7, 5], "mutation is shared, rebinding is local"

    puzzled, message = tuple_puzzle()
    assert puzzled == (1, [2, 3, 4]), "the list was extended before the store failed"
    assert message == "'tuple' object does not support item assignment"
    assert appears_in_order(
        [("BINARY_OP", "[]"), ("BUILD_LIST", ""), ("BINARY_OP", "+="), ("STORE_SUBSCR", "")],
        instructions("t[1] += [4]")), "the in-place add runs before the store"

    assert shared_rows() == [[1, 0, 0], [1, 0, 0], [1, 0, 0]], "one row, three references"
    assert separate_rows() == [[1, 0, 0], [0, 0, 0], [0, 0, 0]], "three rows"
    assert shallow_vs_deep() == ([[1, 2, 99], [3]], [[1, 2], [3]]), "shallow shares inner lists"
    assert deepcopy_keeps_sharing() == (True, False), "deepcopy keeps the shape of sharing"
    assert deepcopy_a_cycle() == (True, False), "deepcopy's memo copies a cycle as a cycle"

    assert small_int_cache() == (True, False), "CPython 3.14 caches -5 to 256"
    assert int("-5") is int("-5") and int("-6") is not int("-6"), "the cache starts at -5"
    assert equal_constants_shared(), "equal constants in one code object are one object"
    assert is_with_literal_message() == ("SyntaxWarning", "\"is\" with 'int' literal. Did you mean \"==\"?")

    assert one_more_reference() == (1, True), "a new name is one more reference"
    assert appears_in_order([("LOAD_FAST", "scores"), ("STORE_FAST", "backup")],
                            instructions(alias_a_list)), "binding copies one pointer"
    assert sys._is_immortal(None) and sys._is_immortal(5), "None and small ints are immortal"

    assert nan_identity() == (False, True, True), "containers check identity before =="
    assert (Anything() == None) is True, "__eq__ can claim anything"
    assert (Anything() is None) is False, "is cannot be overridden"
    print("OK")


if __name__ == "__main__":
    main()
