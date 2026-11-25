const { test } = require('node:test');
const assert = require('node:assert/strict');
const {
  DeficitScheduler, TokenBucket, IdempotencyGate, VersionedCache,
  SequencedInbox, rendezvous, HybridFeed, mergeTopK, SeatHolds,
  Autocomplete, PoliteFrontier, chunkFile
} = require('../dist/src/core.js');

test('deficit round robin handles many tiny tasks and large finite costs', () => {
  const q = new DeficitScheduler(2);
  q.enqueue({id:'large',tenant:'A',cost:8});
  for (let i=0;i<8;i++) q.enqueue({id:'b'+i,tenant:'B',cost:1});
  const picked = [];
  while(q.depth()) picked.push(q.next());
  assert.equal(picked.length,9);
  assert.ok(picked.findIndex(x=>x.id==='large') < 8,'large task must not starve');
  assert.equal(new Set(picked.map(x=>x.id)).size,9);
});
test('two continuously backlogged equal-cost flows alternate under equal quanta', () => {
  const q=new DeficitScheduler(1);
  for(let i=0;i<5;i++){q.enqueue({id:'a'+i,tenant:'a',cost:1});q.enqueue({id:'b'+i,tenant:'b',cost:1});}
  const selected=Array.from({length:10},()=>q.next().tenant);
  assert.deepEqual(selected,['a','b','a','b','a','b','a','b','a','b']);
});
test('large file splits into bounded-cost jobs for admission', () => {
  const j=chunkFile('tenant-1','file-7',23,5);
  assert.deepEqual(j.map(x=>x.cost),[5,5,5,5,3]);
  assert.equal(j.reduce((sum,x)=>sum+x.cost,0),23);
});
test('token bucket accepts a burst, then refills at a measured rate', () => {
  const b=new TokenBucket(2,1);
  assert.equal(b.allow('A',0),true);
  assert.equal(b.allow('A',0),true);
  assert.equal(b.allow('A',0),false);
  assert.equal(b.allow('A',500),false);
  assert.equal(b.allow('A',1000),true);
  assert.equal(b.allow('B',1000),true);
});
test('idempotency coalesces same operation and conflicts on changed payload',async()=>{
  const store=new IdempotencyGate();
  let called=0;
  const fn=async()=>{called++;await Promise.resolve();return 42;};
  const p1=store.run('k','same',fn),p2=store.run('k','same',fn);
  assert.deepEqual(await Promise.all([p1,p2]),[42,42]);
  assert.equal(called,1);
  await assert.rejects(store.run('k','different',fn),/different/);
});
test('version cache drops stale completion and shares one concurrent fill',async()=>{
  const c=new VersionedCache();
  let fetches=0;
  let release;
  const gate=new Promise(resolve=>{release=resolve;});
  const loader=async()=>{fetches++;await gate;return {version:1,value:'old'};};
  const a=c.load('key',loader),b=c.load('key',loader);
  await Promise.resolve();
  c.put('key',2,'new');
  release();
  const rows=await Promise.all([a,b]);
  assert.equal(fetches,1);
  assert.ok(rows.every(x=>x.version===2 && x.value==='new'));
});
test('sequenced inbox buffers gaps, ignores duplicates and emits in order',()=>{
  const i=new SequencedInbox(5);
  assert.deepEqual(i.receive('order',2,'paid').applied,[]);
  assert.deepEqual(i.receive('order',1,'created').applied,['created','paid']);
  assert.deepEqual(i.receive('order',2,'paid-again').applied,[]);
  assert.deepEqual(i.history('order'),['created','paid']);
  assert.throws(()=>i.receive('order',100,'far'),/Gap/);
});
test('rendezvous hashing preserves assignments to surviving nodes after removal',()=>{
  const nodes=['A','B','C'],remaining=['A','B'];
  for(let i=0;i<200;i++){
    const key='key'+i,old=rendezvous(key,nodes),next=rendezvous(key,remaining);
    if(old!=='C')assert.equal(next,old);
  }
});
test('hybrid feed pushes for normal accounts and pulls for high-fan-out accounts',()=>{
  const f=new HybridFeed(3);
  f.follow('alice','ordinary');
  f.publish({id:'p1',author:'ordinary',time:1});
  for(const name of ['alice','bob','carol'])f.follow(name,'celebrity');
  assert.equal(f.publish({id:'p2',author:'celebrity',time:2}),'pull');
  assert.deepEqual(f.read('alice').map(x=>x.id),['p2','p1']);
  assert.deepEqual(f.read('bob').map(x=>x.id),['p2']);
});
test('top-k merge respects global order and tie breaking',()=>{
  const result=mergeTopK([
    [{id:'a',score:9},{id:'d',score:2}],
    [{id:'b',score:10},{id:'c',score:8}]
  ],3);
  assert.deepEqual(result.map(x=>x.id),['b','a','c']);
});
test('seat hold rejects stale fencing tokens even after timeout and new ownership',()=>{
  const seats=new SeatHolds();
  const first=seats.hold('S1','A',0,100);
  assert.equal(first,1);
  assert.equal(seats.hold('S1','B',50,100),undefined);
  const second=seats.hold('S1','B',100,100);
  assert.equal(second,2);
  assert.equal(seats.confirm('S1','A',first,110),false);
  assert.equal(seats.confirm('S1','B',second,110),true);
  assert.equal(seats.hold('S1','C',300,100),undefined);
});
test('autocomplete stores prefix counts and returns deterministic top suggestions',()=>{
  const a=new Autocomplete();
  for(const word of ['cat','car','cat','cart'])a.record(word);
  assert.deepEqual(a.suggest('ca',2),['cat','car']);
  assert.deepEqual(a.suggest('z'),[]);
});
test('crawler respects per-host delay without blocking another host',()=>{
  const f=new PoliteFrontier();
  f.enqueue('https://one.example/a');
  f.enqueue('https://one.example/b');
  f.enqueue('https://two.example/c');
  assert.equal(f.claim(0,1000),'https://one.example/a');
  assert.equal(f.claim(0,1000),'https://two.example/c');
  assert.equal(f.claim(500,1000),undefined);
  assert.equal(f.claim(1000,1000),'https://one.example/b');
});
