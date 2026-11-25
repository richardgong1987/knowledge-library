import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

// Run from this folder with: java TopologicalSortLab.java
public class TopologicalSortLab {

    // The running example, getting dressed. Each edge {u, v} means "u before v".
    // 0 underwear, 1 trousers, 2 shoes, 3 socks, 4 belt, 5 shirt
    static final int[][] DRESSING = {{0, 1}, {1, 2}, {3, 2}, {1, 4}, {5, 4}};

    static List<List<Integer>> adjacency(int n, int[][] edges) {
        List<List<Integer>> next = new ArrayList<>();
        for (int i = 0; i < n; i++) next.add(new ArrayList<>());
        for (int[] e : edges) next.get(e[0]).add(e[1]);
        return next;
    }

    // Level 1: what "a valid order" means, checked directly.
    static boolean isTopologicalOrder(List<Integer> order, int[][] edges) {
        int[] position = new int[order.size()];
        for (int i = 0; i < order.size(); i++) position[order.get(i)] = i;
        for (int[] e : edges) {
            if (position[e[0]] >= position[e[1]]) return false;
        }
        return true;
    }

    // Level 2: Kahn's algorithm. Repeatedly take a task whose prerequisites are all done.
    static List<Integer> kahn(int n, int[][] edges) {
        List<List<Integer>> next = adjacency(n, edges);
        int[] indegree = new int[n];
        for (int[] e : edges) indegree[e[1]]++;
        Deque<Integer> ready = new ArrayDeque<>();
        for (int v = 0; v < n; v++) if (indegree[v] == 0) ready.add(v);
        List<Integer> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            int u = ready.poll();
            order.add(u);
            for (int v : next.get(u)) {
                if (--indegree[v] == 0) ready.add(v);       // v's last prerequisite is done
            }
        }
        return order;                                       // shorter than n exactly when there is a cycle
    }

    // Level 3: depth-first search. A task finishes only after everything that depends on it.
    static List<Integer> dfsOrder(int n, int[][] edges) {
        List<List<Integer>> next = adjacency(n, edges);
        int[] state = new int[n];                           // 0 = new, 1 = on the current path, 2 = finished
        Deque<Integer> finished = new ArrayDeque<>();
        for (int v = 0; v < n; v++) if (state[v] == 0) visit(v, next, state, finished);
        return new ArrayList<>(finished);
    }

    static void visit(int u, List<List<Integer>> next, int[] state, Deque<Integer> finished) {
        state[u] = 1;
        for (int v : next.get(u)) {
            if (state[v] == 1) throw new IllegalStateException("cycle through " + u + " -> " + v);  // back edge
            if (state[v] == 0) visit(v, next, state, finished);
        }
        state[u] = 2;
        finished.push(u);                                   // push to the front: reverse finishing order
    }

    // Level 5: how many valid orders are there? ways[S] counts the orders of the set S done first.
    static long countOrders(int n, int[][] edges) {
        int[] needs = new int[n];                           // bitmask of v's prerequisites
        for (int[] e : edges) needs[e[1]] |= 1 << e[0];
        long[] ways = new long[1 << n];
        ways[0] = 1;
        for (int done = 0; done < (1 << n); done++) {
            for (int v = 0; v < n; v++) {
                boolean canGoNext = (done & (1 << v)) == 0 && (needs[v] & done) == needs[v];
                if (canGoNext) ways[done | (1 << v)] += ways[done];
            }
        }
        return ways[(1 << n) - 1];
    }

    // Level 5: the fewest parallel rounds is the number of tasks on the longest chain.
    static int rounds(int n, int[][] edges) {
        List<List<Integer>> next = adjacency(n, edges);
        int[] level = new int[n];
        Arrays.fill(level, 1);
        for (int u : kahn(n, edges)) {
            for (int v : next.get(u)) level[v] = Math.max(level[v], level[u] + 1);   // u is final before v is read
        }
        return Arrays.stream(level).max().getAsInt();
    }

    // Interview 1: LeetCode 207, Course Schedule.
    static int[][] prerequisiteEdges(int[][] prerequisites) {   // [a, b] means "b before a", so the edge is b -> a
        int[][] edges = new int[prerequisites.length][];
        for (int i = 0; i < prerequisites.length; i++) edges[i] = new int[] {prerequisites[i][1], prerequisites[i][0]};
        return edges;
    }

    static boolean canFinish(int numCourses, int[][] prerequisites) {
        return kahn(numCourses, prerequisiteEdges(prerequisites)).size() == numCourses;
    }

    // Interview 2: LeetCode 210, Course Schedule II.
    static int[] findOrder(int numCourses, int[][] prerequisites) {
        List<Integer> order = kahn(numCourses, prerequisiteEdges(prerequisites));
        if (order.size() < numCourses) return new int[0];
        return order.stream().mapToInt(Integer::intValue).toArray();
    }

    // Interview 3: LeetCode 1203, Sort Items by Groups Respecting Dependencies.
    static int[] sortItems(int n, int m, int[] group, List<List<Integer>> beforeItems) {
        int[] groupOf = group.clone();
        int groups = m;
        for (int i = 0; i < n; i++) if (groupOf[i] == -1) groupOf[i] = groups++;   // a loner is a group of its own
        List<int[]> itemEdges = new ArrayList<>(), groupEdges = new ArrayList<>();
        for (int item = 0; item < n; item++) {
            for (int before : beforeItems.get(item)) {
                itemEdges.add(new int[] {before, item});
                if (groupOf[before] != groupOf[item]) groupEdges.add(new int[] {groupOf[before], groupOf[item]});
            }
        }
        List<Integer> itemOrder = kahn(n, itemEdges.toArray(new int[0][]));
        List<Integer> groupOrder = kahn(groups, groupEdges.toArray(new int[0][]));
        if (itemOrder.size() < n || groupOrder.size() < groups) return new int[0];
        List<List<Integer>> members = new ArrayList<>();
        for (int g = 0; g < groups; g++) members.add(new ArrayList<>());
        for (int item : itemOrder) members.get(groupOf[item]).add(item);         // keeps the item order inside a group
        return groupOrder.stream().flatMap(g -> members.get(g).stream()).mapToInt(Integer::intValue).toArray();
    }

    // Interview 4: LeetCode 2392, Build a Matrix With Conditions.
    static int[][] buildMatrix(int k, int[][] rowConditions, int[][] colConditions) {
        List<Integer> rows = kahn(k, shiftToZero(rowConditions)), cols = kahn(k, shiftToZero(colConditions));
        if (rows.size() < k || cols.size() < k) return new int[0][0];
        int[] colOf = new int[k];
        for (int i = 0; i < k; i++) colOf[cols.get(i)] = i;
        int[][] matrix = new int[k][k];
        for (int i = 0; i < k; i++) matrix[i][colOf[rows.get(i)]] = rows.get(i) + 1;   // number rows[i] + 1 goes in row i
        return matrix;
    }

    static int[][] shiftToZero(int[][] conditions) {         // numbers 1..k become vertices 0..k-1
        int[][] edges = new int[conditions.length][];
        for (int i = 0; i < conditions.length; i++) edges[i] = new int[] {conditions[i][0] - 1, conditions[i][1] - 1};
        return edges;
    }

    static boolean groupsAreContiguousAndOrdered(int[] items, int[] group, List<List<Integer>> beforeItems) {
        List<Integer> order = new ArrayList<>();
        for (int item : items) order.add(item);
        for (int item = 0; item < items.length; item++) {
            for (int before : beforeItems.get(item)) if (order.indexOf(before) > order.indexOf(item)) return false;
        }
        for (int i = 0; i < items.length; i++) {
            for (int j = i + 2; j < items.length; j++) {
                boolean sameGroup = group[items[i]] != -1 && group[items[i]] == group[items[j]];
                if (sameGroup && group[items[j - 1]] != group[items[i]]) return false;
            }
        }
        return true;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        check(isTopologicalOrder(List.of(3, 0, 1, 2, 5, 4), DRESSING), "socks, underwear, trousers, shoes, shirt, belt works");
        check(!isTopologicalOrder(List.of(0, 2, 1, 3, 4, 5), DRESSING), "shoes before trousers and socks fails");

        List<Integer> kahnOrder = kahn(6, DRESSING);
        check(kahnOrder.equals(List.of(0, 3, 5, 1, 2, 4)), "Kahn: underwear, socks, shirt, trousers, shoes, belt");
        check(isTopologicalOrder(kahnOrder, DRESSING), "Kahn's order is valid");

        List<Integer> dfs = dfsOrder(6, DRESSING);
        check(dfs.equals(List.of(5, 3, 0, 1, 4, 2)), "reverse finishing order: shirt, socks, underwear, trousers, belt, shoes");
        check(isTopologicalOrder(dfs, DRESSING), "the DFS order is valid");

        int[][] withCycle = Arrays.copyOf(DRESSING, DRESSING.length + 1);
        withCycle[DRESSING.length] = new int[] {2, 0};      // shoes before underwear: 0 -> 1 -> 2 -> 0
        check(kahn(6, withCycle).equals(List.of(3, 5)), "Kahn stops after 2 of 6 tasks");
        boolean cycleFound = false;
        try {
            dfsOrder(6, withCycle);
        } catch (IllegalStateException e) {
            cycleFound = e.getMessage().equals("cycle through 2 -> 0");
        }
        check(cycleFound, "DFS meets the back edge 2 -> 0");

        check(countOrders(3, new int[][] {{0, 2}, {1, 2}}) == 2, "a and b before c: 2 orders");
        check(countOrders(3, new int[][] {}) == 6, "no rules: 3! = 6 orders");
        check(countOrders(5, new int[][] {{0, 1}, {1, 2}, {3, 4}}) == 10, "two chains of 3 and 2: 5!/(3!2!) = 10");
        check(countOrders(6, DRESSING) == 30, "the dressing DAG has 30 valid orders out of 720");
        check(rounds(6, DRESSING) == 3, "three parallel rounds");

        check(canFinish(2, new int[][] {{1, 0}}), "LC 207 example 1");
        check(!canFinish(2, new int[][] {{1, 0}, {0, 1}}), "LC 207 example 2");
        check(Arrays.equals(findOrder(4, new int[][] {{1, 0}, {2, 0}, {3, 1}, {3, 2}}), new int[] {0, 1, 2, 3}), "LC 210");
        check(findOrder(2, new int[][] {{1, 0}, {0, 1}}).length == 0, "LC 210 cycle");

        int[] group = {-1, -1, 1, 0, 0, 1, 0, -1};
        List<List<Integer>> before = List.of(List.of(), List.of(6), List.of(5), List.of(6), List.of(3, 6), List.of(), List.of(), List.of());
        int[] items = sortItems(8, 2, group, before);
        check(Arrays.equals(items, new int[] {6, 3, 4, 5, 2, 0, 7, 1}), "LC 1203 example 1");
        check(groupsAreContiguousAndOrdered(items, group, before), "LC 1203 answer is valid");
        List<List<Integer>> cyclic = List.of(List.of(), List.of(6), List.of(5), List.of(6), List.of(3), List.of(), List.of(4), List.of());
        check(sortItems(8, 2, group, cyclic).length == 0, "LC 1203 example 2 has a cycle 3 -> 4 -> 6 -> 3");

        int[][] matrix = buildMatrix(3, new int[][] {{1, 2}, {3, 2}}, new int[][] {{2, 1}, {3, 2}});
        check(Arrays.deepEquals(matrix, new int[][] {{0, 0, 1}, {3, 0, 0}, {0, 2, 0}}), "LC 2392 example 1");
        check(buildMatrix(3, new int[][] {{1, 2}, {2, 3}, {3, 1}, {2, 3}}, new int[][] {{2, 1}}).length == 0, "LC 2392 example 2");
        System.out.println("OK");
    }
}
