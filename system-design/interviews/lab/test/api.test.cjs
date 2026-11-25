const { test } = require('node:test');
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const { setTimeout: sleep } = require('node:timers/promises');
const path = require('node:path');

test('NestJS HTTP adapter exposes tested algorithms on localhost', { timeout: 25000 }, async t => {
  const root = path.resolve(__dirname, '..');
  const port = 4313;
  const child = spawn(process.execPath, [path.join(root, 'dist/src/main.js')], {
    cwd: root, env: { ...process.env, PORT: String(port) }, stdio: ['ignore','pipe','pipe']
  });
  let logs = '';
  child.stdout.on('data', b => { logs += b.toString(); });
  child.stderr.on('data', b => { logs += b.toString(); });
  t.after(() => { if (child.exitCode === null) child.kill('SIGTERM'); });
  const url = 'http://127.0.0.1:' + port + '/lab/';
  let ready = false;
  for (let i=0;i<120;i++) {
    if (child.exitCode !== null) throw Error('App terminated: ' + logs);
    try {
      const r = await fetch(url + 'health', { signal: AbortSignal.timeout(250) });
      if (r.ok) { ready = true; break; }
    } catch { /* startup */ }
    await sleep(80);
  }
  assert.ok(ready, 'NestJS did not start: ' + logs);
  const send = async (route, body) => {
    const res = await fetch(url + route, {
      method:'POST', headers:{'content-type':'application/json'}, body:JSON.stringify(body)
    });
    assert.ok(res.ok, 'POST ' + route + ': HTTP ' + res.status);
    return res.json();
  };
  await send('jobs', {tenant:'A',id:'large',cost:8});
  await send('jobs', {tenant:'B',id:'small',cost:1});
  const picked = await (await fetch(url + 'jobs/next')).json();
  assert.equal(picked.id, 'small');
  const cache = await send('cache', {key:'x',version:2,value:'new'});
  assert.equal(cache.accepted,true);
  assert.equal((await send('cache',{key:'x',version:1,value:'old'})).accepted,false);
  const value = await (await fetch(url + 'cache?key=x')).json();
  assert.equal(value.value,'new');
  assert.equal((await send('rate',{key:'demo',nowMs:0})).allowed,true);
  assert.equal((await send('idempotency',{key:'receipt',fingerprint:'amount=10'})).invocations,1);
  assert.equal((await send('idempotency',{key:'receipt',fingerprint:'amount=10'})).invocations,1);
});
