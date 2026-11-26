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

## AI & Mathematics curriculum writing standards

- Keep chapter sequencing and source-section citations traceable to *Mathematics for Machine Learning* (Deisenroth, Faisal and Ong, 2020), without copying the book's prose, illustrations, examples or exercises.
- Use the original six-order delivery dataset across foundational and applied lessons, extending it only where necessary.
- Every lesson needs a **real problem**, mathematically stated relationship, **worked calculation**, interpretation, assumptions or failure cases, and a question that can be solved without memorising a definition.
- Distinguish an **exact theorem** from a fitted estimate or simplified pedagogical model; label illustrative algorithms that do not implement full numerical optimisation.
- Prefer Python standard-library examples that run with a single file; do not silently require optional packages or incompatible library versions.
- Do not copy book PDFs into this public repository. Link to https://mml-book.com/ for authorised reading.

## Advanced system-design interview chapters

- The intellectual difficulty must come from **formal guarantees, failure schedules, counterexamples and trade-offs**, not from unnecessary infrastructure.
- Each interview problem includes a question, an explicit naive design, a violating sequence, mathematics, original small TypeScript/NestJS code, production limitations and difficult follow-ups.
- Distinguish a **single-process proof** from cross-replica correctness; document where atomic database writes, leases, fencing, durable queues or broker semantics become necessary.
- Link to public Meta/Google/Microsoft research and interview-practice resources as *inspiration*, without claiming a specific company asked a private or verbatim question.
- Keep pure algorithms separately executable and test edge cases: duplicates, unknown outcomes, out-of-order events, version reversals, expired leases, and starvation.
- Use stable current language/framework releases; verify with official documentation and CI before claiming a successful build.

## Non-negotiable mathematical teaching requirement

**For every mathematics or algorithm formula anywhere in a chapter:** immediately after the displayed formula, provide (1) a **symbol-by-symbol explanation** including domain, units and indexing conventions; and then (2) **a real numerical substitution** showing each step. The reader must be able to reproduce the calculation. Repeat this for every later formula, not only the first one. Avoid ornamental formulas that add no explanatory value.

### Standard for Algorithms chapters

The model lesson is [`algorithms/hash-table/`](algorithms/hash-table/index.qmd). `algorithms/check-depth.test.mjs` enforces the structure, and `algorithms/check-labs.test.mjs` runs every lab.

**Teach like a patient teacher of a ten-year-old, then go all the way to interview level.** Simple first, advanced last, and never skip a step in between.

Sections, in this order and with these exact headings:

1. `## The problem`: one tiny concrete problem with real values, plus why the obvious method is too slow or wrong.
2. `## Explain it like I'm ten`: a `::: {.callout-tip}` analogy (lockers, queues of children, a maze). Cite well-known teachers where they fit: CLRS, Sedgewick and Wayne (Java), Knuth, Skiena, Bentley, Erickson, Bhargava. Verify every attribution.
3. `## From simple to advanced`: `### Level 1`, `### Level 2`, and so on. Each level adds one idea: the idea, then its math with numbers, then short Java. Cover the real variants of the topic, not only the textbook one. A hash table must cover every collision strategy (linked-list chains, red-black-tree bins as in `java.util.HashMap`, linear, quadratic and double-hash probing, Robin Hood, cuckoo). Backtracking must write out the state-space tree, its node count, the pruning condition and the recurrence.
4. `## Why it works and what it costs`: an invariant or induction proof in plain words, and costs derived by counting operations on the example.
5. `## Common mistakes`: a table of mistake, symptom and fix.
6. `## Interview problems`: two to four problems, ordered by difficulty, at least one LeetCode **(Hard)**. The exception is a tag with no Hard problem on LeetCode (Shell, Concurrency, Reservoir Sampling, Rejection Sampling), which ends with the hardest problem available. Each is `### N. LeetCode <id>: <title> (<difficulty>)` with **Problem.**, **Math first.** (the model as a formula, symbols defined, numbers from the example substituted), **Idea.**, the Java solution, and **Answer.** (the traced result and the complexity).
7. `## Mathematical Abstraction`: the core of the topic in one or two display formulas, with symbols and one numeric example.
8. `## References`: CLRS 4th edition chapter, Sedgewick and Wayne or other sources used, and LeetCode problem links.

**Math.** After every display formula, within a few lines, write a list starting "where" that defines each symbol in plain words. Then give an "Example:" that substitutes the lesson's own numbers step by step to a final value. Do this for every formula. A display equation that is itself the substitution needs no second explanation.

**Code (Java).**
- **The lab file.** Each topic has one lab file, `algorithms/<slug>/<Name>Lab.java`. It is a single file that runs with `java <Name>Lab.java`, builds with only the JDK, and compiles on Java 21 and later without preview features.
- **The lab checks itself.** Its `main` checks every number the lesson quotes, plus edge cases, and prints `OK`.
- **Lesson code comes from the lab.** Every ```java block in the lesson must be an exact excerpt of the lab. A block whose first line is `// sketch` is exempt; use that only for deliberately wrong code.
- **Short and plain.** Keep code and examples concise and easy to understand: one idea per block, usually 5–20 lines, the plainest correct version, with optimisations described in prose. Never pile code up.
- **Other languages where they fit.** The Database topic uses SQL, with a lab `database_lab.py` that runs the queries in Python's built-in `sqlite3`. The Shell topic uses Bash, with a lab `shell_lab.sh`. Each prints `OK`.

**Honesty.**
- Cite CLRS 4th edition chapters where the concept is covered. Do not pretend that LeetCode tags such as Shell or Sliding Window are textbook chapters.
- Write all explanations, code and examples independently. Use LeetCode discussions only for insight and pitfalls.

**A page consisting of generic definitions, one unexplained formula, a prompt to study, or no actual worked example is not a complete lesson.**

### Standard for Programming Languages chapters

Applies to Go, Python, JavaScript, TypeScript, C# and Prolog (Java and Rust keep their own structure). The model chapter is [`programming-languages/go/10-channels.qmd`](programming-languages/go/10-channels.qmd). `programming-languages/check-languages.test.mjs` holds the chapter list, enforces this standard and runs every lab; `ONLY=go/10-channels` narrows it to some chapters.

**Each language is a folder.** `programming-languages/<language>/index.qmd` introduces the language and links every chapter; chapters are `NN-<slug>.qmd`; labs live in `<language>/labs/`.

**Teach the language's core concepts, like a patient teacher of a ten-year-old, then go all the way to interview level.** A chapter is about what the language guarantees and how it works underneath, not a syntax tour.

Sections, in this order and with these exact headings:

1. `## The problem`: a concrete problem with real values that this concept solves, and what goes wrong without it.
2. `## Explain it like I'm ten`: a `::: {.callout-tip}` analogy. Cite the language's designers and well-known teachers where they fit, and verify every attribution and quote.
3. `## From simple to advanced`: `### Level 1`, `### Level 2`, at least three levels. Each level adds one idea: the idea, its math where it fits with numbers, then short code from the lab and its result.
4. `## How it works underneath`: the runtime or compiler mechanism (data structures, the scheduler, the event loop, the type checker's rule), checked against the official source.
5. `## Common mistakes`: a table of mistake, symptom (the exact error message where there is one) and fix.
6. `## Interview questions`: at least three `### N. <question>` entries, each with an **Answer.** that is precise and, where useful, backed by lab code.
7. `## Summary`: the core idea in one display formula, then a short bullet list of rules.
8. `## References`: official documentation and specifications first, then the books, talks and articles used, with working links.

**Math.** Every chapter states its core idea in at least one display formula. After every display formula, within a few lines, write a list starting "where" that defines each symbol in plain words, then an "Example" that substitutes the chapter's own values step by step. Use only what both KaTeX and MathJax render.

**Code.**

- **One lab per chapter**, which checks every result the chapter quotes and prints `OK` last.

  | Language | Lab | Run, from `labs/` |
  |---|---|---|
  | Go 1.27 | `<slug>/main.go` (one module, `labs/go.mod`) | `go run -race ./<slug>`, plus `go vet ./...` |
  | Python 3.14 | `<slug>.py` | `python3 -I <slug>.py` |
  | JavaScript (Node 26) | `<slug>.mjs` | `node <slug>.mjs` |
  | TypeScript 7 | `<slug>.ts` | `npx tsc -p .` (strict, erasable syntax only), then `node <slug>.ts` |
  | C# 14 (.NET 10) | `<slug>.cs` | `dotnet run --no-cache <slug>.cs` (`labs/global.json` pins the .NET 10 SDK, because a file-based app targets whichever SDK runs it; `labs/Directory.Build.props` turns off Native AOT) |
  | SWI-Prolog 10 (CI also runs Ubuntu's 9.2) | `<slug>.pl` | `swipl -q -g main -t halt <slug>.pl`; never assert exact `statistics(inferences)` counts, which change between releases |

- **Lesson code comes from the lab.** Every code block tagged with the language (`go`, `python`, `js`, `ts`, `csharp`, `prolog`) must be an exact excerpt of the chapter's lab, compared line by line after trimming. A block whose first line is a `sketch` comment (`// sketch`, `# sketch`, `% sketch`) is exempt; use it only for code that cannot run in the lab, such as a compile error, a deadlock, or trimmed runtime source. Program output and REPL or Prolog query transcripts go in `text` blocks; commands go in `bash` blocks.
- **Prove compile-time claims.** TypeScript labs use `// @ts-expect-error` for every type error the chapter shows, so `tsc` fails if the error ever disappears.
- **No warnings.** A lab that prints a compiler or runtime warning fails.
- **Short and plain.** One idea per block, usually 5–20 lines, the plainest correct version. Never pile code up.

## Finance lessons — beginner clarity without sacrificing rigour

Use the three supplied references as the basis:
- David G. Luenberger, *Investment Science*, 2nd ed. (2014); 1st ed. (1998) for cross-checking historical structure.
- Frank J. Fabozzi (ed.), *The Handbook of Fixed Income Securities*, 8th ed. (2012).

Do not reproduce copyrighted textbook paragraphs, tables, exercises or images. Attribute the specific chapter and write a **new** miniature example. Avoid treating historical market-specific convention details as automatically current.

Each finance page must contain a **dated cash-flow story**, one important concept, an exact formula, the meaning of every symbol, a substitution with actual numerical values immediately afterwards, an explanation of **why** the equation is valid, and an explicit list of assumptions/conditions that break the shortcut.

Prefer extending the same bond (face 100, annual coupon 5, two years remaining) rather than inventing an unrelated bond for every section. For advanced concepts such as duration, convexity, spot/forward rates, bootstrapping, default/recovery, portfolio covariance, CAPM and option pricing, never erase the derivation just because it seems difficult. Translate the math into ordinary language, then back into a tiny Python demonstration. All interest rates in code are **decimals**, so 5% is 0.05 and 1bp is 0.0001.

The Finance lab must stay dependency-free and must pass its numeric tests before publication. Explicitly distinguish market prices from contractual payments, yield from realised return, and risk-neutral parameters from observed historical probabilities.
