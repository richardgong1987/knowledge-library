#!/usr/bin/env python3
"""Check depth, maths explanations and navigation for all 28 original finance lessons.

Each main formula must immediately explain symbols and substitute numbers.
This verifies structural quality. Numerical correctness is independently tested
in test_finance_lab.py with deterministic expected values.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parent
GROUPS = {
    "foundations": [
        "01-cash-flow-timeline", "02-compounding-and-present-value",
        "03-npv-and-irr", "04-no-arbitrage", "05-total-return-vs-yield"
    ],
    "fixed-income": [
        "bond-pricing", "02-yield-and-total-return",
        "03-clean-dirty-and-accrued", "04-spot-rates-and-discount-factors",
        "05-curve-bootstrapping", "06-forward-rates", "duration",
        "07-convexity", "08-dv01-and-key-rate-risk",
        "09-immunization-and-liability-matching",
        "10-credit-spreads-default-recovery", "11-callable-mbs-and-oas",
        "12-carry-and-roll-down", "13-floating-rate-notes"
    ],
    "portfolio": [
        "01-return-variance-diversification",
        "02-markowitz-and-efficient-frontier", "03-capm-and-beta",
        "04-tracking-error-attribution", "05-var-expected-shortfall-stress"
    ],
    "derivatives": [
        "01-forwards-and-futures", "02-interest-rate-swaps",
        "03-binomial-option-pricing",
        "04-delta-hedging-and-replication"
    ]
}


def assert_math_explained(page: Path, content: str) -> None:
    tokens = list(re.finditer(r"\$\$", content))
    assert len(tokens) >= 2 and len(tokens) % 2 == 0, (
        f"Missing/unpaired display formula: {page}"
    )
    for i in range(0, len(tokens), 2):
        tail = content[tokens[i + 1].end(): tokens[i + 1].end() + 2000]
        # Quarto may attach a {#eq-...} label to a closing display delimiter.
        tail = re.sub(r"^\s*\{#eq-[^}]+\}", "", tail).lstrip()
        near = tail[:380].lower()
        assert re.search(r"symbol|means|notation|explain", near), (
            f"Formula has no immediate symbol explanation: {page}"
        )
        after = tail[:1750].lower()
        assert re.search(
            r"substitut|actual numbers|concrete numbers|numbers|calculate|"
            r"put.*number|real numbers|real value|numerical",
            after,
        ), f"Formula has no worked numerical substitution: {page}"


def main() -> None:
    yml = (REPO / "_quarto.yml").read_text()
    source_guide = (ROOT / "guides/books-and-learning-map.qmd").read_text()
    assert "Luenberger" in source_guide and "Fabozzi" in source_guide
    total = 0
    for group, chapters in GROUPS.items():
        overview = (ROOT / group / "index.qmd").read_text()
        for chapter in chapters:
            page = ROOT / group / (chapter + ".qmd")
            assert page.is_file(), f"Missing chapter {page}"
            text = page.read_text()
            assert len(text) > 1400, f"Chapter too thin: {page} ({len(text)})"
            assert "Luenberger" in text or "Fabozzi" in text, (
                f"Missing textbook basis: {page}"
            )
            assert_math_explained(page, text)
            assert chapter + ".qmd" in overview, (
                f"Not listed in group overview: {page}"
            )
            assert f"finance/{group}/{chapter}.qmd" in yml, (
                f"Not listed in site navigation: {page}"
            )
            total += 1
    assert total == 28, f"Unexpected total: {total}"
    for path in [
        "finance/index.qmd",
        "finance/guides/books-and-learning-map.qmd",
        "finance/examples/index.qmd",
    ]:
        assert (REPO / path).is_file(), f"Missing finance reference page {path}"
    assert "finance/examples/finance_lab.py" in yml, "Python source not published"
    print(f"PASS: {total} complete finance lessons; equations, examples and navigation.")
    print("PASS: reading map, textbook attribution and downloadable original code.")


if __name__ == "__main__":
    main()
