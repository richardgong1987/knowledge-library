#!/usr/bin/env python3
"""Verify the published nine-chapter, 24-lesson linear algebra study path."""
from pathlib import Path
import re

HERE=Path(__file__).resolve().parent
ROOT=HERE.parent.parent
GROUPS={
"01-math-foundations":["01-solving-equations","02-functions-and-vectors"],
"02-intro-linear-algebra":["01-vectors-and-operations","02-dot-product-and-length","03-matrices-and-linearity"],
"03-computational-linear-algebra":["01-gaussian-elimination","02-determinant-and-inverse","03-matrix-multiplication"],
"04-geometric-linear-algebra":["01-lines-planes-and-span","02-projections","03-basis-and-dimension"],
"05-linear-transformations":["01-what-linearity-means","02-change-of-basis","03-invertibility-and-rank"],
"06-theoretical-linear-algebra":["01-eigenvalues-and-eigenvectors","02-gram-schmidt","03-matrix-decompositions"],
"07-applications":["01-least-squares","02-graphics-and-rotations","03-graphs-and-matrices"],
"08-probability":["01-probability-vectors","02-markov-and-pagerank"],
"09-quantum-mechanics":["01-dirac-notation","02-unitary-operators"]
}

def main():
    nav=(ROOT/"_quarto.yml").read_text()
    math=(ROOT/"mathematics/index.qmd").read_text()
    home=(HERE/"index.qmd").read_text()
    readmap=(HERE/"reading-map.qmd").read_text()
    assert "Linear Algebra Fundamentals" in math
    assert 'section: "Linear Algebra Fundamentals"' in nav
    assert "152-page extended" in home and "preview" in home
    assert "596-page full book" in readmap
    assert "linear-algebra/examples/linear_algebra_lab.py" in nav
    total=0
    for group,slugs in GROUPS.items():
        index=(HERE/group/"index.qmd").read_text()
        assert len(index)>400
        assert f"mathematics/linear-algebra/{group}/index.qmd" in nav
        for slug in slugs:
            path=HERE/group/(slug+".qmd")
            assert path.exists(),path
            content=path.read_text()
            assert len(content)>1450,(path,"not substantial enough")
            assert "Savov" in content and "preview" in content.lower()
            matches=list(re.finditer(r"\$\$",content))
            assert len(matches)==2,(path,"Expected one displayed focal formula")
            after=content[matches[1].end():]
            assert after.lstrip().startswith("**Every symbol explained:**"),path
            assert "**Substitute real numbers:**" in after[:1650],path
            assert "## Why" in content,path
            assert "## Your turn" in content,path
            assert f"{slug}.qmd" in index,path
            assert f"mathematics/linear-algebra/{group}/{slug}.qmd" in nav,path
            total+=1
    assert total==24
    print(f"PASS: {total} original linear algebra lessons in {len(GROUPS)} book-aligned groups")
    print("PASS: immediate symbol explanations, numerical substitutions and website navigation")

if __name__=="__main__":
    main()
