# Algorithms — Original, Runnable Examples

The algorithms learning path uses one dependency-free JavaScript module and Node's built-in test runner.

## Run it

From the repository root:

~~~bash
node algorithms/examples/patterns.mjs
node --test algorithms/examples/patterns.test.mjs
~~~

No npm installation or framework setup is required. These examples are intentionally small enough to trace by hand.

## Core examples and classic problems

| Function | Core idea | Related LeetCode exercise |
|---|---|---|
| longestUniqueSubstring | Move a valid no-duplicates window | 3 |
| lowerBound | First true boundary in a sorted sequence | 35 |
| twoSumSorted | Discard impossible extreme pairs | 167 |
| threeSum | Sort, fix one item, use two pointers | 15 |
| subsets | Choose, recurse, undo | 78 |
| permutations | Select each unused item once per path | 46 |
| houseRobber | Optimal substructure and rolling state | 198 |
| coinChange | Minimum over previous optimal states | 322 |
| maxNonOverlapping | Greedy earliest finish | 435 |
| subarraySumEqualsK | Prefix sum and frequency map | 560 |
| componentsDFS | Mark every reachable vertex | 200 concept |
| bfsDistances | Unweighted shortest number of hops | 752 concept |
| dijkstraSimple | Minimum-weight relaxation | 743 concept |
| mergeSort | Divide, sort, merge | 912 |
| nextGreater | Monotonic stack | 496 |
| slidingWindowMax | Monotonic deque | 239 |
| prefixSums | Cumulative interval sums | 303 |
| DSU | Disjoint-set union with compression | 547 |
| kmpFind | Reuse prefix matches | 28 |
| quickselect | Selection by partitioning | 215 |
| maxSubarray | Best subarray ending at each point | 53 |
| topologicalOrder | Remove zero-indegree vertices | 207 |
| maxContainerArea | Two pointers with shorter wall | 11 |
| trappedRainwater | Bound water by left/right maxima | 42 |
| lisLength | Smallest possible tails of subsequences | 300 |
| matrixSum | Two-dimensional traversal | grid basics |
| containsDuplicate | Hash-set membership | 217 |

The Dijkstra implementation intentionally selects the smallest tentative distance by linear scan, making the proof visible without implementing a binary heap. Its complexity is therefore O(V²+E), not the faster bound of a heap-based implementation.

## How to read any function

1. Identify the state variables.
2. Substitute the example from the related topic chapter.
3. Before each loop, say what must already be true.
4. Execute one iteration by hand.
5. Explain why the algorithm will stop and what its result means.
6. Review the input values that would invalidate the method.

These functions are not copies of textbook pseudocode or LeetCode submissions. They are independent teaching examples and are covered by automated tests.

[Algorithms overview](../index.qmd) · [Read the maths](../guides/read-the-math.qmd) · [Proof and running time](../guides/prove-and-count.qmd)
