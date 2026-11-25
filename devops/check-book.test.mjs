import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { join } from 'node:path';

const root = process.cwd();
const read = path => readFileSync(join(root, path), 'utf8');

// Each published part: the minimum lesson count (so a deleted lesson is noticed) and the lab files it must ship.
const PARTS = {
  ansible: { minimum: 12, lab: ['examples/README.md', 'examples/site.yml', 'examples/roles/lemonade_web/tasks/main.yml'] },
  'ci-cd': { minimum: 9, lab: ['examples/lemonade-stand/server.mjs', 'examples/lemonade-stand/server.test.mjs', 'examples/lemonade-stand/workflows/ci.yml'] },
  gcp: { minimum: 9, lab: ['examples/README.md', 'examples/deploy-cloud-run.sh'] },
};

const LESSON_HEADINGS = [
  '## The problem',
  "## Explain it like I'm ten",
  '## Rules and conventions',
  '## Common mistakes',
  '## Mathematical Abstraction',
  '## References',
];

const lessonsOf = part =>
  readdirSync(join(root, 'devops', part))
    .filter(name => /^\d{2}-[a-z0-9-]+\.qmd$/.test(name))
    .sort()
    .map(name => `devops/${part}/${name}`);

// Fenced code can legitimately contain `$$` (the shell PID), so formulas are read from prose only.
const stripFencedCode = text => text.replace(/^(`{3,}|~{3,})[^\n]*\n[\s\S]*?^\1[ \t]*$/gm, '');

function displayFormulas(prose) {
  const parts = prose.split('$$');
  const formulas = [];
  for (let i = 1; i < parts.length - 1; i += 2) {
    formulas.push({
      formula: parts[i].trim(),
      before: parts.slice(0, i).join('$$').slice(-1500),
      after: parts.slice(i + 1).join('$$'),
    });
  }
  return formulas;
}

// A display equation that sits inside a worked example (the last marker before it is "Example",
// not a "where" list) is the numeric substitution itself, not a new formula needing its own symbols.
const lastMatch = (text, regex) => Math.max(-1, ...[...text.matchAll(regex)].map(match => match.index));
const isWorkedSubstitution = before => lastMatch(before, /\bexample\b/gi) > lastMatch(before, /\bwhere\b/gi);

test('the book landing page links every part and explains the notation', () => {
  const landing = read('devops/index.qmd');
  for (const part of Object.keys(PARTS)) assert.ok(landing.includes(`${part}/index.qmd`), part);
  assert.ok(landing.includes('## How to read the math in this book'));
});

for (const [part, { minimum }] of Object.entries(PARTS)) {
  test(`${part}: every lesson is complete, in the sidebar, and teaches its math with numbers`, () => {
    const sidebar = read('_quarto.yml');
    const partIndex = `devops/${part}/index.qmd`;
    assert.ok(existsSync(join(root, partIndex)), partIndex);
    assert.ok(sidebar.includes(partIndex), `Part index not in sidebar: ${partIndex}`);

    const lessons = lessonsOf(part);
    assert.ok(lessons.length >= minimum, `${part} has ${lessons.length} lessons, expected ${minimum}`);

    for (const lesson of lessons) {
      const text = read(lesson);
      assert.ok(sidebar.includes(lesson), `Not in sidebar: ${lesson}`);
      assert.ok(read(partIndex).includes(lesson.split('/').pop()), `Not linked from part index: ${lesson}`);
      assert.ok(text.length > 6000, `Too shallow (${text.length} chars): ${lesson}`);
      for (const heading of LESSON_HEADINGS) assert.ok(text.includes(heading), `${lesson} lacks "${heading}"`);
      assert.ok(/\| *What it (does|means|measures) *\|/.test(text), `No keyword reference table: ${lesson}`);

      const formulas = displayFormulas(stripFencedCode(text));
      assert.ok(formulas.length > 0, `No display formula: ${lesson}`);
      for (const { formula, before, after } of formulas) {
        if (isWorkedSubstitution(before)) continue;
        const label = `${lesson}: $$${formula.slice(0, 40)}…`;
        assert.ok(/\bwhere\b/i.test(after.slice(0, 400)), `Symbols not explained right after ${label}`);
        assert.ok(/\bexample\b/i.test(after.slice(0, 2500)), `No numeric example after ${label}`);
      }
    }
  });
}

for (const [part, { lab }] of Object.entries(PARTS)) {
  test(`${part}: the lab ships its runnable files`, () => {
    for (const file of lab) assert.ok(existsSync(join(root, 'devops', part, file)), `devops/${part}/${file}`);
  });
}
