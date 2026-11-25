import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Run from this folder with: java StronglyConnectedComponentLab.java
public class StronglyConnectedComponentLab {

    // The running example: six crossings joined by one-way streets {from, to}.
    static final int[][] STREETS = {{0, 1}, {1, 2}, {2, 0}, {2, 3}, {3, 4}, {4, 3}, {4, 5}};

    static List<List<Integer>> adjacency(int n, int[][] edges) {
        List<List<Integer>> next = new ArrayList<>();
        for (int i = 0; i < n; i++) next.add(new ArrayList<>());
        for (int[] e : edges) next.get(e[0]).add(e[1]);
        return next;
    }

    static int[][] reverse(int[][] edges) {
        int[][] reversed = new int[edges.length][];
        for (int i = 0; i < edges.length; i++) reversed[i] = new int[] {edges[i][1], edges[i][0]};
        return reversed;
    }

    // Level 1: the definition, by brute force. reach[u][v] is true when u can get to v.
    static boolean[][] reachability(int n, int[][] edges) {
        List<List<Integer>> next = adjacency(n, edges);
        boolean[][] reach = new boolean[n][n];
        for (int s = 0; s < n; s++) {
            Deque<Integer> stack = new ArrayDeque<>(List.of(s));
            reach[s][s] = true;
            while (!stack.isEmpty()) {
                for (int v : next.get(stack.pop())) {
                    if (!reach[s][v]) { reach[s][v] = true; stack.push(v); }
                }
            }
        }
        return reach;
    }

    // Level 2: Kosaraju-Sharir. Pass 1 records finishing order; pass 2 explores the reversed graph.
    static int[] kosaraju(int n, int[][] edges) {
        List<List<Integer>> graph = adjacency(n, edges), reversed = adjacency(n, reverse(edges));
        Deque<Integer> finishOrder = new ArrayDeque<>();
        boolean[] seen = new boolean[n];
        for (int v = 0; v < n; v++) if (!seen[v]) finishDfs(v, graph, seen, finishOrder);
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int count = 0;
        for (int v : finishOrder) if (component[v] == -1) label(v, reversed, component, count++);  // latest finisher first
        return component;
    }

    static void finishDfs(int u, List<List<Integer>> graph, boolean[] seen, Deque<Integer> finishOrder) {
        seen[u] = true;
        for (int v : graph.get(u)) if (!seen[v]) finishDfs(v, graph, seen, finishOrder);
        finishOrder.push(u);                                // the front of the deque finished last
    }

    static void label(int u, List<List<Integer>> reversed, int[] component, int id) {
        component[u] = id;
        for (int v : reversed.get(u)) if (component[v] == -1) label(v, reversed, component, id);
    }

    // Level 3: Tarjan. One DFS; low[u] is the earliest open vertex that u's subtree can reach.
    static final class Tarjan {
        final List<List<Integer>> graph;
        final int[] index, low, component;
        final boolean[] onStack;
        final Deque<Integer> stack = new ArrayDeque<>();
        int time = 0, count = 0;

        Tarjan(int n, int[][] edges) {
            graph = adjacency(n, edges);
            index = new int[n];
            low = new int[n];
            component = new int[n];
            onStack = new boolean[n];
            Arrays.fill(index, -1);
            for (int v = 0; v < n; v++) if (index[v] == -1) visit(v);
        }

        void visit(int u) {
            index[u] = low[u] = time++;
            stack.push(u);
            onStack[u] = true;
            for (int v : graph.get(u)) {
                if (index[v] == -1) { visit(v); low[u] = Math.min(low[u], low[v]); }
                else if (onStack[v]) low[u] = Math.min(low[u], index[v]);   // an edge back into an open component
            }
            if (low[u] == index[u]) {                       // u is the first vertex of its component
                int v;
                do { v = stack.pop(); onStack[v] = false; component[v] = count; } while (v != u);
                count++;
            }
        }
    }

    // Level 4: the condensation. One vertex per component, one edge per street between components.
    static Set<List<Integer>> condensationEdges(int[] component, int[][] edges) {
        Set<List<Integer>> dagEdges = new HashSet<>();
        for (int[] e : edges) {
            if (component[e[0]] != component[e[1]]) dagEdges.add(List.of(component[e[0]], component[e[1]]));
        }
        return dagEdges;
    }

    static int edgesToMakeStronglyConnected(int components, Set<List<Integer>> dagEdges) {
        if (components == 1) return 0;
        boolean[] hasIn = new boolean[components], hasOut = new boolean[components];
        for (List<Integer> e : dagEdges) { hasOut[e.get(0)] = true; hasIn[e.get(1)] = true; }
        int sources = 0, sinks = 0;
        for (int c = 0; c < components; c++) {
            if (!hasIn[c]) sources++;
            if (!hasOut[c]) sinks++;
        }
        return Math.max(sources, sinks);
    }

    // Interview 1: LeetCode 1557, Minimum Number of Vertices to Reach All Nodes.
    static List<Integer> findSmallestSetOfVertices(int n, List<List<Integer>> edges) {
        boolean[] hasIncoming = new boolean[n];
        for (List<Integer> e : edges) hasIncoming[e.get(1)] = true;
        List<Integer> sources = new ArrayList<>();
        for (int v = 0; v < n; v++) if (!hasIncoming[v]) sources.add(v);
        return sources;
    }

    // Interview 2: LeetCode 802, Find Eventual Safe States.
    static List<Integer> eventualSafeNodes(int[][] graph) {
        int n = graph.length;
        List<List<Integer>> parents = new ArrayList<>();
        for (int i = 0; i < n; i++) parents.add(new ArrayList<>());
        int[] exitsLeft = new int[n];
        for (int u = 0; u < n; u++) {
            exitsLeft[u] = graph[u].length;
            for (int v : graph[u]) parents.get(v).add(u);
        }
        Deque<Integer> ready = new ArrayDeque<>();
        for (int v = 0; v < n; v++) if (exitsLeft[v] == 0) ready.add(v);
        boolean[] safe = new boolean[n];
        while (!ready.isEmpty()) {
            int v = ready.poll();
            safe[v] = true;
            for (int u : parents.get(v)) if (--exitsLeft[u] == 0) ready.add(u);   // every exit of u is now safe
        }
        List<Integer> answer = new ArrayList<>();
        for (int v = 0; v < n; v++) if (safe[v]) answer.add(v);
        return answer;
    }

    // Interview 3: LeetCode 2360, Longest Cycle in a Graph.
    static int longestCycle(int[] edges) {
        int n = edges.length, best = -1, clock = 0;
        int[] visitedAt = new int[n];                       // 0 = never visited
        for (int start = 0; start < n; start++) {
            if (visitedAt[start] != 0) continue;
            int walkStart = clock + 1, v = start;
            while (v != -1 && visitedAt[v] == 0) {
                visitedAt[v] = ++clock;
                v = edges[v];
            }
            if (v != -1 && visitedAt[v] >= walkStart) best = Math.max(best, clock - visitedAt[v] + 1);  // met our own trail
        }
        return best;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        boolean[][] reach = reachability(6, STREETS);
        check(reach[0][5] && !reach[5][0], "0 reaches 5 but 5 cannot come back");
        check(reach[3][4] && reach[4][3] && reach[0][2] && reach[2][0], "3 ~ 4 and 0 ~ 2");
        int mutual = 0;
        for (int u = 0; u < 6; u++) for (int v = 0; v < 6; v++) if (reach[u][v] && reach[v][u]) mutual++;
        check(mutual == 9 + 4 + 1, "mutual pairs: 3^2 + 2^2 + 1^2 = 14");

        check(Arrays.equals(kosaraju(6, STREETS), new int[] {0, 0, 0, 1, 1, 2}), "Kosaraju: {0,1,2}, {3,4}, {5} in topological order");

        Tarjan tarjan = new Tarjan(6, STREETS);
        check(Arrays.equals(tarjan.index, new int[] {0, 1, 2, 3, 4, 5}), "Tarjan discovery order 0..5");
        check(Arrays.equals(tarjan.low, new int[] {0, 0, 0, 3, 3, 5}), "Tarjan low-links 0 0 0 3 3 5");
        check(Arrays.equals(tarjan.component, new int[] {2, 2, 2, 1, 1, 0}), "Tarjan finds {5}, then {3,4}, then {0,1,2}");
        check(tarjan.count == 3, "three components");

        Set<List<Integer>> dag = condensationEdges(kosaraju(6, STREETS), STREETS);
        check(dag.equals(Set.of(List.of(0, 1), List.of(1, 2))), "condensation: {0,1,2} -> {3,4} -> {5}");
        check(edgesToMakeStronglyConnected(3, dag) == 1, "one new street, 5 -> 0, makes everything mutual");
        int[][] withReturn = Arrays.copyOf(STREETS, STREETS.length + 1);
        withReturn[STREETS.length] = new int[] {5, 0};
        check(new Tarjan(6, withReturn).count == 1, "after adding 5 -> 0 there is one component");

        List<List<Integer>> lc1557 = List.of(List.of(0, 1), List.of(0, 2), List.of(2, 5), List.of(3, 4), List.of(4, 2));
        check(findSmallestSetOfVertices(6, lc1557).equals(List.of(0, 3)), "LC 1557 example 1");
        List<List<Integer>> lc1557b = List.of(List.of(0, 1), List.of(2, 1), List.of(3, 1), List.of(1, 4), List.of(2, 4));
        check(findSmallestSetOfVertices(5, lc1557b).equals(List.of(0, 2, 3)), "LC 1557 example 2");

        check(eventualSafeNodes(new int[][] {{1, 2}, {2, 3}, {5}, {0}, {5}, {}, {}}).equals(List.of(2, 4, 5, 6)), "LC 802 example 1");
        check(eventualSafeNodes(new int[][] {{1, 2, 3, 4}, {1, 2}, {3, 4}, {0, 4}, {}}).equals(List.of(4)), "LC 802 example 2");

        check(longestCycle(new int[] {3, 3, 4, 2, 3}) == 3, "LC 2360 example 1");
        check(longestCycle(new int[] {2, -1, 3, 1}) == -1, "LC 2360 example 2");
        check(longestCycle(new int[] {1, 0}) == 2, "LC 2360 two-cycle");
        System.out.println("OK");
    }
}
