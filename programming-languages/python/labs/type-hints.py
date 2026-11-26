# Run from programming-languages/python/labs with: python3 -I type-hints.py
# This lab checks run-time behaviour only. A static checker (mypy, pyright) reads the
# same hints without running anything; the chapter describes what it would report.
import annotationlib
import io
import typing
from collections.abc import Callable, Sized
from typing import Literal, Protocol, TypedDict, overload, runtime_checkable


# Level 1: a hint is a note for tools. Python stores it and does not enforce it.
def double(x: int) -> int:
    return x * 2


# Level 2: a union says "one of these"; a None check narrows it.
def parse_age(text: str) -> int | None:
    return int(text) if text.isdigit() else None


def describe(age: int | None) -> str:
    if age is None:
        return "unknown"
    return f"{age} years"  # here the checker knows that age is an int


# Level 3: generics with the Python 3.12 type parameter syntax.
def first[T](items: list[T]) -> T:
    return items[0]


def longest[T: Sized](a: T, b: T) -> T:
    return a if len(a) >= len(b) else b


class Box[T]:
    def __init__(self, item: T) -> None:
        self.item = item

    def get(self) -> T:
        return self.item


type Pair[T] = tuple[T, T]
type Later = NotDefinedYet  # fine: an alias's value is evaluated lazily


# Level 4: structural types. Greeter is satisfied by any class with greet().
class Greeter(Protocol):
    def greet(self) -> str: ...


class English:  # never mentions Greeter
    def greet(self) -> str:
        return "hello"


def welcome(greeter: Greeter) -> str:
    return greeter.greet() + "!"


@runtime_checkable
class Closable(Protocol):
    def close(self) -> None: ...


class Movie(TypedDict):
    title: str
    year: int


type Mode = Literal["r", "w"]


def open_mode(mode: Mode) -> str:
    return mode


# Level 5: overloads give each argument type its own return type.
@overload
def scale(value: int, factor: int) -> int: ...
@overload
def scale(value: str, factor: int) -> str: ...
def scale(value, factor):
    return value * factor


def apply_twice(f: Callable[[int], int], value: int) -> int:
    return f(f(value))


def count_digits(x: object) -> int:  # accepts more than an int: fine as a callback
    return len(str(x))


# Level 6: Python 3.14 evaluates annotations only when someone asks for them.
class Node:
    def link(self, other: Node) -> Node:  # Node is not defined yet: fine in 3.14
        return other


evaluated = []


def label(name):
    evaluated.append(name)
    return str


def greet(name: label("name")) -> label("return"):
    return f"hi {name}"


def load(path: Missing) -> None:
    pass


def raises(error_type, function, *args):
    try:
        function(*args)
    except error_type as error:
        return str(error)
    raise AssertionError(f"expected {error_type.__name__}")


def main():
    assert double(21) == 42
    assert double("21") == "2121"  # wrong type, no error: hints are not enforced
    assert annotationlib.get_annotations(double) == {"x": int, "return": int}
    assert issubclass(bool, int) and double(True) == 2

    assert parse_age("36") == 36 and parse_age("old") is None
    assert describe(36) == "36 years" and describe(None) == "unknown"
    assert isinstance(3, int | str) and not isinstance(3.0, int | str)
    assert (int | None) == typing.Optional[int]
    assert raises(TypeError, isinstance, [1], list[int]) == (
        "isinstance() argument 2 cannot be a parameterized generic")

    assert first([3, 1, 2]) == 3 and first(["a"]) == "a"
    assert longest("ab", "abc") == "abc" and longest([1, 2], [3]) == [1, 2]
    (t,) = first.__type_params__
    assert type(t) is typing.TypeVar and t.__name__ == "T"
    assert longest.__type_params__[0].__bound__ is Sized
    assert Box(7).get() == 7 and Box[int].__origin__ is Box
    assert Box.__mro__ == (Box, typing.Generic, object) and Box[int].__args__ == (int,)
    assert type(Pair) is typing.TypeAliasType
    (p,) = Pair.__type_params__
    assert Pair.__value__ == tuple[p, p]
    assert raises(NameError, lambda: Later.__value__) == "name 'NotDefinedYet' is not defined"
    assert raises(TypeError, Pair, (1, 2)) == "'typing.TypeAliasType' object is not callable"

    assert welcome(English()) == "hello!"
    assert raises(TypeError, isinstance, English(), Greeter) == (
        "Instance and class checks can only be used with @runtime_checkable protocols")
    assert isinstance(io.StringIO(), Closable) and not isinstance(3, Closable)
    movie = Movie(title="Up", year="2009")  # a checker rejects this; Python does not
    assert movie == {"title": "Up", "year": "2009"} and type(movie) is dict
    assert open_mode("x") == "x"  # not one of the literals, still accepted at run time
    assert typing.get_args(Mode.__value__) == ("r", "w")

    assert scale(3, 2) == 6 and scale("ab", 2) == "abab"
    assert len(typing.get_overloads(scale)) == 2
    assert apply_twice(double, 5) == 20
    assert apply_twice(count_digits, 12345) == 1  # 12345 has 5 digits, 5 has 1

    assert annotationlib.get_annotations(Node.link) == {"other": Node, "return": Node}
    assert evaluated == []  # defining greet evaluated nothing
    assert greet.__annotations__ == {"name": str, "return": str}
    assert evaluated == ["name", "return"]
    assert greet.__annotations__ == {"name": str, "return": str}
    assert evaluated == ["name", "return"]  # evaluated once, then cached

    Format = annotationlib.Format
    assert annotationlib.get_annotations(load, format=Format.STRING) == {"path": "Missing", "return": "None"}
    forward = annotationlib.get_annotations(load, format=Format.FORWARDREF)["path"]
    assert type(forward) is annotationlib.ForwardRef and forward.__forward_arg__ == "Missing"
    assert raises(NameError, annotationlib.get_annotations, load) == "name 'Missing' is not defined"
    assert greet.__annotate__(Format.VALUE) == {"name": str, "return": str}
    assert raises(NotImplementedError, greet.__annotate__, Format.STRING) == ""
    print("OK")


if __name__ == "__main__":
    main()
