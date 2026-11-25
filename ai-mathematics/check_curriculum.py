#!/usr/bin/env python3
"""Verify each book-aligned chapter and original lesson exists and is navigable."""
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
AI = ROOT / "ai-mathematics"
catalog = json.loads((AI / "curriculum.json").read_text(encoding="utf-8"))
sidebar = (ROOT / "_quarto.yml").read_text(encoding="utf-8")
assert len(catalog["chapters"]) == 11
assert [c["n"] for c in catalog["chapters"]] == list(range(2, 13))
assert sum(len(c["sections"]) for c in catalog["chapters"]) == 68
all_topics = []
for chapter in catalog["chapters"]:
    prefix = AI / "chapters" / (str(chapter["n"]).zfill(2) + "-" + chapter["slug"])
    rootindex = prefix / "index.qmd"
    assert rootindex.exists(), rootindex
    index_text = rootindex.read_text(encoding="utf-8")
    assert "##" in index_text and "mathematical" in index_text.lower(), rootindex
    assert str(rootindex.relative_to(ROOT)) in sidebar, rootindex
    for pos, (title, slug) in enumerate(chapter["sections"], 1):
        p = prefix / (slug + ".qmd")
        assert p.exists(), p
        body = p.read_text(encoding="utf-8")
        assert "title: " in body, p
        assert "##" in body and body.count("$$") >= 2, p
        assert "To be written" not in body and "placeholder" not in body.lower(), p
        assert len(body) > 850, (p, len(body))
        assert "§" + str(chapter["n"]) + "." + str(pos) in body, p
        assert str(p.relative_to(ROOT)) in sidebar, p
        all_topics.append(str(p))
assert len(all_topics) == len(set(all_topics)) == 68
for name in [
    "index.qmd", "linear-algebra.qmd", "calculus.qmd",
    "probability.qmd", "machine-learning.qmd",
    "interactives/gradient-descent.qmd", "interactives/pca-projection.qmd",
    "chapters/01-introduction/index.qmd", "examples/index.qmd",
]:
    p = AI / name
    assert p.exists(), p
    assert "chapter outline" not in p.read_text(encoding="utf-8").lower(), p
assert (AI / "examples" / "delivery_ml_lab.py").exists()
assert (AI / "examples" / "test_delivery_ml_lab.py").exists()
print("AI & Mathematics: 11 chapters, 68 written lessons, 2 interactives, Python lab, and navigation verified.")
