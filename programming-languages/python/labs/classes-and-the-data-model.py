# Run from programming-languages/python/labs with: python3 -I classes-and-the-data-model.py
import dis
import math
from dataclasses import FrozenInstanceError, dataclass


def error_text(action):
    try:
        action()
    except Exception as error:
        return f"{type(error).__name__}: {error}"


# The problem: a plain class knows nothing about printing, ==, + or len().
class PlainVector:
    def __init__(self, x, y):
        self.x, self.y = x, y


# Level 1: a class is an object, made by calling type.
def make_class_at_run_time():
    Pet = type("Pet", (), {"sound": "woof"})  # what a class statement does
    return Pet.__name__, Pet().sound, type(Pet) is type


# Level 2: __new__ creates the object, __init__ fills it in.
class Traced:
    log = []

    def __new__(cls, name):
        Traced.log.append("new")
        return super().__new__(cls)  # allocate the empty object

    def __init__(self, name):
        Traced.log.append("init")
        self.name = name  # fill it in


class Celsius(float):
    def __new__(cls, degrees):
        return super().__new__(cls, round(degrees, 1))  # a float's value is fixed in __new__


# Level 3: special methods connect a class to Python's syntax.
class Vector:
    def __init__(self, x, y):
        self.x, self.y = x, y

    def __repr__(self):
        return f"Vector({self.x!r}, {self.y!r})"

    def __eq__(self, other):
        if not isinstance(other, Vector):
            return NotImplemented  # let the other operand try
        return (self.x, self.y) == (other.x, other.y)

    def __hash__(self):
        return hash((self.x, self.y))  # equal vectors have equal hashes

    def __abs__(self):
        return math.hypot(self.x, self.y)

    def __add__(self, other):
        if not isinstance(other, Vector):
            return NotImplemented
        return Vector(self.x + other.x, self.y + other.y)

    def __mul__(self, scalar):
        return Vector(self.x * scalar, self.y * scalar)

    def __rmul__(self, scalar):  # called for 3 * v, after int.__mul__ gives up
        return self * scalar


class Playlist:
    def __init__(self, *songs):
        self.songs = list(songs)

    def __len__(self):
        return len(self.songs)

    def __getitem__(self, index):
        return self.songs[index]  # ints and slices both work


# Level 4: a subclass on the right gets the first try with its reflected method.
class LoudVector(Vector):
    def __radd__(self, other):
        return "LoudVector.__radd__ ran first"


# Level 5: descriptors. A data descriptor beats the instance __dict__.
class Positive:
    def __set_name__(self, owner, name):
        self.storage = "_" + name  # the instance attribute that holds the value

    def __get__(self, obj, objtype=None):
        return getattr(obj, self.storage)

    def __set__(self, obj, value):
        if value <= 0:
            raise ValueError(f"{self.storage[1:]} must be positive, got {value}")
        setattr(obj, self.storage, value)


class Product:
    price = Positive()

    def __init__(self, price):
        self.price = price  # goes through Positive.__set__


class Account:
    def __init__(self):
        self._balance = 10

    @property
    def balance(self):  # property is a data descriptor
        return self._balance


class Greeter:
    def hello(self):
        return "from the class"


MISSING = object()


def lookup(obj, name):
    # a model of object.__getattribute__ in Objects/object.c
    class_attr = next((vars(k)[name] for k in type(obj).__mro__ if name in vars(k)), MISSING)
    getter = getattr(type(class_attr), "__get__", None)
    is_data = hasattr(type(class_attr), "__set__") or hasattr(type(class_attr), "__delete__")
    if getter and is_data:
        return getter(class_attr, obj, type(obj))  # 1. data descriptor
    if name in getattr(obj, "__dict__", {}):
        return obj.__dict__[name]  # 2. instance dictionary
    if getter:
        return getter(class_attr, obj, type(obj))  # 3. non-data descriptor, such as a function
    if class_attr is not MISSING:
        return class_attr  # 4. plain class attribute
    raise AttributeError(name)


# Level 6: a function is a non-data descriptor; __get__ makes the bound method.
def how_methods_bind():
    g = Greeter()
    plain = Greeter.__dict__["hello"]  # the plain function stored in the class
    bound = plain.__get__(g, Greeter)  # what g.hello does behind the scenes
    return bound(), bound.__self__ is g, bound.__func__ is plain


# Level 7: __slots__ replaces the per-instance __dict__ with fixed fields.
class SlottedPoint:
    __slots__ = ("x", "y")

    def __init__(self, x, y):
        self.x, self.y = x, y


# Level 8: dataclasses write __init__, __repr__ and __eq__ for you.
@dataclass(frozen=True, slots=True)
class Money:
    amount: int
    currency: str = "GBP"


@dataclass
class Basket:
    total: int = 0  # eq=True and not frozen: __hash__ is set to None


def bad_default():
    @dataclass
    class Order:
        items: list = []  # one list would be shared by every Order


def main():
    plain = PlainVector(1, 2)
    assert repr(plain).startswith("<__main__.PlainVector object at 0x")
    assert plain != PlainVector(1, 2), "default == is identity"
    assert error_text(lambda: plain + plain) == (
        "TypeError: unsupported operand type(s) for +: 'PlainVector' and 'PlainVector'")

    assert make_class_at_run_time() == ("Pet", "woof", True)
    assert type(Vector) is type and type(type) is type

    Traced("a")
    assert Traced.log == ["new", "init"]
    assert Celsius(21.456) == 21.5 and isinstance(Celsius(0), float)

    v, w = Vector(1, 2), Vector(3, 4)
    assert repr(v) == "Vector(1, 2)"
    assert v == Vector(1, 2) and v is not Vector(1, 2)
    assert {v, Vector(1, 2)} == {v}, "hashable, and equal vectors are one set element"
    assert abs(w) == 5.0
    assert v + w == Vector(4, 6)
    assert v * 3 == Vector(3, 6) and 3 * v == Vector(3, 6)
    assert error_text(lambda: v + 1) == "TypeError: unsupported operand type(s) for +: 'Vector' and 'int'"
    assert (v == (1, 2)) is False, "both sides return NotImplemented: == falls back to identity"
    assert v + LoudVector(0, 0) == "LoudVector.__radd__ ran first", "subclass priority"

    songs = Playlist("a", "b", "c")
    assert len(songs) == 3 and songs[0] == "a" and songs[-1] == "c"
    assert songs[0:2] == ["a", "b"]
    assert "b" in songs and list(songs) == ["a", "b", "c"], "in and iteration fall back to __getitem__"
    assert list(reversed(songs)) == ["c", "b", "a"], "reversed uses __len__ and __getitem__"

    product = Product(5)
    assert product.price == 5 and vars(product) == {"_price": 5}
    assert error_text(lambda: Product(-1)) == "ValueError: price must be positive, got -1"

    account = Account()
    account.__dict__["balance"] = 999
    assert account.balance == 10, "a data descriptor beats the instance __dict__"
    g = Greeter()
    g.hello = lambda: "from the instance"
    assert g.hello() == "from the instance", "the instance __dict__ beats a non-data descriptor"

    for obj, name in [(product, "price"), (account, "balance"), (g, "hello"), (v, "x")]:
        model, real = lookup(obj, name), getattr(obj, name)
        assert model == real or (callable(model) and model() == real()), name
    assert lookup(Greeter(), "hello")() == "from the class"
    assert lookup(Product(1), "__module__") == "__main__"

    assert how_methods_bind() == ("from the class", True, True)
    call_ops = [(op.opname, op.argrepr) for op in dis.get_instructions(lambda g: g.hello())]
    assert ("LOAD_ATTR", "hello + NULL|self") in call_ops, "method call without a bound method"

    def raise_the_constant():
        raise NotImplemented  # wrong: NotImplemented is a value, not an exception

    assert error_text(raise_the_constant) == "TypeError: exceptions must derive from BaseException"
    fresh = Greeter()
    assert fresh.hello is not fresh.hello and fresh.hello == fresh.hello, "a new bound method each time"

    class FakeLength:
        def __len__(self):
            return 3

    sneaky = FakeLength()
    sneaky.__len__ = lambda: 99
    assert len(sneaky) == 3, "len() looks the method up on the type, not the instance"

    point = SlottedPoint(1, 2)
    assert not hasattr(point, "__dict__")
    assert object.__basicsize__ == 16 and SlottedPoint.__basicsize__ == 16 + 2 * 8, "header + two slots"
    assert type(SlottedPoint.__dict__["x"]).__name__ == "member_descriptor"
    assert hasattr(type(SlottedPoint.__dict__["x"]), "__set__"), "slots are data descriptors"
    assert error_text(lambda: setattr(point, "z", 3)) == (
        "AttributeError: 'SlottedPoint' object has no attribute 'z' and no __dict__ for setting new attributes")

    cash = Money(5)
    assert cash == Money(5, "GBP") and repr(cash) == "Money(amount=5, currency='GBP')"
    assert len({cash, Money(5)}) == 1, "frozen dataclasses are hashable"
    assert error_text(lambda: setattr(cash, "amount", 6)) == "FrozenInstanceError: cannot assign to field 'amount'"
    assert isinstance(FrozenInstanceError(), AttributeError)
    assert Basket.__hash__ is None
    assert error_text(bad_default) == (
        "ValueError: mutable default <class 'list'> for field items is not allowed: use default_factory")
    print("OK")


if __name__ == "__main__":
    main()
