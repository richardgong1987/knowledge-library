import java.util.function.LongPredicate;

// Run from this folder with: java BinarySearchLab.java
public class BinarySearchLab {

    // Level 1: the classic search on the half-open range [lo, hi). This is LeetCode 704.
    static int search(int[] a, int target) {
        int lo = 0, hi = a.length;                 // if target is in a, it is in a[lo..hi-1]
        while (lo < hi) {                          // the range still holds something
            int mid = lo + (hi - lo) / 2;
            if (a[mid] == target) return mid;
            if (a[mid] < target) lo = mid + 1;     // a[lo..mid] are all too small
            else hi = mid;                         // a[mid..hi-1] are all too big
        }
        return -1;
    }

    // Level 2: two ways to compute the middle.
    static int naiveMid(int lo, int hi) {
        return (lo + hi) / 2;                      // lo + hi can pass 2^31 - 1 and wrap negative
    }

    static int safeMid(int lo, int hi) {
        return lo + (hi - lo) / 2;                 // hi - lo always fits in an int
    }

    // Level 3: the worst case. After a probe, the bigger part left over has at most n / 2 elements.
    static int worstProbes(int n) {
        int probes = 0;
        while (n > 0) {
            n /= 2;
            probes++;
        }
        return probes;
    }

    // Level 3 check: the Level 1 search, counting how many elements it looks at.
    static int probesUsed(int[] a, int target) {
        int lo = 0, hi = a.length, probes = 0;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            probes++;
            if (a[mid] == target) break;
            if (a[mid] < target) lo = mid + 1;
            else hi = mid;
        }
        return probes;
    }

    // Level 4: boundaries in an array with duplicates.
    static int lowerBound(int[] a, int x) {        // first index i with a[i] >= x, or a.length
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] < x) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    static int upperBound(int[] a, int x) {        // first index i with a[i] > x, or a.length
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] <= x) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    // Level 5: binary search on the answer. ok must be false, ..., false, true, ..., true on [lo, hi].
    static long firstTrue(long lo, long hi, LongPredicate ok) {
        while (lo < hi) {
            long mid = lo + (hi - lo) / 2;
            if (ok.test(mid)) hi = mid;            // mid works, so the first true is mid or to its left
            else lo = mid + 1;                     // mid fails, and so does everything to its left
        }
        return lo;
    }

    // Interview 1: LeetCode 33, Search in Rotated Sorted Array.
    static int searchRotated(int[] a, int target) {
        int lo = 0, hi = a.length - 1;             // closed range [lo, hi]: a[hi] is compared below
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] == target) return mid;
            if (a[lo] <= a[mid]) {                 // the left half a[lo..mid] is sorted
                if (a[lo] <= target && target < a[mid]) hi = mid - 1;
                else lo = mid + 1;
            } else {                               // the right half a[mid..hi] is sorted
                if (a[mid] < target && target <= a[hi]) lo = mid + 1;
                else hi = mid - 1;
            }
        }
        return -1;
    }

    // Interview 2: LeetCode 410, Split Array Largest Sum.
    static int splitArray(int[] nums, int k) {
        long lo = 0, hi = 0;
        for (int x : nums) { lo = Math.max(lo, x); hi += x; }
        return (int) firstTrue(lo, hi, cap -> piecesNeeded(nums, cap) <= k);
    }

    static int piecesNeeded(int[] nums, long cap) { // greedy: fill each piece as far as cap allows
        int pieces = 1;
        long sum = 0;
        for (int x : nums) {
            if (sum + x > cap) { pieces++; sum = 0; }
            sum += x;
        }
        return pieces;
    }

    // Interview 3: LeetCode 4, Median of Two Sorted Arrays.
    static double findMedianSortedArrays(int[] a, int[] b) {
        if (a.length > b.length) return findMedianSortedArrays(b, a); // search the shorter array
        int m = a.length, n = b.length, half = (m + n + 1) / 2;
        int lo = 0, hi = m;
        while (true) {
            int i = lo + (hi - lo) / 2, j = half - i;         // a[0..i-1] and b[0..j-1] form the left side
            int aLeft = i == 0 ? Integer.MIN_VALUE : a[i - 1];
            int aRight = i == m ? Integer.MAX_VALUE : a[i];
            int bLeft = j == 0 ? Integer.MIN_VALUE : b[j - 1];
            int bRight = j == n ? Integer.MAX_VALUE : b[j];
            if (aLeft > bRight) hi = i - 1;                   // took too many from a
            else if (bLeft > aRight) lo = i + 1;              // took too few from a
            else if ((m + n) % 2 == 1) return Math.max(aLeft, bLeft);
            else return (Math.max(aLeft, bLeft) + (double) Math.min(aRight, bRight)) / 2;
        }
    }

    static int maxProbesOverAllTargets(int[] a) {
        int worst = 0;
        for (int target = a[0] - 1; target <= a[a.length - 1] + 1; target++) {
            worst = Math.max(worst, probesUsed(a, target));
        }
        return worst;
    }

    static int[] firstNumbers(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = 2 * i;  // even numbers, so odd targets are absent
        return a;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] prices = {3, 8, 15, 21, 30, 42, 57, 64};
        check(search(prices, 42) == 5 && probesUsed(prices, 42) == 3, "42 is at index 5, found on the third probe");
        check(search(prices, 40) == -1 && search(new int[0], 7) == -1, "absent targets give -1");
        check(search(prices, 3) == 0 && search(prices, 64) == 7, "both ends are found");

        check(naiveMid(1_500_000_000, 2_000_000_000) == -397_483_648, "lo + hi wraps to a negative middle");
        check(safeMid(1_500_000_000, 2_000_000_000) == 1_750_000_000, "lo + (hi - lo) / 2 is safe");
        check(((1_500_000_000 + 2_000_000_000) >>> 1) == 1_750_000_000, "the unsigned shift fix");

        check(worstProbes(8) == 4 && worstProbes(100) == 7, "4 probes for 8 elements, 7 for 100");
        check(worstProbes(1_000_000) == 20 && worstProbes(1_000_000_000) == 30, "20 probes for a million, 30 for a billion");
        check(maxProbesOverAllTargets(prices) == 4, "no target needs more than 4 probes among 8 prices");
        check(maxProbesOverAllTargets(firstNumbers(100)) == 7, "no target needs more than 7 probes among 100");
        for (int n = 1; n <= 1000; n++) {
            int ceilLog = 32 - Integer.numberOfLeadingZeros(n);   // ceil(log2(n + 1)) = number of bits of n
            check(worstProbes(n) == ceilLog, "worst probes equal ceil(log2(n + 1)) for n = " + n);
        }

        int[] marks = {1, 2, 2, 2, 5, 7};
        check(lowerBound(marks, 2) == 1 && upperBound(marks, 2) == 4, "the 2s occupy [1, 4)");
        check(upperBound(marks, 2) - lowerBound(marks, 2) == 3, "three copies of 2");
        check(lowerBound(marks, 4) == 4 && lowerBound(marks, 9) == 6 && lowerBound(marks, 0) == 0, "insert positions");

        check(firstTrue(0, 30, side -> side * side >= 30) == 6, "a 6 by 6 square is the first to hold 30 tiles");
        check(firstTrue(0, 36, side -> side * side >= 36) == 6, "exactly 36 tiles also needs side 6");

        int[] rotated = {4, 5, 6, 7, 0, 1, 2};
        check(searchRotated(rotated, 0) == 4 && searchRotated(rotated, 3) == -1, "LC 33");
        check(searchRotated(new int[] {1}, 0) == -1 && searchRotated(new int[] {3, 1}, 1) == 1, "LC 33 tiny cases");

        int[] parts = {7, 2, 5, 10, 8};
        check(piecesNeeded(parts, 18) == 2 && piecesNeeded(parts, 17) == 3, "18 fits in 2 pieces, 17 needs 3");
        check(splitArray(parts, 2) == 18, "LC 410");
        check(splitArray(new int[] {1, 2, 3, 4, 5}, 2) == 9 && splitArray(new int[] {1, 4, 4}, 3) == 4, "LC 410 more");
        int[] tests = {0};
        firstTrue(10, 32, cap -> { tests[0]++; return piecesNeeded(parts, cap) <= 2; });
        check(tests[0] == 4 && worstProbes(32 - 10 + 1) == 5, "[10, 32] took 4 predicate tests; at most 5");

        check(findMedianSortedArrays(new int[] {1, 3, 8}, new int[] {2, 7, 10, 11}) == 7.0, "LC 4 odd total");
        check(findMedianSortedArrays(new int[] {1, 2}, new int[] {3, 4}) == 2.5, "LC 4 even total");
        check(findMedianSortedArrays(new int[] {}, new int[] {1}) == 1.0, "LC 4 one empty array");
        check(findMedianSortedArrays(new int[] {5, 6}, new int[] {1, 2, 3}) == 3.0, "LC 4 all of a on the right");
        System.out.println("OK");
    }
}
