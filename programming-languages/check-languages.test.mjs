// Checks every language chapter against WRITING_GUIDE.md, "Standard for Programming Languages chapters",
// then runs every chapter's lab. Run from the repository root:
//   node --test programming-languages/check-languages.test.mjs
// ONLY=go/10-channels,python checks just the chapters whose "<language>/<file>" starts with one of the prefixes.
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { join } from 'node:path';

const root = join(process.cwd(), 'programming-languages');

const CHAPTERS = {
  go: [
    'values-types-and-pointers', 'slices-and-maps', 'strings-bytes-and-runes', 'structs-methods-and-embedding',
    'interfaces', 'generics', 'errors-defer-and-panic', 'functions-closures-and-iterators',
    'goroutines-and-the-scheduler', 'channels', 'select-and-context', 'sync-and-the-memory-model',
    'concurrency-patterns', 'packages-modules-and-testing', 'memory-and-garbage-collection',
  ],
  python: [
    'names-objects-and-references', 'built-in-collections', 'functions-scope-and-closures', 'iterators-and-generators',
    'decorators', 'classes-and-the-data-model', 'inheritance-mro-and-protocols', 'exceptions-and-context-managers',
    'type-hints', 'modules-and-imports', 'memory-and-garbage-collection', 'threads-processes-and-the-gil', 'coroutines-and-asyncio',
  ],
  javascript: [
    'values-types-and-coercion', 'scope-hoisting-and-closures', 'this-and-function-calls', 'objects-and-prototypes',
    'classes', 'iterators-and-generators', 'the-event-loop', 'promises', 'async-await', 'modules', 'errors',
    'memory-and-garbage-collection',
  ],
  typescript: [
    'structural-typing', 'inference-and-literal-types', 'unions-and-narrowing', 'discriminated-unions', 'generics',
    'keyof-typeof-and-indexed-access', 'mapped-and-utility-types', 'conditional-types-and-infer',
    'template-literal-types', 'classes-and-interfaces', 'any-unknown-and-runtime-validation', 'variance-and-soundness',
  ],
  csharp: [
    'value-and-reference-types', 'nullable-types', 'classes-structs-and-records', 'inheritance-and-interfaces',
    'generics-and-variance', 'delegates-lambdas-and-events', 'iterators-and-yield', 'linq', 'pattern-matching',
    'exceptions-and-disposal', 'tasks-and-async-await', 'threads-and-synchronisation', 'memory-gc-and-span',
  ],
  prolog: [
    'facts-rules-and-queries', 'unification', 'resolution-and-backtracking', 'recursion', 'lists',
    'arithmetic-and-clpfd', 'cut-and-negation', 'accumulators-and-difference-lists', 'all-solutions-and-higher-order',
    'definite-clause-grammars',
  ],
};

// fence: the code-block tag whose blocks must be lab excerpts. comment: the line-comment marker for "sketch".
// run: the command that runs one lab and must print OK. checks: commands that must pass before it.
// All commands run from the language's labs/ folder.
const LANGUAGES = {
  go: {
    fence: 'go', comment: '//', lab: slug => `${slug}/main.go`,
    checks: slug => [['go', ['vet', `./${slug}/...`]], ...(hasGoTests(slug) ? [['go', ['test', '-race', `./${slug}/...`]]] : [])],
    run: slug => ['go', ['run', '-race', `./${slug}`]],
  },
  python: { fence: 'python', comment: '#', lab: slug => `${slug}.py`, run: slug => ['python3', ['-I', `${slug}.py`]] },
  javascript: { fence: 'js', comment: '//', lab: slug => `${slug}.mjs`, run: slug => ['node', [`${slug}.mjs`]] },
  typescript: { fence: 'ts', comment: '//', lab: slug => `${slug}.ts`, run: slug => ['node', [`${slug}.ts`]] },
  csharp: { fence: 'csharp', comment: '//', lab: slug => `${slug}.cs`, run: slug => ['dotnet', ['run', '--no-cache', `${slug}.cs`]] },
  prolog: {
    fence: 'prolog', comment: '%', lab: slug => `${slug}.pl`,
    run: slug => ['swipl', ['-q', '-g', 'main', '-t', 'halt', `${slug}.pl`]],
  },
};

const hasGoTests = slug => readdirSync(join(root, 'go', 'labs', slug), { recursive: true }).some(name => name.endsWith('_test.go'));

// tsc checks the whole labs folder at once; each chapter then owns the errors reported in its own file.
function typeErrorsByFile() {
  const done = spawnSync('npx', ['--no-install', 'tsc', '-p', '.'], { cwd: join(root, 'typescript', 'labs'), encoding: 'utf8' });
  if (done.error) throw done.error;
  const errors = new Map();
  for (const line of done.stdout.split('\n').filter(Boolean)) {
    const file = line.match(/^([\w.-]+\.ts)\(/)?.[1] ?? '(project)';
    errors.set(file, [...(errors.get(file) ?? []), line]);
  }
  return errors;
}

const HEADINGS = [
  '## The problem',
  "## Explain it like I'm ten",
  '## From simple to advanced',
  '## How it works underneath',
  '## Common mistakes',
  '## Interview questions',
  '## Summary',
  '## References',
];

const fileName = (index, slug) => `${String(index + 1).padStart(2, '0')}-${slug}.qmd`;

const ONLY = (process.env.ONLY ?? '').split(',').map(s => s.trim()).filter(Boolean);
const selected = (language, file) => ONLY.length === 0 || ONLY.some(prefix => `${language}/${file}`.startsWith(prefix));

// A language still on its old single page has not been rewritten yet, so its chapters are not required.
const rewritten = language => !existsSync(join(root, `${language}.qmd`));

const stripFencedCode = text => text.replace(/^(`{3,}|~{3,})[^\n]*\n[\s\S]*?^\1[ \t]*$/gm, '');

const lastMatch = (text, regex) => Math.max(-1, ...[...text.matchAll(regex)].map(match => match.index));

// A display equation inside a worked example is the substitution itself, not a new formula.
// Substitutions contain numbers, so a formula without a digit always needs its own "where".
const isWorkedSubstitution = (before, formula) =>
  /\d/.test(formula) && lastMatch(before, /\bexample\b/gi) > lastMatch(before, /\bwhere\b/gi);

function formulasNeedingExplanation(prose) {
  const parts = prose.split('$$');
  const formulas = [];
  for (let i = 1; i < parts.length - 1; i += 2) {
    const before = parts.slice(0, i).join('$$').slice(-1500);
    const formula = parts[i].trim();
    if (!isWorkedSubstitution(before, formula)) formulas.push({ formula, after: parts.slice(i + 1).join('$$') });
  }
  return formulas;
}

const codeBlocks = text => [...text.matchAll(/^```([\w-]*)[ \t]*\n([\s\S]*?)^```[ \t]*$/gm)]
  .map(match => ({ tag: match[1], code: match[2] }));

const codeLines = code => code.split('\n').map(line => line.trim()).filter(Boolean);

function containsInOrder(haystack, needle) {
  for (let start = 0; start + needle.length <= haystack.length; start++) {
    if (needle.every((line, i) => haystack[start + i] === line)) return true;
  }
  return false;
}

const section = (text, heading) => {
  const start = text.indexOf(heading);
  const end = text.indexOf('\n## ', start + heading.length);
  return text.slice(start, end < 0 ? undefined : end);
};

function checkChapter(language, slug, file) {
  const path = `programming-languages/${language}/${file}`;
  const { fence, comment, lab } = LANGUAGES[language];
  const text = readFileSync(join(root, language, file), 'utf8');

  let position = -1;
  for (const heading of HEADINGS) {
    const at = text.indexOf(heading);
    assert.ok(at > position, `${path}: "${heading}" missing or out of order`);
    position = at;
  }
  assert.ok(text.length > 7000, `${path}: too shallow (${text.length} characters, needs more than 7000)`);
  assert.ok(/^### Level 3/m.test(text), `${path}: needs at least three levels under "From simple to advanced"`);
  assert.ok(text.includes('::: {.callout-tip}'), `${path}: the ten-year-old explanation belongs in a callout-tip`);

  const formulas = formulasNeedingExplanation(stripFencedCode(text));
  assert.ok(formulas.length > 0, `${path}: needs at least one display formula for its core idea`);
  for (const { formula, after } of formulas) {
    const label = `${path}: $$${formula.slice(0, 40)}…`;
    assert.ok(/\bwhere\b/i.test(after.slice(0, 400)), `symbols not explained right after ${label}`);
    assert.ok(/\bexample\b/i.test(after.slice(0, 2500)), `no numeric example after ${label}`);
  }

  const interviews = section(text, '## Interview questions');
  const questions = interviews.split('\n### ').length - 1;
  assert.ok(questions >= 3, `${path}: needs at least 3 interview questions`);
  assert.ok((interviews.match(/\*\*Answer\.\*\*/g) ?? []).length >= questions, `${path}: every question needs an **Answer.**`);

  assert.ok(existsSync(join(root, language, 'labs', lab(slug))), `${path}: missing its lab labs/${lab(slug)}`);
  const sources = labSources(language, slug).map(file => codeLines(readFileSync(file, 'utf8')));
  for (const { tag, code } of codeBlocks(text)) {
    assert.ok([fence, 'text', 'bash'].includes(tag), `${path}: code block tagged "${tag}"; use ${fence}, text or bash`);
    if (tag !== fence) continue;
    const lines = codeLines(code);
    if (lines[0]?.startsWith(`${comment} sketch`)) continue;
    assert.ok(sources.some(source => containsInOrder(source, lines)),
      `${path}: this ${fence} block is not an exact excerpt of its lab labs/${lab(slug)}:\n${lines.slice(0, 3).join('\n')}`);
  }
}

// A Go lab is a folder, so its tests and subpackages are lab code too; every other lab is one file.
function labSources(language, slug) {
  if (language !== 'go') return [join(root, language, 'labs', LANGUAGES[language].lab(slug))];
  const folder = join(root, 'go', 'labs', slug);
  return readdirSync(folder, { recursive: true }).filter(name => name.endsWith('.go')).map(name => join(folder, name));
}

function checkIndex(language) {
  const path = `programming-languages/${language}/index.qmd`;
  assert.ok(existsSync(join(root, language, 'index.qmd')), `missing ${path}`);
  const text = readFileSync(join(root, language, 'index.qmd'), 'utf8');
  CHAPTERS[language].forEach((slug, i) => {
    assert.ok(text.includes(`(${fileName(i, slug)})`), `${path} does not link ${fileName(i, slug)}`);
  });
}

function run(language, [command, args], what) {
  const cwd = join(root, language, 'labs');
  const done = spawnSync(command, args, { cwd, encoding: 'utf8', timeout: 180_000 });
  if (done.error?.code === 'ENOENT') {
    assert.ok(!process.env.CI, `${command} is not installed on the CI runner`);
    return `skipped: ${command} not installed`;
  }
  const output = `${done.stdout}\n${done.stderr}`.trim();
  assert.equal(done.status, 0, `${what} failed:\n${output.slice(-2000)}`);
  // Compiler and runtime warnings (C# nullable, Prolog singletons, Python deprecations) fail the lab.
  assert.ok(!/\bwarning\b/i.test(output), `${what} printed a warning:\n${output.slice(-2000)}`);
  return done.stdout;
}

for (const language of Object.keys(CHAPTERS)) {
  const chapters = CHAPTERS[language]
    .map((slug, i) => ({ slug, file: fileName(i, slug) }))
    .filter(({ file }) => selected(language, file));

  const skip = ONLY.length === 0 && !rewritten(language) && 'old page still in place';

  test(`${language}: every chapter meets the standard`, { skip }, () => {
    if (ONLY.length === 0 || ONLY.includes(language)) checkIndex(language);
    for (const { slug, file } of chapters) {
      assert.ok(existsSync(join(root, language, file)), `missing chapter programming-languages/${language}/${file}`);
      checkChapter(language, slug, file);
    }
  });

  // One subtest per lab, so a failing lab does not hide the result of the labs after it.
  test(`${language}: every lab runs and prints OK`, { skip }, async t => {
    if (chapters.length === 0) return;
    const typeErrors = language === 'typescript' ? typeErrorsByFile() : null;
    if (typeErrors && ONLY.length === 0) assert.ok(!typeErrors.has('(project)'), typeErrors.get('(project)')?.join('\n'));
    for (const { slug } of chapters) {
      const { lab, checks, run: command } = LANGUAGES[language];
      await t.test(`labs/${lab(slug)}`, () => {
        const errors = typeErrors?.get(lab(slug)) ?? [];
        assert.equal(errors.length, 0, `tsc reports errors in labs/${lab(slug)}:\n${errors.join('\n')}`);
        for (const check of checks?.(slug) ?? []) run(language, check, `${check.flat().join(' ')}`);
        const stdout = run(language, command(slug), `${language} lab labs/${lab(slug)}`);
        if (!stdout.startsWith('skipped')) assert.match(stdout, /^OK$/m, `${language} lab labs/${lab(slug)} did not print OK`);
      });
    }
  });
}
