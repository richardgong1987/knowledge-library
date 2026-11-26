# Run from programming-languages/python/labs with: python3 -I modules-and-imports.py
# The lab writes a tiny package into a temporary folder, puts that folder on
# sys.path, and imports from it. The strings below are the files it writes.
import contextlib
import importlib
import importlib.machinery
import importlib.util
import io
import subprocess
import sys
import tempfile
import types
from pathlib import Path

SHOP_INIT = '''\
# shop/__init__.py
print("loading shop")
'''

PRICES = '''\
# shop/prices.py
print("loading shop.prices")
TAX = 0.2


def with_tax(amount):
    return round(amount * (1 + TAX), 2)
'''

REPORT = '''\
# shop/report.py
from .prices import with_tax


def line(amount):
    return f"total {with_tax(amount)}"


if __name__ == "__main__":
    print(line(10))
'''

COUPONS = '''\
CODE = "SAVE5"
'''

BROKEN_ORDERS = '''\
# broken/orders.py
from broken.customers import greet

DISCOUNT = 5
'''

BROKEN_CUSTOMERS = '''\
# broken/customers.py
from broken.orders import DISCOUNT  # orders has not reached DISCOUNT yet


def greet(name):
    return f"hello {name}, {DISCOUNT}% off"
'''

FIXED_ORDERS = '''\
# fixed/orders.py
from fixed.customers import greet

DISCOUNT = 5
'''

FIXED_CUSTOMERS = '''\
# fixed/customers.py
from fixed import orders  # bind the module object, not a name inside it


def greet(name):
    return f"hello {name}, {orders.DISCOUNT}% off"  # looked up at call time
'''

FILES = {
    "shop/__init__.py": SHOP_INIT,
    "shop/prices.py": PRICES,
    "shop/report.py": REPORT,
    "shop/coupons.py": COUPONS,
    "broken/__init__.py": "",
    "broken/orders.py": BROKEN_ORDERS,
    "broken/customers.py": BROKEN_CUSTOMERS,
    "fixed/__init__.py": "",
    "fixed/orders.py": FIXED_ORDERS,
    "fixed/customers.py": FIXED_CUSTOMERS,
    "colorsys.py": "SHADOW = True\n",  # same name as a standard library module
    "one/plugins/alpha.py": 'NAME = "alpha"\n',  # no __init__.py: a namespace package
    "two/plugins/beta.py": 'NAME = "beta"\n',
}


def write_files(root):
    for name, text in FILES.items():
        path = root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)


# Level 1: the first import runs the file; later imports reuse sys.modules.
def import_twice():
    output = io.StringIO()
    with contextlib.redirect_stdout(output):
        import shop.prices
        import shop.prices  # found in sys.modules: the file does not run again
    return output.getvalue()


# Level 2: the same file, run as a program or imported as a module.
def run_python(root, *args):
    done = subprocess.run([sys.executable, *args], cwd=root, capture_output=True, text=True)
    return done.returncode, done.stdout, done.stderr.strip().splitlines()[-1:]


# Underneath: the import algorithm, simplified from importlib._bootstrap.
def mini_import(name):
    if name in sys.modules:
        return sys.modules[name]
    parent, _, child = name.rpartition(".")
    search_path = mini_import(parent).__path__ if parent else None
    for finder in sys.meta_path:
        spec = finder.find_spec(name, search_path)
        if spec is not None:
            break
    else:
        raise ModuleNotFoundError(f"No module named {name!r}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module  # registered before it runs: this is what makes cycles possible
    try:
        spec.loader.exec_module(module)
    except BaseException:
        del sys.modules[name]
        raise
    if parent:
        setattr(sys.modules[parent], child, module)
    return module


def import_error(name):
    try:
        importlib.import_module(name)
    except ImportError as error:
        return str(error).rsplit(" (", 1)[0]  # drop the trailing "(/path/to/file.py)"
    raise AssertionError("expected ImportError")


def check_import_once():
    assert import_twice() == "loading shop\nloading shop.prices\n"
    prices = sys.modules["shop.prices"]
    assert type(prices) is types.ModuleType and prices.__name__ == "shop.prices"
    assert prices.TAX == prices.__dict__["TAX"] == 0.2 and prices.with_tax(10) == 12.0
    assert sys.modules["shop"].prices is prices  # a submodule is an attribute of its package
    assert Path(prices.__file__).parts[-2:] == ("shop", "prices.py")
    assert Path(prices.__spec__.cached).name == "prices.cpython-314.pyc"
    assert Path(prices.__spec__.cached).exists()

    from shop.prices import TAX  # copies the current binding into this namespace
    prices.TAX = 0.25
    assert TAX == 0.2 and prices.with_tax(10) == 12.5  # the module changed, the copy did not
    prices.TAX = 0.2

    output = io.StringIO()
    with contextlib.redirect_stdout(output):
        assert importlib.reload(prices) is prices  # same object, file run again
    assert output.getvalue() == "loading shop.prices\n"


def check_main_and_packages(root):
    import shop.report
    assert shop.report.__name__ == "shop.report" and shop.report.__spec__.parent == "shop"
    assert shop.report.line(10) == "total 12.0"
    code, out, _ = run_python(root, "-m", "shop.report")
    assert code == 0 and out == "loading shop\nloading shop.prices\ntotal 12.0\n"
    code, out, last = run_python(root, "shop/report.py")
    assert code == 1 and last == ["ImportError: attempted relative import with no known parent package"]

    assert importlib.util.resolve_name(".prices", "shop") == "shop.prices"
    assert importlib.util.resolve_name("..tax", "shop.sub") == "shop.tax"


def check_finders(root):
    machinery = importlib.machinery
    order = [sys.meta_path.index(f) for f in (machinery.BuiltinImporter, machinery.FrozenImporter, machinery.PathFinder)]
    assert order == sorted(order)
    assert importlib.util.find_spec("sys").origin == "built-in"
    assert Path(importlib.util.find_spec("json").origin).parts[-2:] == ("json", "__init__.py")
    assert sys.path[0] == str(root)

    import colorsys  # our colorsys.py comes first on sys.path and wins
    assert colorsys.SHADOW and not hasattr(colorsys, "rgb_to_hsv")
    del sys.modules["colorsys"]

    sys.path += [str(root / "one"), str(root / "two")]  # two folders, each with plugins/
    import plugins.alpha
    import plugins.beta
    assert plugins.__file__ is None and len(plugins.__path__) == 2
    assert (plugins.alpha.NAME, plugins.beta.NAME) == ("alpha", "beta")
    del sys.path[-2:]

    coupons = mini_import("shop.coupons")
    assert coupons.CODE == "SAVE5" and sys.modules["shop"].coupons is coupons
    assert mini_import("shop.coupons") is coupons
    try:
        mini_import("shop.nothing")
    except ModuleNotFoundError as error:
        assert str(error) == "No module named 'shop.nothing'"


def check_cycles():
    message = import_error("broken.orders")
    assert message == ("cannot import name 'DISCOUNT' from partially initialized module "
                       "'broken.orders' (most likely due to a circular import)")
    assert "broken.orders" not in sys.modules  # a failed import is removed again
    import fixed.orders
    assert fixed.orders.greet("Ada") == "hello Ada, 5% off"


def main():
    with tempfile.TemporaryDirectory() as folder:
        root = Path(folder)
        write_files(root)
        sys.path.insert(0, str(root))
        try:
            check_import_once()
            check_main_and_packages(root)
            check_finders(root)
            check_cycles()
        finally:
            sys.path.remove(str(root))
    print("OK")


if __name__ == "__main__":
    main()
