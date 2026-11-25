import java.util.Arrays;

// Run from this folder with: java UnionFindLab.java
public class UnionFindLab {

    // Level 1: quick-find. id[x] names x's group, so "same group?" is one comparison.
    static final class QuickFind {
        final int[] id;

        QuickFind(int n) {
            id = new int[n];
            for (int i = 0; i < n; i++) id[i] = i;
        }

        boolean connected(int a, int b) {
            return id[a] == id[b];
        }

        void union(int a, int b) {
            int from = id[a], to = id[b];
            for (int i = 0; i < id.length; i++) {
                if (id[i] == from) id[i] = to;          // relabel the whole group: n steps every time
            }
        }
    }

    // Level 2: quick-union. Each element points at a parent; the root names the group.
    static final class QuickUnion {
        final int[] parent;

        QuickUnion(int n) {
            parent = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;
        }

        int find(int x) {
            while (parent[x] != x) x = parent[x];
            return x;
        }

        void union(int a, int b) {
            parent[find(a)] = find(b);
        }
    }

    // Levels 3 and 4: union by size plus path compression, the version to use.
    static final class UnionFind {
        final int[] parent, size;
        int count;                                      // number of separate groups

        UnionFind(int n) {
            parent = new int[n];
            size = new int[n];
            for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; }
            count = n;
        }

        int find(int x) {
            if (parent[x] != x) parent[x] = find(parent[x]);   // path compression: point x straight at the root
            return parent[x];
        }

        boolean union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return false;                 // already one group: this edge would close a cycle
            if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
            parent[rb] = ra;                            // hang the smaller tree under the bigger root
            size[ra] += size[rb];
            count--;
            return true;
        }
    }

    static int depth(int[] parent, int x) {
        int steps = 0;
        while (parent[x] != x) { x = parent[x]; steps++; }
        return steps;
    }

    // Interview 1: LeetCode 547, Number of Provinces.
    static int findCircleNum(int[][] isConnected) {
        int n = isConnected.length;
        UnionFind uf = new UnionFind(n);
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (isConnected[i][j] == 1) uf.union(i, j);
            }
        }
        return uf.count;
    }

    // Interview 2: LeetCode 684, Redundant Connection.
    static int[] findRedundantConnection(int[][] edges) {
        UnionFind uf = new UnionFind(edges.length + 1);
        for (int[] e : edges) {
            if (!uf.union(e[0], e[1])) return e;        // both ends already joined: e closes the cycle
        }
        throw new IllegalArgumentException("the input has no cycle");
    }

    // Interview 3: LeetCode 685, Redundant Connection II.
    static int[] findRedundantDirectedConnection(int[][] edges) {
        int n = edges.length;
        int[] parentOf = new int[n + 1];
        int[] first = null, second = null;              // the two edges into a node with two parents
        for (int[] e : edges) {
            if (parentOf[e[1]] == 0) parentOf[e[1]] = e[0];
            else { first = new int[] {parentOf[e[1]], e[1]}; second = e; }
        }
        UnionFind uf = new UnionFind(n + 1);
        for (int[] e : edges) {
            if (e == second) continue;                  // try the tree without the later parent edge
            if (!uf.union(e[0], e[1])) return first == null ? e : first;
        }
        return second;
    }

    // Interview 4: LeetCode 1579, Remove Max Number of Edges to Keep Graph Fully Traversable.
    static int maxNumEdgesToRemove(int n, int[][] edges) {
        UnionFind alice = new UnionFind(n), bob = new UnionFind(n);
        int used = 0;
        for (int[] e : edges) {
            if (e[0] != 3) continue;                    // shared edges first: one edge serves both people
            boolean forAlice = alice.union(e[1] - 1, e[2] - 1);
            boolean forBob = bob.union(e[1] - 1, e[2] - 1);
            if (forAlice || forBob) used++;
        }
        for (int[] e : edges) {
            if (e[0] == 1 && alice.union(e[1] - 1, e[2] - 1)) used++;
            if (e[0] == 2 && bob.union(e[1] - 1, e[2] - 1)) used++;
        }
        if (alice.count > 1 || bob.count > 1) return -1;
        return edges.length - used;
    }

    // Level 4: CLRS's A_k(j), applying A_(k-1) to j, j + 1 times. Only tiny arguments are computable.
    static long ackermann(int k, long j) {
        if (k == 0) return j + 1;
        long x = j;
        for (long i = 0; i <= j; i++) x = ackermann(k - 1, x);
        return x;
    }

    static int floorLog2(long n) {
        return 63 - Long.numberOfLeadingZeros(n);
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        QuickFind quickFind = new QuickFind(6);
        quickFind.union(0, 1);
        quickFind.union(2, 3);
        quickFind.union(0, 2);
        check(Arrays.equals(quickFind.id, new int[] {3, 3, 3, 3, 4, 5}), "quick-find relabels 0 and 1 to 3");
        check(quickFind.connected(1, 3) && !quickFind.connected(1, 4), "quick-find answers in one comparison");

        QuickUnion chain = new QuickUnion(6);
        for (int x = 1; x < 6; x++) chain.union(0, x);
        check(Arrays.equals(chain.parent, new int[] {1, 2, 3, 4, 5, 5}), "naive unions build the chain 0→1→2→3→4→5");
        check(depth(chain.parent, 0) == 5, "the chain has height n - 1 = 5");

        UnionFind bySize = new UnionFind(6);
        for (int x = 1; x < 6; x++) bySize.union(0, x);
        check(Arrays.equals(bySize.parent, new int[] {0, 0, 0, 0, 0, 0}), "union by size keeps every node one step from 0");

        UnionFind uf = new UnionFind(6);
        uf.union(0, 1);
        uf.union(2, 3);
        check(Arrays.equals(uf.parent, new int[] {0, 0, 2, 2, 4, 5}), "after union(0,1), union(2,3)");
        uf.union(0, 2);
        check(Arrays.equals(uf.parent, new int[] {0, 0, 0, 2, 4, 5}) && uf.size[0] == 4, "after union(0,2)");
        check(depth(uf.parent, 3) == 2, "3 sits at depth 2 = log2(4)");
        uf.union(4, 5);
        check(Arrays.equals(uf.parent, new int[] {0, 0, 0, 2, 4, 4}), "after union(4,5)");
        check(uf.find(3) == 0 && Arrays.equals(uf.parent, new int[] {0, 0, 0, 0, 4, 4}), "find(3) compresses 3 to the root");
        uf.union(5, 3);
        check(Arrays.equals(uf.parent, new int[] {0, 0, 0, 0, 0, 4}) && uf.size[0] == 6, "the size-2 tree hangs under the size-4 root");
        check(uf.count == 1 && !uf.union(1, 5), "6 - 5 successful unions = 1 group; a sixth union fails");
        check(floorLog2(6) == 2 && floorLog2(4) == 2 && floorLog2(1_000_000) == 19 && floorLog2(100_000) == 16, "log2 depth bounds");
        check(ackermann(0, 1) == 2 && ackermann(1, 1) == 3 && ackermann(2, 1) == 7 && ackermann(3, 1) == 2047, "A_k(1) = 2, 3, 7, 2047");
        check(ackermann(2, 1) < 1_000_000 && ackermann(3, 1) < 1_000_000, "alpha(10^6) = 4: A_3(1) is still below 10^6");

        check(findCircleNum(new int[][] {{1, 1, 0}, {1, 1, 0}, {0, 0, 1}}) == 2, "LC 547");
        check(findCircleNum(new int[][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}) == 3, "LC 547 no roads");
        check(Arrays.equals(findRedundantConnection(new int[][] {{1, 2}, {1, 3}, {2, 3}}), new int[] {2, 3}), "LC 684 example 1");
        check(Arrays.equals(findRedundantConnection(new int[][] {{1, 2}, {2, 3}, {3, 4}, {1, 4}, {1, 5}}), new int[] {1, 4}),
                "LC 684 example 2");
        check(Arrays.equals(findRedundantDirectedConnection(new int[][] {{1, 2}, {1, 3}, {2, 3}}), new int[] {2, 3}),
                "LC 685 two parents, no cycle");
        check(Arrays.equals(findRedundantDirectedConnection(new int[][] {{1, 2}, {2, 3}, {3, 4}, {4, 1}, {1, 5}}), new int[] {4, 1}),
                "LC 685 cycle, no two parents");
        check(Arrays.equals(findRedundantDirectedConnection(new int[][] {{2, 1}, {3, 1}, {4, 2}, {1, 4}}), new int[] {2, 1}),
                "LC 685 two parents and a cycle");
        check(maxNumEdgesToRemove(4, new int[][] {{3, 1, 2}, {3, 2, 3}, {1, 1, 3}, {1, 2, 4}, {1, 1, 2}, {2, 3, 4}}) == 2, "LC 1579");
        check(maxNumEdgesToRemove(4, new int[][] {{3, 1, 2}, {3, 2, 3}, {1, 1, 4}, {2, 1, 4}}) == 0, "LC 1579 nothing spare");
        check(maxNumEdgesToRemove(4, new int[][] {{3, 2, 3}, {1, 1, 2}, {2, 3, 4}}) == -1, "LC 1579 impossible");
        System.out.println("OK");
    }
}
