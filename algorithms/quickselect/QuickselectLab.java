import java.util.Arrays;
import java.util.Random;

// Run from this folder with: java QuickselectLab.java
public class QuickselectLab {

    // Level 1: the obvious answer. Sort, then read one position.
    static int kthSmallestBySorting(int[] a, int k) {    // k = 1 is the minimum
        int[] copy = a.clone();
        Arrays.sort(copy);
        return copy[k - 1];
    }

    // Level 2: Lomuto partition, as in CLRS. Everything left of the pivot is smaller.
    static int partition(int[] a, int lo, int hi) {     // pivot a[hi]; returns its final index
        int pivot = a[hi], store = lo;
        for (int j = lo; j < hi; j++) {
            if (a[j] < pivot) swap(a, store++, j);      // grow the "smaller than pivot" block
        }
        swap(a, store, hi);
        return store;
    }

    static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    // Level 3: partition, then keep only the side that holds the target.
    static int quickselect(int[] a, int target) {      // target = 0-based index in sorted order
        int lo = 0, hi = a.length - 1;
        while (true) {
            int p = partition(a, lo, hi);
            if (p == target) return a[p];
            if (target < p) hi = p - 1; else lo = p + 1;   // keep only the side that holds the target
        }
    }

    // Level 5: a random pivot and a three-way split, so sorted input and duplicates stay fast.
    static int select(int[] a, int target, Random random) {
        int lo = 0, hi = a.length - 1;
        while (true) {
            int pivot = a[lo + random.nextInt(hi - lo + 1)];
            int lt = lo, i = lo, gt = hi;               // [lo, lt) < pivot, [lt, i) == pivot, (gt, hi] > pivot
            while (i <= gt) {
                if (a[i] < pivot) swap(a, lt++, i++);
                else if (a[i] > pivot) swap(a, i, gt--);
                else i++;
            }
            if (target < lt) hi = lt - 1;
            else if (target > gt) lo = gt + 1;
            else return pivot;                          // the target lies in the block of pivot copies
        }
    }

    // Interview 1: LeetCode 215, Kth Largest Element in an Array.
    static int findKthLargest(int[] nums, int k) {
        return select(nums.clone(), nums.length - k, new Random(42));   // k-th largest = (n - k)-th smallest
    }

    // Interview 2: LeetCode 973, K Closest Points to Origin.
    static int[][] kClosest(int[][] points, int k) {
        int[][] a = points.clone();
        int lo = 0, hi = a.length - 1;
        while (lo < hi) {                               // stop once the first k slots hold the k closest
            int p = partitionByDistance(a, lo, hi);
            if (p == k - 1) break;
            if (k - 1 < p) hi = p - 1; else lo = p + 1;
        }
        return Arrays.copyOf(a, k);
    }

    static int partitionByDistance(int[][] a, int lo, int hi) {   // Lomuto on squared distance
        int pivot = squaredDistance(a[hi]), store = lo;
        for (int j = lo; j < hi; j++) {
            if (squaredDistance(a[j]) < pivot) swap(a, store++, j);
        }
        swap(a, store, hi);
        return store;
    }

    static int squaredDistance(int[] point) {           // x^2 + y^2: same order as the distance, no sqrt
        return point[0] * point[0] + point[1] * point[1];
    }

    static void swap(Object[] a, int i, int j) {
        Object t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    // Interview 3: LeetCode 719, Find K-th Smallest Pair Distance.
    static int smallestDistancePair(int[] nums, int k) {
        int[] a = nums.clone();
        Arrays.sort(a);
        int lo = 0, hi = a[a.length - 1] - a[0];
        while (lo < hi) {                               // smallest d with pairsWithin(d) >= k
            int mid = (lo + hi) / 2;
            if (pairsWithin(a, mid) >= k) hi = mid; else lo = mid + 1;
        }
        return lo;
    }

    static long pairsWithin(int[] sorted, int d) {      // pairs i < j with sorted[j] - sorted[i] <= d
        long count = 0;
        for (int i = 0, j = 0; j < sorted.length; j++) {
            while (sorted[j] - sorted[i] > d) i++;
            count += j - i;
        }
        return count;
    }

    // Checks only: the same Lomuto quickselect, counting comparisons.
    static long comparisons(int[] a, int target, Random random) {
        long count = 0;
        int lo = 0, hi = a.length - 1;
        while (lo < hi) {
            if (random != null) swap(a, lo + random.nextInt(hi - lo + 1), hi);
            int p = partition(a, lo, hi);
            count += hi - lo;
            if (p == target) break;
            if (target < p) hi = p - 1; else lo = p + 1;
        }
        return count;
    }

    static int smallestDistancePairBruteForce(int[] nums, int k) {
        int n = nums.length, t = 0;
        int[] distances = new int[n * (n - 1) / 2];
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++) distances[t++] = Math.abs(nums[i] - nums[j]);
        Arrays.sort(distances);
        return distances[k - 1];
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] a = {7, 2, 9, 4, 1, 5};
        check(kthSmallestBySorting(a, 2) == 2 && kthSmallestBySorting(a, 6) == 9, "sorting answers");

        int[] traced = a.clone();
        check(partition(traced, 0, 5) == 3 && Arrays.equals(traced, new int[] {2, 4, 1, 5, 9, 7}), "Lomuto trace");
        check(comparisons(a.clone(), 3, null) == 5, "one partition of 6 items makes 5 comparisons");

        int[] left = {2, 4, 1, 5, 9, 7};
        check(partition(left, 0, 2) == 0 && Arrays.equals(left, new int[] {1, 4, 2, 5, 9, 7}), "second partition");
        check(partition(left, 1, 2) == 1 && Arrays.equals(left, new int[] {1, 2, 4, 5, 9, 7}), "third partition");
        check(quickselect(a.clone(), 1) == 2, "2nd smallest is 2");
        check(comparisons(a.clone(), 1, null) == 5 + 2 + 1, "8 comparisons to find the 2nd smallest");
        for (int t = 0; t < 6; t++) check(quickselect(a.clone(), t) == kthSmallestBySorting(a, t + 1), "every rank");

        long halvings = 0;
        for (long size = 1_000_000; size >= 1; size /= 2) halvings += size;
        check(halvings < 2_000_000 && halvings > 1_999_000, "n + n/2 + n/4 + ... < 2n");
        check(Math.abs(1_000_000 * Math.log(1_000_000) / Math.log(2) - 19_931_569) < 1, "n log2 n for n = 10^6");

        check(comparisons(new int[] {1, 2, 3, 4, 5, 6}, 0, null) == 15 && 6 * 5 / 2 == 15, "sorted input: n(n-1)/2");
        check(comparisons(new int[] {5, 5, 5, 5, 5, 5}, 5, null) == 15, "all equal: Lomuto is quadratic too");

        Random random = new Random(2024);
        int n = 100_001;
        long total = 0;
        int trials = 20;
        for (int trial = 0; trial < trials; trial++) {
            int[] perm = new int[n];
            for (int i = 0; i < n; i++) perm[i] = i;
            for (int i = n - 1; i > 0; i--) swap(perm, i, random.nextInt(i + 1));
            total += comparisons(perm, n / 2, random);
        }
        double perElement = (double) total / trials / n;
        check(perElement > 3.2 && perElement < 3.6, "random pivot, median: about 3.4n comparisons, got " + perElement);
        check(2 * (1 + Math.log(2)) > 3.38 && 2 * (1 + Math.log(2)) < 3.39, "2 + 2 ln 2 = 3.386");

        check(findKthLargest(new int[] {3, 2, 1, 5, 6, 4}, 2) == 5, "LC 215 example 1");
        check(findKthLargest(new int[] {3, 2, 3, 1, 2, 4, 5, 5, 6}, 4) == 4, "LC 215 example 2");
        int[] ones = new int[100_000];
        Arrays.fill(ones, 1);
        check(findKthLargest(ones, 50_000) == 1, "LC 215 all equal, one three-way pass");
        int[] sorted = new int[100_000];
        for (int i = 0; i < sorted.length; i++) sorted[i] = i;
        check(findKthLargest(sorted, 1) == 99_999, "LC 215 sorted input");

        int[][] closest = kClosest(new int[][] {{1, 3}, {-2, 2}}, 1);
        check(closest.length == 1 && closest[0][0] == -2 && closest[0][1] == 2, "LC 973 example 1");
        int[][] two = kClosest(new int[][] {{3, 3}, {5, -1}, {-2, 4}}, 2);
        check(squaredDistance(two[0]) + squaredDistance(two[1]) == 18 + 20, "LC 973 example 2: (3,3) and (-2,4)");
        check(squaredDistance(new int[] {5, -1}) == 26, "d^2 of (5, -1)");
        check(kClosest(new int[][] {{0, 1}, {1, 0}}, 2).length == 2, "LC 973 k = n");

        check(smallestDistancePair(new int[] {1, 3, 1}, 1) == 0, "LC 719 example 1");
        check(smallestDistancePair(new int[] {1, 1, 1}, 2) == 0, "LC 719 example 2");
        check(smallestDistancePair(new int[] {1, 6, 1}, 3) == 5, "LC 719 example 3");
        check(smallestDistancePair(new int[] {1, 3, 6, 10}, 3) == 4, "LC 719 traced example");
        int[] four = {1, 3, 6, 10};
        check(pairsWithin(four, 4) == 3 && pairsWithin(four, 2) == 1 && pairsWithin(four, 3) == 2, "pair counts");
        check(10_000L * 9_999 / 2 == 49_995_000L, "pairs for n = 10^4");
        for (int t = 0; t < 300; t++) {
            int[] nums = random.ints(2 + random.nextInt(9), 0, 20).toArray();
            int k = 1 + random.nextInt(nums.length * (nums.length - 1) / 2);
            check(smallestDistancePair(nums, k) == smallestDistancePairBruteForce(nums, k), "LC 719 random");
        }
        System.out.println("OK");
    }
}
