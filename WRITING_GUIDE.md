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
