import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';

const root = process.cwd();
const catalog = JSON.parse(readFileSync(join(root,'algorithms/leetcode-topic-catalog.json'),'utf8'));
const read = p => readFileSync(join(root,p),'utf8');

test('every LeetCode topic is a substantive original worked lesson', () => {
  assert.equal(catalog.topics.length, 73);
  const names = new Set();
  for (const topic of catalog.topics) {
    const path = 'algorithms/' + topic.slug + '/index.qmd';
    assert.ok(existsSync(join(root,path)), 'Missing chapter: '+path);
    const text = read(path);
    assert.ok(text.length > 2200, 'Insufficient lesson depth: '+path+' '+text.length);
    assert.ok(!text.includes('A useful starting question is:'), 'Old outline remains: '+path);
    assert.ok(!text.includes('The formula captures a central structural relationship'), 'Old placeholder remains: '+path);
    assert.ok(!text.includes('To be written'), 'Placeholder remains: '+path);
    assert.ok(text.includes('$$'), 'No mathematical formalisation: '+path);
    assert.ok(/(?:proof|correctness|invariant|why it works|justified)/i.test(text), 'No argument: '+path);
    assert.ok(/(?:limitation|pitfall|counterexample|mistake|failure|assumption)/i.test(text), 'No assumptions or counterexample: '+path);
    assert.ok(/(?:O\(|Θ\(|omega|complexity|time and space|time and memory|cost)/i.test(text), 'No complexity discussion: '+path);
    assert.ok(/(?:LeetCode|leetcode)/.test(text), 'No interview practice: '+path);
    assert.ok(!names.has(topic.slug),'Duplicate slug');
    names.add(topic.slug);

    // Check first full display formula, and enforce adjacent meaning + actual values.
    const first = text.indexOf('$$');
    const end = text.indexOf('$$',first+2);
    assert.ok(end > first + 4, 'Broken display formula: '+path);
    const next = text.slice(end+2, end+1300);
    assert.ok(/(?:symbol|notation|what each|what every)/i.test(next.slice(0,280)),
      'Explain each symbol immediately after formula: '+path);
    assert.ok(/(?:substitut|actual|numeric|numbers|calculate|worked)/i.test(next.slice(0,1200)),
      'Show a numerical substitution after formula: '+path);
  }
});

test('the algorithm road map and all seven groups link to the new teaching guides',()=>{
  for (const name of ['read-the-math','choose-a-pattern','prove-and-count','clrs-roadmap']) {
    const file='algorithms/guides/'+name+'.qmd';
    assert.ok(existsSync(join(root,file)),file);
    assert.ok(read(file).length>2500,'Guide is empty: '+file);
  }
  const idx=read('algorithms/index.qmd');
  assert.ok(idx.includes('Understand first. Prove second. Code third.'));
  assert.ok(idx.includes('guides/read-the-math.qmd'));
  for(const group of catalog.groups){
    const path='algorithms/groups/'+group.id+'.qmd';
    assert.ok(read(path).length>1300);
    for(const t of catalog.topics.filter(t=>t.group===group.id))
      assert.ok(read(path).includes('../'+t.slug+'/index.qmd'),t.slug);
  }
});

test('all original pattern functions have a runnable source and tests',()=>{
  assert.ok(read('algorithms/examples/patterns.mjs').includes('export function'));
  assert.ok(read('algorithms/examples/patterns.test.mjs').includes('node:test'));
  assert.ok(read('algorithms/examples/README.md').includes('node --test'));
});
