#!/usr/bin/env python3
"""Validate the three computing-foundations subjects and worked formulas."""
from pathlib import Path
import re

HERE=Path(__file__).resolve().parent
ROOT=HERE.parent
SECTIONS={
    "operating-system-principles": [
        "01-processes-and-threads","02-cpu-scheduling",
        "03-virtual-memory-and-paging","04-synchronization-and-deadlocks"
    ],
    "compiler-principles": [
        "01-lexing-and-parsing","02-types-and-semantic-analysis",
        "03-intermediate-representation-and-ssa","04-optimization-and-correctness"
    ],
    "quantum-computing": [
        "01-qubits-and-measurement","02-gates-and-matrices",
        "03-entanglement-and-bell-states","04-quantum-circuits-and-sampling"
    ]
}


def main():
    nav=(ROOT/"_quarto.yml").read_text()
    home=(ROOT/"index.qmd").read_text()
    assert 'text: "Operating Systems"' in nav
    assert "operating-systems/index.qmd" in home
    assert "operating-systems/examples/foundations_lab.py" in nav
    count=0
    for section,chapters in SECTIONS.items():
        landing=(HERE/section/"index.qmd").read_text()
        assert f"operating-systems/{section}/index.qmd" in nav
        for slug in chapters:
            p=HERE/section/(slug+".qmd")
            assert p.exists(),p
            doc=p.read_text()
            assert len(doc)>1300,(p,"too brief")
            assert "## Start with an ordinary situation" in doc
            assert "## Why it works" in doc
            assert "## Common misunderstanding" in doc
            matches=list(re.finditer(r"\$\$",doc))
            assert len(matches)==2,(p,"formula must be one complete display equation")
            after=doc[matches[1].end():]
            assert after.lstrip().startswith("**Every symbol explained:**"),p
            assert "**Substitute actual numbers:**" in after[:1700],p
            assert slug+".qmd" in landing,p
            assert f"operating-systems/{section}/{slug}.qmd" in nav,p
            assert "https://" in doc,p
            count+=1
    assert count==12
    print(f"PASS: {count} substantial lessons, mathematical explanations, worked substitutions and navigation")
    print("PASS: three learning paths and downloadable one-file lab")


if __name__=="__main__":
    main()
