/**
 * Original, deliberately small system-design primitives.
 * No Nest dependency: reasoning and correctness can be tested in isolation.
 * These in-memory implementations demonstrate algorithms, NOT distributed safety.
 */
export type Job = { id: string; tenant: string; cost: number; kind?: string };
type Lane = { weight: number; deficit: number; queue: Job[] };

export class DeficitScheduler {
  private lanes = new Map<string, Lane>();
  private active: string[] = [];
  private cursor = 0;
  private needsCredit = true;
  constructor(private readonly quantum = 2) {
    if (!(quantum > 0 && Number.isFinite(quantum))) throw Error('Invalid quantum');
  }
  enqueue(job: Job, weight = 1): void {
    if (!job.id || !job.tenant || !Number.isInteger(job.cost) || job.cost <= 0)
      throw Error('Job needs positive integer cost, ID and tenant');
    if (!Number.isInteger(weight) || weight < 1) throw Error('Invalid weight');
    let lane = this.lanes.get(job.tenant);
    if (!lane) {
      lane = { weight, deficit: 0, queue: [] };
      this.lanes.set(job.tenant, lane);
      this.active.push(job.tenant);
    } else if (lane.weight !== weight) {
      throw Error('Tenant weight is fixed until its queue drains');
    }
    lane.queue.push(job);
  }
  private advance(): void {
    this.cursor = (this.cursor + 1) % this.active.length;
    this.needsCredit = true;
  }
  next(): Job | undefined {
    while (this.active.length) {
      const tenant = this.active[this.cursor];
      const lane = this.lanes.get(tenant)!;
      if (this.needsCredit) {
        lane.deficit += this.quantum * lane.weight;
        this.needsCredit = false;
      }
      if (lane.queue[0].cost <= lane.deficit) {
        const job = lane.queue.shift()!;
        lane.deficit -= job.cost;
        if (lane.queue.length === 0) {
          // Do not allow idle flows to bank unlimited future credit.
          this.lanes.delete(tenant);
          this.active.splice(this.cursor, 1);
          if (this.active.length) this.cursor %= this.active.length;
          else this.cursor = 0;
          this.needsCredit = true;
        } else if (lane.queue[0].cost > lane.deficit) {
          this.advance();
        }
        return job;
      }
      this.advance(); // Carry unused credit into the tenant's next round.
    }
    return undefined;
  }
  depth(tenant?: string): number {
    if (tenant) return this.lanes.get(tenant)?.queue.length ?? 0;
    return [...this.lanes.values()].reduce((sum, lane) => sum + lane.queue.length, 0);
  }
}

type Bucket = { tokens: number; at: number };
export class TokenBucket {
  private buckets = new Map<string, Bucket>();
  constructor(readonly capacity: number, readonly refillPerSecond: number) {
    if (!(capacity > 0 && refillPerSecond > 0)) throw Error('Invalid bucket');
  }
  allow(key: string, nowMs: number, cost = 1): boolean {
    if (!key || !Number.isFinite(nowMs) || !(cost > 0) || cost > this.capacity)
      throw Error('Invalid request');
    const entry = this.buckets.get(key) ?? { tokens: this.capacity, at: nowMs };
    const elapsed = Math.max(0, nowMs - entry.at) / 1000;
    entry.tokens = Math.min(this.capacity, entry.tokens + elapsed * this.refillPerSecond);
    entry.at = Math.max(entry.at, nowMs);
    const allowed = entry.tokens >= cost;
    if (allowed) entry.tokens -= cost;
    this.buckets.set(key, entry);
    return allowed;
  }
  available(key: string): number {
    return this.buckets.get(key)?.tokens ?? this.capacity;
  }
}

/** Single-process request coalescing; a DB unique constraint is needed across replicas. */
export class IdempotencyGate<T> {
  private records = new Map<string, { fingerprint: string; result: Promise<T> }>();
  run(key: string, fingerprint: string, action: () => Promise<T>): Promise<T> {
    if (!key || !fingerprint) return Promise.reject(Error('Missing idempotency identity'));
    const old = this.records.get(key);
    if (old) {
      if (old.fingerprint !== fingerprint)
        return Promise.reject(Error('Idempotency key reused with different request'));
      return old.result;
    }
    const result = Promise.resolve().then(action);
    this.records.set(key, { fingerprint, result });
    void result.catch(() => {
      // Failed attempt may be retried; compare identity before removal.
      if (this.records.get(key)?.result === result) this.records.delete(key);
    });
    return result;
  }
  count(): number { return this.records.size; }
}

/** Drop stale fills by version and coalesce simultaneous cache misses. */
export class VersionedCache<T> {
  private data = new Map<string, { version: number; value: T }>();
  private flights = new Map<string, Promise<void>>();
  put(key: string, version: number, value: T): boolean {
    if (!Number.isSafeInteger(version) || version < 0) throw Error('Invalid version');
    const old = this.data.get(key);
    if (old && version < old.version) return false;
    this.data.set(key, { version, value });
    return true;
  }
  get(key: string): { version: number; value: T } | undefined {
    return this.data.get(key);
  }
  async load(key: string, fetcher: () => Promise<{version: number; value: T}>):
      Promise<{version: number; value: T} | undefined> {
    let pending = this.flights.get(key);
    if (!pending) {
      pending = Promise.resolve().then(fetcher).then(row => {
        this.put(key, row.version, row.value);
      });
      this.flights.set(key, pending);
      void pending.finally(() => {
        if (this.flights.get(key) === pending) this.flights.delete(key);
      }).catch(() => undefined);
    }
    await pending;
    return this.get(key);
  }
}

/** Per-key ordered state machine: sequences start at 1 and have no gaps after delivery. */
export class SequencedInbox<T> {
  private state = new Map<string, { next: number; waiting: Map<number,T>; applied: T[] }>();
  constructor(private readonly maxBuffered = 100) {}
  receive(key: string, seq: number, value: T): { applied: T[]; next: number } {
    if (!key || !Number.isSafeInteger(seq) || seq < 1) throw Error('Invalid sequence');
    const s = this.state.get(key) ?? { next: 1, waiting: new Map<number,T>(), applied: [] };
    this.state.set(key, s);
    if (seq < s.next || s.waiting.has(seq)) return { applied: [], next: s.next };
    if (seq - s.next > this.maxBuffered || s.waiting.size >= this.maxBuffered)
      throw Error('Gap too large; request replay or snapshot');
    s.waiting.set(seq, value);
    const applied: T[] = [];
    while (s.waiting.has(s.next)) {
      const item = s.waiting.get(s.next)!;
      s.waiting.delete(s.next++);
      s.applied.push(item);
      applied.push(item);
    }
    return { applied, next: s.next };
  }
  history(key: string): T[] { return [...(this.state.get(key)?.applied ?? [])]; }
}

/** FNV-1a is chosen for reproducibility, not cryptographic security. */
export function hash32(s: string): number {
  let h = 2166136261;
  for (const char of s) {
    h ^= char.codePointAt(0)!;
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}
/** Highest-random-weight (rendezvous) hashing, with no load caps. */
export function rendezvous(key: string, nodes: readonly string[]): string {
  if (nodes.length === 0) throw Error('No nodes');
  let best = nodes[0], score = -1;
  for (const node of nodes) {
    const s = hash32(key + '::' + node);
    if (s > score || (s === score && node < best)) { best = node; score = s; }
  }
  return best;
}

export type FeedPost = { id: string; author: string; time: number };
export class HybridFeed {
  private followers = new Map<string, Set<string>>();
  private posts = new Map<string, FeedPost[]>();
  private inbox = new Map<string, FeedPost[]>();
  constructor(private readonly celebrityThreshold = 3) {}
  follow(reader: string, author: string): void {
    const set = this.followers.get(author) ?? new Set<string>();
    set.add(reader);
    this.followers.set(author, set);
  }
  publish(post: FeedPost): 'push' | 'pull' {
    const own = this.posts.get(post.author) ?? [];
    own.push(post);
    this.posts.set(post.author, own);
    const readers = this.followers.get(post.author) ?? new Set();
    if (readers.size >= this.celebrityThreshold) return 'pull';
    for (const reader of readers) {
      const entries = this.inbox.get(reader) ?? [];
      entries.push(post);
      this.inbox.set(reader, entries);
    }
    return 'push';
  }
  read(reader: string, k = 20): FeedPost[] {
    const candidates = [...(this.inbox.get(reader) ?? [])];
    // Pull for high-fan-out authors; relationship changes need backfill in production.
    for (const [author, followers] of this.followers) {
      if (followers.size >= this.celebrityThreshold && followers.has(reader))
        candidates.push(...(this.posts.get(author) ?? []));
    }
    const seen = new Map<string, FeedPost>();
    for (const p of candidates) seen.set(p.id, p);
    return [...seen.values()].sort((a,b) => b.time - a.time || a.id.localeCompare(b.id)).slice(0,k);
  }
}

/** Exact global top-k if each shard emits its local top-k under the SAME total order. */
export function mergeTopK<T extends { id: string; score: number }>(
  shardResults: readonly (readonly T[])[], k: number): T[] {
  if (!Number.isInteger(k) || k < 0) throw Error('Invalid k');
  const seen = new Map<string,T>();
  for (const shard of shardResults) for (const item of shard) {
    const old = seen.get(item.id);
    if (!old || item.score > old.score) seen.set(item.id, item);
  }
  return [...seen.values()]
    .sort((a,b) => b.score - a.score || a.id.localeCompare(b.id)).slice(0,k);
}

/** Single-process seat holds and monotonic fencing tokens. */
export class SeatHolds {
  private state = new Map<string, { token: number; owner: string; expires: number; confirmed: boolean }>();
  private sequence = new Map<string, number>();
  hold(seat: string, owner: string, now: number, ttlMs: number): number | undefined {
    if (!seat || !owner || !(ttlMs > 0)) throw Error('Invalid hold');
    const old = this.state.get(seat);
    if (old?.confirmed || (old && old.expires > now)) return undefined;
    const token = (this.sequence.get(seat) ?? 0) + 1;
    this.sequence.set(seat, token);
    this.state.set(seat, { token, owner, expires: now + ttlMs, confirmed: false });
    return token;
  }
  confirm(seat: string, owner: string, token: number, now: number): boolean {
    const hold = this.state.get(seat);
    if (!hold || hold.owner !== owner || hold.token !== token || hold.expires <= now) return false;
    hold.confirmed = true;
    return true;
  }
  status(seat: string): string { return this.state.get(seat)?.confirmed ? 'confirmed' : 'not-confirmed'; }
}

/** Prefix counts; ranking combines popularity and stable alphabetic ties. */
export class Autocomplete {
  private prefixes = new Map<string, Map<string,number>>();
  record(term: string, delta = 1): void {
    const word = term.trim().toLowerCase();
    if (!word || !(delta > 0)) throw Error('Invalid term');
    for (let i = 1; i <= word.length; i++) {
      const prefix = word.slice(0,i);
      const row = this.prefixes.get(prefix) ?? new Map<string,number>();
      row.set(word,(row.get(word) ?? 0) + delta);
      this.prefixes.set(prefix,row);
    }
  }
  suggest(prefix: string,k = 5): string[] {
    return [...(this.prefixes.get(prefix.toLowerCase()) ?? new Map()).entries()]
      .sort((a,b) => b[1]-a[1] || a[0].localeCompare(b[0])).slice(0,k).map(([word])=>word);
  }
}

/** A deterministic per-host politeness gate; no HTTP fetching or robots parser. */
export class PoliteFrontier {
  private hosts = new Map<string,number>();
  private pending: string[] = [];
  private seen = new Set<string>();
  enqueue(url: string): boolean {
    const parsed = new URL(url);
    if (!['https:','http:'].includes(parsed.protocol)) throw Error('HTTP(S) only');
    if (this.seen.has(parsed.href)) return false;
    this.seen.add(parsed.href);
    this.pending.push(parsed.href);
    return true;
  }
  claim(now: number, minIntervalMs: number): string | undefined {
    if (!(minIntervalMs >= 0)) throw Error('Bad interval');
    for (let i=0;i<this.pending.length;i++) {
      const url = this.pending[i], host = new URL(url).host;
      if ((this.hosts.get(host) ?? -Infinity) > now) continue;
      this.pending.splice(i,1);
      this.hosts.set(host, now+minIntervalMs);
      return url;
    }
    return undefined;
  }
  size(): number { return this.pending.length; }
}

/** Split a large file into finite-cost tasks before it enters the fair scheduler. */
export function chunkFile(tenant: string, fileId: string, units: number, maxChunk: number): Job[] {
  if (!tenant || !fileId || !Number.isInteger(units) || units < 1 ||
      !Number.isInteger(maxChunk) || maxChunk < 1) throw Error('Bad chunk parameters');
  const jobs: Job[] = [];
  for (let i = 0, n = 0; i < units; i += maxChunk, n++)
    jobs.push({ id: fileId + '#' + n, tenant, cost: Math.min(maxChunk, units-i), kind:'chunk' });
  return jobs;
}
