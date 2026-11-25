// Original, dependency-free examples: node algorithms/examples/patterns.mjs
// Inputs use small, familiar arrays and strings; all exported functions are testable.

// LC 3 — maintain a half-open window [left, right + 1) without duplicates.
export function longestUniqueSubstring(s) {
  let left = 0, best = 0;
  const last = new Map();
  for (let right = 0; right < s.length; right++) {
    const old = last.get(s[right]);
    if (old !== undefined && old >= left) left = old + 1;
    last.set(s[right], right);
    best = Math.max(best, right - left + 1);
  }
  return best;
}

// LC 35 — first index with value >= target; answer may be a.length.
export function lowerBound(a, target) {
  let l = 0, r = a.length;
  while (l < r) {
    const mid = l + Math.floor((r - l) / 2);
    if (a[mid] < target) l = mid + 1;
    else r = mid;
  }
  return l;
}

// LC 167 — sorted input; moving one pointer discards impossible pairs.
export function twoSumSorted(a, target) {
  let l = 0, r = a.length - 1;
  while (l < r) {
    const sum = a[l] + a[r];
    if (sum === target) return [l, r];
    if (sum < target) l++;
    else r--;
  }
  return null;
}

// LC 15 — sort + fixed first element + inward search; omit duplicate triples.
export function threeSum(nums) {
  const a = [...nums].sort((x, y) => x - y), out = [];
  for (let i = 0; i < a.length - 2; i++) {
    if (i > 0 && a[i] === a[i - 1]) continue;
    let l = i + 1, r = a.length - 1;
    while (l < r) {
      const sum = a[i] + a[l] + a[r];
      if (sum < 0) l++;
      else if (sum > 0) r--;
      else {
        out.push([a[i], a[l], a[r]]);
        const oldL = a[l], oldR = a[r];
        while (l < r && a[l] === oldL) l++;
        while (l < r && a[r] === oldR) r--;
      }
    }
  }
  return out;
}

// LC 78 — choose / skip each element.
export function subsets(a) {
  const result = [], path = [];
  function visit(i) {
    if (i === a.length) { result.push([...path]); return; }
    visit(i + 1);
    path.push(a[i]);
    visit(i + 1);
    path.pop();
  }
  visit(0);
  return result;
}

// LC 46 — permutations; position is fixed one choice at a time.
export function permutations(a) {
  const out = [], path = [], used = Array(a.length).fill(false);
  function visit() {
    if (path.length === a.length) { out.push([...path]); return; }
    for (let i = 0; i < a.length; i++) if (!used[i]) {
      used[i] = true; path.push(a[i]);
      visit();
      path.pop(); used[i] = false;
    }
  }
  visit();
  return out;
}

// LC 198 — maximum independent set of a path, with two rolling states.
export function houseRobber(a) {
  let beforePrevious = 0, previous = 0;
  for (const money of a) {
    const current = Math.max(previous, beforePrevious + money);
    beforePrevious = previous; previous = current;
  }
  return previous;
}

// LC 322 — optimal coin count; not generally solved by largest-coin greedy.
export function coinChange(coins, amount) {
  const dp = Array(amount + 1).fill(Infinity);
  dp[0] = 0;
  for (let x = 1; x <= amount; x++) for (const coin of coins)
    if (coin > 0 && coin <= x) dp[x] = Math.min(dp[x], dp[x - coin] + 1);
  return Number.isFinite(dp[amount]) ? dp[amount] : -1;
}

// Greedy activity selection — earliest compatible finishing activity.
export function maxNonOverlapping(intervals) {
  const a = [...intervals].sort((x, y) => x[1] - y[1]);
  let end = -Infinity, count = 0;
  for (const [start, stop] of a)
    if (start >= end) { count++; end = stop; }
  return count;
}

// LC 560 — prefix sum counting; negative integers are supported.
export function subarraySumEqualsK(a, k) {
  let prefix = 0, answer = 0;
  const freq = new Map([[0, 1]]);
  for (const x of a) {
    prefix += x;
    answer += freq.get(prefix - k) ?? 0;
    freq.set(prefix, (freq.get(prefix) ?? 0) + 1);
  }
  return answer;
}

// Count connected components in an undirected adjacency list.
export function componentsDFS(graph) {
  const seen = new Set();
  let components = 0;
  function visit(v) {
    seen.add(v);
    for (const u of graph[v]) if (!seen.has(u)) visit(u);
  }
  for (let v = 0; v < graph.length; v++)
    if (!seen.has(v)) { components++; visit(v); }
  return components;
}

// Unweighted shortest number of edges from source; queue head avoids shift().
export function bfsDistances(graph, source) {
  const d = Array(graph.length).fill(Infinity);
  d[source] = 0;
  const queue = [source];
  for (let head = 0; head < queue.length; head++) {
    const v = queue[head];
    for (const u of graph[v]) if (d[u] === Infinity) {
      d[u] = d[v] + 1;
      queue.push(u);
    }
  }
  return d;
}

// Dijkstra demonstration with a linear minimum search, O(V²+E).
// Nonnegative edge weights are REQUIRED; the simplicity aids explanation.
export function dijkstraSimple(graph, source) {
  const d = Array(graph.length).fill(Infinity);
  const settled = Array(graph.length).fill(false);
  d[source] = 0;
  for (let step = 0; step < graph.length; step++) {
    let u = -1;
    for (let i = 0; i < graph.length; i++)
      if (!settled[i] && (u < 0 || d[i] < d[u])) u = i;
    if (u < 0 || d[u] === Infinity) break;
    settled[u] = true;
    for (const [v, w] of graph[u]) {
      if (w < 0) throw Error('Dijkstra needs nonnegative weights');
      d[v] = Math.min(d[v], d[u] + w);
    }
  }
  return d;
}

// Merge sort: divide, recursively sort, and merge in stable order.
export function mergeSort(a) {
  if (a.length <= 1) return [...a];
  const m = Math.floor(a.length / 2);
  const l = mergeSort(a.slice(0, m)), r = mergeSort(a.slice(m));
  const result = [];
  let i = 0, j = 0;
  while (i < l.length || j < r.length) {
    if (j === r.length || (i < l.length && l[i] <= r[j]))
      result.push(l[i++]);
    else result.push(r[j++]);
  }
  return result;
}

// LC 496 pattern — indices on a decreasing stack wait for their next greater.
export function nextGreater(a) {
  const out = Array(a.length).fill(-1), stack = [];
  for (let i = 0; i < a.length; i++) {
    while (stack.length && a[stack[stack.length - 1]] < a[i])
      out[stack.pop()] = a[i];
    stack.push(i);
  }
  return out;
}

// LC 239 — decreasing deque of indices; each index is inserted/removed once.
export function slidingWindowMax(a, k) {
  if (k <= 0 || k > a.length) return [];
  const q = [], out = [];
  let head = 0;
  for (let r = 0; r < a.length; r++) {
    while (head < q.length && q[head] <= r - k) head++;
    while (q.length > head && a[q[q.length - 1]] <= a[r]) q.pop();
    q.push(r);
    if (r + 1 >= k) out.push(a[q[head]]);
  }
  return out;
}

export function prefixSums(a) {
  const p = [0];
  for (const x of a) p.push(p[p.length - 1] + x);
  return p;
}

// Disjoint set union — union by size + path compression.
export class DSU {
  constructor(n) {
    this.parent = Array.from({length:n},(_,i)=>i);
    this.size = Array(n).fill(1);
  }
  find(x) {
    if (this.parent[x] !== x) this.parent[x] = this.find(this.parent[x]);
    return this.parent[x];
  }
  union(a,b) {
    a = this.find(a); b = this.find(b);
    if (a === b) return false;
    if (this.size[a] < this.size[b]) [a,b] = [b,a];
    this.parent[b] = a;
    this.size[a] += this.size[b];
    return true;
  }
}

// KMP pattern search — prefix function avoids re-reading earlier text.
export function kmpFind(text, pattern) {
  if (pattern.length === 0) return 0;
  const pi = Array(pattern.length).fill(0);
  for (let i = 1, j = 0; i < pattern.length; i++) {
    while (j > 0 && pattern[i] !== pattern[j]) j = pi[j - 1];
    if (pattern[i] === pattern[j]) j++;
    pi[i] = j;
  }
  for (let i = 0, j = 0; i < text.length; i++) {
    while (j > 0 && text[i] !== pattern[j]) j = pi[j - 1];
    if (text[i] === pattern[j]) j++;
    if (j === pattern.length) return i - j + 1;
  }
  return -1;
}

// An in-place quickselect on a COPY; middle pivot avoids trivial sorted worst cases.
export function quickselect(a, k) {
  if (!Number.isInteger(k) || k < 0 || k >= a.length) throw Error('invalid k');
  const b = [...a];
  let left = 0, right = b.length - 1;
  while (left <= right) {
    const pivot = b[Math.floor((left + right)/2)];
    let i=left,j=right;
    while (i <= j) {
      while (b[i] < pivot) i++;
      while (b[j] > pivot) j--;
      if (i <= j) { [b[i],b[j]]=[b[j],b[i]]; i++; j--; }
    }
    if (k <= j) right = j;
    else if (k >= i) left = i;
    else return b[k];
  }
  return b[k];
}

// Kadane — the best nonempty subarray ending at each position.
export function maxSubarray(a) {
  if (!a.length) throw Error('nonempty array required');
  let current=a[0], best=a[0];
  for(let i=1;i<a.length;i++){
    current=Math.max(a[i],current+a[i]);
    best=Math.max(best,current);
  }
  return best;
}

// Kahn topological order; cycle => no full order.
export function topologicalOrder(n, edges) {
  const graph=Array.from({length:n},()=>[]);
  const indegree=Array(n).fill(0);
  for(const [u,v] of edges){graph[u].push(v);indegree[v]++;}
  const q=[];
  for(let i=0;i<n;i++)if(indegree[i]===0)q.push(i);
  const out=[];
  for(let head=0;head<q.length;head++){
    const u=q[head]; out.push(u);
    for(const v of graph[u])if(--indegree[v]===0)q.push(v);
  }
  return out.length===n?out:null;
}

// LC 11 — area is width times the shorter wall; discard the shorter wall.
export function maxContainerArea(a) {
  let l=0,r=a.length-1,best=0;
  while(l<r){
    best=Math.max(best,(r-l)*Math.min(a[l],a[r]));
    if(a[l]<=a[r])l++;else r--;
  }
  return best;
}

// LC 42 — bounded by the shorter maximum wall.
export function trappedRainwater(a) {
  let l=0,r=a.length-1,lMax=0,rMax=0,water=0;
  while(l<r){
    if(a[l]<=a[r]){
      lMax=Math.max(lMax,a[l]);
      water+=lMax-a[l++];
    } else {
      rMax=Math.max(rMax,a[r]);
      water+=rMax-a[r--];
    }
  }
  return water;
}

// LC 300 — tails[k] is the smallest ending value of a length-(k+1) subsequence.
export function lisLength(a) {
  const tails=[];
  for(const x of a){
    const i=lowerBound(tails,x);
    tails[i]=x;
  }
  return tails.length;
}

// LC 74-ish matrix scanning: sum entries with a straightforward nested traversal.
export function matrixSum(a) {
  return a.reduce((sum,row)=>sum+row.reduce((s,x)=>s+x,0),0);
}

// LC 217 — duplicate detection with a set.
export function containsDuplicate(a) {
  return new Set(a).size !== a.length;
}

if (process.argv[1] && import.meta.url === new URL('file://' + process.argv[1]).href) {
  console.log({
    slidingWindow: longestUniqueSubstring('abcabcbb'),
    binarySearch: lowerBound([1,3,3,6],3),
    dynamicProgramming: houseRobber([2,7,9,3,1]),
    prefixSum: subarraySumEqualsK([1,2,1,2],3),
    shortestPath: dijkstraSimple([[[1,2],[2,5]],[[2,1]],[]],0)
  });
}
