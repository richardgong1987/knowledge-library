import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

// Run from this folder with: java BiconnectedComponentLab.java
public class BiconnectedComponentLab {

    // The running example: two triangles joined by one road. Each edge {u, v} is two-way.
    static final int[][] ROADS = {{0, 1}, {1, 2}, {2, 0}, {1, 3}, {3, 4}, {4, 5}, {5, 3}};

    static List<List<Integer>> adjacency(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) {
            adj.get(e[0]).add(e[1]);
            adj.get(e[1]).add(e[0]);
        }
        return adj;
    }

    // Level 1: the definition, by brute force. How many pieces are left without vertex `removed`?
    static int componentsWithout(int n, int[][] edges, int removed) {
        List<List<Integer>> adj = adjacency(n, edges);
        boolean[] seen = new boolean[n];
        int pieces = 0;
        for (int s = 0; s < n; s++) {
            if (s == removed || seen[s]) continue;
            pieces++;
            Deque<Integer> stack = new ArrayDeque<>(List.of(s));
            seen[s] = true;
            while (!stack.isEmpty()) {
                for (int v : adj.get(stack.pop())) {
                    if (v != removed && !seen[v]) { seen[v] = true; stack.push(v); }
                }
            }
        }
        return pieces;
    }

    // Levels 3 and 4: one DFS computes disc and low, then reads off bridges and cut vertices.
    static final class CutFinder {
        final List<List<Integer>> adj;
        final int[] disc, low;
        final List<int[]> bridges = new ArrayList<>();
        final Set<Integer> cutVertices = new TreeSet<>();
        int time = 0;

        CutFinder(int n, int[][] edges) {
            adj = adjacency(n, edges);
            disc = new int[n];
            low = new int[n];
            Arrays.fill(disc, -1);
            for (int v = 0; v < n; v++) if (disc[v] == -1) dfs(v, -1);
        }

        void dfs(int u, int parent) {
            disc[u] = low[u] = time++;
            int children = 0;
            for (int v : adj.get(u)) {
                if (v == parent) continue;
                if (disc[v] != -1) { low[u] = Math.min(low[u], disc[v]); continue; }   // back edge to an ancestor
                children++;
                dfs(v, u);
                low[u] = Math.min(low[u], low[v]);
                if (low[v] > disc[u]) bridges.add(new int[] {u, v});                 // v's subtree cannot reach u or above
                if (parent != -1 && low[v] >= disc[u]) cutVertices.add(u);         // v's subtree cannot reach above u
            }
            if (parent == -1 && children >= 2) cutVertices.add(u);                 // a root needs two separate subtrees
        }
    }

    // Level 5: the blocks themselves. Edges wait on a stack until their block is complete.
    static final class BlockFinder {
        final List<List<Integer>> adj;
        final int[] disc, low;
        final Deque<int[]> edgeStack = new ArrayDeque<>();
        final List<Set<Integer>> blocks = new ArrayList<>();
        int time = 0;

        BlockFinder(int n, int[][] edges) {
            adj = adjacency(n, edges);
            disc = new int[n];
            low = new int[n];
            Arrays.fill(disc, -1);
            for (int v = 0; v < n; v++) if (disc[v] == -1) dfs(v, -1);
        }

        void dfs(int u, int parent) {
            disc[u] = low[u] = time++;
            for (int v : adj.get(u)) {
                if (v == parent) continue;
                if (disc[v] == -1) {
                    edgeStack.push(new int[] {u, v});
                    dfs(v, u);
                    low[u] = Math.min(low[u], low[v]);
                    if (low[v] >= disc[u]) blocks.add(popBlock(u, v));   // u seals off v's subtree: a block ends here
                } else if (disc[v] < disc[u]) {
                    edgeStack.push(new int[] {u, v});                    // a back edge, pushed once from its lower end
                    low[u] = Math.min(low[u], disc[v]);
                }
            }
        }

        Set<Integer> popBlock(int u, int v) {
            Set<Integer> block = new TreeSet<>();
            int[] edge;
            do {
                edge = edgeStack.pop();
                block.add(edge[0]);
                block.add(edge[1]);
            } while (edge[0] != u || edge[1] != v);
            return block;
        }
    }

    // Interview 1: LeetCode 1192, Critical Connections in a Network.
    static List<List<Integer>> criticalConnections(int n, List<List<Integer>> connections) {
        int[][] edges = new int[connections.size()][];
        for (int i = 0; i < edges.length; i++) edges[i] = new int[] {connections.get(i).get(0), connections.get(i).get(1)};
        List<List<Integer>> answer = new ArrayList<>();
        for (int[] bridge : new CutFinder(n, edges).bridges) answer.add(List.of(bridge[0], bridge[1]));
        return answer;
    }

    // Interview 2: LeetCode 1568, Minimum Number of Days to Disconnect Island.
    static int minDays(int[][] grid) {
        if (countIslands(grid) != 1) return 0;                  // already disconnected
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                if (grid[r][c] == 0) continue;
                grid[r][c] = 0;                                 // try removing this one cell
                boolean split = countIslands(grid) != 1;
                grid[r][c] = 1;
                if (split) return 1;                            // it was a cut vertex of the island
            }
        }
        return 2;                                               // a corner cell has at most two land neighbours
    }

    static int countIslands(int[][] grid) {
        int islands = 0;
        boolean[][] seen = new boolean[grid.length][grid[0].length];
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                if (grid[r][c] == 1 && !seen[r][c]) { islands++; flood(grid, seen, r, c); }
            }
        }
        return islands;
    }

    static void flood(int[][] grid, boolean[][] seen, int r, int c) {
        if (r < 0 || r >= grid.length || c < 0 || c >= grid[0].length || grid[r][c] == 0 || seen[r][c]) return;
        seen[r][c] = true;
        flood(grid, seen, r + 1, c);
        flood(grid, seen, r - 1, c);
        flood(grid, seen, r, c + 1);
        flood(grid, seen, r, c - 1);
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] pieces = new int[6];
        for (int v = 0; v < 6; v++) pieces[v] = componentsWithout(6, ROADS, v);
        check(Arrays.equals(pieces, new int[] {1, 2, 1, 2, 1, 1}), "removing 1 or 3 leaves 2 pieces; any other vertex leaves 1");

        CutFinder cuts = new CutFinder(6, ROADS);
        check(Arrays.equals(cuts.disc, new int[] {0, 1, 2, 3, 4, 5}), "discovery times 0..5");
        check(Arrays.equals(cuts.low, new int[] {0, 0, 0, 3, 3, 3}), "low values 0 0 0 3 3 3");
        check(cuts.bridges.size() == 1 && Arrays.equals(cuts.bridges.get(0), new int[] {1, 3}), "the only bridge is 1-3");
        check(cuts.cutVertices.equals(Set.of(1, 3)), "cut vertices are 1 and 3");

        CutFinder path = new CutFinder(3, new int[][] {{0, 1}, {0, 2}});
        check(path.cutVertices.equals(Set.of(0)), "a root with two DFS children is a cut vertex");
        CutFinder triangle = new CutFinder(3, new int[][] {{0, 1}, {1, 2}, {2, 0}});
        check(triangle.cutVertices.isEmpty() && triangle.bridges.isEmpty(), "a triangle has no weak spot");

        BlockFinder blocks = new BlockFinder(6, ROADS);
        check(blocks.blocks.equals(List.of(Set.of(3, 4, 5), Set.of(1, 3), Set.of(0, 1, 2))), "blocks {3,4,5}, {1,3}, {0,1,2}");
        check(2 + 2 == 3 + 2 - 1, "block-cut tree: 3 blocks + 2 cut vertices, 4 edges");

        List<List<Integer>> network = List.of(List.of(0, 1), List.of(1, 2), List.of(2, 0), List.of(1, 3));
        check(criticalConnections(4, network).equals(List.of(List.of(1, 3))), "LC 1192 example 1");
        check(criticalConnections(2, List.of(List.of(0, 1))).equals(List.of(List.of(0, 1))), "LC 1192 example 2");

        check(minDays(new int[][] {{0, 1, 1, 0}, {0, 1, 1, 0}, {0, 0, 0, 0}}) == 2, "LC 1568 example 1: a 2x2 block");
        check(minDays(new int[][] {{1, 1}}) == 2, "LC 1568 example 2");
        check(minDays(new int[][] {{1, 1, 1}}) == 1, "the middle of a 1x3 strip is a cut cell");
        check(minDays(new int[][] {{1, 0, 1}}) == 0, "two islands already");
        check(minDays(new int[][] {{1}}) == 1, "one cell: remove it and nothing is left");
        System.out.println("OK");
    }
}
