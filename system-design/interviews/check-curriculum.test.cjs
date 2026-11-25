const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = process.cwd();
const catalog = JSON.parse(fs.readFileSync(path.join(root, 'system-design/interviews/catalog.json'), 'utf8'));
const yaml = fs.readFileSync(path.join(root, '_quarto.yml'), 'utf8');
const guide = fs.readFileSync(path.join(root, 'system-design/index.qmd'), 'utf8');

test('18 original advanced chapters, five groups, code and sidebar are complete', () => {
  assert.equal(catalog.topics.length, 18);
  assert.equal(catalog.groups.length, 5);
  assert.equal(new Set(catalog.topics.map(t => t.slug)).size, 18);
  assert.ok(guide.includes('interviews/index.qmd'));
  const items = new Set(catalog.topics.map(t => t.slug));
  for (const g of catalog.groups) {
    const groupPage = 'system-design/interviews/groups/' + g.id + '.qmd';
    assert.ok(fs.existsSync(path.join(root, groupPage)), groupPage);
    assert.ok(yaml.includes(groupPage), groupPage);
    for (const slug of g.topics) {
      assert.ok(items.has(slug), slug);
      const file = 'system-design/interviews/' + g.id + '/' + slug + '.qmd';
      assert.ok(fs.existsSync(path.join(root, file)), file);
      assert.ok(yaml.includes(file), 'Not in Quarto sidebar: ' + file);
      const text = fs.readFileSync(path.join(root, file), 'utf8');
      assert.ok(text.length > 2200, 'Too shallow: ' + file);
      assert.ok(text.includes('## Mathematical Abstraction'), 'Missing formal model: ' + file);
      assert.ok(text.includes('~~~ts') || text.includes('~~~sql'), 'Missing actual code: ' + file);
      assert.ok(text.includes('lab/') || text.includes('/lab/'), 'Missing lab link: ' + file);
    }
  }
  for (const file of ['fundamentals.qmd','distributed-systems.qmd','data-consistency.qmd']) {
    const text = fs.readFileSync(path.join(root,'system-design',file),'utf8');
    assert.ok(text.length > 2000);
    assert.ok(text.includes('## Mathematical Abstraction'));
    assert.ok(!text.includes('This chapter is an outline'));
  }
  for (const file of [
    'system-design/interviews/lab/src/core.ts',
    'system-design/interviews/lab/src/main.ts',
    'system-design/interviews/lab/test/core.test.cjs',
    'system-design/interviews/lab/package.json',
    'system-design/interviews/lab/README.md'
  ]) assert.ok(fs.existsSync(path.join(root,file)), file);
});
