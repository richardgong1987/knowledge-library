import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

// Run from this folder with: java DivideAndConquerLab.java
public class DivideAndConquerLab {

    // Level 1: split, solve both halves, combine with one comparison.
    static int maxOf(int[] a, int lo, int hi) {          // largest value in a[lo..hi]
        if (lo == hi) return a[lo];                       // one element: nothing to split
        int mid = (lo + hi) / 2;
        return Math.max(maxOf(a, lo, mid), maxOf(a, mid + 1, hi));
    }

    // Level 2: evaluate the merge sort recurrence directly.
    static long mergeSortWork(long n) {                   // T(n) = 2 T(n/2) + n, T(1) = 0
        return n <= 1 ? 0 : 2 * mergeSortWork(n / 2) + n;
    }

    // Level 3: the exponent the master theorem compares f(n) against.
    static double criticalExponent(int a, int b) {        // the exponent in n^(log_b a)
        return Math.log(a) / Math.log(b);
    }

    // Level 4: digit products used when each multiplication makes `a` half-size multiplications.
    static long digitProducts(long n, int a) {            // T(n) = a T(n/2), T(1) = 1
        return n <= 1 ? 1 : a * digitProducts(n / 2, a);
    }

    // Level 4: Karatsuba multiplication, three half-size products instead of four.
    static long karatsuba(long x, long y) {
        if (x < 10 || y < 10) return x * y;                // one digit: multiply directly
        int m = Math.max(Long.toString(x).length(), Long.toString(y).length()) / 2;
        long p = (long) Math.pow(10, m);
        long x1 = x / p, x0 = x % p, y1 = y / p, y0 = y % p;
        long high = karatsuba(x1, y1);
        long low = karatsuba(x0, y0);
        long middle = karatsuba(x1 + x0, y1 + y0) - high - low;   // the third and last product
        return high * p * p + middle * p + low;
    }

    // Level 5: split at every position; the number of ways is a Catalan number.
    static long catalan(int n) {                          // C(n) = sum of C(i) * C(n-1-i)
        if (n <= 1) return 1;
        long total = 0;
        for (int i = 0; i < n; i++) total += catalan(i) * catalan(n - 1 - i);
        return total;
    }

    static long callsOf(int n, int k) {                  // how often catalan(n) ends up calling catalan(k)
        if (n == k) return 1;
        if (n <= 1) return 0;
        long total = 0;
        for (int i = 0; i < n; i++) total += callsOf(i, k) + callsOf(n - 1 - i, k);
        return total;
    }

    // Interview 1: LeetCode 53, Maximum Subarray, by divide and conquer.
    static int maxSubArray(int[] a, int lo, int hi) {
        if (lo == hi) return a[lo];
        int mid = (lo + hi) / 2;
        int leftSuffix = Integer.MIN_VALUE, rightPrefix = Integer.MIN_VALUE;
        for (int i = mid, sum = 0; i >= lo; i--) {
            sum += a[i];
            leftSuffix = Math.max(leftSuffix, sum);       // best sum ending exactly at mid
        }
        for (int j = mid + 1, sum = 0; j <= hi; j++) {
            sum += a[j];
            rightPrefix = Math.max(rightPrefix, sum);     // best sum starting exactly at mid + 1
        }
        int crossing = leftSuffix + rightPrefix;
        return Math.max(crossing, Math.max(maxSubArray(a, lo, mid), maxSubArray(a, mid + 1, hi)));
    }

    // Interview 2: LeetCode 241, Different Ways to Add Parentheses.
    static List<Integer> diffWaysToCompute(String expression) {
        List<Integer> results = new ArrayList<>();
        for (int i = 0; i < expression.length(); i++) {
            char op = expression.charAt(i);
            if (op != '+' && op != '-' && op != '*') continue;
            for (int left : diffWaysToCompute(expression.substring(0, i)))
                for (int right : diffWaysToCompute(expression.substring(i + 1)))
                    results.add(op == '+' ? left + right : op == '-' ? left - right : left * right);
        }
        if (results.isEmpty()) results.add(Integer.parseInt(expression));   // no operator: a plain number
        return results;
    }

    // Interview 3: LeetCode 327, Count of Range Sum.
    static int countRangeSum(int[] nums, int lower, int upper) {
        long[] prefix = new long[nums.length + 1];
        for (int i = 0; i < nums.length; i++) prefix[i + 1] = prefix[i] + nums[i];
        return countAndSort(prefix, 0, prefix.length - 1, lower, upper);
    }

    static int countAndSort(long[] p, int lo, int hi, int lower, int upper) {
        if (lo >= hi) return 0;
        int mid = (lo + hi) / 2;
        int count = countAndSort(p, lo, mid, lower, upper) + countAndSort(p, mid + 1, hi, lower, upper);
        int from = mid + 1, to = mid + 1;                 // right-half window where lower <= p[j] - p[i] <= upper
        for (int i = lo; i <= mid; i++) {
            while (from <= hi && p[from] - p[i] < lower) from++;
            while (to <= hi && p[to] - p[i] <= upper) to++;
            count += to - from;
        }
        Arrays.sort(p, lo, hi + 1);                        // both halves sorted: a merge would also do
        return count;
    }

    static int countRangeSumBruteForce(int[] nums, int lower, int upper) {
        int count = 0;
        for (int i = 0; i < nums.length; i++) {
            long sum = 0;
            for (int j = i; j < nums.length; j++) {
                sum += nums[j];
                if (lower <= sum && sum <= upper) count++;
            }
        }
        return count;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] scores = {3, 8, 2, 5, 9, 1, 7, 4};
        check(maxOf(scores, 0, 7) == 9 && maxOf(scores, 0, 3) == 8 && maxOf(scores, 4, 7) == 9, "max by halves");

        check(mergeSortWork(8) == 24 && mergeSortWork(1 << 20) == 20L * (1 << 20), "merge sort: n log2 n");
        check(20L * (1 << 20) == 20_971_520L && (1L << 40) == 1_099_511_627_776L, "2^20 * 20 versus (2^20)^2");

        check(Math.abs(criticalExponent(3, 2) - 1.585) < 1e-3, "log2 3 = 1.585");
        check(criticalExponent(2, 2) == 1 && criticalExponent(1, 2) == 0 && criticalExponent(4, 2) == 2, "exponents");

        check(digitProducts(4, 4) == 16 && digitProducts(4, 3) == 9, "4-digit numbers: 16 versus 9 digit products");
        check(digitProducts(1024, 4) == 1_048_576 && digitProducts(1024, 3) == 59_049, "1,024 digits: 4^10 vs 3^10");

        check(12 * 56 == 672 && 34 * 78 == 2652 && 46 * 134 == 6164 && 6164 - 672 - 2652 == 2840, "Karatsuba pieces");
        check(karatsuba(1234, 5678) == 7_006_652 && 1234 * 5678 == 7_006_652, "1234 * 5678");
        Random random = new Random(1);
        for (int t = 0; t < 2000; t++) {
            long x = random.nextInt(1_000_000_000), y = random.nextInt(1_000_000_000);
            check(karatsuba(x, y) == x * y, "Karatsuba agrees with * on " + x + " and " + y);
        }

        check(catalan(3) == 5 && catalan(4) == 14 && catalan(9) == 4862, "Catalan numbers");
        check(callsOf(9, 2) == 1458, "catalan(9) recomputes catalan(2) 1,458 times");
        check(1 * 4 + 3 * 2 + 9 * 1 == 19, "Karatsuba recursion tree for n = 4");

        int[] profits = {-2, 1, -3, 4, -1, 2, 1, -5, 4};
        check(maxSubArray(profits, 0, 8) == 6, "LC 53");
        check(maxSubArray(profits, 0, 4) == 4 && maxSubArray(profits, 5, 8) == 4, "LC 53: each half alone reaches 4");
        check(maxSubArray(new int[] {-3, -1, -2}, 0, 2) == -1 && maxSubArray(new int[] {5}, 0, 0) == 5, "LC 53 edges");

        List<Integer> ways = diffWaysToCompute("2*3-4*5");
        ways.sort(null);
        check(ways.equals(List.of(-34, -14, -10, -10, 10)) && ways.size() == catalan(3), "LC 241 example 2");
        check(diffWaysToCompute("2-1-1").equals(List.of(2, 0)), "LC 241 example 1, in split order");
        check(diffWaysToCompute("7").equals(List.of(7)), "LC 241 single number");

        check(countRangeSum(new int[] {-2, 5, -1}, -2, 2) == 3, "LC 327 example 1");
        check(countRangeSum(new int[] {0}, 0, 0) == 1, "LC 327 example 2");
        check(countRangeSum(new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE}, -1, 0) == 0, "LC 327 no int overflow");
        for (int t = 0; t < 500; t++) {
            int[] nums = random.ints(1 + random.nextInt(12), -5, 6).toArray();
            int lower = random.nextInt(11) - 5, upper = lower + random.nextInt(6);
            check(countRangeSum(nums, lower, upper) == countRangeSumBruteForce(nums, lower, upper), "LC 327 random");
        }
        System.out.println("OK");
    }
}
