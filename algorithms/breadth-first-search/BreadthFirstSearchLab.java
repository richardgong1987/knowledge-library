import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Run from this folder with: java BreadthFirstSearchLab.java
public class BreadthFirstSearchLab {

    // Level 1: BFS distances from one or more sources (Level 4 passes several).
    static int[] distances(List<List<Integer>> graph, int... sources) {
        int[] dist = new int[graph.size()];
        Arrays.fill(dist, -1);                           // -1 = not reached yet
        Deque<Integer> queue = new ArrayDeque<>();
        for (int s : sources) { dist[s] = 0; queue.add(s); }
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : graph.get(u)) {
                if (dist[v] == -1) {                     // first time seen: one step further than u
                    dist[v] = dist[u] + 1;
                    queue.add(v);
                }
            }
        }
        return dist;
    }

    // Level 2: the same search, one whole layer at a time.
    static List<List<Integer>> layers(List<List<Integer>> graph, int source) {
        boolean[] seen = new boolean[graph.size()];
        seen[source] = true;
        List<List<Integer>> layers = new ArrayList<>();
        List<Integer> current = List.of(source);
        while (!current.isEmpty()) {
            layers.add(current);
            List<Integer> next = new ArrayList<>();
            for (int u : current) {
                for (int v : graph.get(u)) {
                    if (!seen[v]) { seen[v] = true; next.add(v); }
                }
            }
            current = next;
        }
        return layers;
    }

    // Level 3: remember who discovered whom, then walk back from the target.
    static List<Integer> shortestPath(List<List<Integer>> graph, int source, int target) {
        int[] parent = new int[graph.size()];
        Arrays.fill(parent, -1);
        parent[source] = source;
        Deque<Integer> queue = new ArrayDeque<>(List.of(source));
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : graph.get(u)) {
                if (parent[v] == -1) { parent[v] = u; queue.add(v); }   // u discovered v
            }
        }
        if (parent[target] == -1) return List.of();
        LinkedList<Integer> path = new LinkedList<>();
        for (int v = target; v != source; v = parent[v]) path.addFirst(v);
        path.addFirst(source);
        return path;
    }

    // Interview 1: LeetCode 994, Rotting Oranges.
    static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    static int orangesRotting(int[][] grid) {
        int rows = grid.length, cols = grid[0].length, fresh = 0;
        Deque<int[]> queue = new ArrayDeque<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 2) queue.add(new int[] {r, c});    // every rotten orange is a source
                if (grid[r][c] == 1) fresh++;
            }
        }
        int minutes = 0;
        while (!queue.isEmpty() && fresh > 0) {
            for (int size = queue.size(); size > 0; size--) {        // one layer = one minute
                int[] cell = queue.poll();
                for (int[] d : DIRECTIONS) {
                    int r = cell[0] + d[0], c = cell[1] + d[1];
                    if (r >= 0 && r < rows && c >= 0 && c < cols && grid[r][c] == 1) {
                        grid[r][c] = 2;
                        fresh--;
                        queue.add(new int[] {r, c});
                    }
                }
            }
            minutes++;
        }
        return fresh == 0 ? minutes : -1;
    }

    // Interview 2: LeetCode 127, Word Ladder.
    static int ladderLength(String begin, String end, List<String> wordList) {
        Set<String> unused = new HashSet<>(wordList);
        if (!unused.contains(end)) return 0;
        Deque<String> queue = new ArrayDeque<>(List.of(begin));
        for (int words = 1; !queue.isEmpty(); words++) {             // words in the ladder so far
            for (int size = queue.size(); size > 0; size--) {
                String word = queue.poll();
                if (word.equals(end)) return words;
                char[] letters = word.toCharArray();
                for (int i = 0; i < letters.length; i++) {
                    char original = letters[i];
                    for (char c = 'a'; c <= 'z'; c++) {
                        letters[i] = c;
                        String next = new String(letters);
                        if (unused.remove(next)) queue.add(next);     // removing it marks it visited
                    }
                    letters[i] = original;
                }
            }
        }
        return 0;
    }

    // Interview 3: LeetCode 815, Bus Routes.
    static int numBusesToDestination(int[][] routes, int source, int target) {
        if (source == target) return 0;
        Map<Integer, List<Integer>> busesAtStop = new HashMap<>();
        for (int bus = 0; bus < routes.length; bus++) {
            for (int stop : routes[bus]) busesAtStop.computeIfAbsent(stop, s -> new ArrayList<>()).add(bus);
        }
        boolean[] boarded = new boolean[routes.length];
        Set<Integer> seenStops = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        for (int bus : busesAtStop.getOrDefault(source, List.of())) { boarded[bus] = true; queue.add(bus); }
        for (int buses = 1; !queue.isEmpty(); buses++) {             // the vertices are buses, not stops
            for (int size = queue.size(); size > 0; size--) {
                int bus = queue.poll();
                for (int stop : routes[bus]) {
                    if (stop == target) return buses;
                    if (!seenStops.add(stop)) continue;               // its buses are already queued
                    for (int next : busesAtStop.get(stop)) {
                        if (!boarded[next]) { boarded[next] = true; queue.add(next); }
                    }
                }
            }
        }
        return -1;
    }

    // Level 2 check: how deep plain DFS goes when it follows the lists in order.
    static void dfsDepth(List<List<Integer>> graph, int v, int depth, int[] depthOf) {
        depthOf[v] = depth;
        for (int next : graph.get(v)) if (depthOf[next] == -1) dfsDepth(graph, next, depth + 1, depthOf);
    }

    static List<List<Integer>> undirected(int n, int[][] edges) {
        List<List<Integer>> g = new ArrayList<>();
        for (int i = 0; i < n; i++) g.add(new ArrayList<>());
        for (int[] e : edges) {
            g.get(e[0]).add(e[1]);
            g.get(e[1]).add(e[0]);
        }
        return g;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        List<List<Integer>> metro = undirected(5, new int[][] {{0, 1}, {1, 2}, {2, 3}, {0, 3}, {3, 4}});
        check(Arrays.equals(distances(metro, 0), new int[] {0, 1, 2, 1, 2}), "BFS distances from station 0");
        check(layers(metro, 0).equals(List.of(List.of(0), List.of(1, 3), List.of(2, 4))), "three layers");
        int[] depthOf = new int[5];
        Arrays.fill(depthOf, -1);
        dfsDepth(metro, 0, 0, depthOf);
        check(depthOf[4] == 4 && depthOf[3] == 3, "DFS reaches station 4 after 4 hops, not 2");
        check(shortestPath(metro, 0, 4).equals(List.of(0, 3, 4)), "the path 0 -> 3 -> 4");
        check(shortestPath(metro, 2, 2).equals(List.of(2)), "a path from a station to itself");
        List<List<Integer>> split = undirected(3, new int[][] {{0, 1}});
        check(shortestPath(split, 0, 2).isEmpty() && distances(split, 0)[2] == -1, "unreachable station");

        List<List<Integer>> street = undirected(6, new int[][] {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}});
        check(Arrays.equals(distances(street, 0, 5), new int[] {0, 1, 2, 2, 1, 0}), "two fire stations");
        check(distances(street, 0)[2] == 2 && distances(street, 5)[2] == 3, "house 2 is 2 from station 0 and 3 from 5");

        check(orangesRotting(new int[][] {{2, 1, 1}, {1, 1, 0}, {0, 1, 1}}) == 4, "LC 994");
        check(orangesRotting(new int[][] {{2, 1, 1}, {0, 1, 1}, {1, 0, 1}}) == -1, "LC 994 an orange out of reach");
        check(orangesRotting(new int[][] {{0, 2}}) == 0, "LC 994 nothing fresh");

        List<String> words = List.of("hot", "dot", "dog", "lot", "log", "cog");
        check(ladderLength("hit", "cog", words) == 5, "LC 127: hit hot dot dog cog");
        check(ladderLength("hit", "cog", List.of("hot", "dot", "dog", "lot", "log")) == 0, "LC 127 no cog");

        check(numBusesToDestination(new int[][] {{1, 2, 7}, {3, 6, 7}}, 1, 6) == 2, "LC 815");
        check(numBusesToDestination(new int[][] {{7, 12}, {4, 5, 15}, {6}, {15, 19}, {9, 12, 13}}, 15, 12) == -1,
                "LC 815 unreachable");
        check(numBusesToDestination(new int[][] {{1, 2}}, 1, 1) == 0, "LC 815 already there");
        System.out.println("OK");
    }
}
