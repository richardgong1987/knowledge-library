# Knowledge Library

An English-language technical knowledge website powered by [Quarto](https://quarto.org/), with LaTeX mathematics, searchable chapters and browser-based Observable JavaScript experiments.

## Topics

- **Programming Languages:** core concepts with a runnable lab per chapter: Go (15 chapters), Python (13), JavaScript (12), TypeScript (12), C# (13), Prolog (10), Rust (16), and Java (including JVM Topics → Java Memory Model)
- **Algorithms:** 73 LeetCode topics in seven categories, each taught from simple to advanced with a child-level explanation, every formula followed by its symbols and real numbers, and hard interview problems solved math-first. Every topic has a runnable Java lab (SQL for Database, Bash for Shell) that checks each number its lesson quotes; lesson code is an exact excerpt of the lab
- **System Design:** fully written fundamentals plus 18 advanced mathematically grounded interview cases (fair scheduling, RAG, idempotency, cache coherence, feeds, search, consistency), with a runnable and tested NestJS 12 teaching lab, alongside the microservices course
- **DevOps:** a hands-on book in six parts (Ansible, k3s, CI/CD, AWS, Google Cloud, Terraform) built around one running example, the Lemonade Stand, with child-friendly analogies, keyword-by-keyword reference tables, labs verified on real tools (Ansible, a k3s cluster, LocalStack, Terraform) and worked numerical formulas
- **Finance:** fixed income, bond pricing, duration, and future financial topics
- **AI & Mathematics:** 11 original mathematical chapter guides, 68 worked topic lessons aligned with *Mathematics for Machine Learning*, a dependency-free Python lab, and interactive experiments

## Edit and preview locally

1. Install [Quarto](https://quarto.org/docs/get-started/) and VS Code with the Quarto extension.
2. Clone this repository.
3. Run `quarto preview` from its root.
4. Edit `.qmd` files in **Visual** or **Source** mode.
5. Run `quarto render` to verify the entire site.

The site renders to `_site/`, which is not committed.

## Writing principles

Start with a concrete problem, keep one example throughout the explanation, show observations, derive the concept, and only then introduce abstraction. Use LaTeX for equations and Observable JS (`ojs`) for browser-side interactive models. Prefer standalone Markdown-compatible prose where possible.

See [the writing guide](WRITING_GUIDE.md).

## GitHub Pages

A workflow at `.github/workflows/pages.yml` builds the website and deploys its static HTML on every push to `main`.

In repository **Settings → Pages → Build and deployment**, set **Source** to **GitHub Actions**. The expected site URL is:

https://richardgong1987.github.io/knowledge-library/

GitHub Pages runs client-side JavaScript but does not run persistent Python/Node.js application servers. The interactive bond example runs entirely in the browser.

## Licence

No licence has been selected yet. Do not reuse site content without permission.
