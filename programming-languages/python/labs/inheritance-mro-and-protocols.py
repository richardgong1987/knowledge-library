# Run from programming-languages/python/labs with: python3 -I inheritance-mro-and-protocols.py
import dis
from abc import ABC, abstractmethod
from collections.abc import Sized
from types import SimpleNamespace
from typing import Protocol, runtime_checkable


def error_text(action):
    try:
        action()
    except Exception as error:
        return f"{type(error).__name__}: {error}"


# The problem: calling parents by name runs the shared base twice.
class Base:
    def __init__(self, log):
        log.append("Base")


class NaiveLeft(Base):
    def __init__(self, log):
        log.append("Left")
        Base.__init__(self, log)  # names the parent explicitly


class NaiveRight(Base):
    def __init__(self, log):
        log.append("Right")
        Base.__init__(self, log)


class NaiveBottom(NaiveLeft, NaiveRight):
    def __init__(self, log):
        log.append("Bottom")
        NaiveLeft.__init__(self, log)
        NaiveRight.__init__(self, log)


# Level 1: a subclass overrides a method and extends it with super().
class Sender:
    def __init__(self, name):
        self.name = name

    def send(self, text):
        return f"{self.name}: {text}"


class EmailSender(Sender):
    def send(self, text):
        return super().send(text) + " [email]"  # extend the parent's behaviour


# Level 2: in a diamond, super() follows the MRO, so each class runs once.
class Left(Base):
    def __init__(self, log):
        log.append("Left")
        super().__init__(log)


class Right(Base):
    def __init__(self, log):
        log.append("Right")
        super().__init__(log)


class Bottom(Left, Right):
    def __init__(self, log):
        log.append("Bottom")
        super().__init__(log)


# Level 3: the C3 linearisation, written out.
def c3(cls):
    # L[C] = C + merge(L[B1], ..., L[Bn], [B1, ..., Bn])
    sequences = [c3(base) for base in cls.__bases__] + [list(cls.__bases__)]
    result = [cls]
    while any(sequences):
        sequences = [seq for seq in sequences if seq]
        for seq in sequences:
            head = seq[0]
            if not any(head in other[1:] for other in sequences):
                break  # a good head: it is in no list's tail
        else:
            raise TypeError("no consistent order")
        result.append(head)
        sequences = [[c for c in seq if c is not head] for seq in sequences]
    return result


class A:
    pass


class B(A):
    pass


def inconsistent_bases():
    class X(A, B):  # A must come before B, but B is a subclass of A
        pass


# Level 4: a mixin adds one behaviour and passes the call along with super().
class ShoutMixin:
    def send(self, text):
        return super().send(text.upper())  # adds one behaviour, then passes on


class LoudEmailSender(ShoutMixin, EmailSender):
    pass


# Level 5: an abstract base class refuses to be instantiated until filled in.
class Shape(ABC):
    @abstractmethod
    def area(self): ...

    def describe(self):  # concrete methods can use the abstract ones
        return f"area {self.area()}"


class Square(Shape):
    def __init__(self, side):
        self.side = side

    def area(self):
        return self.side**2


class Shelf:
    def __len__(self):  # no inheritance, yet it is a Sized
        return 3


# Level 6: duck typing. Anything with a send method will do.
class Pigeon:  # unrelated to Sender, but it can send
    def send(self, text):
        return f"coo: {text}"


def broadcast(senders, text):
    return [sender.send(text) for sender in senders]


# Level 7: typing.Protocol is duck typing that a type checker can verify.
@runtime_checkable
class CanSend(Protocol):
    def send(self, text: str) -> str: ...


class WrongSend:
    def send(self):  # wrong signature, but the name is there
        return "?"


class NotRuntime(Protocol):
    def send(self, text: str) -> str: ...


@runtime_checkable
class Named(Protocol):
    name: str


def main():
    naive = []
    NaiveBottom(naive)
    assert naive == ["Bottom", "Left", "Base", "Right", "Base"], "Base ran twice"

    assert EmailSender("ann").send("hi") == "ann: hi [email]"
    assert isinstance(EmailSender("ann"), Sender) and issubclass(EmailSender, Sender)

    log = []
    Bottom(log)
    assert log == ["Bottom", "Left", "Right", "Base"], "each class once, in MRO order"
    assert Bottom.__mro__ == (Bottom, Left, Right, Base, object)
    assert Left.__init__.__code__.co_freevars == ("__class__",), "super() reads the __class__ cell"
    assert "LOAD_SUPER_ATTR" in [op.opname for op in dis.get_instructions(Left.__init__)]

    def outside_a_class(self):
        return super().send("hi")  # no __class__ cell here

    assert error_text(lambda: outside_a_class(Sender("ann"))) == "RuntimeError: super(): __class__ cell not found"

    assert c3(Bottom) == list(Bottom.__mro__), "the model agrees with CPython"
    assert c3(LoudEmailSender) == list(LoudEmailSender.__mro__)
    assert error_text(lambda: c3(SimpleNamespace(__bases__=(A, B)))) == "TypeError: no consistent order"
    assert error_text(inconsistent_bases) == (
        "TypeError: Cannot create a consistent method resolution order (MRO) for bases A, B")

    assert LoudEmailSender("ann").send("hi") == "ann: HI [email]"
    assert LoudEmailSender.__mro__ == (LoudEmailSender, ShoutMixin, EmailSender, Sender, object)
    assert error_text(lambda: ShoutMixin().send("hi")) == "AttributeError: 'super' object has no attribute 'send'"

    assert error_text(Shape) == (
        "TypeError: Can't instantiate abstract class Shape without an implementation for abstract method 'area'")
    assert Square(3).describe() == "area 9"
    assert Shape.__abstractmethods__ == frozenset({"area"})
    assert isinstance(Shelf(), Sized) and Sized not in Shelf.__mro__, "__subclasshook__ checks for __len__"

    assert broadcast([EmailSender("ann"), Pigeon()], "hi") == ["ann: hi [email]", "coo: hi"]
    assert error_text(lambda: broadcast([42], "hi")) == "AttributeError: 'int' object has no attribute 'send'"

    assert isinstance(Pigeon(), CanSend) and CanSend not in Pigeon.__mro__, "structural, not nominal"
    assert not isinstance(42, CanSend)
    assert isinstance(WrongSend(), CanSend), "runtime checks only look for the name"
    assert error_text(lambda: isinstance(Pigeon(), NotRuntime)) == (
        "TypeError: Instance and class checks can only be used with @runtime_checkable protocols")
    assert error_text(lambda: issubclass(Sender, Named)) == (
        "TypeError: Protocols with non-method members don't support issubclass(). Non-method members: 'name'.")
    assert isinstance(Sender("ann"), Named), "isinstance works: the instance has a name"
    assert error_text(CanSend) == "TypeError: Protocols cannot be instantiated"
    print("OK")


if __name__ == "__main__":
    main()
