# Writing Guide

## Purpose

Write durable, understandable technical knowledge rather than a collection of disconnected definitions.

## Recommended structure

1. **Problem and motivation:** Describe a concrete need.
2. **A single running example:** Keep input values and assumptions consistent.
3. **Observation:** Show what happens before naming the concept.
4. **Derivation:** Introduce notation, equations, and reasoning step by step.
5. **Interactive experiment:** Allow a parameter to change and explain its effect.
6. **Limitations:** State assumptions, edge cases, and failure modes.
7. **Connections:** Link prerequisite and related chapters.
8. **References:** Cite primary documentation, textbooks, or papers.

## Authoring conventions

- Write all public content in English.
- Use `.qmd` for Quarto pages.
- Use `$...$` for inline mathematics and `$$...$$` for display equations.
- For cross-referenced equations, use `$$ ... $$ {#eq-example}` and refer to `@eq-example`.
- Keep JavaScript experiments deterministic and browser-side when hosting on GitHub Pages.
- Add a new page to `website.sidebar.contents` in `_quarto.yml`.
- Distinguish explanations, code examples, assumptions, and numerical approximations.
- Do not copy copyrighted articles or technical books into the library.

## Example metadata

```yaml
---
title: "Concept Name"
description: "A concise statement of the problem addressed."
---
```

## Mathematical formalisation in computer-science lessons

**Global editorial requirement:** Use mathematical notation whenever it improves precision or abstraction. For every JVM and Microservices lesson, conclude with a **Mathematical Abstraction** section. Apply the same principle to future computer-science, algorithm and system-design chapters as appropriate.

1. Begin with one concrete running example; derive an abstraction from its actual states, actions, constraints or dependencies.
2. Define every symbol, domain, unit and condition that readers need to interpret the relation.
3. Prefer sets, functions, relations, transition systems, invariants, graphs, inequalities and equations according to what the concept really expresses.
4. Distinguish **normative guarantees**, **definitions**, **idealised models**, **approximations**, **heuristics** and **measured observations**. Do not present the last three as specification theorems.
5. Write LaTeX in Quarto using inline `$...$` and display `$$...$$`. Introduce notation in the body where it helps; close the lesson with a concise mathematical synthesis.
6. Explain the formal relation in clear English, map it back to the same code or example, and state limits or assumptions.
7. Do not add ornamental formulae just to make a page look academic. Where no useful formalisation exists, explain the limitation instead of inventing false precision.

### Good examples

- **JMM:** `hb = (po ∪ sw)⁺` expresses ordering (with correct symbol definitions).
- **JVM GC:** live objects form the root-reachable set of a reference graph.
- **Microservices:** model dependencies as a directed graph; model deadlines as inequalities and circuit breakers as state transitions.
- **Queues:** use Little's law only under appropriate assumptions, and distinguish it from a deterministic queue-growth approximation.

### Required closing section

Use `## Mathematical Abstraction`, include a display formula or formal relation when justified, and explain the symbols and assumptions immediately below it.
