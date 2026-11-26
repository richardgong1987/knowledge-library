# Run from programming-languages/python/labs with: python3 -I exceptions-and-context-managers.py
import contextlib


# Level 1: the order in which try, except, else and finally run.
def run_steps(text):
    steps = []
    try:
        steps.append("try")
        int(text)
    except ValueError:
        steps.append("except")
    else:
        steps.append("else")
    finally:
        steps.append("finally")
    return steps


def unmatched(steps):
    try:
        steps.append("try")
        {}["port"]  # KeyError, which the handler below does not match
    except ValueError:
        steps.append("except")
    finally:
        steps.append("finally")


def finally_beats_return(steps):
    try:
        return "from try"
    finally:
        steps.append("finally ran")


# Level 2: a handler matches its class and every subclass; the first match wins.
def classify(error):
    try:
        raise error
    except ZeroDivisionError:
        return "zero division"
    except ArithmeticError:
        return "arithmetic"
    except Exception:
        return "other"


class BankError(Exception):
    """Base class for every error this module raises."""


class InsufficientFundsError(BankError):
    def __init__(self, balance, amount):
        super().__init__(f"cannot withdraw {amount} from a balance of {balance}")
        self.balance = balance
        self.amount = amount


def withdraw(balance, amount):
    if amount > balance:
        raise InsufficientFundsError(balance, amount)
    return balance - amount


# Level 3: chaining. "from" sets __cause__; raising inside except sets __context__.
class ConfigError(Exception):
    pass


def read_port(text):
    try:
        return int(text)
    except ValueError as error:
        raise ConfigError(f"port must be a number, got {text!r}") from error


def read_port_implicit(text):
    try:
        return int(text)
    except ValueError:
        raise ConfigError("port must be a number")


def read_port_quiet(text):
    try:
        return int(text)
    except ValueError:
        raise ConfigError("port must be a number") from None


def parse_line(number, text):
    try:
        return int(text)
    except ValueError as error:
        error.add_note(f"while parsing line {number}")
        raise


def caught(function, *args):
    try:
        function(*args)
    except BaseException as error:
        return error
    raise AssertionError("expected an exception")


# Level 4: look before you leap, or ask forgiveness.
def port_lbyl(config):
    if "port" in config:
        return config["port"]
    return 80


def port_eafp(config):
    try:
        return config["port"]
    except KeyError:
        return 80


# Level 5: a context manager is any object with __enter__ and __exit__.
class Tracked:
    def __init__(self, log, suppress=False):
        self.log = log
        self.suppress = suppress

    def __enter__(self):
        self.log.append("enter")
        return self

    def __exit__(self, exc_type, exc, tb):
        self.log.append(("exit", exc_type))
        return self.suppress  # a true value swallows the exception


@contextlib.contextmanager
def transaction(account):
    snapshot = dict(account)
    try:
        yield account
    except Exception:
        account.clear()
        account.update(snapshot)  # roll back, then let the error continue
        raise


@contextlib.contextmanager
def resource(name, log):
    log.append(f"open {name}")
    try:
        yield name
    finally:
        log.append(f"close {name}")


def use_all(names, log):
    with contextlib.ExitStack() as stack:
        opened = [stack.enter_context(resource(name, log)) for name in names]
        log.append(f"using {len(opened)}")


@contextlib.contextmanager
def opened_without_finally(log):
    log.append("open")
    yield
    log.append("close")  # skipped when the body raises


# Level 6: several errors at once, handled by type with except*.
def validate(form):
    errors = []
    if not form.get("name"):
        errors.append(ValueError("name is required"))
    if not isinstance(form.get("age"), int):
        errors.append(TypeError("age must be an int"))
    if errors:
        raise ExceptionGroup("invalid form", errors)


def report(form):
    handled = []
    try:
        validate(form)
    except* ValueError as group:
        handled.append(("value", len(group.exceptions)))
    except* TypeError as group:
        handled.append(("type", len(group.exceptions)))
    return handled


# Underneath: the "as" name is deleted when the except clause ends.
def name_after_except():
    try:
        int("x")
    except ValueError as error:
        pass
    try:
        return error
    except UnboundLocalError:
        return "deleted"


def main():
    assert run_steps("42") == ["try", "else", "finally"]
    assert run_steps("forty") == ["try", "except", "finally"]
    steps = []
    try:
        unmatched(steps)
    except KeyError:
        steps.append("caller")
    assert steps == ["try", "finally", "caller"]
    steps = []
    assert finally_beats_return(steps) == "from try" and steps == ["finally ran"]

    assert str(caught(int, "eighty")) == "invalid literal for int() with base 10: 'eighty'"
    assert classify(ZeroDivisionError()) == "zero division"
    assert classify(OverflowError()) == "arithmetic"
    assert classify(KeyError("port")) == "other"
    assert isinstance(caught(classify, KeyboardInterrupt()), KeyboardInterrupt)
    assert ZeroDivisionError.__mro__ == (ZeroDivisionError, ArithmeticError, Exception, BaseException, object)
    assert KeyboardInterrupt.__mro__ == (KeyboardInterrupt, BaseException, object)

    assert withdraw(100, 30) == 70
    error = caught(withdraw, 100, 130)
    assert isinstance(error, BankError) and str(error) == "cannot withdraw 130 from a balance of 100"
    assert error.amount - error.balance == 30

    explicit = caught(read_port, "eighty")
    assert str(explicit) == "port must be a number, got 'eighty'"
    assert isinstance(explicit.__cause__, ValueError) and explicit.__suppress_context__
    implicit = caught(read_port_implicit, "eighty")
    assert implicit.__cause__ is None and isinstance(implicit.__context__, ValueError)
    assert not implicit.__suppress_context__
    quiet = caught(read_port_quiet, "eighty")
    assert quiet.__cause__ is None and quiet.__suppress_context__
    assert isinstance(quiet.__context__, ValueError)  # still recorded, just not printed
    assert caught(parse_line, 3, "x").__notes__ == ["while parsing line 3"]

    assert port_lbyl({"port": 8080}) == port_eafp({"port": 8080}) == 8080
    assert port_lbyl({}) == port_eafp({}) == 80

    log = []
    with Tracked(log):
        log.append("body")
    assert log == ["enter", "body", ("exit", None)]
    log = []
    try:
        with Tracked(log):
            raise ValueError("boom")
    except ValueError:
        log.append("propagated")
    assert log == ["enter", ("exit", ValueError), "propagated"]
    log = []
    with Tracked(log, suppress=True):
        raise ValueError("boom")
    log.append("after with")
    assert log == ["enter", ("exit", ValueError), "after with"]

    account = {"balance": 100}
    try:
        with transaction(account):
            account["balance"] -= 30
            raise ValueError("card declined")
    except ValueError:
        pass
    assert account == {"balance": 100}
    with transaction(account):
        account["balance"] -= 30
    assert account == {"balance": 70}

    log = []
    use_all(["a", "b", "c"], log)
    assert log == ["open a", "open b", "open c", "using 3", "close c", "close b", "close a"]
    log = []
    with contextlib.suppress(OSError):
        with contextlib.ExitStack() as stack:
            stack.enter_context(resource("a", log))
            raise OSError("cannot open b")
    assert log == ["open a", "close a"]
    log = []
    with contextlib.suppress(ValueError):
        with opened_without_finally(log):
            raise ValueError("boom")
    assert log == ["open"]

    assert report({"name": "", "age": "ten"}) == [("value", 1), ("type", 1)]
    assert report({"name": "", "age": 10}) == [("value", 1)]
    assert report({"name": "Ada", "age": 36}) == []
    group = ExceptionGroup("invalid form", [ValueError("a"), TypeError("b")])
    assert str(group) == "invalid form (2 sub-exceptions)"
    matched, rest = group.split(ValueError)
    assert [type(e) for e in matched.exceptions] == [ValueError]
    assert [type(e) for e in rest.exceptions] == [TypeError]

    assert name_after_except() == "deleted"
    print("OK")


if __name__ == "__main__":
    main()
