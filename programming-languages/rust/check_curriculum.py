#!/usr/bin/env python3
"""Compile and run every tiny Rust lesson independently with official Rust 1.100.0.

The scripts intentionally do not combine examples into one large Rust programme.
The proc-macro example is a separate Cargo workspace and gets tested in CI.
"""
import re
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
EXPECTED = {
    "01-ownership": "hello",
    "02-borrowing": "Ada!",
    "03-lifetimes": "apple",
    "04-slices-and-strings": "hello",
    "05-enums-and-match": "5",
    "06-option-and-result": "Ok(42)",
    "07-traits-and-generics": "Ada",
    "08-collections-and-iterators": "4",
    "09-smart-pointers": "hello hello",
    "10-concurrency": "1",
    "11-async-await": "a future was created",
    "12-declarative-macros": "6",
    "13-attributes-and-proc-macros": "User { id: 7 }",
    "14-modules-cargo-and-tests": "6",
    "15-unsafe-and-ffi": "4",
    "16-copy-clone-and-drop": "3 3 hi hi",
}


def main():
    chapters = sorted(ROOT.glob("[0-9][0-9]-*.qmd"))
    assert {p.stem for p in chapters} == set(EXPECTED), "Incomplete Rust topic directory"
    with tempfile.TemporaryDirectory() as temp:
        for path in chapters:
            text = path.read_text()
            assert "## The question" in text, f"Missing question: {path.name}"
            assert ("## The rule to remember" in text or "## The core rule" in text), (
                f"Missing core rule: {path.name}"
            )
            assert "## Important limitation" in text or "## Common misunderstanding" in text, path.name
            blocks = re.findall(r"~~~rust\s*\n(.*?)\n~~~", text, re.DOTALL)
            assert len(blocks) == 1, f"One tiny executable Rust block expected: {path.name}"
            program = blocks[0]
            assert "fn main()" in program, f"Standalone main missing: {path.name}"
            if "$$" in text:
                assert "What every symbol means" in text or "**Symbols:**" in text
                assert "Substitute actual values" in text or "Concrete substitution" in text
            source = Path(temp) / "main.rs"
            binary = Path(temp) / "sample"
            source.write_text(program)
            compiled = subprocess.run(
                ["rustc", "+1.100.0", "--edition=2024", str(source), "-o", str(binary)],
                capture_output=True, text=True,
            )
            assert compiled.returncode == 0, (
                f"Rust compiler rejected {path.name}:\n{compiled.stderr}"
            )
            ran = subprocess.run(
                [str(binary)], capture_output=True, text=True, timeout=10,
            )
            assert ran.returncode == 0, f"Example crashed {path.name}:\n{ran.stderr}"
            assert EXPECTED[path.stem] in ran.stdout, (
                f"Unexpected output in {path.name}: {ran.stdout!r}"
            )
            print(f"PASS: {path.name}")
    print(f"All {len(chapters)} independent Rust lessons compile and run.")


if __name__ == "__main__":
    main()
