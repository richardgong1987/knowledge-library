import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

// Run from this folder with: java PrefixSumLab.java
public class PrefixSumLab {

    // Level 1: add the range up every time it is asked for.
    static int rangeSumBrute(int[] a, int l, int r) {
        int sum = 0;
        for (int i = l; i <= r; i++) sum += a[i];
        return sum;
    }

    // Level 2: prefix[i] = a[0] + ... + a[i - 1], so prefix has one more slot than a.
    static int[] prefixSums(int[] a) {
        int[] prefix = new int[a.length + 1];             // prefix[0] = 0 is the empty prefix
        for (int i = 0; i < a.length; i++) prefix[i + 1] = prefix[i] + a[i];
        return prefix;
    }

    // Level 3: S[i][j] = sum of the rectangle of rows 0..i-1 and columns 0..j-1.
    static int[][] prefixSums2D(int[][] grid) {
        int rows = grid.length, cols = grid[0].length;
        int[][] s = new int[rows + 1][cols + 1];          // row 0 and column 0 stay 0
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                s[i + 1][j + 1] = s[i][j + 1] + s[i + 1][j] - s[i][j] + grid[i][j];
            }
        }
        return s;
    }

    // Level 4: many "add v to a[l..r]" updates, applied with a difference array.
    static int[] applyRangeAdds(int n, int[][] updates) {
        int[] diff = new int[n + 1];                      // one extra slot for r + 1 = n
        for (int[] u : updates) {
            int l = u[0], r = u[1], v = u[2];
            diff[l] += v;                                 // from l onwards, everything is v higher
            diff[r + 1] -= v;                             // from r + 1 onwards, cancel it again
        }
        int[] a = new int[n];
        int running = 0;
        for (int i = 0; i < n; i++) {
            running += diff[i];                           // a prefix sum of diff rebuilds the array
            a[i] = running;
        }
        return a;
    }

    // Interview 1: LeetCode 303, Range Sum Query - Immutable.
    static final class NumArray {
        private final int[] prefix;

        NumArray(int[] nums) {
            prefix = prefixSums(nums);
        }

        int sumRange(int left, int right) {
            return prefix[right + 1] - prefix[left];
        }
    }

    // Interview 2: LeetCode 304, Range Sum Query 2D - Immutable.
    static final class NumMatrix {
        private final int[][] s;

        NumMatrix(int[][] matrix) {
            s = prefixSums2D(matrix);
        }

        int sumRegion(int row1, int col1, int row2, int col2) {
            return s[row2 + 1][col2 + 1] - s[row1][col2 + 1] - s[row2 + 1][col1] + s[row1][col1];
        }
    }

    // Interview 3: LeetCode 525, Contiguous Array.
    static int findMaxLength(int[] nums) {
        Map<Integer, Integer> firstSeen = new HashMap<>(Map.of(0, 0)); // prefix value -> first index where it occurred
        int prefix = 0, best = 0;
        for (int j = 1; j <= nums.length; j++) {
            prefix += nums[j - 1] == 1 ? 1 : -1;
            if (firstSeen.containsKey(prefix)) best = Math.max(best, j - firstSeen.get(prefix));
            else firstSeen.put(prefix, j);
        }
        return best;
    }

    // Interview 4: LeetCode 363, Max Sum of Rectangle No Larger Than K.
    static int maxSumSubmatrix(int[][] matrix, int k) {
        int rows = matrix.length, cols = matrix[0].length, best = Integer.MIN_VALUE;
        for (int top = 0; top < rows; top++) {
            int[] colSum = new int[cols];                 // column sums of rows top..bottom
            for (int bottom = top; bottom < rows; bottom++) {
                for (int c = 0; c < cols; c++) colSum[c] += matrix[bottom][c];
                best = Math.max(best, maxSubarrayAtMost(colSum, k));
            }
        }
        return best;
    }

    static int maxSubarrayAtMost(int[] a, int k) {
        TreeSet<Integer> seen = new TreeSet<>(List.of(0)); // prefix sums so far, starting with the empty prefix
        int prefix = 0, best = Integer.MIN_VALUE;
        for (int x : a) {
            prefix += x;
            Integer smallest = seen.ceiling(prefix - k);  // the smallest earlier prefix that is >= prefix - k
            if (smallest != null) best = Math.max(best, prefix - smallest);
            seen.add(prefix);
        }
        return best;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] a = {3, 1, 4, 1, 5, 9};

        int[] prefix = prefixSums(a);
        check(Arrays.equals(prefix, new int[] {0, 3, 4, 8, 9, 14, 23}), "prefix sums");
        check(prefix[5] - prefix[2] == 10 && rangeSumBrute(a, 2, 4) == 10, "sum of a[2..4] = 14 - 4 = 10");
        check(prefix[6] - prefix[0] == 23 && prefix[1] - prefix[1] == 0, "whole array and empty range");

        int[][] grid = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        int[][] s = prefixSums2D(grid);
        check(Arrays.deepEquals(s, new int[][] {{0, 0, 0, 0}, {0, 1, 3, 6}, {0, 5, 12, 21}, {0, 12, 27, 45}}), "2D prefix table");
        check(s[3][3] - s[1][3] - s[3][1] + s[1][1] == 28, "rows 1..2, columns 1..2 = 45 - 6 - 12 + 1 = 28");

        check(Arrays.equals(applyRangeAdds(6, new int[][] {{1, 3, 2}, {2, 5, 3}}), new int[] {0, 2, 5, 5, 3, 3}), "difference array");

        int[] xorPrefix = new int[a.length + 1];
        for (int i = 0; i < a.length; i++) xorPrefix[i + 1] = xorPrefix[i] ^ a[i];
        check(Arrays.equals(xorPrefix, new int[] {0, 3, 2, 6, 7, 2, 11}), "prefix xor");
        check((xorPrefix[4] ^ xorPrefix[1]) == 4 && (1 ^ 4 ^ 1) == 4, "xor of a[1..3] = 7 xor 3 = 4");

        NumArray numArray = new NumArray(new int[] {-2, 0, 3, -5, 2, -1});
        check(numArray.sumRange(0, 2) == 1 && numArray.sumRange(2, 5) == -1 && numArray.sumRange(0, 5) == -3, "LC 303");
        check(Arrays.equals(prefixSums(new int[] {-2, 0, 3, -5, 2, -1}), new int[] {0, -2, -2, 1, -4, -2, -3}), "LC 303 prefixes");

        NumMatrix numMatrix = new NumMatrix(new int[][] {
            {3, 0, 1, 4, 2}, {5, 6, 3, 2, 1}, {1, 2, 0, 1, 5}, {4, 1, 0, 1, 7}, {1, 0, 3, 0, 5}});
        check(numMatrix.sumRegion(2, 1, 4, 3) == 8 && numMatrix.sumRegion(1, 1, 2, 2) == 11
                && numMatrix.sumRegion(1, 2, 2, 4) == 12, "LC 304");
        check(21 - 4 - 9 + 3 == 11, "LC 304 traced query");

        check(findMaxLength(new int[] {0, 1, 1, 1, 1, 1, 0, 0, 0}) == 6, "LC 525");
        check(findMaxLength(new int[] {0, 1, 0}) == 2 && findMaxLength(new int[] {0, 0}) == 0, "LC 525 small");

        check(maxSumSubmatrix(new int[][] {{1, 0, 1}, {0, -2, 3}}, 2) == 2, "LC 363");
        check(maxSumSubmatrix(new int[][] {{2, 2, -1}}, 3) == 3, "LC 363 example 2");
        check(maxSubarrayAtMost(new int[] {1, -2, 4}, 2) == 2 && maxSubarrayAtMost(new int[] {0, -2, 3}, 2) == 1, "LC 363 row pairs");
        System.out.println("OK");
    }
}
