import test from 'node:test';
import assert from 'node:assert/strict';
import {
  longestUniqueSubstring, lowerBound, twoSumSorted, threeSum, subsets,
  permutations, houseRobber, coinChange, maxNonOverlapping,
  subarraySumEqualsK, componentsDFS, bfsDistances, dijkstraSimple,
  mergeSort, nextGreater, slidingWindowMax, prefixSums, DSU, kmpFind,
  quickselect, maxSubarray, topologicalOrder, maxContainerArea,
  trappedRainwater, lisLength, matrixSum, containsDuplicate
} from './patterns.mjs';

test('sliding window does not move left backwards after a duplicate',()=>{
  assert.equal(longestUniqueSubstring('abba'),2);
  assert.equal(longestUniqueSubstring('abcabcbb'),3);
  assert.equal(longestUniqueSubstring(''),0);
});
test('binary search half-open interval returns first matching boundary',()=>{
  assert.equal(lowerBound([1,3,3,6],3),1);
  assert.equal(lowerBound([1,3,3,6],4),3);
  assert.equal(lowerBound([],0),0);
});
test('two pointers and sorted 3-sum support duplicates',()=>{
  assert.deepEqual(twoSumSorted([2,7,11,15],9),[0,1]);
  assert.deepEqual(threeSum([-1,0,1,2,-1,-4]),[[-1,-1,2],[-1,0,1]]);
});
test('backtracking creates all subsets and permutations',()=>{
  assert.equal(subsets([1,2,3]).length,8);
  assert.equal(permutations([1,2,3]).length,6);
  assert.deepEqual(subsets([]),[[]]);
});
test('dynamic programming correctly considers take or skip and coin choices',()=>{
  assert.equal(houseRobber([2,7,9,3,1]),12);
  assert.equal(houseRobber([2,1,1,2]),4);
  assert.equal(coinChange([1,3,4],6),2);
  assert.equal(coinChange([2],3),-1);
});
test('greedy interval selection chooses earliest finishing end',()=>{
  assert.equal(maxNonOverlapping([[1,4],[2,3],[3,5]]),2);
});
test('prefix hash works with negative values and duplicate prefixes',()=>{
  assert.equal(subarraySumEqualsK([1,1,1],2),2);
  assert.equal(subarraySumEqualsK([1,-1,0],0),3);
  assert.deepEqual(prefixSums([2,-1,4]),[0,2,1,5]);
});
test('DFS components and BFS unweighted shortest paths',()=>{
  const g=[[1],[0],[3],[2],[]];
  assert.equal(componentsDFS(g),3);
  assert.deepEqual(bfsDistances([[1,2],[3],[3],[]],0),[0,1,1,2]);
});
test('Dijkstra nonnegative shortest path and negative weight rejection',()=>{
  assert.deepEqual(dijkstraSimple([[[1,2],[2,5]],[[2,1]],[]],0),[0,2,3]);
  assert.throws(()=>dijkstraSimple([[[1,-1]],[]],0),/nonnegative/);
});
test('merge sort stable numerical order, quickselect median',()=>{
  assert.deepEqual(mergeSort([5,1,4,1,3]),[1,1,3,4,5]);
  assert.equal(quickselect([8,2,7,1,9],2),7);
});
test('monotonic structures retain only useful candidates',()=>{
  assert.deepEqual(nextGreater([2,1,3]),[3,3,-1]);
  assert.deepEqual(slidingWindowMax([1,3,-1,-3,5,3,6,7],3),[3,3,5,5,6,7]);
});
test('union find merges equivalence classes',()=>{
  const d=new DSU(4);
  assert.equal(d.union(0,1),true);
  assert.equal(d.union(1,2),true);
  assert.equal(d.union(0,2),false);
  assert.equal(d.find(0),d.find(2));
  assert.notEqual(d.find(0),d.find(3));
});
test('KMP finds overlapping pattern candidate',()=>{
  assert.equal(kmpFind('ababcabc','abc'),2);
  assert.equal(kmpFind('aaaaa','b'),-1);
  assert.equal(kmpFind('abcd',''),0);
});
test('Kadane maximum subarray works for negative-only data',()=>{
  assert.equal(maxSubarray([-2,1,-3,4,-1,2,1,-5,4]),6);
  assert.equal(maxSubarray([-3,-2,-5]),-2);
});
test('topological order obeys dependency constraints and detects cycles',()=>{
  const order=topologicalOrder(4,[[0,1],[0,2],[2,3]]);
  const i=x=>order.indexOf(x);
  assert.ok(i(0)<i(1)&&i(0)<i(2)&&i(2)<i(3));
  assert.equal(topologicalOrder(2,[[0,1],[1,0]]),null);
});
test('two-pointer area and trapped rainwater',()=>{
  assert.equal(maxContainerArea([1,8,6,2,5,4,8,3,7]),49);
  assert.equal(trappedRainwater([0,1,0,2,1,0,1,3,2,1,2,1]),6);
});
test('LIS only stores minimal possible ending value',()=>{
  assert.equal(lisLength([10,9,2,5,3,7,101,18]),4);
  assert.equal(lisLength([]),0);
});
test('data structure basics',()=>{
  assert.equal(matrixSum([[1,2],[3,4]]),10);
  assert.equal(containsDuplicate([1,2,3,1]),true);
  assert.equal(containsDuplicate([1,2,3]),false);
});
