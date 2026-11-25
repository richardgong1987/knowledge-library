import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

const root = process.cwd();
const cat = JSON.parse(readFileSync(join(root, 'algorithms/leetcode-topic-catalog.json'), 'utf8'));
const sidebar = readFileSync(join(root, '_quarto.yml'), 'utf8');
const homepage = readFileSync(join(root, 'index.qmd'), 'utf8');

test('all 73 visible LeetCode topics have unique, navigable chapters', () => {
  assert.equal(cat.topics.length, 73, 'The 2026-11-25 official topic snapshot has 73 labels');
  assert.equal(new Set(cat.topics.map(t => t.name)).size, cat.topics.length);
  assert.equal(new Set(cat.topics.map(t => t.slug)).size, cat.topics.length);
  assert.equal(cat.groups.length, 7);
  assert.ok(sidebar.includes('href: algorithms/index.qmd'));
  assert.ok(homepage.includes('[Algorithms](algorithms/index.qmd)'));

  const groupIds = new Set(cat.groups.map(g => g.id));
  for (const group of cat.groups) {
    const path = join(root, 'algorithms/groups', group.id + '.qmd');
    assert.ok(existsSync(path), 'Missing group ' + group.id);
    assert.ok(sidebar.includes('algorithms/groups/' + group.id + '.qmd'));
  }

  const listed = new Set();
  for (const topic of cat.topics) {
    assert.ok(groupIds.has(topic.group), 'Unknown group: ' + topic.group);
    const relative = 'algorithms/' + topic.slug + '/index.qmd';
    const p = join(root, relative);
    assert.ok(existsSync(p), 'Missing topic file ' + relative);
    const body = readFileSync(p, 'utf8');
    assert.ok(body.includes('$'), 'No mathematical relationship: ' + relative);
    assert.ok(body.length > 2200, 'Lesson is not substantive: ' + relative);
    assert.ok(sidebar.includes('              - ' + relative), 'Missing sidebar entry: ' + relative);
    listed.add(topic.slug);
  }

  const folders = readdirSync(join(root, 'algorithms'), { withFileTypes: true })
    .filter(d => d.isDirectory() && !['groups', 'guides', 'examples'].includes(d.name)).map(d => d.name);
  assert.deepEqual(new Set(folders), listed, 'There are orphan or missing topic directories');
});
