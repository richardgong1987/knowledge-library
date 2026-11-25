import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Run from this folder with: java GraphTheoryLab.java
public class GraphTheoryLab {

    // Level 2: the same graph stored two ways.
    static boolean[][] adjacencyMatrix(int n, int[][] edges) {
        boolean[][] matrix = new boolean[n][n];
        for (int[] e : edges) matrix[e[0]][e[1]] = matrix[e[1]][e[0]] = true;
        return matrix;
    }

    static List<List<Integer>> adjacencyList(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) {
            adj.get(e[0]).add(e[1]);
            adj.get(e[1]).add(e[0]);   // undirected: the edge is stored at both ends
        }
        return adj;
    }

    // Level 3: the handshake lemma, counted.
    static int degreeSum(List<List<Integer>> adj) {
        int sum = 0;
        for (List<Integer> neighbours : adj) sum += neighbours.size();
        return sum;
    }

    // Level 4: one-way streets with lengths.
    record Edge(int to, int weight) {}

    static List<List<Edge>> directedWeighted(int n, int[][] edges) {   // each edge is {from, to, weight}
        List<List<Edge>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) adj.get(e[0]).add(new Edge(e[1], e[2]));   // stored once, at its tail
        return adj;
    }

    // Level 5: label every vertex with the number of its connected component.
    static int[] components(List<List<Integer>> adj) {
        int[] label = new int[adj.size()];
        Arrays.fill(label, -1);
        int count = 0;
        for (int start = 0; start < adj.size(); start++) {
            if (label[start] != -1) continue;
            Deque<Integer> stack = new ArrayDeque<>(List.of(start));
            label[start] = count;
            while (!stack.isEmpty()) {
                int u = stack.pop();
                for (int v : adj.get(u)) {
                    if (label[v] == -1) { label[v] = count; stack.push(v); }
                }
            }
            count++;
        }
        return label;
    }

    // Level 6: the definition of a 2-colouring, checked edge by edge.
    static boolean isProperTwoColouring(int[][] edges, int[] colour) {
        for (int[] e : edges) {
            if (colour[e[0]] == colour[e[1]]) return false;
        }
        return true;
    }

    // Interview 1: LeetCode 133, Clone Graph.
    static final class Node {
        int val;
        List<Node> neighbors = new ArrayList<>();

        Node(int val) { this.val = val; }
    }

    static Node cloneGraph(Node node) {
        return node == null ? null : clone(node, new HashMap<>());
    }

    static Node clone(Node node, Map<Node, Node> copies) {
        if (copies.containsKey(node)) return copies.get(node);
        Node copy = new Node(node.val);
        copies.put(node, copy);   // register before recursing, so a cycle stops here
        for (Node next : node.neighbors) copy.neighbors.add(clone(next, copies));
        return copy;
    }

    // Interview 2: LeetCode 785, Is Graph Bipartite?
    static boolean isBipartite(int[][] graph) {
        int[] colour = new int[graph.length];
        Arrays.fill(colour, -1);
        for (int start = 0; start < graph.length; start++) {
            if (colour[start] != -1) continue;
            colour[start] = 0;
            Deque<Integer> queue = new ArrayDeque<>(List.of(start));
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (int v : graph[u]) {
                    if (colour[v] == -1) { colour[v] = 1 - colour[u]; queue.add(v); }
                    else if (colour[v] == colour[u]) return false;   // both ends share a colour
                }
            }
        }
        return true;
    }

    // Interview 3: LeetCode 2493, Divide Nodes Into the Maximum Number of Groups.
    static int magnificentSets(int n, int[][] edges) {
        List<List<Integer>> adj = adjacencyList(n + 1, edges);   // nodes are 1..n, so vertex 0 stays alone
        int[] component = components(adj);
        Map<Integer, Integer> bestLayers = new HashMap<>();
        for (int start = 1; start <= n; start++) {
            int layers = bfsLayers(adj, start);
            if (layers == -1) return -1;
            bestLayers.merge(component[start], layers, Math::max);
        }
        int total = 0;
        for (int layers : bestLayers.values()) total += layers;
        return total;
    }

    static int bfsLayers(List<List<Integer>> adj, int start) {
        int[] dist = new int[adj.size()];
        Arrays.fill(dist, -1);
        dist[start] = 0;
        Deque<Integer> queue = new ArrayDeque<>(List.of(start));
        int deepest = 0;
        while (!queue.isEmpty()) {
            int u = queue.poll();
            deepest = dist[u];
            for (int v : adj.get(u)) {
                if (dist[v] == -1) { dist[v] = dist[u] + 1; queue.add(v); }
                else if (dist[v] == dist[u]) return -1;   // an edge inside one layer closes an odd cycle
            }
        }
        return deepest + 1;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int n = 6;
        int[][] edges = {{0, 1}, {0, 2}, {1, 2}, {1, 3}, {4, 5}};

        // Level 1: vocabulary.
        int m = edges.length;
        check(m == 5 && n * (n - 1) / 2 == 15, "5 of the 15 possible edges");

        // Level 2: matrix versus list.
        boolean[][] matrix = adjacencyMatrix(n, edges);
        List<List<Integer>> adj = adjacencyList(n, edges);
        check(matrix.length * matrix.length == 36, "matrix has n^2 = 36 cells");
        check(matrix[1][3] && matrix[3][1] && !matrix[0][3], "matrix is symmetric");
        int listEntries = n + degreeSum(adj);
        check(listEntries == 16, "list has n + 2m = 16 entries");
        check(adj.get(1).equals(List.of(0, 2, 3)), "neighbours of 1");
        check(1000L * 1000 == 1_000_000 && 1000 + 2 * 3000 == 7000, "sparse example");

        // Level 3: handshake lemma.
        int[] degree = new int[n];
        for (int v = 0; v < n; v++) degree[v] = adj.get(v).size();
        check(Arrays.equals(degree, new int[] {2, 3, 2, 1, 1, 1}), "degrees");
        check(degreeSum(adj) == 2 * m, "sum of degrees = 2m = 10");
        int odd = 0;
        for (int d : degree) if (d % 2 == 1) odd++;
        check(odd == 4, "four odd-degree vertices, an even number");
        check(5 + 3 + 3 + 3 == 2 * 7, "Koenigsberg: degrees 5, 3, 3, 3 and 7 bridges");

        // Level 4: directed and weighted.
        List<List<Edge>> roads = directedWeighted(3, new int[][] {{0, 1, 4}, {0, 2, 1}, {2, 1, 2}});
        int outSum = 0;
        int[] in = new int[3];
        for (List<Edge> out : roads) {
            outSum += out.size();
            for (Edge e : out) in[e.to()]++;
        }
        check(outSum == 3 && Arrays.equals(in, new int[] {0, 2, 1}), "sum of out-degrees = sum of in-degrees = m = 3");
        check(roads.get(0).get(1).weight() + roads.get(2).get(0).weight() == 3, "0 -> 2 -> 1 costs 1 + 2 = 3 < 4");

        // Level 5: components.
        int[] label = components(adj);
        check(Arrays.equals(label, new int[] {0, 0, 0, 0, 1, 1}), "two components");
        check(m - n + 2 == 1, "one independent cycle");

        // Level 6: two colourings.
        check(!isProperTwoColouring(edges, new int[] {0, 1, 1, 0, 0, 1}), "triangle 0-1-2 cannot be 2-coloured");
        int[][] withoutEdge12 = {{0, 1}, {0, 2}, {1, 3}, {4, 5}};
        check(isProperTwoColouring(withoutEdge12, new int[] {0, 1, 1, 0, 0, 1}), "removing 1-2 makes it bipartite");

        // Interview 1: clone a 4-cycle 1-2-3-4-1.
        Node[] original = new Node[5];
        for (int v = 1; v <= 4; v++) original[v] = new Node(v);
        int[][] cycle = {{2, 4}, {1, 3}, {2, 4}, {1, 3}};
        for (int v = 1; v <= 4; v++) for (int w : cycle[v - 1]) original[v].neighbors.add(original[w]);
        Map<Node, Node> copies = new HashMap<>();
        Node copy = clone(original[1], copies);
        check(copies.size() == 4 && copy != original[1] && copy.val == 1, "4 new nodes");
        check(copy.neighbors.get(0).val == 2 && copy.neighbors.get(1).val == 4, "neighbours of the copy of 1");
        check(copy.neighbors.get(0).neighbors.get(0) == copy, "copy of 2 points back to copy of 1");
        check(cloneGraph(null) == null, "empty graph");

        // Interview 2.
        check(!isBipartite(new int[][] {{1, 2, 3}, {0, 2}, {0, 1, 3}, {0, 2}}), "LC 785 example 1");
        check(isBipartite(new int[][] {{1, 3}, {0, 2}, {1, 3}, {0, 2}}), "LC 785 example 2");
        check(isBipartite(new int[][] {{}, {}}), "LC 785 no edges");

        // Interview 3.
        int[][] example = {{1, 2}, {1, 4}, {1, 5}, {2, 6}, {2, 3}, {4, 6}};
        List<List<Integer>> exampleAdj = adjacencyList(7, example);
        check(bfsLayers(exampleAdj, 5) == 4 && bfsLayers(exampleAdj, 3) == 4 && bfsLayers(exampleAdj, 1) == 3,
                "layers from 5, 3 and 1");
        check(magnificentSets(6, example) == 4, "LC 2493 example 1");
        check(magnificentSets(3, new int[][] {{1, 2}, {2, 3}, {3, 1}}) == -1, "LC 2493 triangle");
        check(magnificentSets(4, new int[][] {{1, 2}}) == 4, "LC 2493 components add up: 2 + 1 + 1");
        System.out.println("OK");
    }
}
