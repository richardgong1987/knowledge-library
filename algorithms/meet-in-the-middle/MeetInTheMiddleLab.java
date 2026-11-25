import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

// Run from this folder with: java MeetInTheMiddleLab.java
public class MeetInTheMiddleLab {

    // Level 1: every subset sum of a[from..to-1]. Each item doubles the list.
    static long[] allSums(int[] a, int from, int to) {
        long[] sums = {0};                                    // the empty subset
        for (int i = from; i < to; i++) {
            long[] next = Arrays.copyOf(sums, 2 * sums.length);
            for (int s = 0; s < sums.length; s++) next[sums.length + s] = sums[s] + a[i];   // old subsets plus a[i]
            sums = next;
        }
        return sums;
    }

    static int countBruteForce(int[] a, long target) {
        int count = 0;
        for (long sum : allSums(a, 0, a.length)) if (sum == target) count++;
        return count;
    }

    // Level 3: split in half, then match each left sum with right sums through a hash map.
    static long countSubsetsWithSum(int[] a, long target) {
        int half = a.length / 2;
        Map<Long, Integer> rightCount = new HashMap<>();
        for (long y : allSums(a, half, a.length)) rightCount.merge(y, 1, Integer::sum);
        long count = 0;
        for (long x : allSums(a, 0, half)) count += rightCount.getOrDefault(target - x, 0);   // x + y = target
        return count;
    }

    // Level 4: with both lists sorted, one sweep counts the pairs that fit a budget.
    static long countAtMost(long[] left, long[] right, long budget) {   // pairs with x + y <= budget; both sorted
        long count = 0;
        int j = right.length - 1;
        for (long x : left) {                             // x grows, so the largest partner y can only shrink
            while (j >= 0 && x + right[j] > budget) j--;
            count += j + 1;
        }
        return count;
    }

    // Interview 1: LeetCode 454, 4Sum II.
    static int fourSumCount(int[] nums1, int[] nums2, int[] nums3, int[] nums4) {
        Map<Integer, Integer> pairSums = new HashMap<>();
        for (int a : nums1)
            for (int b : nums2) pairSums.merge(a + b, 1, Integer::sum);
        int count = 0;
        for (int c : nums3)
            for (int d : nums4) count += pairSums.getOrDefault(-(c + d), 0);   // a + b = -(c + d)
        return count;
    }

    // Interview 2: LeetCode 1755, Closest Subsequence Sum.
    static int minAbsDifference(int[] nums, int goal) {
        int half = nums.length / 2;
        long[] left = allSums(nums, 0, half), right = allSums(nums, half, nums.length);
        Arrays.sort(left);
        Arrays.sort(right);
        long best = Long.MAX_VALUE;
        int i = 0, j = right.length - 1;
        while (i < left.length && j >= 0) {
            long sum = left[i] + right[j];
            best = Math.min(best, Math.abs(sum - goal));
            if (sum < goal) i++;                          // too small: take a bigger left sum
            else if (sum > goal) j--;                     // too big: take a smaller right sum
            else return 0;
        }
        return (int) best;
    }

    // Interview 3: LeetCode 2035, Partition Array Into Two Arrays to Minimize Sum Difference.
    static int minimumDifference(int[] nums) {
        int n = nums.length / 2;
        long total = Arrays.stream(nums).asLongStream().sum();
        List<TreeSet<Long>> left = sumsBySize(nums, 0, n), right = sumsBySize(nums, n, 2 * n);
        long best = Long.MAX_VALUE;
        for (int k = 0; k <= n; k++) {
            TreeSet<Long> partners = right.get(n - k);           // k picks on the left, n - k on the right
            for (long x : left.get(k)) {
                long ideal = Math.floorDiv(total - 2 * x, 2);    // y near this makes the two sides equal
                for (Long y : Arrays.asList(partners.floor(ideal), partners.ceiling(ideal)))
                    if (y != null) best = Math.min(best, Math.abs(total - 2 * (x + y)));
            }
        }
        return (int) best;
    }

    static List<TreeSet<Long>> sumsBySize(int[] a, int from, int to) {
        int n = to - from;
        List<TreeSet<Long>> bySize = new ArrayList<>();
        for (int k = 0; k <= n; k++) bySize.add(new TreeSet<>());
        for (int mask = 0; mask < (1 << n); mask++) {
            long sum = 0;
            for (int i = 0; i < n; i++) if ((mask >> i & 1) == 1) sum += a[from + i];
            bySize.get(Integer.bitCount(mask)).add(sum);           // group the sums by how many items they use
        }
        return bySize;
    }

    static int minAbsDifferenceBruteForce(int[] nums, int goal) {
        long best = Long.MAX_VALUE;
        for (long sum : allSums(nums, 0, nums.length)) best = Math.min(best, Math.abs(sum - goal));
        return (int) best;
    }

    static int minimumDifferenceBruteForce(int[] nums) {
        int n = nums.length / 2;
        long total = Arrays.stream(nums).asLongStream().sum(), best = Long.MAX_VALUE;
        for (int mask = 0; mask < (1 << nums.length); mask++) {
            if (Integer.bitCount(mask) != n) continue;
            long sum = 0;
            for (int i = 0; i < nums.length; i++) if ((mask >> i & 1) == 1) sum += nums[i];
            best = Math.min(best, Math.abs(total - 2 * sum));
        }
        return (int) best;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] weights = {4, 9, 2, 7, 5, 1};
        check(allSums(weights, 0, 6).length == 64, "2^6 = 64 subsets");
        check(Arrays.equals(allSums(weights, 0, 3), new long[] {0, 4, 9, 13, 2, 6, 11, 15}), "left half sums");
        check(Arrays.equals(allSums(weights, 3, 6), new long[] {0, 7, 5, 12, 1, 8, 6, 13}), "right half sums");
        check(countBruteForce(weights, 12) == 4 && countSubsetsWithSum(weights, 12) == 4, "4 subsets sum to 12");
        check((1L << 40) == 1_099_511_627_776L && 2 * (1L << 20) == 2_097_152L && (1L << 19) == 524_288L,
                "n = 40: 2^40 versus 2 * 2^20, a factor 2^19");
        check(20L * (1 << 20) == 20_971_520L, "sorting one half of 40 items");

        long[] left = allSums(weights, 0, 3), right = allSums(weights, 3, 6);
        Arrays.sort(left);
        Arrays.sort(right);
        check(Arrays.equals(left, new long[] {0, 2, 4, 6, 9, 11, 13, 15}), "left sorted");
        check(Arrays.equals(right, new long[] {0, 1, 5, 6, 7, 8, 12, 13}), "right sorted");
        long atMostTen = 0;
        for (long sum : allSums(weights, 0, 6)) if (sum <= 10) atMostTen++;
        check(countAtMost(left, right, 10) == 20 && atMostTen == 20 && 6 + 6 + 4 + 2 + 2 == 20, "20 subsets weigh <= 10");

        check(fourSumCount(new int[] {1, 2}, new int[] {-2, -1}, new int[] {-1, 2}, new int[] {0, 2}) == 2, "LC 454");
        check(fourSumCount(new int[] {0}, new int[] {0}, new int[] {0}, new int[] {0}) == 1, "LC 454 example 2");
        check(200L * 200 * 200 * 200 == 1_600_000_000L && 2 * 200 * 200 == 80_000, "LC 454 counts for n = 200");

        check(minAbsDifference(new int[] {5, -7, 3, 5}, 6) == 0, "LC 1755 example 1");
        check(minAbsDifference(new int[] {7, -9, 15, -2}, -5) == 1, "LC 1755 example 2");
        check(minAbsDifference(new int[] {1, 2, 3}, -7) == 7, "LC 1755 example 3");
        check(minAbsDifference(new int[] {1}, 5) == 4, "LC 1755 one element");

        check(minimumDifference(new int[] {3, 9, 7, 3}) == 2, "LC 2035 example 1");
        check(minimumDifference(new int[] {-36, 36}) == 72, "LC 2035 example 2");
        check(minimumDifference(new int[] {2, -1, 0, 4, -2, -9}) == 0, "LC 2035 example 3");
        check(1 << 15 == 32_768, "2^15 sums per half for n = 15");

        Random random = new Random(7);
        for (int t = 0; t < 300; t++) {
            int[] nums = random.ints(1 + random.nextInt(10), -20, 21).toArray();
            int goal = random.nextInt(81) - 40;
            check(minAbsDifference(nums, goal) == minAbsDifferenceBruteForce(nums, goal), "LC 1755 random");
            int[] even = random.ints(2 * (1 + random.nextInt(5)), -20, 21).toArray();
            check(minimumDifference(even) == minimumDifferenceBruteForce(even), "LC 2035 random");
            check(countSubsetsWithSum(nums, goal) == countBruteForce(nums, goal), "counting random");
        }
        System.out.println("OK");
    }
}
