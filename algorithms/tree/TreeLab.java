import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

// Run from this folder with: java TreeLab.java
public class TreeLab {

    // The lesson's example tree, a company chart rooted at person 0:
    //
    //            0
    //          / | \
    //         1  2  3
    //        / \     \
    //       4   5     6
    static final int[] PARENT = {-1, 0, 0, 0, 1, 1, 3};
    static final int[][] EDGES = {{0, 1}, {0, 2}, {0, 3}, {1, 4}, {1, 5}, {3, 6}};

    // Level 1: the depth of v is the number of steps up to the root.
    static int depth(int[] parent, int v) {
        int steps = 0;
        while (parent[v] != -1) {   // only the root has no parent
            v = parent[v];
            steps++;
        }
        return steps;
    }

    // Level 2: turn "who is my parent?" into "who are my children?".
    static List<List<Integer>> childLists(int[] parent) {
        List<List<Integer>> children = new ArrayList<>();
        for (int v = 0; v < parent.length; v++) children.add(new ArrayList<>());
        for (int v = 0; v < parent.length; v++) {
            if (parent[v] != -1) children.get(parent[v]).add(v);
        }
        return children;
    }

    // Level 3: an undirected edge list, stored both ways round.
    static List<List<Integer>> adjacency(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int v = 0; v < n; v++) adj.add(new ArrayList<>());
        for (int[] edge : edges) {
            adj.get(edge[0]).add(edge[1]);
            adj.get(edge[1]).add(edge[0]);
        }
        return adj;
    }

    static boolean isTree(int n, int[][] edges) {
        if (edges.length != n - 1) return false;      // a tree on n nodes has exactly n - 1 edges
        List<List<Integer>> adj = adjacency(n, edges);
        boolean[] seen = new boolean[n];
        Deque<Integer> stack = new ArrayDeque<>(List.of(0));
        seen[0] = true;
        int reached = 1;
        while (!stack.isEmpty()) {
            for (int next : adj.get(stack.pop())) {
                if (!seen[next]) { seen[next] = true; reached++; stack.push(next); }
            }
        }
        return reached == n;                          // connected with n - 1 edges, so no cycle
    }

    // Level 4: answers that flow up from the leaves.
    static int height(List<List<Integer>> children, int v) {
        int tallestChild = -1;                        // a leaf has no child, so it gets height 0
        for (int child : children.get(v)) tallestChild = Math.max(tallestChild, height(children, child));
        return tallestChild + 1;
    }

    static int subtreeSizes(List<List<Integer>> adj, int v, int parent, int[] size) {
        size[v] = 1;
        for (int next : adj.get(v)) {
            if (next != parent) size[v] += subtreeSizes(adj, next, v, size);   // never walk back up
        }
        return size[v];
    }

    // Level 5: move the root from v to each child, updating the answer in O(1).
    static void reroot(List<List<Integer>> adj, int v, int parent, int[] size, int[] answer) {
        int n = adj.size();
        for (int child : adj.get(v)) {
            if (child == parent) continue;
            answer[child] = answer[v] - size[child] + (n - size[child]);
            reroot(adj, child, v, size, answer);
        }
    }

    // Interview 1: LeetCode 559, Maximum Depth of N-ary Tree.
    static final class Node {
        final int val;
        final List<Node> children;

        Node(int val, Node... children) {
            this.val = val;
            this.children = List.of(children);
        }
    }

    static int maxDepth(Node root) {
        if (root == null) return 0;
        int deepestChild = 0;
        for (Node child : root.children) deepestChild = Math.max(deepestChild, maxDepth(child));
        return deepestChild + 1;                      // LeetCode counts nodes, not edges
    }

    // Interview 2: LeetCode 310, Minimum Height Trees. Peel off the leaves, layer by layer.
    static List<Integer> findMinHeightTrees(int n, int[][] edges) {
        if (n == 1) return List.of(0);
        List<List<Integer>> adj = adjacency(n, edges);
        int[] degree = new int[n];
        List<Integer> leaves = new ArrayList<>();
        for (int v = 0; v < n; v++) {
            degree[v] = adj.get(v).size();
            if (degree[v] == 1) leaves.add(v);
        }
        int remaining = n;
        while (remaining > 2) {
            remaining -= leaves.size();
            List<Integer> nextLeaves = new ArrayList<>();
            for (int leaf : leaves) {
                for (int next : adj.get(leaf)) {
                    if (--degree[next] == 1) nextLeaves.add(next);
                }
            }
            leaves = nextLeaves;
        }
        return leaves;
    }

    // Interview 3: LeetCode 834, Sum of Distances in Tree.
    static int[] sumOfDistancesInTree(int n, int[][] edges) {
        List<List<Integer>> adj = adjacency(n, edges);
        int[] size = new int[n], answer = new int[n];
        subtreeSizes(adj, 0, -1, size);
        for (int v = 1; v < n; v++) answer[0] += size[v];   // the edge above v lies on size[v] paths from 0
        reroot(adj, 0, -1, size, answer);
        return answer;
    }

    // Brute force, used only to check the fast answers: breadth-first distances from one node.
    static int[] distancesFrom(List<List<Integer>> adj, int source) {
        int[] dist = new int[adj.size()];
        Arrays.fill(dist, -1);
        dist[source] = 0;
        Deque<Integer> queue = new ArrayDeque<>(List.of(source));
        while (!queue.isEmpty()) {
            int v = queue.poll();
            for (int next : adj.get(v)) {
                if (dist[next] == -1) { dist[next] = dist[v] + 1; queue.add(next); }
            }
        }
        return dist;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int n = PARENT.length;

        // Level 1
        check(depth(PARENT, 0) == 0 && depth(PARENT, 1) == 1 && depth(PARENT, 6) == 2, "depths 0, 1, 2");

        // Level 2
        List<List<Integer>> children = childLists(PARENT);
        check(children.equals(List.of(List.of(1, 2, 3), List.of(4, 5), List.of(), List.of(6), List.of(), List.of(), List.of())),
                "child lists");
        int childEntries = children.stream().mapToInt(List::size).sum();
        check(childEntries == n - 1 && n + childEntries == 13, "6 child entries, 13 cells with the 7 list heads");

        // Level 3
        check(EDGES.length == n - 1 && isTree(n, EDGES), "7 nodes, 6 edges, connected");
        int[][] withCycle = {{0, 1}, {0, 2}, {0, 3}, {1, 4}, {1, 5}, {3, 6}, {2, 3}};
        check(!isTree(n, withCycle), "a 7th edge makes the cycle 0-2-3-0");
        int[][] cutOff = {{0, 1}, {0, 2}, {1, 4}, {1, 5}, {3, 6}};
        check(!isTree(n, cutOff), "removing 0-3 cuts off 3 and 6");
        int[][] cycleAndPath = {{0, 1}, {1, 2}, {2, 0}, {3, 4}, {4, 5}, {5, 6}};
        check(!isTree(n, cycleAndPath), "n - 1 edges alone are not enough");

        // Level 4
        check(height(children, 0) == 2 && height(children, 1) == 1 && height(children, 3) == 1
                && height(children, 2) == 0, "heights");
        List<List<Integer>> adj = adjacency(n, EDGES);
        check(adj.stream().mapToInt(List::size).sum() == 12, "each of the 6 edges is stored twice");
        int[] size = new int[n];
        subtreeSizes(adj, 0, -1, size);
        check(Arrays.equals(size, new int[] {7, 3, 1, 2, 1, 1, 1}), "subtree sizes");

        // Level 5: every number in the rerooting table, checked against brute force
        int[] sums = sumOfDistancesInTree(n, EDGES);
        check(Arrays.equals(sums, new int[] {9, 10, 14, 12, 15, 15, 17}), "sum of distances for every root");
        for (int v = 0; v < n; v++) {
            check(Arrays.stream(distancesFrom(adj, v)).sum() == sums[v], "rerooting agrees with BFS at " + v);
        }
        check(n * n == 49 && 30_000L * 30_000L == 900_000_000L, "brute-force step counts");

        // Interview 1: the company chart as N-ary nodes, and LeetCode's example [1,null,3,2,4,null,5,6]
        Node chart = new Node(0, new Node(1, new Node(4), new Node(5)), new Node(2), new Node(3, new Node(6)));
        check(maxDepth(chart) == 3 && maxDepth(null) == 0, "LC 559");
        check(maxDepth(new Node(1, new Node(3, new Node(5), new Node(6)), new Node(2), new Node(4))) == 3, "LC 559 example");

        // Interview 2: heights for every root, the diameter, and the centres
        int[] eccentricity = new int[n];
        for (int v = 0; v < n; v++) eccentricity[v] = Arrays.stream(distancesFrom(adj, v)).max().getAsInt();
        check(Arrays.equals(eccentricity, new int[] {2, 3, 3, 3, 4, 4, 4}), "height for each root");
        check(Arrays.stream(eccentricity).max().getAsInt() == 4, "diameter 4: 4-1-0-3-6");
        check(findMinHeightTrees(n, EDGES).equals(List.of(0)), "LC 310 on the chart");
        check(findMinHeightTrees(6, new int[][] {{3, 0}, {3, 1}, {3, 2}, {3, 4}, {5, 4}}).equals(List.of(3, 4)), "LC 310 two centres");
        check(findMinHeightTrees(4, new int[][] {{1, 0}, {1, 2}, {1, 3}}).equals(List.of(1)), "LC 310 star");
        check(findMinHeightTrees(1, new int[][] {}).equals(List.of(0)), "LC 310 single node");
        check(findMinHeightTrees(2, new int[][] {{0, 1}}).equals(List.of(0, 1)), "LC 310 two nodes");

        // Interview 3: LeetCode's example
        check(Arrays.equals(sumOfDistancesInTree(6, new int[][] {{0, 1}, {0, 2}, {2, 3}, {2, 4}, {2, 5}}),
                new int[] {8, 12, 6, 10, 10, 10}), "LC 834 example");
        check(Arrays.equals(sumOfDistancesInTree(1, new int[][] {}), new int[] {0}), "LC 834 single node");

        System.out.println("OK");
    }
}
