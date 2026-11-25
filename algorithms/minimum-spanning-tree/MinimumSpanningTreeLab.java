import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

// Run from this folder with: java MinimumSpanningTreeLab.java
public class MinimumSpanningTreeLab {

    // The running example: villages A=0, B=1, C=2, D=3, E=4; each edge is {u, v, cost}.
    static final int[][] CABLES = {
        {0, 1, 4}, {0, 2, 1}, {1, 2, 2}, {1, 3, 1}, {2, 3, 5}, {3, 4, 3}, {2, 4, 8},
    };

    // The smallest useful union-find: path compression only (see the Union-Find lesson for more).
    static final class UnionFind {
        final int[] parent;

        UnionFind(int n) {
            parent = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;
        }

        int find(int x) {
            if (parent[x] != x) parent[x] = find(parent[x]);
            return parent[x];
        }

        boolean union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return false;
            parent[ra] = rb;
            return true;
        }
    }

    // Level 1: is a set of n - 1 edges a spanning tree? Yes exactly when it never closes a cycle.
    static boolean isSpanningTree(int n, List<int[]> chosen) {
        UnionFind uf = new UnionFind(n);
        for (int[] e : chosen) if (!uf.union(e[0], e[1])) return false;
        return chosen.size() == n - 1;
    }

    // Level 3: Kruskal. Cheapest edges first; skip any edge whose ends are already connected.
    static List<int[]> kruskal(int n, int[][] edges) {
        int[][] sorted = edges.clone();
        Arrays.sort(sorted, Comparator.comparingInt(e -> e[2]));
        UnionFind uf = new UnionFind(n);
        List<int[]> tree = new ArrayList<>();
        for (int[] e : sorted) {
            if (uf.union(e[0], e[1])) tree.add(e);          // joins two pieces, so no cycle: keep it
            if (tree.size() == n - 1) break;
        }
        return tree;
    }

    // Level 4: Prim. Grow one tree; always add the cheapest edge leaving it.
    static int prim(List<List<int[]>> adj) {
        boolean[] inTree = new boolean[adj.size()];
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(entry -> entry[1]));
        heap.add(new int[] {0, 0});                         // {vertex, cost of the edge that reaches it}
        int total = 0;
        while (!heap.isEmpty()) {
            int[] top = heap.poll();
            int u = top[0];
            if (inTree[u]) continue;                        // a cheaper edge already brought u in
            inTree[u] = true;
            total += top[1];
            for (int[] edge : adj.get(u)) {
                if (!inTree[edge[0]]) heap.add(new int[] {edge[0], edge[1]});
            }
        }
        return total;
    }

    static List<List<int[]>> undirectedAdjacency(int n, int[][] edges) {
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) {
            adj.get(e[0]).add(new int[] {e[1], e[2]});
            adj.get(e[1]).add(new int[] {e[0], e[2]});
        }
        return adj;
    }

    static int weight(List<int[]> edges) {
        return edges.stream().mapToInt(e -> e[2]).sum();
    }

    // Interview 1: LeetCode 1584, Min Cost to Connect All Points. Prim on a dense graph, no heap.
    static int minCostConnectPoints(int[][] points) {
        int n = points.length, total = 0;
        int[] best = new int[n];                            // cheapest known edge from the tree to each point
        Arrays.fill(best, Integer.MAX_VALUE);
        best[0] = 0;
        boolean[] inTree = new boolean[n];
        for (int round = 0; round < n; round++) {
            int u = -1;
            for (int v = 0; v < n; v++) if (!inTree[v] && (u == -1 || best[v] < best[u])) u = v;
            inTree[u] = true;
            total += best[u];
            for (int v = 0; v < n; v++) {
                int d = Math.abs(points[u][0] - points[v][0]) + Math.abs(points[u][1] - points[v][1]);
                if (!inTree[v] && d < best[v]) best[v] = d;
            }
        }
        return total;
    }

    // Interview 2: LeetCode 1489, Find Critical and Pseudo-Critical Edges in Minimum Spanning Tree.
    static int mstWeight(int n, int[][] edges, int skip, int force) {
        Integer[] order = new Integer[edges.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingInt(i -> edges[i][2]));
        UnionFind uf = new UnionFind(n);
        int total = 0, used = 0;
        if (force >= 0) { uf.union(edges[force][0], edges[force][1]); total += edges[force][2]; used++; }
        for (int i : order) {
            if (i == skip) continue;
            if (uf.union(edges[i][0], edges[i][1])) { total += edges[i][2]; used++; }
        }
        return used == n - 1 ? total : Integer.MAX_VALUE;   // MAX_VALUE: the graph falls apart
    }

    static List<List<Integer>> findCriticalAndPseudoCriticalEdges(int n, int[][] edges) {
        int best = mstWeight(n, edges, -1, -1);
        List<Integer> critical = new ArrayList<>(), pseudoCritical = new ArrayList<>();
        for (int i = 0; i < edges.length; i++) {
            if (mstWeight(n, edges, i, -1) > best) critical.add(i);               // without it, every tree costs more
            else if (mstWeight(n, edges, -1, i) == best) pseudoCritical.add(i);   // some cheapest tree uses it
        }
        return List.of(critical, pseudoCritical);
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        // Level 1: count the spanning trees among all 4-edge subsets of the 7 cables.
        int subsets = 0, trees = 0, cheapest = Integer.MAX_VALUE, cheapestCount = 0;
        for (int mask = 0; mask < (1 << 7); mask++) {
            if (Integer.bitCount(mask) != 4) continue;
            subsets++;
            List<int[]> chosen = new ArrayList<>();
            for (int i = 0; i < 7; i++) if ((mask & (1 << i)) != 0) chosen.add(CABLES[i]);
            if (!isSpanningTree(5, chosen)) continue;
            trees++;
            int w = weight(chosen);
            if (w < cheapest) { cheapest = w; cheapestCount = 0; }
            if (w == cheapest) cheapestCount++;
        }
        check(subsets == 35, "C(7, 4) = 35 ways to pick 4 cables");
        check(trees == 21, "21 of them are spanning trees");
        check(cheapest == 7 && cheapestCount == 1, "exactly one tree costs the minimum, 7");
        check((int) Math.pow(5, 5 - 2) == 125, "Cayley: K5 has 5^3 = 125 spanning trees");

        List<int[]> tree = kruskal(5, CABLES);
        check(tree.size() == 4 && weight(tree) == 7, "Kruskal keeps 4 = V - 1 edges of total 7");
        check(Arrays.equals(tree.get(0), new int[] {0, 2, 1}) && Arrays.equals(tree.get(1), new int[] {1, 3, 1})
                && Arrays.equals(tree.get(2), new int[] {1, 2, 2}) && Arrays.equals(tree.get(3), new int[] {3, 4, 3}),
                "Kruskal takes A-C 1, B-D 1, B-C 2, D-E 3");
        check(prim(undirectedAdjacency(5, CABLES)) == 7, "Prim from A also totals 7");
        check(1 + 2 + 1 + 3 == 7 && 4 - 1 == 3, "swapping A-B (4) for A-C (1) saves 3");

        check(minCostConnectPoints(new int[][] {{0, 0}, {2, 2}, {3, 10}, {5, 2}, {7, 0}}) == 20, "LC 1584");
        check(minCostConnectPoints(new int[][] {{3, 12}, {-2, 5}, {-4, 1}}) == 18, "LC 1584 example 2");
        check(minCostConnectPoints(new int[][] {{0, 0}}) == 0, "LC 1584 one point");

        int[][] lc1489 = {{0, 1, 1}, {1, 2, 1}, {2, 3, 2}, {0, 3, 2}, {0, 4, 3}, {3, 4, 3}, {1, 4, 6}};
        check(mstWeight(5, lc1489, -1, -1) == 7, "LC 1489 cheapest tree costs 7");
        check(mstWeight(5, lc1489, 0, -1) == 8, "without edge 0 the cheapest tree costs 8");
        check(mstWeight(5, lc1489, 2, -1) == 7 && mstWeight(5, lc1489, -1, 2) == 7, "edge 2 is optional but usable");
        check(mstWeight(5, lc1489, -1, 6) == 10, "forcing edge 6 costs 10");
        check(findCriticalAndPseudoCriticalEdges(5, lc1489).equals(List.of(List.of(0, 1), List.of(2, 3, 4, 5))), "LC 1489 example 1");
        int[][] square = {{0, 1, 1}, {1, 2, 1}, {2, 3, 1}, {0, 3, 1}};
        check(findCriticalAndPseudoCriticalEdges(4, square).equals(List.of(List.of(), List.of(0, 1, 2, 3))), "LC 1489 example 2");
        System.out.println("OK");
    }
}
