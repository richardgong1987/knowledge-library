import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

// Run from this folder with: java BitmaskLab.java
public class BitmaskLab {

    // Level 1: a set of small numbers is an integer whose bit i says "i is in the set".
    static int maskOf(int... members) {
        int mask = 0;
        for (int i : members) mask |= (1 << i);
        return mask;
    }

    static List<Integer> membersOf(int mask) {
        List<Integer> members = new ArrayList<>();
        for (int i = 0; i < 31; i++) if (has(mask, i)) members.add(i);
        return members;
    }

    // Level 2: one-line set operations.
    static boolean has(int mask, int i) { return ((mask >> i) & 1) == 1; }
    static int add(int mask, int i)     { return mask | (1 << i); }
    static int remove(int mask, int i)  { return mask & ~(1 << i); }
    static int toggle(int mask, int i)  { return mask ^ (1 << i); }
    static int lowestBit(int mask)      { return mask & -mask; }

    // Level 3: every submask of m, from m itself down to 0.
    static List<Integer> submasks(int m) {
        List<Integer> result = new ArrayList<>();
        for (int s = m; ; s = (s - 1) & m) {
            result.add(s);
            if (s == 0) break;                           // 0 is the last submask; (0 - 1) & m would wrap round to m
        }
        return result;
    }

    static long countAllSubmaskPairs(int n) {
        long pairs = 0;
        for (int m = 0; m < 1 << n; m++) pairs += submasks(m).size();
        return pairs;
    }

    // Level 4: Held-Karp. best[mask][v] = shortest path that starts at city 0, visits exactly mask, and ends at v.
    static final int INF = Integer.MAX_VALUE / 2;        // "no such path"; half of MAX_VALUE so INF + distance cannot overflow

    static int[][] heldKarpTable(int[][] dist) {
        int n = dist.length;
        int[][] best = new int[1 << n][n];
        for (int[] row : best) Arrays.fill(row, INF);
        best[1][0] = 0;                                  // only city 0 visited, standing on it
        for (int mask = 1; mask < 1 << n; mask++) {
            for (int v = 0; v < n; v++) {
                int before = remove(mask, v);
                if (!has(mask, v) || before == 0) continue;
                for (int u = 0; u < n; u++) {            // u = the city visited just before v
                    if (has(before, u)) best[mask][v] = Math.min(best[mask][v], best[before][u] + dist[u][v]);
                }
            }
        }
        return best;
    }

    static int shortestTour(int[][] dist) {
        int n = dist.length, full = (1 << n) - 1;
        int[][] best = heldKarpTable(dist);
        int tour = INF;
        for (int v = 1; v < n; v++) tour = Math.min(tour, best[full][v] + dist[v][0]); // return home to city 0
        return tour;
    }

    // Level 5: a row of seats as a mask. No two chosen seats may be neighbours.
    static boolean noTwoAdjacent(int row) {
        return (row & (row >> 1)) == 0;                  // shifting by one lines each seat up with its neighbour
    }

    static int countRowPatterns(int width) {
        int count = 0;
        for (int row = 0; row < 1 << width; row++) if (noTwoAdjacent(row)) count++;
        return count;
    }

    // Interview 1: LeetCode 698, Partition to K Equal Sum Subsets.
    static boolean canPartitionKSubsets(int[] nums, int k) {
        int total = Arrays.stream(nums).sum();
        if (total % k != 0) return false;
        int target = total / k, n = nums.length;
        int[] fill = new int[1 << n];                    // fill[mask] = sum in the bucket being filled; -1 = unreachable
        Arrays.fill(fill, -1);
        fill[0] = 0;
        for (int mask = 0; mask < 1 << n; mask++) {
            if (fill[mask] < 0) continue;
            for (int i = 0; i < n; i++) {
                int next = add(mask, i);
                if (next != mask && fill[next] < 0 && fill[mask] + nums[i] <= target) {
                    fill[next] = (fill[mask] + nums[i]) % target; // a full bucket closes; the next one starts at 0
                }
            }
        }
        return fill[(1 << n) - 1] == 0;
    }

    // Interview 2: LeetCode 847, Shortest Path Visiting All Nodes.
    record State(int node, int visited) {}

    static int shortestPathLength(int[][] graph) {
        int n = graph.length, full = (1 << n) - 1;
        boolean[][] seen = new boolean[n][1 << n];
        Deque<State> queue = new ArrayDeque<>();
        for (int v = 0; v < n; v++) {                    // start everywhere at once
            queue.add(new State(v, 1 << v));
            seen[v][1 << v] = true;
        }
        for (int steps = 0; !queue.isEmpty(); steps++) {
            for (int size = queue.size(); size > 0; size--) {
                State s = queue.poll();
                if (s.visited() == full) return steps;
                for (int w : graph[s.node()]) {
                    int visited = add(s.visited(), w);
                    if (!seen[w][visited]) {
                        seen[w][visited] = true;
                        queue.add(new State(w, visited));
                    }
                }
            }
        }
        throw new IllegalArgumentException("the graph is not connected");
    }

    // Interview 3: LeetCode 1125, Smallest Sufficient Team.
    static int[] smallestSufficientTeam(String[] skills, List<List<String>> people) {
        Map<String, Integer> bit = new HashMap<>();
        for (int i = 0; i < skills.length; i++) bit.put(skills[i], i);
        int full = (1 << skills.length) - 1;
        long[] team = new long[full + 1];                // team[mask] = the people, as bits, of a smallest team covering mask
        int[] size = new int[full + 1];
        Arrays.fill(size, Integer.MAX_VALUE);            // MAX_VALUE = no team found yet
        size[0] = 0;
        for (int p = 0; p < people.size(); p++) {
            int skillsOfP = 0;
            for (String s : people.get(p)) skillsOfP |= (1 << bit.get(s));
            for (int mask = 0; mask <= full; mask++) {
                if (size[mask] == Integer.MAX_VALUE) continue;
                int covered = mask | skillsOfP;
                if (size[mask] + 1 < size[covered]) {    // adding person p gives a smaller team for covered
                    size[covered] = size[mask] + 1;
                    team[covered] = team[mask] | (1L << p);
                }
            }
        }
        return membersOfLong(team[full]);
    }

    static int[] membersOfLong(long people) {
        return IntStream.range(0, 64).filter(p -> ((people >> p) & 1) == 1).toArray();
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        check(maskOf(0, 2, 3) == 13 && Integer.toBinaryString(13).equals("1101"), "{0, 2, 3} is 13 = 1101");
        check(membersOf(13).equals(List.of(0, 2, 3)) && membersOf(0).isEmpty(), "and back again");

        check(!has(13, 1) && has(13, 2), "test bits");
        check(add(13, 1) == 15 && remove(13, 2) == 9 && toggle(13, 0) == 12, "set, clear, toggle");
        check(lowestBit(12) == 4 && lowestBit(13) == 1 && Integer.bitCount(13) == 3, "lowest bit and size");
        check((13 | 6) == 15 && (13 & 6) == 4 && (13 & ~6) == 9, "union, intersection, difference");
        check(-12 == ~12 + 1, "two's complement: -x = ~x + 1");

        check(submasks(13).equals(List.of(13, 12, 9, 8, 5, 4, 1, 0)), "submasks of 1101");
        check(submasks(0).equals(List.of(0)), "the empty set has one submask");
        check(countAllSubmaskPairs(4) == 81 && countAllSubmaskPairs(10) == 59_049, "3^4 and 3^10 (mask, submask) pairs");

        int[][] dist = {
                {0, 1, 4, 3},
                {1, 0, 2, 5},
                {4, 2, 0, 1},
                {3, 5, 1, 0}};
        check(noTwoAdjacent(0b0101) && !noTwoAdjacent(0b0110), "0101 is fine, 0110 has neighbours");
        check(countRowPatterns(4) == 8 && countRowPatterns(8) == 55, "F(w + 2) patterns: 8 for width 4, 55 for width 8");

        check(shortestTour(dist) == 7, "0 -> 1 -> 2 -> 3 -> 0 costs 1 + 2 + 1 + 3 = 7");
        int[][] t = heldKarpTable(dist);
        check(t[0b0111][2] == 3 && t[0b0111][1] == 6, "best[{0,1,2}][2] = 3 and best[{0,1,2}][1] = 6");
        check(t[0b1011][3] == 6 && t[0b1011][1] == 8 && t[0b1101][3] == 5 && t[0b1101][2] == 4, "three-city entries");
        check(t[0b1111][1] == 6 && t[0b1111][2] == 7 && t[0b1111][3] == 4, "full-mask entries");
        check((1L << 16) * 16 * 16 == 16_777_216L, "Held-Karp on 16 cities: 2^16 * 16^2 steps");

        check(canPartitionKSubsets(new int[] {4, 3, 2, 3, 5, 2, 1}, 4), "LC 698 example 1");
        check(!canPartitionKSubsets(new int[] {1, 2, 3, 4}, 3), "LC 698 example 2");
        check(!canPartitionKSubsets(new int[] {2, 2, 2, 2, 3, 4, 5}, 4), "LC 698: the sum 20 divides by 4, but the 4 has no partner");

        check(shortestPathLength(new int[][] {{1, 2, 3}, {0}, {0}, {0}}) == 4, "LC 847 example 1");
        check(shortestPathLength(new int[][] {{1}, {0, 2, 4}, {1, 3, 4}, {2}, {1, 2}}) == 4, "LC 847 example 2");
        check(shortestPathLength(new int[][] {{}}) == 0, "LC 847 single node");

        int[] team = smallestSufficientTeam(new String[] {"java", "nodejs", "reactjs"},
                List.of(List.of("java"), List.of("nodejs"), List.of("nodejs", "reactjs")));
        check(Arrays.equals(team, new int[] {0, 2}), "LC 1125 example 1");
        int[] team2 = smallestSufficientTeam(new String[] {"algorithms", "math", "java", "reactjs", "csharp", "aws"},
                List.of(List.of("algorithms", "math", "java"), List.of("algorithms", "math", "reactjs"),
                        List.of("java", "csharp", "aws"), List.of("reactjs", "csharp"), List.of("csharp", "math"),
                        List.of("aws", "java")));
        check(Arrays.equals(team2, new int[] {1, 2}), "LC 1125 example 2");
        System.out.println("OK");
    }
}
