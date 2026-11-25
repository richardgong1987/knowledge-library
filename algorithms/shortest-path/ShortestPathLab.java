import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;

// Run from this folder with: java ShortestPathLab.java
public class ShortestPathLab {

    static final int INF = Integer.MAX_VALUE / 2;          // "no path yet"; INF + INF still fits in an int

    // The running example: A=0, B=1, C=2, D=3, E=4; each edge is {from, to, weight}.
    static final int[][] ROADS = {
        {0, 1, 4}, {0, 2, 1}, {2, 1, 2}, {1, 3, 1}, {2, 3, 5}, {3, 4, 3}, {2, 4, 8},
    };

    static List<List<int[]>> adjacency(int n, int[][] edges) {
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) adj.get(e[0]).add(new int[] {e[1], e[2]});
        return adj;
    }

    // Level 2: every edge costs 1, so a plain queue visits vertices in order of distance.
    static int[] bfs(List<List<int[]>> adj, int source) {
        int[] dist = new int[adj.size()];
        Arrays.fill(dist, INF);
        dist[source] = 0;
        Deque<Integer> queue = new ArrayDeque<>(List.of(source));
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int[] edge : adj.get(u)) {
                int v = edge[0];
                if (dist[v] == INF) { dist[v] = dist[u] + 1; queue.add(v); }   // first visit uses the fewest edges
            }
        }
        return dist;
    }

    // Level 3: Dijkstra. Always settle the closest unsettled vertex next.
    static int[] dijkstra(List<List<int[]>> adj, int source) {
        int[] dist = new int[adj.size()];
        Arrays.fill(dist, INF);
        dist[source] = 0;
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(entry -> entry[1]));
        heap.add(new int[] {source, 0});
        while (!heap.isEmpty()) {
            int[] top = heap.poll();
            int u = top[0];
            if (top[1] > dist[u]) continue;                  // stale entry: u was already settled more cheaply
            for (int[] edge : adj.get(u)) {
                int v = edge[0], w = edge[1];
                if (dist[u] + w < dist[v]) {                 // relax (u, v)
                    dist[v] = dist[u] + w;
                    heap.add(new int[] {v, dist[v]});
                }
            }
        }
        return dist;
    }

    // Level 4: Bellman-Ford. One round relaxes every edge once, reading only last round's distances.
    static int[] relaxAllEdges(int[] dist, int[][] edges) {
        int[] next = dist.clone();
        for (int[] e : edges) {
            if (dist[e[0]] != INF && dist[e[0]] + e[2] < next[e[1]]) next[e[1]] = dist[e[0]] + e[2];
        }
        return next;
    }

    static int[] bellmanFord(int n, int[][] edges, int source) {
        int[] dist = new int[n];
        Arrays.fill(dist, INF);
        dist[source] = 0;
        for (int round = 1; round <= n - 1; round++) dist = relaxAllEdges(dist, edges);
        if (!Arrays.equals(relaxAllEdges(dist, edges), dist)) {
            throw new IllegalStateException("negative cycle reachable from the source"); // round V still improves
        }
        return dist;
    }

    // Level 5: 0-1 BFS. A free edge keeps its vertex at the front of the line.
    static int[] zeroOneBfs(List<List<int[]>> adj, int source) {
        int[] dist = new int[adj.size()];
        Arrays.fill(dist, INF);
        dist[source] = 0;
        Deque<Integer> deque = new ArrayDeque<>(List.of(source));
        while (!deque.isEmpty()) {
            int u = deque.pollFirst();
            for (int[] edge : adj.get(u)) {
                int v = edge[0], w = edge[1];
                if (dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    if (w == 0) deque.addFirst(v); else deque.addLast(v);
                }
            }
        }
        return dist;
    }

    // Level 6: Floyd-Warshall. After round k, paths may stop over at vertices 0..k.
    static int[][] floydWarshall(int n, int[][] edges) {
        int[][] dist = new int[n][n];
        for (int[] row : dist) Arrays.fill(row, INF);
        for (int i = 0; i < n; i++) dist[i][i] = 0;
        for (int[] e : edges) dist[e[0]][e[1]] = Math.min(dist[e[0]][e[1]], e[2]);
        for (int k = 0; k < n; k++)
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    if (dist[i][k] + dist[k][j] < dist[i][j]) dist[i][j] = dist[i][k] + dist[k][j];
        return dist;
    }

    // Interview 1: LeetCode 743, Network Delay Time.
    static int networkDelayTime(int[][] times, int n, int k) {
        int[][] edges = new int[times.length][];
        for (int i = 0; i < times.length; i++) edges[i] = new int[] {times[i][0] - 1, times[i][1] - 1, times[i][2]};
        int[] dist = dijkstra(adjacency(n, edges), k - 1);
        int latest = Arrays.stream(dist).max().getAsInt();
        return latest == INF ? -1 : latest;
    }

    // Interview 2: LeetCode 787, Cheapest Flights Within K Stops.
    static int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
        int[] dist = new int[n];
        Arrays.fill(dist, INF);
        dist[src] = 0;
        for (int round = 1; round <= k + 1; round++) dist = relaxAllEdges(dist, flights);  // k stops = k + 1 flights
        return dist[dst] == INF ? -1 : dist[dst];
    }

    // Interview 3: LeetCode 1368, Minimum Cost to Make at Least One Valid Path in a Grid.
    static int minCost(int[][] grid) {
        int rows = grid.length, cols = grid[0].length;
        int[][] moves = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};          // signs 1, 2, 3, 4: right, left, down, up
        int[][] dist = new int[rows][cols];
        for (int[] row : dist) Arrays.fill(row, INF);
        dist[0][0] = 0;
        Deque<int[]> deque = new ArrayDeque<>(List.of(new int[] {0, 0}));
        while (!deque.isEmpty()) {
            int[] cell = deque.pollFirst();
            int r = cell[0], c = cell[1];
            for (int sign = 1; sign <= 4; sign++) {
                int nr = r + moves[sign - 1][0], nc = c + moves[sign - 1][1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
                int cost = grid[r][c] == sign ? 0 : 1;               // following the arrow is free
                if (dist[r][c] + cost < dist[nr][nc]) {
                    dist[nr][nc] = dist[r][c] + cost;
                    if (cost == 0) deque.addFirst(new int[] {nr, nc}); else deque.addLast(new int[] {nr, nc});
                }
            }
        }
        return dist[rows - 1][cols - 1];
    }

    // Interview 4: LeetCode 882, Reachable Nodes In Subdivided Graph.
    static int reachableNodes(int[][] edges, int maxMoves, int n) {
        int[][] arcs = new int[2 * edges.length][];
        for (int i = 0; i < edges.length; i++) {
            int[] e = edges[i];
            arcs[2 * i] = new int[] {e[0], e[1], e[2] + 1};         // cnt new nodes means cnt + 1 steps
            arcs[2 * i + 1] = new int[] {e[1], e[0], e[2] + 1};
        }
        int[] dist = dijkstra(adjacency(n, arcs), 0);
        int reached = 0;
        for (int d : dist) if (d <= maxMoves) reached++;
        for (int[] e : edges) {
            int fromU = Math.max(0, maxMoves - dist[e[0]]), fromV = Math.max(0, maxMoves - dist[e[1]]);
            reached += Math.min(e[2], fromU + fromV);
        }
        return reached;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        List<List<int[]>> roads = adjacency(5, ROADS);

        // The problem: the four routes from A to E.
        check(4 + 1 + 3 == 8 && 1 + 2 + 1 + 3 == 7 && 1 + 5 + 3 == 9 && 1 + 8 == 9, "route totals 8, 7, 9, 9");

        check(Arrays.equals(bfs(roads, 0), new int[] {0, 1, 1, 2, 2}), "BFS counts edges: A0 B1 C1 D2 E2");
        check(Arrays.equals(dijkstra(roads, 0), new int[] {0, 3, 1, 4, 7}), "Dijkstra: A0 B3 C1 D4 E7");

        int[] round = {0, INF, INF, INF, INF};
        int[][] expected = {{0, 4, 1, INF, INF}, {0, 3, 1, 5, 9}, {0, 3, 1, 4, 8}, {0, 3, 1, 4, 7}};
        for (int k = 0; k < 4; k++) {
            round = relaxAllEdges(round, ROADS);
            check(Arrays.equals(round, expected[k]), "Bellman-Ford round " + (k + 1));
        }
        check(Arrays.equals(bellmanFord(5, ROADS, 0), new int[] {0, 3, 1, 4, 7}), "Bellman-Ford after V - 1 = 4 rounds");
        int[][] withNegativeCycle = Arrays.copyOf(ROADS, ROADS.length + 1);
        withNegativeCycle[ROADS.length] = new int[] {4, 1, -6};       // B -> D -> E -> B costs 1 + 3 - 6 = -2
        boolean detected = false;
        try {
            bellmanFord(5, withNegativeCycle, 0);
        } catch (IllegalStateException e) {
            detected = true;
        }
        check(detected, "Bellman-Ford reports the negative cycle B-D-E-B");

        int[][] tolls = {{0, 1, 1}, {0, 2, 0}, {2, 1, 0}, {1, 3, 1}, {2, 3, 1}, {3, 4, 0}, {2, 4, 1}};
        check(Arrays.equals(zeroOneBfs(adjacency(5, tolls), 0), new int[] {0, 0, 0, 1, 1}), "0-1 BFS on the toll roads");
        check(Arrays.equals(dijkstra(adjacency(5, tolls), 0), new int[] {0, 0, 0, 1, 1}), "0-1 BFS agrees with Dijkstra");

        int[][] all = floydWarshall(5, ROADS);
        check(Arrays.equals(all[0], new int[] {0, 3, 1, 4, 7}), "Floyd-Warshall row A");
        check(Arrays.equals(all[2], new int[] {INF, 2, 0, 3, 6}), "Floyd-Warshall row C: C to E drops from 8 to 6");
        check(Math.min(4, 1 + 2) == 3 && 5 * 5 * 5 == 125, "k = C improves A to B; V^3 = 125 updates");

        check(networkDelayTime(new int[][] {{2, 1, 1}, {2, 3, 1}, {3, 4, 1}}, 4, 2) == 2, "LC 743");
        check(networkDelayTime(new int[][] {{1, 2, 1}}, 2, 2) == -1, "LC 743 unreachable");
        int[][] flights = {{0, 1, 100}, {1, 2, 100}, {2, 0, 100}, {1, 3, 600}, {2, 3, 200}};
        check(findCheapestPrice(4, flights, 0, 3, 1) == 700, "LC 787 with one stop");
        check(findCheapestPrice(4, flights, 0, 3, 2) == 400, "LC 787 with two stops");
        check(findCheapestPrice(4, flights, 0, 3, 0) == -1, "LC 787 no direct flight");
        check(minCost(new int[][] {{1, 1, 1, 1}, {2, 2, 2, 2}, {1, 1, 1, 1}, {2, 2, 2, 2}}) == 3, "LC 1368 example 1");
        check(minCost(new int[][] {{1, 2}, {4, 3}}) == 1, "LC 1368 example 3");
        check(minCost(new int[][] {{4}}) == 0, "LC 1368 single cell");
        check(reachableNodes(new int[][] {{0, 1, 10}, {0, 2, 1}, {1, 2, 2}}, 6, 3) == 13, "LC 882 example 1");
        check(reachableNodes(new int[][] {{0, 1, 4}, {1, 2, 6}, {0, 2, 8}, {1, 3, 1}}, 10, 4) == 23, "LC 882 example 2");
        check(reachableNodes(new int[][] {{1, 2, 4}, {1, 4, 5}, {1, 3, 1}, {2, 3, 4}, {3, 4, 5}}, 17, 5) == 1, "LC 882 cut off");
        System.out.println("OK");
    }
}
