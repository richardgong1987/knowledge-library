#!/usr/bin/env python3
"""Audit the published, book-grounded mathematics puzzle curriculum."""
from pathlib import Path
import re

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[1]
GROUPS={
    "tutorial": [
        "tutorial-magic-square","tutorial-four-queens","tutorial-bridge-crossing",
    ],
    "easy": [
        "easy-01-river-crossing","easy-02-glove-selection",
        "easy-03-rectangle-dissection","easy-10-eight-coins",
        "easy-19-page-numbering","easy-20-maximum-path",
        "easy-32-tournament","easy-35-water-jugs",
    ],
    "medium": [
        "medium-57-rabbits","medium-79-lockers",
        "medium-81-celebrity","medium-94-sorted-table",
    ],
    "hard": [
        "hard-121-two-eggs","hard-130-binary-identification",
        "hard-141-josephus",
    ],
}

def main():
    nav=(ROOT/"_quarto.yml").read_text()
    main_index=(HERE/"index.qmd").read_text()
    home=(ROOT/"index.qmd").read_text()
    assert 'text: "Mathematics"' in nav and 'href: mathematics/index.qmd' in nav
    assert 'section: "Mathematical Puzzles"' in nav
    assert "mathematics/index.qmd" in home
    assert "Anany" in (HERE/"reading-map.qmd").read_text()
    expected=set(sum(GROUPS.values(),[]))
    found={p.stem for p in HERE.glob("*.qmd") if
           p.stem.startswith(("tutorial-","easy-","medium-","hard-"))}
    assert found==expected, f"Missing or unexpected puzzle chapters: {found^expected}"
    count=0
    for name in sorted(expected):
        path=HERE/(name+".qmd")
        content=path.read_text()
        assert len(content)>1800, f"Too short: {name}"
        assert "Levitin" in content and "printed" in content, (
            f"Unattributed book puzzle or missing printed page: {name}"
        )
        assert "## 1." in content and "## 7." in content
        assert len(re.findall(r"\$\$",content)) == 2, (
            f"Expected one carefully worked display formula: {name}"
        )
        closing=list(re.finditer(r"\$\$",content))[1].end()
        after=content[closing:closing+1400]
        # Text may have an equation label; definitions must still be adjacent.
        assert re.search(
            r"\*\*(Explain every symbol|What every symbol means):\*\*",
            after[:130],
        ),f"Symbols not defined immediately: {name}"
        assert re.search(
            r"\*\*Substitute actual numbers:\*\*",
            after[:1200],
        ),f"No worked arithmetic immediately after formula: {name}"
        assert name+".qmd" in main_index, f"Not linked in course index: {name}"
        assert "mathematics/mathematical-puzzles/"+name+".qmd" in nav, (
            f"Not linked in site sidebar: {name}"
        )
        count += 1

    for item in ("symbol-guide","strategy-guide","reading-map"):
        assert (HERE/(item+".qmd")).exists()
    assert (HERE/"examples/puzzle_math.py").exists()
    assert (HERE/"examples/test_puzzle_math.py").exists()
    assert "mathematics/mathematical-puzzles/examples/puzzle_math.py" in nav
    assert count==18
    print(f"PASS: all {count} mathematical puzzles have worked numbers, symbol definitions and proof sections")
    print("PASS: difficulty index, source references, navigation and calculator lab")

if __name__=="__main__":
    main()
