import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// Run from this folder with: java DepthFirstSearchLab.java
public class DepthFirstSearchLab {

    // Level 1: recursive DFS. visited stops us from walking in circles.
    static void dfs(List<List<Integer>> graph, int v, boolean[] visited, List<Integer> order) {
        visited[v] = true;
        order.add(v);
        for (int next : graph.get(v)) {
            if (!visited[next]) dfs(graph, next, visited, order);
        }
    }

    // Level 2: the same order with an explicit stack, so deep graphs cannot overflow the call stack.
    static List<Integer> dfsIterative(List<List<Integer>> graph, int start) {
        boolean[] visited = new boolean[graph.size()];
        Deque<Integer> stack = new ArrayDeque<>(List.of(start));
        List<Integer> order = new ArrayList<>();
        while (!stack.isEmpty()) {
            int v = stack.pop();
            if (visited[v]) continue;                 // a vertex can be pushed twice before it is visited
            visited[v] = true;
            order.add(v);
            List<Integer> next = graph.get(v);
            for (int i = next.size() - 1; i >= 0; i--) stack.push(next.get(i)); // reversed: pop in list order
        }
        return order;
    }

    // Level 3: discovery and finish times for a DFS of the whole graph.
    static final class Timestamps {
        final List<List<Integer>> graph;
        final int[] discovered, finished;             // 0 = not discovered yet
        int time = 0;

        Timestamps(List<List<Integer>> graph) {
            this.graph = graph;
            discovered = new int[graph.size()];
            finished = new int[graph.size()];
            for (int v = 0; v < graph.size(); v++) if (discovered[v] == 0) visit(v);
        }

        void visit(int v) {
            discovered[v] = ++time;                   // v turns grey
            for (int next : graph.get(v)) if (discovered[next] == 0) visit(next);
            finished[v] = ++time;                     // v turns black
        }
    }

    // Level 4: cycle detection in a directed graph with three colours.
    static final int WHITE = 0, GREY = 1, BLACK = 2;

    static boolean hasCycle(List<List<Integer>> graph) {
        int[] color = new int[graph.size()];          // every vertex starts WHITE
        for (int v = 0; v < graph.size(); v++) {
            if (color[v] == WHITE && reachesCycle(graph, v, color)) return true;
        }
        return false;
    }

    static boolean reachesCycle(List<List<Integer>> graph, int v, int[] color) {
        color[v] = GREY;                              // v is on the current path
        for (int next : graph.get(v)) {
            if (color[next] == GREY) return true;     // back edge: next is an ancestor of v
            if (color[next] == WHITE && reachesCycle(graph, next, color)) return true;
        }
        color[v] = BLACK;                             // v and everything below it are finished
        return false;
    }

    // Level 5: a grid is a graph whose neighbours are up, down, left and right.
    static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    static int sink(char[][] grid, int r, int c) {    // flood fill: returns how many cells it sank
        if (r < 0 || r >= grid.length || c < 0 || c >= grid[0].length || grid[r][c] != '1') return 0;
        grid[r][c] = '0';                             // sinking the cell is the visited mark
        int cells = 1;
        for (int[] d : DIRECTIONS) cells += sink(grid, r + d[0], c + d[1]);
        return cells;
    }

    // Interview 1: LeetCode 200, Number of Islands.
    static int numIslands(char[][] grid) {
        int islands = 0;
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                if (grid[r][c] == '1') {              // land no earlier island reached: a new island
                    islands++;
                    sink(grid, r, c);
                }
            }
        }
        return islands;
    }

    // Interview 2: LeetCode 802, Find Eventual Safe States.
    static List<Integer> eventualSafeNodes(int[][] graph) {
        int[] color = new int[graph.length];
        List<Integer> safe = new ArrayList<>();
        for (int v = 0; v < graph.length; v++) if (isSafe(graph, v, color)) safe.add(v);
        return safe;
    }

    static boolean isSafe(int[][] graph, int v, int[] color) {
        if (color[v] != WHITE) return color[v] == BLACK;       // grey: v is on a path that loops
        color[v] = GREY;
        for (int next : graph[v]) if (!isSafe(graph, next, color)) return false; // v stays grey
        color[v] = BLACK;
        return true;
    }

    // Interview 3: LeetCode 329, Longest Increasing Path in a Matrix.
    static int longestIncreasingPath(int[][] m) {
        int[][] memo = new int[m.length][m[0].length];          // 0 = not computed yet
        int best = 0;
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m[0].length; c++) best = Math.max(best, longestFrom(m, r, c, memo));
        }
        return best;
    }

    static int longestFrom(int[][] m, int r, int c, int[][] memo) {
        if (memo[r][c] > 0) return memo[r][c];
        int best = 1;                                           // the path of just this cell
        for (int[] d : DIRECTIONS) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < m.length && nc >= 0 && nc < m[0].length && m[nr][nc] > m[r][c]) {
                best = Math.max(best, 1 + longestFrom(m, nr, nc, memo));
            }
        }
        return memo[r][c] = best;
    }

    static List<List<Integer>> graph(int n, int[][] edges, boolean directed) {
        List<List<Integer>> g = new ArrayList<>();
        for (int i = 0; i < n; i++) g.add(new ArrayList<>());
        for (int[] e : edges) {
            g.get(e[0]).add(e[1]);
            if (!directed) g.get(e[1]).add(e[0]);
        }
        return g;
    }

    static char[][] grid(String... rows) {
        char[][] g = new char[rows.length][];
        for (int i = 0; i < rows.length; i++) g[i] = rows[i].toCharArray();
        return g;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[][] edges = {{0, 1}, {0, 2}, {1, 3}, {2, 4}, {3, 4}};
        List<List<Integer>> rooms = graph(6, edges, false);
        check(rooms.get(0).equals(List.of(1, 2)) && rooms.get(4).equals(List.of(2, 3)), "adjacency lists");

        boolean[] visited = new boolean[6];
        List<Integer> order = new ArrayList<>();
        dfs(rooms, 0, visited, order);
        check(order.equals(List.of(0, 1, 3, 4, 2)) && !visited[5], "DFS order from 0; room 5 is unreachable");
        check(dfsIterative(rooms, 0).equals(List.of(0, 1, 3, 4, 2)), "the explicit stack gives the same order");
        int degreeSum = 0;
        for (List<Integer> next : rooms) degreeSum += next.size();
        check(degreeSum == 2 * edges.length && degreeSum == 10, "each undirected edge is stored twice");
        check(6 + degreeSum == 16, "V + 2E = 16 steps for the whole graph");

        List<List<Integer>> corridor = new ArrayList<>();       // rooms 0 -> 1 -> ... -> 299,999 in a line
        for (int i = 0; i < 300_000; i++) corridor.add(i + 1 < 300_000 ? List.of(i + 1) : List.of());
        check(dfsIterative(corridor, 0).size() == 300_000, "the explicit stack walks 300,000 rooms");
        boolean overflowed = false;
        try {
            dfs(corridor, 0, new boolean[300_000], new ArrayList<>());
        } catch (StackOverflowError e) {
            overflowed = true;
        }
        check(overflowed, "recursion 300,000 calls deep overflows the default call stack");

        Timestamps t = new Timestamps(rooms);
        check(java.util.Arrays.equals(t.discovered, new int[] {1, 2, 5, 3, 4, 11}), "discovery times");
        check(java.util.Arrays.equals(t.finished, new int[] {10, 9, 6, 8, 7, 12}), "finish times");

        List<List<Integer>> cyclic = graph(4, new int[][] {{0, 1}, {1, 2}, {2, 0}, {2, 3}}, true);
        List<List<Integer>> dag = graph(4, new int[][] {{0, 1}, {0, 2}, {1, 2}, {2, 3}}, true);
        check(hasCycle(cyclic), "0 -> 1 -> 2 -> 0 is a cycle");
        check(!hasCycle(dag), "reaching 2 twice is not a cycle");
        check(!hasCycle(graph(1, new int[0][], true)) && hasCycle(graph(1, new int[][] {{0, 0}}, true)), "self-loop");

        check(sink(grid("110", "010", "101"), 0, 0) == 3, "the island at the top left has 3 cells");

        String[] map = {"11000", "11000", "00100", "00011"};
        check(numIslands(grid(map)) == 3, "LC 200");
        int land = 0;
        for (String row : map) for (char c : row.toCharArray()) if (c == '1') land++;
        check(land == 7, "7 land cells");
        check(numIslands(grid("0")) == 0 && numIslands(grid("1")) == 1, "LC 200 one cell");

        int[][] arrows = {{1, 2}, {2, 3}, {5}, {0}, {5}, {}, {}};
        check(eventualSafeNodes(arrows).equals(List.of(2, 4, 5, 6)), "LC 802");

        int[][] heights = {{9, 9, 4}, {6, 6, 8}, {2, 1, 1}};
        check(longestIncreasingPath(heights) == 4, "LC 329: 1 -> 2 -> 6 -> 9");
        int[][] memo = new int[3][3];
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) longestFrom(heights, r, c, memo);
        check(java.util.Arrays.deepEquals(memo, new int[][] {{1, 1, 2}, {2, 2, 1}, {3, 4, 2}}), "LC 329 memo table");
        check(longestIncreasingPath(new int[][] {{3, 4, 5}, {3, 2, 6}, {2, 2, 1}}) == 4, "LC 329 second example");
        check(longestIncreasingPath(new int[][] {{1}}) == 1, "LC 329 one cell");
        System.out.println("OK");
    }
}
