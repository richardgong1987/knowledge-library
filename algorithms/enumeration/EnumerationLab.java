import java.util.ArrayList;
import java.util.List;

// Run from this folder with: java EnumerationLab.java
public class EnumerationLab {

    // Level 1: try every code on a three-dial lock and count those with three different digits.
    static int codesWithDistinctDigits() {
        int count = 0;
        for (int a = 0; a <= 9; a++)
            for (int b = 0; b <= 9; b++)
                for (int c = 0; c <= 9; c++)
                    if (a != b && b != c && a != c) count++;
        return count;
    }

    // Level 2: every subset of n items is one n-bit number, from 0 to 2^n - 1.
    static int subsetsWithSum(int[] items, int target) {
        int n = items.length, count = 0;
        for (int mask = 0; mask < (1 << n); mask++) {     // each mask is one subset
            int sum = 0;
            for (int i = 0; i < n; i++)
                if ((mask >> i & 1) == 1) sum += items[i];
            if (sum == target) count++;
        }
        return count;
    }

    // Level 3: divisors come in pairs d and n / d, so d never needs to pass the square root.
    static int countDivisors(int n) {
        int count = 0;
        for (int d = 1; (long) d * d <= n; d++) {         // d * d <= n avoids floating-point sqrt
            if (n % d != 0) continue;
            count += (d == n / d) ? 1 : 2;               // d and n / d, counted once when equal
        }
        return count;
    }

    // Level 4: walk the answers instead of the inputs.
    static List<Integer> squaresUpTo(int limit) {
        List<Integer> squares = new ArrayList<>();
        for (int k = 1; k * k <= limit; k++) squares.add(k * k);   // 31 steps for 1,000, not 1,000
        return squares;
    }

    // Level 5: the slow, obviously correct check that the counting formula must agree with.
    static boolean hasRepeatedDigit(int x) {
        boolean[] seen = new boolean[10];
        for (; x > 0; x /= 10) {
            if (seen[x % 10]) return true;
            seen[x % 10] = true;
        }
        return false;
    }

    // Interview 1: LeetCode 507, Perfect Number.
    static boolean checkPerfectNumber(int num) {
        if (num == 1) return false;                      // 1 has no proper divisor except itself
        int sum = 1;                                     // 1 always divides num
        for (int d = 2; d * d <= num; d++) {
            if (num % d != 0) continue;
            sum += d;
            if (d != num / d) sum += num / d;            // the partner divisor above the square root
        }
        return sum == num;
    }

    // Interview 2: LeetCode 1291, Sequential Digits.
    static List<Integer> sequentialDigits(int low, int high) {
        List<Integer> result = new ArrayList<>();
        for (int length = 2; length <= 9; length++) {
            for (int start = 1; start + length - 1 <= 9; start++) {
                int x = 0;
                for (int d = start; d < start + length; d++) x = x * 10 + d;
                if (low <= x && x <= high) result.add(x);
            }
        }
        return result;
    }

    // Interview 3: LeetCode 1012, Numbers With Repeated Digits.
    static int numDupDigitsAtMostN(int n) {
        int[] digits = String.valueOf(n).chars().map(c -> c - '0').toArray();
        int digitCount = digits.length, distinct = 0;
        for (int length = 1; length < digitCount; length++) distinct += 9 * permutations(9, length - 1);
        boolean[] used = new boolean[10];
        for (int i = 0; i < digitCount; i++) {
            for (int d = (i == 0 ? 1 : 0); d < digits[i]; d++)
                if (!used[d]) distinct += permutations(9 - i, digitCount - i - 1);   // smaller digit here, rest free
            if (used[digits[i]]) return n - distinct;                         // n's own prefix repeats: stop
            used[digits[i]] = true;
        }
        return n - (distinct + 1);                                            // + 1 for n itself
    }

    static int permutations(int m, int k) {             // m * (m - 1) * ... * (m - k + 1)
        int p = 1;
        for (int j = 0; j < k; j++) p *= m - j;
        return p;
    }

    static int countRepeatedByBruteForce(int n) {
        int count = 0;
        for (int x = 1; x <= n; x++) if (hasRepeatedDigit(x)) count++;
        return count;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        check(10 * 10 * 10 == 1000 && codesWithDistinctDigits() == 720 && 10 * 9 * 8 == 720, "lock codes");

        int n = 4;
        check(n * (n - 1) / 2 == 6 && n * (n + 1) / 2 == 10 && (1 << n) == 16, "search-space sizes for n = 4");
        check(20 * 19 / 2 == 190 && 20 * 21 / 2 == 210 && (1 << 20) == 1_048_576, "search-space sizes for n = 20");
        check((1L << 40) == 1_099_511_627_776L, "2^40");
        long factorial = 1;
        for (int k = 2; k <= 20; k++) factorial *= k;
        check(2 * 3 * 4 == 24 && factorial == 2_432_902_008_176_640_000L, "4! and 20!");
        check(subsetsWithSum(new int[] {3, 1, 4, 2}, 5) == 2, "two subsets of {3, 1, 4, 2} sum to 5");
        check(subsetsWithSum(new int[] {}, 0) == 1, "the empty set is the one subset of nothing");

        check(countDivisors(36) == 9 && countDivisors(12) == 6 && countDivisors(1) == 1 && countDivisors(97) == 2,
                "divisor counts");
        check(countDivisors(Integer.MAX_VALUE) == 2, "2^31 - 1 is prime, and d * d does not overflow");

        List<Integer> squares = squaresUpTo(1000);
        check(squares.size() == 31 && squares.get(30) == 961 && 32 * 32 == 1024, "31 squares up to 1,000");

        int threeDigitRepeated = 0;
        for (int x = 100; x <= 999; x++) if (hasRepeatedDigit(x)) threeDigitRepeated++;
        check(9 * 9 * 8 == 648 && 900 - 648 == 252 && threeDigitRepeated == 252, "3-digit numbers by complement");

        check(checkPerfectNumber(28) && checkPerfectNumber(6) && checkPerfectNumber(496), "perfect numbers");
        check(!checkPerfectNumber(1) && !checkPerfectNumber(12) && !checkPerfectNumber(2), "not perfect");

        check(sequentialDigits(100, 300).equals(List.of(123, 234)), "LC 1291 example 1");
        check(sequentialDigits(1000, 13000).equals(List.of(1234, 2345, 3456, 4567, 5678, 6789, 12345)),
                "LC 1291 example 2");
        check(sequentialDigits(10, 1_000_000_000).size() == 36, "only 36 sequential-digit numbers exist");

        check(numDupDigitsAtMostN(20) == 1 && numDupDigitsAtMostN(100) == 10 && numDupDigitsAtMostN(1000) == 262,
                "LC 1012 examples");
        check(numDupDigitsAtMostN(345) == 83 && 345 - 262 == 83, "LC 1012 traced example: 262 distinct, 83 repeated");
        check(9 + 81 == 90 && 2 * 72 + 3 * 8 + 3 * 1 + 1 == 172 && 90 + 172 == 262, "the trace for 345");
        for (int x = 1; x <= 3000; x++) {
            check(numDupDigitsAtMostN(x) == countRepeatedByBruteForce(x), "LC 1012 against brute force at " + x);
        }
        check(numDupDigitsAtMostN(1) == 0 && numDupDigitsAtMostN(11) == 1, "LC 1012 small edges");
        System.out.println("OK");
    }
}
