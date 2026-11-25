#!/usr/bin/env python3
"""Check that language-chapter examples are independent, small and executable."""
import re
import shutil
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent

LANGUAGES = {
    "javascript": ("js", "node", "sample.mjs", 6),
    "python": ("python", "python3", "sample.py", 6),
    "go": ("go", "go", "main.go", 5),
    "java": ("java", "javac", "Main.java", 4),
}


def main():
    for name, (lang, executable, filename, expected) in LANGUAGES.items():
        text = (ROOT / f"{name}.qmd").read_text()
        blocks = re.findall(rf"~~~{lang}\s*\n(.*?)\n~~~", text, re.DOTALL)
        assert len(blocks) == expected, (
            f"Expected {expected} independent {name} snippets; got {len(blocks)}"
        )
        if not shutil.which(executable):
            print(f"SKIP {name}: {executable} is not on this runner")
            continue
        for i, block in enumerate(blocks, 1):
            with tempfile.TemporaryDirectory() as temp:
                path = Path(temp) / filename
                path.write_text(block)
                if name == "java":
                    command = ["javac", "Main.java"]
                    compiled = subprocess.run(command, cwd=temp, capture_output=True, text=True)
                    assert compiled.returncode == 0, (
                        f"Java example {i} does not compile:\n{compiled.stderr}"
                    )
                    command = ["java", "Main"]
                elif name == "go":
                    command = ["go", "run", "main.go"]
                elif name == "javascript":
                    command = ["node", "sample.mjs"]
                else:
                    command = ["python3", "sample.py"]
                done = subprocess.run(command, cwd=temp, capture_output=True, text=True, timeout=90)
                assert done.returncode == 0, (
                    f"{name} example {i} failed:\n{done.stderr}"
                )
                print(f"PASS {name} example {i}: {done.stdout.strip()[:90]}")
    print("Short language-snippet checks finished.")


if __name__ == "__main__":
    main()
