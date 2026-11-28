#!/usr/bin/env python3
"""Check 30 independent math-for-interview lessons and their Quarto navigation."""
from pathlib import Path
import re

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
GROUPS={
"01-state-search":["01-dfs-state-invariant","02-dfs-entry-exit-interval",
"03-backtracking-choose-explore-undo","04-search-tree-size",
"05-bfs-distance-layers","06-parity-and-graph-bipartite"],
"02-arrays-and-search":["01-prefix-sum-difference-of-prefixes",
"02-prefix-frequency-subarray-count","03-sliding-window-delta",
"04-two-pointers-monotonicity","05-binary-search-on-answer",
"06-difference-array-range-updates"],
"03-dynamic-and-greedy":["01-dp-state-recurrence-base","02-dp-knapsack-choice",
"03-lis-invariant","04-greedy-exchange-argument",
"05-amortised-two-pointer","06-bitmask-subset-recurrence"],
"04-counting-and-number-theory":["01-permutations-vs-combinations",
"02-inclusion-exclusion-overlaps","03-pigeonhole-lower-bound",
"04-modular-arithmetic-residue","05-euclid-gcd-invariant",
"06-triangular-numbers-interval-count"],
"05-advanced-patterns":["01-shortest-path-relaxation",
"02-dag-topological-order","03-binary-lifting-powers",
"04-monotonic-stack-amortisation","05-kadane-local-versus-global",
"06-rolling-hash-polynomial"]
}

def main():
    yml=(ROOT/"_quarto.yml").read_text()
    landing=(HERE/"index.qmd").read_text()
    tips=(HERE.parent/"index.qmd").read_text()
    assert "Thirty mathematical invariants" in landing
    assert "algorithm-math/index.qmd" in tips
    assert 'section: "Algorithm Mathematical Techniques"' in yml
    assert "algorithm-math/examples/algorithm_math_lab.py" in yml
    assert "algorithm-math/examples/test_algorithm_math_lab.py" in yml

    expected={f"{g}/{s}.qmd" for g,sub in GROUPS.items() for s in sub}
    actual={str(p.relative_to(HERE)) for g in GROUPS for p in (HERE/g).glob("[0-9][0-9]-*.qmd")}
    assert expected==actual, ("Mismatch in lesson set",expected^actual)

    total=0
    for group,lessons in GROUPS.items():
        group_index=(HERE/group/"index.qmd").read_text()
        assert f"algorithm-math/{group}/index.qmd" in yml
        for name in lessons:
            path=HERE/group/(name+".qmd")
            lesson=path.read_text()
            assert len(lesson)>1200,(path,"too short")
            assert "## Formula worth memorising" in lesson,path
            markers=list(re.finditer(r"\$\$",lesson))
            assert len(markers)==2,(path,"Expected one focused display formula")
            after=lesson[markers[1].end():]
            assert after.lstrip().startswith("**Every symbol explained:**"),path
            assert "**Substitute actual numbers:**" in after[:1250],path
            assert "## The 20-second memory card" in lesson,path
            assert "## " in lesson and "https://" in lesson,path
            assert "proof" in lesson.lower() or "why" in lesson.lower(),path
            assert name+".qmd" in group_index,path
            assert f"algorithm-math/{group}/{name}.qmd" in yml,path
            total+=1
    assert total==30
    memory=(HERE/"memory-sheet.qmd").read_text()
    for i in range(1,31):
        assert re.search(rf"(?m)^{i}\. ",memory),f"Missing recall card {i}"
    assert (HERE/"examples/index.qmd").exists()
    print(f"PASS: {total} full mathematical lessons in 5 subjects with immediate symbol explanations")
    print("PASS: 30 recall cards, numeric substitutions, navigation, sources and example assets")

if __name__=="__main__":
    main()
