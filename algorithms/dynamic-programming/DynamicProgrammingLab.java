import java.util.Arrays;

// Run from this folder with: java DynamicProgrammingLab.java
public class DynamicProgrammingLab {

    // Level 1: climbing stairs one or two steps at a time.
    static long climbWays(int n) {
        long[] ways = new long[n + 1];
        ways[0] = 1;                                     // one way to climb nothing: do nothing
        for (int i = 1; i <= n; i++) {
            ways[i] = ways[i - 1] + (i >= 2 ? ways[i - 2] : 0);
        }
        return ways[n];
    }

    // Level 2: house robber. No two neighbouring houses.
    static int rob(int[] money) {
        int[] best = new int[money.length + 1];          // best[i] = most cash from the first i houses
        if (money.length > 0) best[1] = money[0];
        for (int i = 2; i <= money.length; i++) {
            best[i] = Math.max(best[i - 1], best[i - 2] + money[i - 1]); // skip house i, or rob it
        }
        return best[money.length];
    }

    // Level 3a: paths through a grid that move only right or down.
    static int gridPaths(boolean[][] blocked) {
        int rows = blocked.length, cols = blocked[0].length;
        int[][] paths = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (blocked[r][c]) continue;             // a rock: zero paths end here
                if (r == 0 && c == 0) paths[r][c] = 1;
                else paths[r][c] = (r > 0 ? paths[r - 1][c] : 0) + (c > 0 ? paths[r][c - 1] : 0);
            }
        }
        return paths[rows - 1][cols - 1];
    }

    // Level 3b: longest common subsequence (LeetCode 1143).
    static int[][] commonTable(String a, String b) {
        int[][] common = new int[a.length() + 1][b.length() + 1]; // common[i][j] = LCS of a[0..i) and b[0..j)
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) common[i][j] = common[i - 1][j - 1] + 1;
                else common[i][j] = Math.max(common[i - 1][j], common[i][j - 1]);
            }
        }
        return common;
    }

    // Level 4: 0/1 knapsack. Each item is taken whole or left behind.
    static int[][] knapsackTable(int[] weight, int[] value, int capacity) {
        int n = weight.length;
        int[][] best = new int[n + 1][capacity + 1];     // best[i][c] = top value from the first i items within weight c
        for (int i = 1; i <= n; i++) {
            for (int c = 0; c <= capacity; c++) {
                best[i][c] = best[i - 1][c];             // leave item i
                if (weight[i - 1] <= c) {
                    best[i][c] = Math.max(best[i][c], best[i - 1][c - weight[i - 1]] + value[i - 1]); // take item i
                }
            }
        }
        return best;
    }

    // Level 5: longest increasing subsequence (LeetCode 300), first in O(n^2).
    static int[] endsHereTable(int[] a) {
        int[] endsHere = new int[a.length];              // endsHere[i] = longest increasing run ending at a[i]
        for (int i = 0; i < a.length; i++) {
            endsHere[i] = 1;
            for (int j = 0; j < i; j++) {
                if (a[j] < a[i]) endsHere[i] = Math.max(endsHere[i], endsHere[j] + 1);
            }
        }
        return endsHere;
    }

    static int lisQuadratic(int[] a) {
        return Arrays.stream(endsHereTable(a)).max().orElse(0); // the best run ends somewhere
    }

    // Level 5: the same answer in O(n log n).
    static int lisFast(int[] a) {
        int[] tails = new int[a.length];                 // tails[k] = smallest last value of an increasing run of length k + 1
        int size = 0;
        for (int x : a) {
            int lo = 0, hi = size;                       // binary search for the first tail >= x
            while (lo < hi) {
                int mid = (lo + hi) / 2;
                if (tails[mid] < x) lo = mid + 1;
                else hi = mid;
            }
            tails[lo] = x;                               // x extends a run, or gives a run a smaller tail
            if (lo == size) size++;
        }
        return size;
    }

    // Interview 1: LeetCode 322, Coin Change.
    static int coinChange(int[] coins, int amount) {
        int[] fewest = new int[amount + 1];              // fewest[a] = fewest coins that make a
        Arrays.fill(fewest, amount + 1);                 // amount + 1 means "impossible": no answer needs that many
        fewest[0] = 0;
        for (int a = 1; a <= amount; a++) {
            for (int c : coins) {
                if (c <= a) fewest[a] = Math.min(fewest[a], fewest[a - c] + 1);
            }
        }
        return fewest[amount] > amount ? -1 : fewest[amount];
    }

    // Interview 2: LeetCode 72, Edit Distance.
    static int[][] editTable(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1]; // d[i][j] = edits that turn a[0..i) into b[0..j)
        for (int i = 0; i <= a.length(); i++) d[i][0] = i;   // delete all i letters
        for (int j = 0; j <= b.length(); j++) d[0][j] = j;   // insert all j letters
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) d[i][j] = d[i - 1][j - 1];
                else d[i][j] = 1 + Math.min(d[i - 1][j - 1], Math.min(d[i - 1][j], d[i][j - 1])); // replace, delete, insert
            }
        }
        return d;
    }

    static int minDistance(String a, String b) {
        return editTable(a, b)[a.length()][b.length()];
    }

    // Interview 3: LeetCode 312, Burst Balloons.
    static int maxCoins(int[] nums) {
        int n = nums.length;
        int[] p = new int[n + 2];                        // the balloons with a 1 added at each end
        p[0] = p[n + 1] = 1;
        for (int i = 0; i < n; i++) p[i + 1] = nums[i];
        int[][] best = new int[n + 2][n + 2];            // best[i][j] = most coins from bursting everything strictly between i and j
        for (int gap = 2; gap <= n + 1; gap++) {
            for (int i = 0; i + gap <= n + 1; i++) {
                int j = i + gap;
                for (int k = i + 1; k < j; k++) {        // k is the LAST balloon to burst between i and j
                    best[i][j] = Math.max(best[i][j], best[i][k] + p[i] * p[k] * p[j] + best[k][j]);
                }
            }
        }
        return best[0][n + 1];
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        check(climbWays(1) == 1 && climbWays(2) == 2 && climbWays(3) == 3 && climbWays(4) == 5 && climbWays(5) == 8,
                "stairs 1..5 give 1, 2, 3, 5, 8 ways");
        check(climbWays(45) == 1_836_311_903L, "stairs 45 (LeetCode's largest n)");

        check(rob(new int[] {2, 7, 9, 3, 1}) == 12, "house robber: 2 + 9 + 1 = 12");
        check(rob(new int[] {4, 5, 4}) == 8, "house robber beats greedy on 4, 5, 4");
        check(rob(new int[] {}) == 0 && rob(new int[] {5}) == 5, "house robber edge cases");

        check(gridPaths(new boolean[3][3]) == 6, "3x3 open grid has 6 paths");
        boolean[][] rock = new boolean[3][3];
        rock[1][1] = true;
        check(gridPaths(rock) == 2, "a rock in the middle leaves 2 paths");

        int[][] common = commonTable("abcde", "ace");
        check(common[5][3] == 3, "LCS of abcde and ace is 3");
        check(Arrays.equals(common[3], new int[] {0, 1, 2, 2}), "LCS row for abc");
        check(commonTable("abc", "def")[3][3] == 0, "no common letters");

        int[][] knap = knapsackTable(new int[] {1, 3, 4}, new int[] {1, 4, 5}, 5);
        check(Arrays.equals(knap[1], new int[] {0, 1, 1, 1, 1, 1}), "knapsack row A");
        check(Arrays.equals(knap[2], new int[] {0, 1, 1, 4, 5, 5}), "knapsack row B");
        check(Arrays.equals(knap[3], new int[] {0, 1, 1, 4, 5, 6}), "knapsack row C");

        int[] digits = {3, 1, 4, 1, 5, 9, 2, 6};
        check(Arrays.equals(endsHereTable(digits), new int[] {1, 1, 2, 1, 3, 4, 2, 4}), "E values");
        check(lisQuadratic(digits) == 4 && lisFast(digits) == 4, "LIS of 3 1 4 1 5 9 2 6 is 4");
        check(lisFast(new int[] {10, 9, 2, 5, 3, 7, 101, 18}) == 4, "LC 300 example");
        check(lisFast(new int[] {7, 7, 7}) == 1 && lisFast(new int[] {}) == 0, "LIS is strictly increasing");

        check(coinChange(new int[] {1, 2, 5}, 11) == 3, "LC 322: 5 + 5 + 1");
        check(coinChange(new int[] {2}, 3) == -1 && coinChange(new int[] {1}, 0) == 0, "LC 322 edge cases");
        check(coinChange(new int[] {1, 3, 4}, 6) == 2, "coins 1, 3, 4: 3 + 3");

        int[][] horse = editTable("horse", "ros");
        check(Arrays.equals(horse[0], new int[] {0, 1, 2, 3}), "edit row empty");
        check(Arrays.equals(horse[1], new int[] {1, 1, 2, 3}), "edit row h");
        check(Arrays.equals(horse[2], new int[] {2, 2, 1, 2}), "edit row o");
        check(Arrays.equals(horse[3], new int[] {3, 2, 2, 2}), "edit row r");
        check(Arrays.equals(horse[4], new int[] {4, 3, 3, 2}), "edit row s");
        check(Arrays.equals(horse[5], new int[] {5, 4, 4, 3}), "edit row e");
        check(minDistance("horse", "ros") == 3, "LC 72: horse -> ros");
        check(minDistance("intention", "execution") == 5 && minDistance("", "abc") == 3, "LC 72 more cases");

        check(maxCoins(new int[] {3, 1, 5, 8}) == 167, "LC 312: 15 + 120 + 24 + 8");
        check(maxCoins(new int[] {1, 5}) == 10, "LC 312 two balloons");
        System.out.println("OK");
    }
}
