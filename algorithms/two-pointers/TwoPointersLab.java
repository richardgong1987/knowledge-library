import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Run from this folder with: java TwoPointersLab.java
public class TwoPointersLab {

    // Level 1: pointers at opposite ends of a sorted array. LeetCode 167 is this with 1-based indices.
    static int[] pairWithSum(int[] a, int target) {   // a is sorted ascending
        int lo = 0, hi = a.length - 1;
        while (lo < hi) {
            int sum = a[lo] + a[hi];
            if (sum == target) return new int[] {lo, hi};
            if (sum < target) lo++;    // a[lo] is too small even with the largest partner left
            else hi--;                 // a[hi] is too big even with the smallest partner left
        }
        return new int[0];
    }

    // Level 1 check: how many sums pairWithSum computes.
    static int pairSteps(int[] a, int target) {
        int lo = 0, hi = a.length - 1, steps = 0;
        while (lo < hi) {
            steps++;
            int sum = a[lo] + a[hi];
            if (sum == target) break;
            if (sum < target) lo++;
            else hi--;
        }
        return steps;
    }

    // Level 2: two pointers moving the same way. a[0..write-1] holds the distinct values seen so far.
    static int removeDuplicates(int[] a) {            // a is sorted; returns how many distinct values
        int write = 0;
        for (int read = 0; read < a.length; read++) {
            if (write == 0 || a[read] != a[write - 1]) a[write++] = a[read];
        }
        return write;
    }

    // Level 3: three pointers partition 0s, 1s and 2s (Dijkstra's Dutch national flag; LeetCode 75).
    static void sortColors(int[] a) {
        int low = 0, mid = 0, high = a.length - 1;    // a[0..low-1] = 0, a[low..mid-1] = 1, a[high+1..] = 2
        while (mid <= high) {
            if (a[mid] == 0) swap(a, low++, mid++);
            else if (a[mid] == 1) mid++;
            else swap(a, mid, high--);                // the swapped-in value is unknown, so mid stays
        }
    }

    static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    // Level 4: one pointer in each of two sequences (LeetCode 392, Is Subsequence).
    static boolean isSubsequence(String small, String big) {
        int i = 0;
        for (int j = 0; j < big.length() && i < small.length(); j++) {
            if (small.charAt(i) == big.charAt(j)) i++;   // matched small[i]; look for the next letter
        }
        return i == small.length();
    }

    // Interview 1: LeetCode 15, 3Sum.
    static List<List<Integer>> threeSum(int[] nums) {
        int[] a = nums.clone();
        Arrays.sort(a);
        List<List<Integer>> triples = new ArrayList<>();
        for (int i = 0; i < a.length - 2; i++) {
            if (i > 0 && a[i] == a[i - 1]) continue;            // same first number, same triples
            int lo = i + 1, hi = a.length - 1;
            while (lo < hi) {
                int sum = a[i] + a[lo] + a[hi];
                if (sum < 0) lo++;
                else if (sum > 0) hi--;
                else {
                    triples.add(List.of(a[i], a[lo], a[hi]));
                    while (lo < hi && a[lo] == a[lo + 1]) lo++; // skip copies of this pair
                    lo++;
                    hi--;
                }
            }
        }
        return triples;
    }

    // Interview 2: LeetCode 11, Container With Most Water.
    static int maxArea(int[] h) {
        int lo = 0, hi = h.length - 1, best = 0;
        while (lo < hi) {
            best = Math.max(best, (hi - lo) * Math.min(h[lo], h[hi]));
            if (h[lo] <= h[hi]) lo++;    // every narrower container using h[lo] is no better
            else hi--;
        }
        return best;
    }

    // Interview 3: LeetCode 42, Trapping Rain Water.
    static int trap(int[] h) {
        int lo = 0, hi = h.length - 1, maxLeft = 0, maxRight = 0, water = 0;
        while (lo < hi) {
            maxLeft = Math.max(maxLeft, h[lo]);
            maxRight = Math.max(maxRight, h[hi]);
            if (maxLeft <= maxRight) water += maxLeft - h[lo++];   // the right wall is at least maxLeft
            else water += maxRight - h[hi--];                     // the left wall is more than maxRight
        }
        return water;
    }

    // Interview 3 check: the formula min(maxLeft, maxRight) - h for every index.
    static int[] waterPerIndex(int[] h) {
        int n = h.length;
        int[] left = new int[n], right = new int[n], water = new int[n];
        for (int i = 0; i < n; i++) left[i] = Math.max(h[i], i > 0 ? left[i - 1] : 0);
        for (int i = n - 1; i >= 0; i--) right[i] = Math.max(h[i], i < n - 1 ? right[i + 1] : 0);
        for (int i = 0; i < n; i++) water[i] = Math.min(left[i], right[i]) - h[i];
        return water;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] prices = {1, 3, 4, 6, 8, 11};
        check(Arrays.equals(pairWithSum(prices, 10), new int[] {2, 3}), "4 + 6 = 10 at indices 2 and 3");
        check(pairSteps(prices, 10) == 5, "found after 5 sums, which is n - 1");
        check(pairWithSum(prices, 100).length == 0 && pairWithSum(new int[] {5}, 10).length == 0, "no pair");
        check(prices.length * (prices.length - 1) / 2 == 15, "brute force would try 15 pairs");

        int[] withCopies = {1, 1, 2, 3, 3, 3, 4};
        int distinct = removeDuplicates(withCopies);
        check(distinct == 4 && Arrays.equals(Arrays.copyOf(withCopies, 4), new int[] {1, 2, 3, 4}), "LC 26");
        check(removeDuplicates(new int[0]) == 0, "empty array");

        int[] colors = {2, 0, 2, 1, 1, 0};
        sortColors(colors);
        check(Arrays.equals(colors, new int[] {0, 0, 1, 1, 2, 2}), "Dutch national flag");
        int[] oneColor = {2, 2};
        sortColors(oneColor);
        check(Arrays.equals(oneColor, new int[] {2, 2}), "a single colour stays put");

        check(isSubsequence("ace", "abcde") && !isSubsequence("aec", "abcde"), "LC 392");
        check(isSubsequence("", "abc") && !isSubsequence("abc", ""), "LC 392 empty strings");

        check(threeSum(new int[] {-1, 0, 1, 2, -1, -4}).equals(List.of(List.of(-1, -1, 2), List.of(-1, 0, 1))), "LC 15");
        check(threeSum(new int[] {0, 0, 0, 0}).equals(List.of(List.of(0, 0, 0))), "LC 15 duplicates");
        check(threeSum(new int[] {0, 1, 1}).isEmpty(), "LC 15 no triple");

        check(maxArea(new int[] {1, 8, 6, 2, 5, 4, 8, 3, 7}) == 49, "LC 11");
        check(maxArea(new int[] {1, 1}) == 1, "LC 11 two lines");

        check(Arrays.equals(waterPerIndex(new int[] {3, 0, 2, 0, 4}), new int[] {0, 3, 1, 3, 0}), "water per index");
        check(trap(new int[] {3, 0, 2, 0, 4}) == 7, "LC 42 small");
        check(trap(new int[] {0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1}) == 6, "LC 42 LeetCode example");
        check(trap(new int[] {}) == 0 && trap(new int[] {5, 4, 3}) == 0, "LC 42 no basin");
        System.out.println("OK");
    }
}
