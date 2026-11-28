#!/usr/bin/env python3
"""Audit 30 numerical identities, proofs, substitutions and Quarto navigation."""
from pathlib import Path
import re

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
GROUPS={
    "01-algebraic-identities":[
        "01-difference-of-squares","02-perfect-square-expansions",
        "03-cubes-sum-difference","04-completing-the-square",
        "05-rationalising-conjugates","06-difference-of-powers"],
    "02-sums-and-sequences":[
        "01-arithmetic-series","02-arithmetic-progression",
        "03-geometric-series","04-squares-sum",
        "05-telescoping-series","06-prefix-sum-identity"],
    "03-divisibility-and-modulo":[
        "01-divisibility","02-quotient-remainder",
        "03-equal-remainders","04-gcd-euclid",
        "05-lcm-gcd","06-multiply-modulo"],
    "04-inequalities-and-bounds":[
        "01-am-gm","02-square-nonnegative",
        "03-triangle-inequality","04-cauchy-schwarz",
        "05-ceil-floor","06-quadratic-discriminant"],
    "05-binary-and-counting":[
        "01-powers-of-two","02-bit-shift-and-mask",
        "03-xor-cancellation","04-count-set-bits",
        "05-binomials-pascal","06-digit-count-placevalue"]
}

def main():
    nav=(ROOT/"_quarto.yml").read_text()
    tips=(HERE.parent/"index.qmd").read_text()
    home=(HERE/"index.qmd").read_text()
    assert 'section: "Numerical & Algebraic Tricks"' in nav
    assert "numerical-math/index.qmd" in tips
    assert "difference of squares" in home.lower()
    assert "algorithm-math/index.qmd" in tips, "Preserve separate algorithm notes"
    assert 'section: "Algorithm Reasoning Patterns"' in nav
    count=0
    root="interviews/tips-and-notes/numerical-math/"
    for group,lessons in GROUPS.items():
        idx=(HERE/group/"index.qmd").read_text()
        assert f"{root}{group}/index.qmd" in nav
        for name in lessons:
            path=HERE/group/(name+".qmd")
            assert path.is_file(),path
            txt=path.read_text()
            assert len(txt)>850,(path,"too short")
            markers=list(re.finditer(r"\$\$",txt))
            assert len(markers)==2,(path,"Expected one main display identity")
            after=txt[markers[1].end():]
            assert after.lstrip().startswith("**Every symbol explained:**"),path
            assert "**Substitute actual numbers:**" in after[:1200],path
            assert "## Why it is true" in txt,path
            assert "## Your turn" in txt,path
            assert "## Assumptions and pitfalls" in txt,path
            assert name+".qmd" in idx,path
            assert f"{root}{group}/{name}.qmd" in nav,path
            count+=1
    assert count==30
    memory=(HERE/"memory-sheet.qmd").read_text()
    # The recall sheet now contains 50 numbered Markdown table rows.
    cards = [int(n) for n in re.findall(r"(?m)^\|\s*(\d+)\s*\|[^\n]+\|[^\n]+\|\s*$", memory)]
    assert cards == list(range(1, 51)), f"Expected recall cards 1–50 in order, got {cards}"
    assert (HERE/"examples/numerical_math_lab.py").is_file()
    assert (HERE/"examples/test_numerical_math_lab.py").is_file()
    assert f"{root}examples/numerical_math_lab.py" in nav
    print("PASS: 30 numerical and algebraic identity lessons with proofs and numeric substitutions")
    print("PASS: five topic directories, recall sheet, source files, Quarto navigation")
    
if __name__=="__main__":
    main()
