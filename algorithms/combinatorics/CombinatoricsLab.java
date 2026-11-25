import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Run from this folder with: java CombinatoricsLab.java
public class CombinatoricsLab {

    static final long MOD = 1_000_000_007;

    // Level 1: the product rule. Each item is in or out, so n items have 2^n subsets.
    static List<String> subsets(String items) {
        List<String> result = new ArrayList<>();
        for (int mask = 0; mask < (1 << items.length()); mask++) {
            StringBuilder subset = new StringBuilder();
            for (int i = 0; i < items.length(); i++) if ((mask >> i & 1) == 1) subset.append(items.charAt(i));
            result.add(subset.toString());
        }
        return result;
    }

    // Level 2: ordered choices, n * (n-1) * ... * (n-k+1).
    static long permutations(int n, int k) {
        long ways = 1;
        for (int i = 0; i < k; i++) ways *= n - i;
        return ways;
    }

    // Level 3: unordered choices, built so that every intermediate value is itself a binomial coefficient.
    static long choose(int n, int k) {
        if (k < 0 || k > n) return 0;
        k = Math.min(k, n - k);                             // C(n, k) = C(n, n-k): use the shorter loop
        long ways = 1;
        for (int i = 1; i <= k; i++) ways = ways * (n - k + i) / i; // C(n-k+i-1, i-1) becomes C(n-k+i, i), exactly
        return ways;
    }

    // Level 4: Pascal's rule needs only additions, so it works under any modulus.
    static long[][] pascal(int rows) {                      // c[n][k] = C(n, k) mod 1e9+7
        long[][] c = new long[rows][];
        for (int n = 0; n < rows; n++) {
            c[n] = new long[n + 1];
            c[n][0] = c[n][n] = 1;
            for (int k = 1; k < n; k++) c[n][k] = (c[n - 1][k - 1] + c[n - 1][k]) % MOD; // teams without Ann, plus teams with Ann
        }
        return c;
    }

    // Level 5: stars and bars.
    static long distribute(int items, int groups) {         // identical items, distinct groups, empty groups allowed
        return choose(items + groups - 1, groups - 1);      // place (groups - 1) bars among items + groups - 1 spots
    }

    // Level 6: Catalan numbers.
    static long catalan(int n) {
        return choose(2 * n, n) / (n + 1);
    }

    // Level 7: C(n, k) modulo a prime p, dividing by k! and (n-k)! with Fermat inverses.
    static long chooseMod(int n, int k, long p) {           // p is prime and n < p
        if (k < 0 || k > n) return 0;
        long[] fact = new long[n + 1];
        fact[0] = 1;
        for (int i = 1; i <= n; i++) fact[i] = fact[i - 1] * i % p;
        return fact[n] * power(fact[k], p - 2, p) % p * power(fact[n - k], p - 2, p) % p;
    }

    static long power(long base, long exponent, long mod) { // fast power, from the Math lesson
        long result = 1;
        base %= mod;
        while (exponent > 0) {
            if ((exponent & 1) == 1) result = result * base % mod;
            base = base * base % mod;
            exponent >>= 1;
        }
        return result;
    }

    // Interview 1: LeetCode 62, Unique Paths.
    static int uniquePaths(int m, int n) {
        return (int) choose(m + n - 2, m - 1);              // choose which of the m+n-2 moves go down
    }

    // Interview 2: LeetCode 96, Unique Binary Search Trees.
    static int numTrees(int n) {
        long[] trees = new long[n + 1];                     // trees[i] = number of BST shapes with i keys
        trees[0] = 1;
        for (int i = 1; i <= n; i++)
            for (int root = 1; root <= i; root++) trees[i] += trees[root - 1] * trees[i - root]; // left shapes times right shapes
        return (int) trees[n];
    }

    // Interview 3: LeetCode 60, Permutation Sequence, by the factorial number system.
    static String getPermutation(int n, int k) {
        List<Integer> unused = new ArrayList<>();
        int[] factorial = new int[n];
        factorial[0] = 1;
        for (int i = 1; i < n; i++) factorial[i] = factorial[i - 1] * i;
        for (int i = 1; i <= n; i++) unused.add(i);
        StringBuilder result = new StringBuilder();
        int rank = k - 1;                                   // count from 0
        for (int i = n - 1; i >= 0; i--) {
            result.append(unused.remove(rank / factorial[i])); // each choice of the next digit covers i! permutations
            rank %= factorial[i];
        }
        return result.toString();
    }

    // Interview 4: LeetCode 1569, Number of Ways to Reorder Array to Get Same BST.
    static int numOfWays(int[] nums) {
        List<Integer> order = new ArrayList<>();
        for (int x : nums) order.add(x);
        return (int) ((waysToBuild(order, pascal(nums.length)) - 1 + MOD) % MOD); // every order except the given one
    }

    static long waysToBuild(List<Integer> order, long[][] c) {
        if (order.size() <= 2) return 1;
        List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
        for (int x : order.subList(1, order.size())) {
            if (x < order.get(0)) left.add(x); else right.add(x);
        }
        long interleavings = c[order.size() - 1][left.size()];       // where the left keys sit among the other n-1
        return interleavings * waysToBuild(left, c) % MOD * waysToBuild(right, c) % MOD;
    }

    // Brute-force helpers for the checks.
    static int countDistributions(int items, int groups) {
        if (groups == 1) return 1;
        int ways = 0;
        for (int first = 0; first <= items; first++) ways += countDistributions(items - first, groups - 1);
        return ways;
    }

    static void balanced(String prefix, int open, int close, int n, List<String> out) {
        if (prefix.length() == 2 * n) { out.add(prefix); return; }
        if (open < n) balanced(prefix + "(", open + 1, close, n, out);
        if (close < open) balanced(prefix + ")", open, close + 1, n, out);
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        // Level 1
        check(3 * 2 * 2 == 12 && (int) Math.pow(10, 4) == 10_000, "product rule");
        check(subsets("abc").equals(List.of("", "a", "b", "ab", "c", "ac", "bc", "abc")), "8 subsets of abc");

        // Level 2
        check(permutations(5, 3) == 60 && permutations(4, 4) == 24 && permutations(5, 0) == 1, "permutations");
        check(permutations(20, 20) == 2_432_902_008_176_640_000L && permutations(21, 21) < 0, "21! overflows a long");

        // Level 3
        check(choose(5, 3) == 10 && choose(5, 2) == 10 && 60 / 6 == 10, "teams of 3 from 5");
        check(choose(5, 0) == 1 && choose(5, 6) == 0 && choose(52, 5) == 2_598_960, "choose edges and poker hands");
        check(choose(8, 2) == 28, "3 x 7 grid");
        check(choose(7, 3) == 35 && 5 * 6 / 2 == 15 && 15 * 7 / 3 == 35 && 5 / 2 * 6 / 3 * 7 != 35, "multiply before dividing");
        BigInteger c198 = BigInteger.ONE;
        for (int i = 1; i <= 99; i++) c198 = c198.multiply(BigInteger.valueOf(99 + i)).divide(BigInteger.valueOf(i));
        check(c198.toString().startsWith("2275088") && c198.toString().length() == 59, "C(198, 99) = 2.275 x 10^58");

        // Level 4
        long[][] c = pascal(7);
        check(c[5][2] == 10 && c[4][1] + c[4][2] == 10 && c[4][1] == 4 && c[4][2] == 6, "Pascal's rule");
        check(c[6][3] == 20 && c[6][4] == 15, "row 6");
        long rowSum = 0;
        for (long v : c[5]) rowSum += v;
        check(rowSum == 32, "row 5 sums to 2^5");

        // Level 5
        check(distribute(5, 3) == 21 && choose(7, 2) == 21 && countDistributions(5, 3) == 21, "5 sweets, 3 children");
        check(choose(4, 2) == 6 && distribute(2, 3) == 6, "at least one sweet each");

        // Level 6
        check(catalan(3) == 5 && choose(6, 3) == 20 && choose(6, 4) == 15, "C_3 = 20 / 4 = 20 - 15");
        List<String> brackets = new ArrayList<>();
        balanced("", 0, 0, 3, brackets);
        check(brackets.size() == 5 && brackets.containsAll(List.of("((()))", "(()())", "(())()", "()(())", "()()()")), "5 bracketings");
        check(catalan(0) == 1 && catalan(4) == 14 && catalan(5) == 42, "Catalan sequence");

        // Level 7
        check(chooseMod(5, 2, 7) == 3 && 10 % 7 == 3, "C(5, 2) mod 7");
        check(120 % 7 == 1 && 2 * 4 % 7 == 1 && 6 * 6 % 7 == 1 && 1 * 4 * 6 % 7 == 3, "the hand computation");
        check(chooseMod(1000, 500, MOD) == pascal(1001)[1000][500], "Fermat and Pascal agree");

        // Interview problems
        check(uniquePaths(3, 7) == 28 && uniquePaths(3, 2) == 3 && uniquePaths(1, 1) == 1, "LC 62");
        check(numTrees(3) == 5 && numTrees(1) == 1 && numTrees(19) == 1_767_263_190, "LC 96");
        check(getPermutation(3, 3).equals("213") && getPermutation(4, 9).equals("2314") && getPermutation(3, 1).equals("123"), "LC 60");
        check(getPermutation(4, 24).equals("4321") && getPermutation(1, 1).equals("1"), "LC 60 edges");
        check(numOfWays(new int[] {2, 1, 3}) == 1 && numOfWays(new int[] {3, 4, 5, 1, 2}) == 5 && numOfWays(new int[] {1, 2, 3}) == 0, "LC 1569");

        // LC 1569 by brute force: count orders of {1..5} that give the same tree as [3, 4, 5, 1, 2].
        Set<String> sameTree = new HashSet<>();
        int[] p = {1, 2, 3, 4, 5};
        String target = shape(new int[] {3, 4, 5, 1, 2});
        do { if (shape(p).equals(target)) sameTree.add(Arrays.toString(p)); } while (nextPermutation(p));
        check(sameTree.size() == 6, "6 orders build the same tree, so the answer is 5");
        System.out.println("OK");
    }

    static String shape(int[] order) {                      // insert in order; the child arrays describe the tree
        int[] left = new int[order.length + 1], right = new int[order.length + 1];
        for (int i = 1; i < order.length; i++) {
            int node = order[0];
            while (true) {
                int[] side = order[i] < node ? left : right;
                if (side[node] == 0) { side[node] = order[i]; break; }
                node = side[node];
            }
        }
        return Arrays.toString(left) + Arrays.toString(right);
    }

    static boolean nextPermutation(int[] a) {
        int i = a.length - 2;
        while (i >= 0 && a[i] >= a[i + 1]) i--;
        if (i < 0) return false;
        int j = a.length - 1;
        while (a[j] <= a[i]) j--;
        int t = a[i]; a[i] = a[j]; a[j] = t;
        for (int l = i + 1, r = a.length - 1; l < r; l++, r--) { t = a[l]; a[l] = a[r]; a[r] = t; }
        return true;
    }
}
