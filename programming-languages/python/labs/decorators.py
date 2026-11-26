# Run from programming-languages/python/labs with: python3 -I decorators.py
import dis
import functools
import inspect


def error_text(action):
    try:
        action()
    except Exception as error:
        return f"{type(error).__name__}: {error}"


# Level 1: a decorator is a function that takes a function and returns a function.
def shout_result(func):
    def wrapper(*args, **kwargs):
        return func(*args, **kwargs).upper()

    return wrapper


def greet(name):
    return f"hello, {name}"


loud_greet = shout_result(greet)


# Level 2: @d above def f means f = d(f), and it runs when def runs.
@shout_result
def welcome(name):
    """Say welcome."""
    return f"welcome, {name}"


registry = []


def register(func):
    registry.append(func.__name__)  # runs once, when def runs
    return func  # hand back the same function


@register
def home():
    return "home page"


# Level 3: functools.wraps keeps the wrapped function's name and docstring.
def counted(func):
    @functools.wraps(func)  # copy __name__, __doc__, ... and set __wrapped__
    def wrapper(*args, **kwargs):
        wrapper.calls += 1
        return func(*args, **kwargs)

    wrapper.calls = 0
    return wrapper


@counted
def fib(n):
    """The n-th Fibonacci number, the slow way."""
    return n if n < 2 else fib(n - 1) + fib(n - 2)


def calls_for_fib(n):
    fib.calls = 0
    value = fib(n)
    return value, fib.calls


# Level 4: a decorator with arguments is a function that returns a decorator.
def repeat(times):
    def decorate(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            return [func(*args, **kwargs) for _ in range(times)]

        return wrapper

    return decorate


@repeat(3)
def roll():
    return "six"


# Level 5: stacked decorators compose; the one nearest def is applied first.
def tag(name):
    def decorate(func):
        @functools.wraps(func)
        def wrapper():
            return f"<{name}>{func()}</{name}>"

        return wrapper

    return decorate


@tag("b")
@tag("i")
def hello():
    return "hi"


@tag("i")
@tag("b")
def hello_swapped():
    return "hi"


# Level 6: a class decorator receives the class.
def with_repr(cls):
    def __repr__(self):
        fields = ", ".join(f"{name}={value!r}" for name, value in vars(self).items())
        return f"{cls.__name__}({fields})"

    cls.__repr__ = __repr__
    return cls


@with_repr
class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y


# Level 7: functools.cache and lru_cache remember results.
@functools.cache
def fib_cached(n):
    return n if n < 2 else fib_cached(n - 1) + fib_cached(n - 2)


@functools.lru_cache(maxsize=2)
def square(n):
    return n * n


def lru_story():
    square.cache_clear()
    for n in [1, 2, 1, 3, 2]:  # 3 evicts 2, then 2 evicts 1
        square(n)
    return square.cache_info()


# Level 8: property, classmethod and staticmethod are decorators too.
class Temperature:
    def __init__(self, celsius):
        self.celsius = celsius

    @property
    def fahrenheit(self):  # read like an attribute, computed on access
        return self.celsius * 9 / 5 + 32

    @fahrenheit.setter
    def fahrenheit(self, value):
        self.celsius = (value - 32) * 5 / 9

    @classmethod
    def from_fahrenheit(cls, value):  # receives the class: an alternative constructor
        return cls((value - 32) * 5 / 9)

    @staticmethod
    def is_freezing(celsius):  # receives neither self nor cls
        return celsius <= 0


def opnames(source):
    return [op.opname for op in dis.get_instructions(compile(source, "<lab>", "exec"))]


def main():
    assert loud_greet("ann") == "HELLO, ANN"
    assert welcome("bo") == "WELCOME, BO"
    assert welcome.__name__ == "wrapper" and welcome.__doc__ is None, "no wraps: the name is lost"
    assert registry == ["home"], "register ran at definition time, before any call"
    assert home() == "home page"

    assert fib.__name__ == "fib" and fib.__doc__.startswith("The n-th"), "wraps kept them"
    assert fib.__wrapped__(10) == 55
    assert calls_for_fib(5) == (5, 15)
    assert calls_for_fib(30) == (832_040, 2_692_537), "2 F(31) - 1 calls"

    assert roll() == ["six", "six", "six"] and roll.__name__ == "roll"
    forgot_the_call = repeat(roll)  # what @repeat without (3) does
    assert error_text(forgot_the_call) == (
        "TypeError: repeat.<locals>.decorate() missing 1 required positional argument: 'func'")
    assert str(inspect.signature(fib)) == "(n)", "inspect follows __wrapped__"

    assert hello() == "<b><i>hi</i></b>"
    assert hello_swapped() == "<i><b>hi</b></i>", "composition does not commute"
    ops = opnames('@tag("b")\n@tag("i")\ndef hello():\n    return "hi"\n')
    assert ops[ops.index("MAKE_FUNCTION"):ops.index("STORE_NAME")] == ["MAKE_FUNCTION", "CALL", "CALL"]
    assert ops.count("CALL") == 4, "two tag(...) calls first, then two wrapper applications"

    assert repr(Point(1, 2)) == "Point(x=1, y=2)"

    assert fib_cached(30) == 832_040
    info = fib_cached.cache_info()
    assert (info.hits, info.misses, info.maxsize, info.currsize) == (28, 31, None, 31)
    assert lru_story() == (1, 4, 2, 2), "hits, misses, maxsize, currsize"
    assert error_text(lambda: square([1])) == "TypeError: unhashable type: 'list'"

    t = Temperature(100)
    assert t.fahrenheit == 212.0
    t.fahrenheit = 32
    assert t.celsius == 0.0
    assert Temperature.from_fahrenheit(212).celsius == 100.0
    assert Temperature.is_freezing(-1) and not t.is_freezing(5)
    assert type(Temperature.__dict__["fahrenheit"]) is property
    assert functools.WRAPPER_ASSIGNMENTS == (
        "__module__", "__name__", "__qualname__", "__doc__", "__annotate__", "__type_params__")
    print("OK")


if __name__ == "__main__":
    main()
