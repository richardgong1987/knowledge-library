import test from 'node:test';
import assert from 'node:assert/strict';
import { readdirSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { join } from 'node:path';

const algorithms = join(process.cwd(), 'algorithms');

// Every topic lab is one runnable file that checks the lesson's numbers and prints OK.
const RUNNERS = [
  { pattern: /Lab\.java$/, command: name => ['java', [name]] },    // compiled in memory by the source launcher
  { pattern: /_lab\.py$/, command: name => ['python3', ['-I', name]] },
  { pattern: /_lab\.sh$/, command: name => ['bash', [name]] },
];

const labs = readdirSync(algorithms, { withFileTypes: true })
  .filter(entry => entry.isDirectory())
  .flatMap(entry => readdirSync(join(algorithms, entry.name)).flatMap(name => {
    const runner = RUNNERS.find(r => r.pattern.test(name));
    return runner ? [{ topic: entry.name, folder: join(algorithms, entry.name), name, runner }] : [];
  }));

for (const { topic, folder, name, runner } of labs) {
  test(`${topic}/${name} runs and prints OK`, () => {
    const [program, args] = runner.command(name);
    const run = spawnSync(program, args, { cwd: folder, encoding: 'utf8', timeout: 60_000 });
    assert.equal(run.status, 0, run.stderr || run.stdout);
    assert.match(run.stdout, /^OK$/m);
  });
}
