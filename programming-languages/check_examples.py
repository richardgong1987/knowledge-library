#!/usr/bin/env python3
"""Compile and run the four independent snippets on the Java page.

The other languages have per-chapter labs, checked by check-languages.test.mjs.
"""
import re
import shutil
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
EXPECTED_SNIPPETS = 4


def main():
    blocks = re.findall(r"~~~java\s*\n(.*?)\n~~~", (ROOT / "java.qmd").read_text(), re.DOTALL)
    assert len(blocks) == EXPECTED_SNIPPETS, f"Expected {EXPECTED_SNIPPETS} Java snippets; got {len(blocks)}"
    if not shutil.which("javac"):
        print("SKIP java: javac is not on this machine")
        return
    for i, block in enumerate(blocks, 1):
        with tempfile.TemporaryDirectory() as temp:
            (Path(temp) / "Main.java").write_text(block)
            compiled = subprocess.run(["javac", "Main.java"], cwd=temp, capture_output=True, text=True)
            assert compiled.returncode == 0, f"Java example {i} does not compile:\n{compiled.stderr}"
            done = subprocess.run(["java", "Main"], cwd=temp, capture_output=True, text=True, timeout=90)
            assert done.returncode == 0, f"Java example {i} failed:\n{done.stderr}"
            print(f"PASS java example {i}: {done.stdout.strip()[:90]}")


if __name__ == "__main__":
    main()
