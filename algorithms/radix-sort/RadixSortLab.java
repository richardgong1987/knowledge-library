import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Run from this folder with: java RadixSortLab.java
public class RadixSortLab {

    // Level 1: the digit of x at place 1, 10, 100, ... (long, so place can pass 10^9 without overflow).
    static int digit(long x, long place) {
        return (int) (x / place % 10);
    }

    // Level 2: one stable counting-sort pass on a single digit.
    static int[] sortByDigit(int[] a, long place) {
        int[] end = new int[10];
        for (int x : a) end[digit(x, place)]++;
        for (int d = 1; d < 10; d++) end[d] += end[d - 1];   // end[d] = how many digits are <= d
        int[] out = new int[a.length];
        for (int i = a.length - 1; i >= 0; i--) {            // right to left keeps equal digits in order
            out[--end[digit(a[i], place)]] = a[i];
        }
        return out;
    }

    // Level 3: LSD radix sort, least significant digit first.
    static int[] lsdRadixSort(int[] a) {   // non-negative ints
        int max = Arrays.stream(a).max().orElse(0);
        for (long place = 1; max / place > 0; place *= 10) a = sortByDigit(a, place);
        return a;
    }

    // Level 4: total work d(n + b) for keys of the given bit width, with digits of r bits.
    static long radixCost(long n, int keyBits, int r) {
        long passes = (keyBits + r - 1) / r;                 // d = ceil(keyBits / r)
        return passes * (n + (1L << r));                     // each pass: n keys plus b = 2^r counters
    }

    // Level 5: MSD radix sort for lowercase words, first character first.
    static List<String> msdSort(List<String> words, int pos) {   // all words agree on their first pos characters
        if (words.size() <= 1) return words;
        List<String> sorted = new ArrayList<>();                 // words that end here come first: "do" < "dog"
        List<List<String>> buckets = new ArrayList<>();
        for (int c = 0; c < 26; c++) buckets.add(new ArrayList<>());
        for (String w : words) {
            if (w.length() == pos) sorted.add(w);
            else buckets.get(w.charAt(pos) - 'a').add(w);
        }
        for (List<String> bucket : buckets) sorted.addAll(msdSort(bucket, pos + 1));
        return sorted;
    }

    // Interview 1: LeetCode 2343, Query Kth Smallest Trimmed Number.
    static int[] smallestTrimmedNumbers(String[] nums, int[][] queries) {
        int n = nums.length, len = nums[0].length();
        int[][] orderAfter = new int[len + 1][];                 // orderAfter[p] = indices sorted by the last p digits
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;                // ties go to the smaller index
        orderAfter[0] = order;
        for (int pass = 1; pass <= len; pass++) {
            int pos = len - pass;                                // the next character from the right
            int[] end = new int[10];
            for (int i : order) end[nums[i].charAt(pos) - '0']++;
            for (int d = 1; d < 10; d++) end[d] += end[d - 1];
            int[] next = new int[n];
            for (int j = n - 1; j >= 0; j--) next[--end[nums[order[j]].charAt(pos) - '0']] = order[j];
            orderAfter[pass] = order = next;
        }
        int[] answer = new int[queries.length];
        for (int q = 0; q < queries.length; q++) answer[q] = orderAfter[queries[q][1]][queries[q][0] - 1];
        return answer;
    }

    // Interview 2: LeetCode 164, Maximum Gap, by sorting in linear time.
    static int maximumGap(int[] nums) {
        int[] sorted = lsdRadixSort(nums.clone());
        int best = 0;
        for (int i = 1; i < sorted.length; i++) best = Math.max(best, sorted[i] - sorted[i - 1]);
        return best;
    }

    // Interview 3: LeetCode 1707, Maximum XOR With an Element From Array.
    static int[] maximizeXor(int[] nums, int[][] queries) {
        int[] sorted = lsdRadixSort(nums.clone());
        int[] answer = new int[queries.length];
        for (int q = 0; q < queries.length; q++) {
            int x = queries[q][0], limit = queries[q][1];
            if (sorted[0] > limit) { answer[q] = -1; continue; }
            int prefix = 0;                                      // high bits of the element chosen so far
            for (int bit = 29; bit >= 0; bit--) {                // values are below 2^30
                int xBit = (x >>> bit) & 1;
                int want = prefix | ((1 - xBit) << bit);         // the opposite bit makes this XOR bit 1
                if (anyInRange(sorted, want, Math.min(limit, want + (1 << bit) - 1))) prefix = want;
                else prefix |= xBit << bit;
            }
            answer[q] = x ^ prefix;
        }
        return answer;
    }

    static boolean anyInRange(int[] sorted, int lo, int hi) {   // is some element in [lo, hi]?
        int left = 0, right = sorted.length;                     // find the first element >= lo
        while (left < right) {
            int mid = (left + right) >>> 1;
            if (sorted[mid] < lo) left = mid + 1;
            else right = mid;
        }
        return left < sorted.length && sorted[left] <= hi;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        check(digit(253, 1) == 3 && digit(253, 10) == 5 && digit(253, 100) == 2, "253 = 2*100 + 5*10 + 3");

        int[] ids = {253, 146, 512, 340, 418, 237, 651, 124};
        int[] pass1 = sortByDigit(ids, 1);
        check(Arrays.equals(pass1, new int[] {340, 651, 512, 253, 124, 146, 237, 418}), "pass 1: ones digit");
        int[] pass2 = sortByDigit(pass1, 10);
        check(Arrays.equals(pass2, new int[] {512, 418, 124, 237, 340, 146, 651, 253}), "pass 2: tens digit");
        int[] pass3 = sortByDigit(pass2, 100);
        check(Arrays.equals(pass3, new int[] {124, 146, 237, 253, 340, 418, 512, 651}), "pass 3: hundreds digit");
        check(Arrays.equals(lsdRadixSort(ids.clone()), pass3), "LSD radix sort");
        check(3 * (8 + 10) == 54, "d(n + b) = 3(8 + 10) = 54");
        check(Arrays.equals(lsdRadixSort(new int[] {Integer.MAX_VALUE, 0, 7}), new int[] {0, 7, Integer.MAX_VALUE}),
                "10-digit keys do not overflow the place value");
        check(Arrays.equals(lsdRadixSort(new int[] {}), new int[] {}), "empty input");

        check(radixCost(1_000_000, 32, 8) == 4_001_024, "4 passes of 8 bits");
        check(radixCost(1_000_000, 32, 16) == 2_131_072, "2 passes of 16 bits");
        check(radixCost(1_000_000, 32, 4) == 8_000_128, "8 passes of 4 bits");
        check(Math.round(1e6 * Math.log(1e6) / Math.log(2)) == 19_931_569, "n log2 n at 10^6");

        check(msdSort(List.of("dog", "cat", "do", "car", "dot", "a"), 0).equals(List.of("a", "car", "cat", "do", "dog", "dot")),
                "MSD on words of different lengths");

        check(Arrays.equals(smallestTrimmedNumbers(new String[] {"102", "473", "251", "814"},
                new int[][] {{1, 1}, {2, 3}, {4, 2}, {1, 2}}), new int[] {2, 2, 1, 0}), "LC 2343 example 1");
        check(Arrays.equals(smallestTrimmedNumbers(new String[] {"24", "37", "96", "04"},
                new int[][] {{2, 1}, {2, 2}}), new int[] {3, 0}), "LC 2343 example 2");

        check(maximumGap(new int[] {3, 6, 9, 1}) == 3, "LC 164");
        check(maximumGap(new int[] {10}) == 0, "LC 164 one number");
        check(maximumGap(new int[] {1_000_000_000, 0}) == 1_000_000_000, "LC 164 extreme values");

        check(Arrays.equals(maximizeXor(new int[] {0, 1, 2, 3, 4}, new int[][] {{3, 1}, {1, 3}, {5, 6}}), new int[] {3, 3, 7}),
                "LC 1707 example 1");
        check(Arrays.equals(maximizeXor(new int[] {5, 2, 4, 6, 6, 3}, new int[][] {{12, 4}, {8, 1}, {6, 3}}), new int[] {15, -1, 5}),
                "LC 1707 example 2");
        check(Arrays.equals(maximizeXor(new int[] {1_000_000_000}, new int[][] {{0, 1_000_000_000}}), new int[] {1_000_000_000}),
                "LC 1707 largest values");
        System.out.println("OK");
    }
}
