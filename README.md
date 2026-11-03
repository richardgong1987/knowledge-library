# Knowledge Library

An English-language technical knowledge website powered by [Quarto](https://quarto.org/), with LaTeX mathematics, searchable chapters and browser-based Observable JavaScript experiments.

## Topics

- **Programming Languages:** Java, Rust, Go, TypeScript, JavaScript, Python, C#, Prolog
- **DevOps:** Ansible, Kubernetes, CI/CD pipelines
- **Finance:** fixed income, bond pricing, duration, and future financial topics
- **AI & Mathematics:** linear algebra, calculus, probability, machine learning

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
