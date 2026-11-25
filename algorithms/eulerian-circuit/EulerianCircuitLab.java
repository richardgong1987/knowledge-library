import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

// Run from this folder with: java EulerianCircuitLab.java
public class EulerianCircuitLab {

    // Königsberg: A = Kneiphof island, B = north bank, C = south bank, D = east island. Seven bridges.
    static final int[][] KONIGSBERG = {{0, 1}, {0, 1}, {0, 2}, {0, 2}, {0, 3}, {1, 3}, {2, 3}};

    // "The house of Santa Claus": BL=0, BR=1, TL=2, TR=3, roof=4. Eight lines.
    static final int[][] HOUSE = {{0, 1}, {0, 2}, {1, 3}, {2, 3}, {0, 3}, {1, 2}, {2, 4}, {3, 4}};

    // The running directed example: two loops, 0 -> 1 -> 2 -> 0 and 1 -> 3 -> 4 -> 1, sharing vertex 1.
    static final int[][] LOOPS = {{0, 1}, {1, 2}, {2, 0}, {1, 3}, {3, 4}, {4, 1}};

    // Level 2: undirected graphs. Count the vertices with an odd number of edges.
    static int countOddDegrees(int n, int[][] edges) {
        int[] degree = new int[n];
        for (int[] e : edges) { degree[e[0]]++; degree[e[1]]++; }
        int odd = 0;
        for (int d : degree) if (d % 2 == 1) odd++;
        return odd;
    }

    // Level 3: directed graphs. Where must an Euler path start? (Connectivity is assumed.)
    static int eulerStart(int[][] edges) {
        Map<Integer, Integer> balance = new HashMap<>();            // out-degree minus in-degree
        for (int[] e : edges) { balance.merge(e[0], 1, Integer::sum); balance.merge(e[1], -1, Integer::sum); }
        int start = edges[0][0], plusOne = 0, minusOne = 0;
        for (Map.Entry<Integer, Integer> entry : balance.entrySet()) {
            int b = entry.getValue();
            if (b == 1) { plusOne++; start = entry.getKey(); }
            else if (b == -1) minusOne++;
            else if (b != 0) throw new IllegalArgumentException("vertex " + entry.getKey() + " is unbalanced by " + b);
        }
        if (plusOne != minusOne || plusOne > 1) throw new IllegalArgumentException("more than one start or end");
        return start;
    }

    // Level 4: Hierholzer. Walk until stuck; a stuck vertex is final, so it goes on the path.
    static Map<Integer, Deque<Integer>> exitLists(int[][] edges) {
        Map<Integer, Deque<Integer>> unused = new HashMap<>();
        for (int[] e : edges) unused.computeIfAbsent(e[0], v -> new ArrayDeque<>()).add(e[1]);
        return unused;
    }

    static List<Integer> hierholzer(Map<Integer, Deque<Integer>> unused, int start) {
        Deque<Integer> stack = new ArrayDeque<>(List.of(start));
        List<Integer> path = new ArrayList<>();
        while (!stack.isEmpty()) {
            Deque<Integer> exits = unused.get(stack.peek());
            if (exits != null && !exits.isEmpty()) stack.push(exits.poll());   // walk along an unused edge
            else path.add(stack.pop());                                       // stuck: this vertex is final
        }
        Collections.reverse(path);
        return path;
    }

    // Interview 1: LeetCode 332, Reconstruct Itinerary.
    static List<String> findItinerary(List<List<String>> tickets) {
        Map<String, PriorityQueue<String>> unused = new HashMap<>();
        for (List<String> t : tickets) unused.computeIfAbsent(t.get(0), k -> new PriorityQueue<>()).add(t.get(1));
        Deque<String> stack = new ArrayDeque<>(List.of("JFK"));
        LinkedList<String> route = new LinkedList<>();
        while (!stack.isEmpty()) {
            PriorityQueue<String> exits = unused.get(stack.peek());
            if (exits != null && !exits.isEmpty()) stack.push(exits.poll());   // smallest unused ticket first
            else route.addFirst(stack.pop());
        }
        return route;
    }

    // Interview 2: LeetCode 753, Cracking the Safe.
    static String crackSafe(int n, int k) {
        StringBuilder digits = new StringBuilder();
        visit("0".repeat(n - 1), k, new HashSet<>(), digits);
        return digits.append("0".repeat(n - 1)).toString();
    }

    static void visit(String node, int k, Set<String> used, StringBuilder digits) {
        for (int d = 0; d < k; d++) {
            String password = node + d;                     // each edge is one n-digit password
            if (used.add(password)) {
                visit(password.substring(1), k, used, digits);   // the next node keeps the last n - 1 digits
                digits.append(d);                           // record the edge once everything after it is done
            }
        }
    }

    // Interview 3: LeetCode 2097, Valid Arrangement of Pairs.
    static int[][] validArrangement(int[][] pairs) {
        List<Integer> path = hierholzer(exitLists(pairs), eulerStart(pairs));
        int[][] arrangement = new int[pairs.length][];
        for (int i = 0; i < pairs.length; i++) arrangement[i] = new int[] {path.get(i), path.get(i + 1)};
        return arrangement;
    }

    static boolean containsEveryPassword(String s, int n, int k) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i + n <= s.length(); i++) seen.add(s.substring(i, i + n));
        return seen.size() == (int) Math.pow(k, n);
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    // The largest LeetCode case recurses 4,096 calls deep. The JVM's default thread stack differs between
    // machines (it is smaller on Linux than on macOS), so this check runs on a thread with a fixed 64 MB stack.
    static boolean largestSafeIsCracked() {
        boolean[] ok = new boolean[1];
        Thread deep = new Thread(null, () -> ok[0] = crackSafe(4, 8).length() == 4_099
                && containsEveryPassword(crackSafe(4, 8), 4, 8), "lc753-largest", 64L << 20);
        deep.start();
        try {
            deep.join();
        } catch (InterruptedException e) {
            throw new AssertionError("interrupted while checking LC 753", e);
        }
        return ok[0];
    }

    public static void main(String[] args) {
        int[] degree = new int[4];
        for (int[] e : KONIGSBERG) { degree[e[0]]++; degree[e[1]]++; }
        check(Arrays.equals(degree, new int[] {5, 3, 3, 3}), "Königsberg degrees 5, 3, 3, 3");
        check(Arrays.stream(degree).sum() == 2 * 7, "handshake: 5 + 3 + 3 + 3 = 14 = 2 x 7");
        check(countOddDegrees(4, KONIGSBERG) == 4, "four odd land masses: no walk at all");
        check(countOddDegrees(5, HOUSE) == 2 && HOUSE.length == 8, "the house: two odd corners, eight lines");
        check(countOddDegrees(5, LOOPS) == 0, "the two loops, as an undirected graph, have all degrees even");

        check(eulerStart(LOOPS) == 0, "balanced graph: start anywhere with an edge");
        check(eulerStart(new int[][] {{0, 1}, {1, 2}}) == 0, "out - in = +1 marks the start");
        boolean rejected = false;
        try {
            eulerStart(new int[][] {{0, 1}, {0, 2}});
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check(rejected, "vertex 0 has balance +2: no Euler path");

        List<Integer> circuit = hierholzer(exitLists(LOOPS), 0);
        check(circuit.equals(List.of(0, 1, 3, 4, 1, 2, 0)), "Hierholzer splices 1 -> 3 -> 4 -> 1 into 0 -> 1 -> 2 -> 0");
        check(circuit.size() == LOOPS.length + 1, "E + 1 = 7 vertices on the circuit");

        check(findItinerary(List.of(List.of("MUC", "LHR"), List.of("JFK", "MUC"), List.of("SFO", "SJC"), List.of("LHR", "SFO")))
                .equals(List.of("JFK", "MUC", "LHR", "SFO", "SJC")), "LC 332 example 1");
        check(findItinerary(List.of(List.of("JFK", "SFO"), List.of("JFK", "ATL"), List.of("SFO", "ATL"), List.of("ATL", "JFK"),
                List.of("ATL", "SFO"))).equals(List.of("JFK", "ATL", "JFK", "SFO", "ATL", "SFO")), "LC 332 example 2");
        check(findItinerary(List.of(List.of("JFK", "KUL"), List.of("JFK", "NRT"), List.of("NRT", "JFK")))
                .equals(List.of("JFK", "NRT", "JFK", "KUL")), "LC 332: the dead end KUL is spliced last");

        check(crackSafe(1, 2).length() == 2 && containsEveryPassword(crackSafe(1, 2), 1, 2), "LC 753 n=1 k=2");
        check(crackSafe(2, 2).equals("01100"), "LC 753 n=2 k=2 gives 01100");
        check(containsEveryPassword("0001110100", 3, 2), "0001110100 holds all eight 3-bit strings");
        check(7 + 2 == 9, "Chinese postman: Königsberg needs 2 repeated bridges, 9 crossings");
        check(crackSafe(3, 2).length() == 10 && containsEveryPassword(crackSafe(3, 2), 3, 2), "LC 753 n=3 k=2: 2^3 + 2 = 10");
        check(largestSafeIsCracked(), "LC 753 largest case 8^4 = 4096");
        check(crackSafe(1, 2).equals("10"), "LC 753 n=1 k=2 gives 10");

        check(Arrays.deepEquals(validArrangement(new int[][] {{5, 1}, {4, 5}, {11, 9}, {9, 4}}),
                new int[][] {{11, 9}, {9, 4}, {4, 5}, {5, 1}}), "LC 2097 example 1");
        check(Arrays.deepEquals(validArrangement(new int[][] {{1, 3}, {3, 2}, {2, 1}}),
                new int[][] {{1, 3}, {3, 2}, {2, 1}}), "LC 2097 example 2: a circuit");
        check(Arrays.deepEquals(validArrangement(new int[][] {{1, 2}, {1, 3}, {2, 1}}),
                new int[][] {{1, 2}, {2, 1}, {1, 3}}), "LC 2097 example 3");
        System.out.println("OK");
    }
}
