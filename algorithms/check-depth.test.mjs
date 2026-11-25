import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { join } from 'node:path';

const root = process.cwd();
const catalog = JSON.parse(readFileSync(join(root, 'algorithms/leetcode-topic-catalog.json'), 'utf8'));
const read = path => readFileSync(join(root, path), 'utf8');

// Groups already rewritten to the Java, kid-first, math-first standard in WRITING_GUIDE.md.
// While writing, check more locally with ALGO_REWRITTEN=<group-id>,... or ALGO_TOPICS=<slug>,...
const fromEnv = name => (process.env[name] ?? '').split(',').filter(Boolean);
const REWRITTEN = new Set(['01-data-structures', '03-optimisation', '04-sorting-range', '05-graph-algorithms', ...fromEnv('ALGO_REWRITTEN')]);
const REWRITTEN_TOPICS = new Set(fromEnv('ALGO_TOPICS'));
const isRewritten = topic => REWRITTEN.has(topic.group) || REWRITTEN_TOPICS.has(topic.slug);

// LeetCode publishes no Hard problem under these tags, so their lessons end with the hardest available ones.
const NO_HARD_PROBLEM_ON_LEETCODE = new Set(['shell', 'concurrency', 'reservoir-sampling', 'rejection-sampling']);

const HEADINGS = [
  '## The problem',
  "## Explain it like I'm ten",
  '## From simple to advanced',
  '## Why it works and what it costs',
  '## Common mistakes',
  '## Interview problems',
  '## Mathematical Abstraction',
  '## References',
];

// Fenced code may contain `$$` (for example the shell PID), so formulas are read from prose only.
const stripFencedCode = text => text.replace(/^(`{3,}|~{3,})[^\n]*\n[\s\S]*?^\1[ \t]*$/gm, '');

const lastMatch = (text, regex) => Math.max(-1, ...[...text.matchAll(regex)].map(match => match.index));

// A display equation inside a worked example is the substitution itself, not a new formula.
const isWorkedSubstitution = before => lastMatch(before, /\bexample\b/gi) > lastMatch(before, /\bwhere\b/gi);

function formulasNeedingExplanation(prose) {
  const parts = prose.split('$$');
  const formulas = [];
  for (let i = 1; i < parts.length - 1; i += 2) {
    const before = parts.slice(0, i).join('$$').slice(-1500);
    if (!isWorkedSubstitution(before)) formulas.push({ formula: parts[i].trim(), after: parts.slice(i + 1).join('$$') });
  }
  return formulas;
}

const javaBlocks = text =>
  [...text.matchAll(/^```java[ \t]*\n([\s\S]*?)^```[ \t]*$/gm)].map(match => match[1]);

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

function checkRewrittenLesson(topic, text) {
  const path = `algorithms/${topic.slug}/index.qmd`;
  const folder = join(root, 'algorithms', topic.slug);

  let position = -1;
  for (const heading of HEADINGS) {
    const at = text.indexOf(heading);
    assert.ok(at > position, `${path}: "${heading}" missing or out of order`);
    position = at;
  }
  assert.ok(text.length > 9000, `${path}: too shallow (${text.length} chars)`);
  assert.ok(!text.includes('patterns.mjs'), `${path}: still points at the old JavaScript lab`);

  for (const { formula, after } of formulasNeedingExplanation(stripFencedCode(text))) {
    const label = `${path}: $$${formula.slice(0, 40)}…`;
    assert.ok(/\bwhere\b/i.test(after.slice(0, 400)), `symbols not explained right after ${label}`);
    assert.ok(/\bexample\b/i.test(after.slice(0, 2500)), `no numeric example after ${label}`);
  }

  const interviews = section(text, '## Interview problems');
  const problems = interviews.split('\n### ').length - 1;
  assert.ok(problems >= 2, `${path}: needs at least 2 interview problems`);
  assert.ok((interviews.match(/\*\*Math first\.\*\*/g) ?? []).length >= problems, `${path}: every problem starts with **Math first.**`);
  if (!NO_HARD_PROBLEM_ON_LEETCODE.has(topic.slug)) {
    assert.ok(/\(Hard\)/.test(interviews), `${path}: needs at least one Hard problem`);
  }

  const labs = readdirSync(folder).filter(name => /(Lab\.java|_lab\.py|_lab\.sh)$/.test(name));
  assert.ok(labs.length > 0, `${path}: no runnable lab file next to the lesson`);
  const labLines = labs.filter(name => name.endsWith('.java')).map(name => codeLines(readFileSync(join(folder, name), 'utf8')));
  for (const block of javaBlocks(text)) {
    const lines = codeLines(block);
    if (lines[0]?.startsWith('// sketch')) continue;
    assert.ok(labLines.some(lab => containsInOrder(lab, lines)),
      `${path}: this Java block is not an exact excerpt of the lab:\n${lines.slice(0, 3).join('\n')}`);
  }
}

// The original template check, kept only for groups that have not been rewritten yet.
function checkLegacyLesson(topic, text) {
  const path = `algorithms/${topic.slug}/index.qmd`;
  assert.ok(text.length > 2200, 'Insufficient lesson depth: ' + path);
  assert.ok(text.includes('$$'), 'No mathematical formalisation: ' + path);
  assert.ok(/(?:LeetCode|leetcode)/.test(text), 'No interview practice: ' + path);
}

test('every LeetCode topic is a substantive lesson', () => {
  assert.equal(catalog.topics.length, 73);
  for (const topic of catalog.topics) {
    const path = `algorithms/${topic.slug}/index.qmd`;
    assert.ok(existsSync(join(root, path)), 'Missing chapter: ' + path);
    const text = read(path);
    if (isRewritten(topic)) checkRewrittenLesson(topic, text);
    else checkLegacyLesson(topic, text);
  }
});

test('every group page links all of its topics', () => {
  for (const group of catalog.groups) {
    const page = read(`algorithms/groups/${group.id}.qmd`);
    assert.ok(page.length > 1300, 'Group page is empty: ' + group.id);
    for (const topic of catalog.topics.filter(t => t.group === group.id)) {
      assert.ok(page.includes(`../${topic.slug}/index.qmd`), `${group.id} does not link ${topic.slug}`);
    }
  }
});
