import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

// Run from this folder with: java BucketSortLab.java
public class BucketSortLab {

    // Level 1: scatter into n buckets, sort each bucket, gather.
    static double[] bucketSort(double[] a) {   // every value is in [0, 1)
        int n = a.length;
        List<List<Double>> buckets = new ArrayList<>();
        for (int i = 0; i < n; i++) buckets.add(new ArrayList<>());
        for (double x : a) buckets.get((int) (n * x)).add(x);   // bucket i holds [i/n, (i+1)/n)
        double[] out = new double[n];
        int k = 0;
        for (List<Double> bucket : buckets) {
            Collections.sort(bucket);                            // tiny lists: a binary insertion sort
            for (double x : bucket) out[k++] = x;
        }
        return out;
    }

    // Level 2: the work inside the buckets grows like the sum of the squared bucket sizes.
    static int sumOfSquaredBucketSizes(double[] a) {
        int n = a.length;
        int[] size = new int[n];
        for (double x : a) size[(int) (n * x)]++;
        int sum = 0;
        for (int s : size) sum += s * s;
        return sum;
    }

    static double averageSumOfSquares(int n, int trials, long seed) {   // uniform random inputs
        Random random = new Random(seed);
        double total = 0;
        for (int t = 0; t < trials; t++) {
            double[] a = new double[n];
            for (int i = 0; i < n; i++) a[i] = random.nextDouble();
            total += sumOfSquaredBucketSizes(a);
        }
        return total / trials;
    }

    // Level 4: integer buckets of a fixed width. floorDiv, not /, so negative keys round down too.
    static long bucketId(long x, long width) {
        return Math.floorDiv(x, width);
    }

    // Interview 1: LeetCode 347, Top K Frequent Elements.
    static int[] topKFrequent(int[] nums, int k) {
        Map<Integer, Integer> frequency = new HashMap<>();
        for (int x : nums) frequency.merge(x, 1, Integer::sum);
        List<List<Integer>> byFrequency = new ArrayList<>();      // byFrequency.get(f) = values seen exactly f times
        for (int f = 0; f <= nums.length; f++) byFrequency.add(new ArrayList<>());
        frequency.forEach((value, f) -> byFrequency.get(f).add(value));
        int[] top = new int[k];
        int found = 0;
        for (int f = nums.length; f >= 1 && found < k; f--) {
            for (int value : byFrequency.get(f)) {
                if (found < k) top[found++] = value;
            }
        }
        return top;
    }

    // Interview 2: LeetCode 164, Maximum Gap.
    static int maximumGap(int[] nums) {
        int n = nums.length;
        int min = Arrays.stream(nums).min().getAsInt(), max = Arrays.stream(nums).max().getAsInt();
        if (n < 2 || min == max) return 0;
        int width = Math.max(1, (max - min) / (n - 1));       // the largest gap is at least this wide
        int count = (max - min) / width + 1;
        int[] low = new int[count], high = new int[count];
        Arrays.fill(low, Integer.MAX_VALUE);
        Arrays.fill(high, Integer.MIN_VALUE);
        for (int x : nums) {
            int b = (x - min) / width;
            low[b] = Math.min(low[b], x);
            high[b] = Math.max(high[b], x);
        }
        int best = 0, previousHigh = min;
        for (int b = 0; b < count; b++) {
            if (low[b] == Integer.MAX_VALUE) continue;        // empty bucket: the gap stretches across it
            best = Math.max(best, low[b] - previousHigh);
            previousHigh = high[b];
        }
        return best;
    }

    // Interview 3: LeetCode 220, Contains Duplicate III.
    static boolean containsNearbyAlmostDuplicate(int[] nums, int indexDiff, int valueDiff) {
        long width = (long) valueDiff + 1;                     // two values in one bucket differ by <= valueDiff
        Map<Long, Long> window = new HashMap<>();              // bucket id -> the one window value in it
        for (int i = 0; i < nums.length; i++) {
            long x = nums[i], id = bucketId(x, width);
            if (window.containsKey(id)) return true;
            Long left = window.get(id - 1), right = window.get(id + 1);
            if (left != null && x - left <= valueDiff) return true;
            if (right != null && right - x <= valueDiff) return true;
            window.put(id, x);
            if (i >= indexDiff) window.remove(bucketId(nums[i - indexDiff], width));   // slide the window
        }
        return false;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        double[] times = {0.61, 0.17, 0.92, 0.34, 0.08, 0.55, 0.39, 0.73};
        check(Arrays.equals(bucketSort(times), new double[] {0.08, 0.17, 0.34, 0.39, 0.55, 0.61, 0.73, 0.92}), "bucket sort");
        check((int) (8 * 0.61) == 4 && (int) (8 * 0.55) == 4 && (int) (8 * 0.92) == 7, "0.61 and 0.55 share bucket 4");
        check(sumOfSquaredBucketSizes(times) == 10, "sizes 1,1,1,1,2,1,0,1: squares add to 10");
        check(Math.abs(averageSumOfSquares(8, 100_000, 42) - 15) < 0.1, "random inputs: about 2n - 1 = 15");
        check(Math.abs((2 - 1.0 / 8) - 1.875) < 1e-12, "E[n_i^2] = 2 - 1/n at n = 8");

        double[] crowded = {0.08, 0.02, 0.07, 0.01, 0.05, 0.03, 0.06, 0.04};
        check(sumOfSquaredBucketSizes(crowded) == 64, "all 8 keys in bucket 0: 8^2 = 64");
        check(Arrays.equals(bucketSort(crowded), new double[] {0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08}), "still sorts");

        check(bucketId(-3, 4) == -1 && bucketId(-1, 4) == -1 && bucketId(0, 4) == 0 && bucketId(3, 4) == 0
                && bucketId(4, 4) == 1, "floorDiv buckets of width 4");
        check(-3 / 4 == 0, "plain division rounds -3/4 toward zero");

        int[] top = topKFrequent(new int[] {1, 1, 1, 2, 2, 3}, 2);
        check(top.length == 2 && top[0] == 1 && top[1] == 2, "LC 347 example 1");
        check(Arrays.equals(topKFrequent(new int[] {1}, 1), new int[] {1}), "LC 347 example 2");
        int[] third = topKFrequent(new int[] {1, 2, 1, 2, 1, 2, 3, 1, 3, 2}, 2);
        Arrays.sort(third);
        check(Arrays.equals(third, new int[] {1, 2}), "LC 347 example 3");

        check(maximumGap(new int[] {3, 6, 9, 1}) == 3, "LC 164 example");
        check(maximumGap(new int[] {10}) == 0, "LC 164 one number");
        check(maximumGap(new int[] {1, 1, 1}) == 0, "LC 164 all equal");
        check(maximumGap(new int[] {1, 10000000}) == 9999999, "LC 164 two far apart");

        check(containsNearbyAlmostDuplicate(new int[] {1, 2, 3, 1}, 3, 0), "LC 220 example 1");
        check(!containsNearbyAlmostDuplicate(new int[] {1, 5, 9, 1, 5, 9}, 2, 3), "LC 220 example 2");
        check(!containsNearbyAlmostDuplicate(new int[] {-3, 3}, 1, 3), "LC 220: -3 and 3 differ by 6");
        check(containsNearbyAlmostDuplicate(new int[] {-1, 2}, 1, 3), "LC 220: -1 and 2 in neighbouring buckets");
        check(containsNearbyAlmostDuplicate(new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE}, 1, Integer.MAX_VALUE) == false,
                "LC 220 extreme values do not overflow");
        System.out.println("OK");
    }
}
