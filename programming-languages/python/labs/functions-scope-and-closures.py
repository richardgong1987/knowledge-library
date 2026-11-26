# Run from programming-languages/python/labs with: python3 -I functions-scope-and-closures.py
import dis
from functools import partial


def error_text(action):
    try:
        action()
    except Exception as error:
        return f"{type(error).__name__}: {error}"


# Level 1: a function is an object like any other.
def shout(text):
    return text.upper() + "!"


def apply_twice(func, value):
    return func(func(value))


# Level 2: LEGB. A name is looked up in Local, Enclosing, Global, then Built-in scope.
place = "global"


def outer():
    place = "enclosing"

    def inner():
        return place, len("abc")  # place: found in E; len: found in B

    return inner()


count = 0


def increment():
    count += 1  # assignment makes count local to the whole function
    return count


# Level 3: global and nonlocal rebind a name in an outer scope.
visits = 0


def record_visit():
    global visits  # rebind the module's name
    visits += 1
    return visits


def make_counter():
    count = 0

    def next_ticket():
        nonlocal count  # rebind the enclosing function's name
        count += 1
        return count

    return next_ticket


# Level 4: a closure keeps its free variables in cell objects.
def make_multiplier(factor):
    def multiply(x):
        return x * factor

    return multiply


# Level 5: closures capture variables, not values.
def late_binding():
    callbacks = [lambda: i for i in range(3)]
    return [f() for f in callbacks]


def shared_cell():
    callbacks = [lambda: i for i in range(3)]
    return callbacks[0].__closure__[0] is callbacks[2].__closure__[0]


def early_binding():
    callbacks = [lambda i=i: i for i in range(3)]  # the default is evaluated now
    return [f() for f in callbacks]


# Level 6: default values are evaluated once, when def runs.
def add_mark(mark, marks=[]):
    marks.append(mark)  # the same list object on every call
    return marks


def add_mark_safely(mark, marks=None):
    if marks is None:
        marks = []  # a new list on every call
    marks.append(mark)
    return marks


# Level 7: *args, **kwargs, positional-only and keyword-only parameters.
def total(*numbers, **labels):
    return sum(numbers), labels


def ratio(numerator, denominator, /, *, rounded=False):
    value = numerator / denominator
    return round(value) if rounded else value


def lambdas():
    by_length = sorted(["pear", "fig", "banana"], key=lambda word: len(word))
    return by_length, (lambda: 0).__name__


# Interview: a class body is not an enclosing scope for its methods.
class Config:
    limit = 3

    def broken(self):
        return limit  # NameError: the class scope is skipped

    def fixed(self):
        return self.limit


def opnames(func):
    return [op.opname for op in dis.get_instructions(func)]


def appears_in_order(wanted, found):
    remaining = iter(found)
    return all(step in remaining for step in wanted)


def main():
    assert apply_twice(shout, "hi") == "HI!!", "functions can be passed around"
    assert type(shout).__name__ == "function" and shout.__name__ == "shout"
    actions = {"shout": shout}
    assert actions["shout"]("ok") == "OK!", "functions can live in a dict"

    assert outer() == ("enclosing", 3)
    assert error_text(increment) == (
        "UnboundLocalError: cannot access local variable 'count' where it is not associated with a value")

    assert [record_visit(), record_visit()] == [1, 2] and visits == 2
    tickets = make_counter()
    assert [tickets(), tickets(), tickets()] == [1, 2, 3]
    assert make_counter()() == 1, "each call of make_counter makes a new cell"
    assert appears_in_order(["MAKE_CELL", "STORE_DEREF", "BUILD_TUPLE", "MAKE_FUNCTION", "SET_FUNCTION_ATTRIBUTE"],
                            opnames(make_counter))
    assert appears_in_order(["COPY_FREE_VARS", "LOAD_DEREF", "BINARY_OP", "STORE_DEREF"], opnames(tickets))
    assert error_text(lambda: compile("def f():\n    nonlocal count\n", "<lab>", "exec")) == (
        "SyntaxError: no binding for nonlocal 'count' found (<lab>, line 2)")

    triple = make_multiplier(3)
    assert triple(5) == 15
    assert triple.__code__.co_freevars == ("factor",)
    assert make_multiplier.__code__.co_cellvars == ("factor",)
    assert type(triple.__closure__[0]).__name__ == "cell"
    assert triple.__closure__[0].cell_contents == 3

    assert late_binding() == [2, 2, 2], "every lambda reads the one cell for i"
    assert shared_cell(), "three closures, one cell"
    assert early_binding() == [0, 1, 2]
    assert [f() for f in [partial(lambda i: i, i) for i in range(3)]] == [0, 1, 2]

    assert add_mark(1) == [1]
    assert add_mark(2) == [1, 2], "the default list remembers the first call"
    assert add_mark.__defaults__ == ([1, 2],)
    assert add_mark_safely(1) == [1] and add_mark_safely(2) == [2]

    assert total(1, 2, 3, unit="cm") == (6, {"unit": "cm"})
    assert total(*[1, 2], **{"unit": "kg"}) == (3, {"unit": "kg"}), "unpacking at the call"
    assert ratio(7, 2) == 3.5 and ratio(7, 2, rounded=True) == 4
    assert error_text(lambda: ratio(numerator=7, denominator=2)) == (
        "TypeError: ratio() got some positional-only arguments passed as keyword arguments: "
        "'numerator, denominator'")
    assert error_text(lambda: ratio(7, 2, True)) == (
        "TypeError: ratio() takes 2 positional arguments but 3 were given")

    assert lambdas() == (["fig", "pear", "banana"], "<lambda>")
    assert error_text(lambda: len(obj=[])) == "TypeError: len() takes no keyword arguments"
    assert error_text(lambda: exec("list = [1, 2]\nlist('ab')", {})) == "TypeError: 'list' object is not callable"

    assert error_text(Config().broken) == "NameError: name 'limit' is not defined"
    assert Config().fixed() == 3
    print("OK")


if __name__ == "__main__":
    main()
