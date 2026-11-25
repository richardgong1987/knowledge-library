import java.util.Arrays;

// Run from this folder with: java BinaryIndexedTreeLab.java
public class BinaryIndexedTreeLab {

    // Level 1: the lowest set bit of i. In two's complement -i = ~i + 1, which keeps only that bit in common with i.
    static int lowbit(int i) {
        return i & -i;
    }

    // Levels 2-4: prefix sums with point updates. Positions are 1..n, and tree[i] covers (i - lowbit(i), i].
    static final class FenwickTree {
        final int[] tree;

        FenwickTree(int n) {
            tree = new int[n + 1];                        // tree[0] is unused
        }

        FenwickTree(int[] a) {                            // a[0] goes to position 1
            this(a.length);
            for (int i = 1; i <= a.length; i++) add(i, a[i - 1]);
        }

        void add(int i, int delta) {
            for (; i < tree.length; i += lowbit(i)) tree[i] += delta;   // every box that contains position i
        }

        int prefix(int i) {
            int sum = 0;
            for (; i > 0; i -= lowbit(i)) sum += tree[i];    // boxes that tile (0, i], right to left
            return sum;
        }

        int rangeSum(int l, int r) {
            return prefix(r) - prefix(l - 1);
        }
    }

    // Level 5: add v to every position in [l, r], read one position. The tree holds a difference array.
    static final class RangeAddFenwick {
        final FenwickTree diff;

        RangeAddFenwick(int n) {
            diff = new FenwickTree(n);
        }

        void rangeAdd(int l, int r, int v) {
            diff.add(l, v);
            diff.add(r + 1, -v);                          // when r = n this lands past the end and does nothing
        }

        int valueAt(int i) {
            return diff.prefix(i);
        }
    }

    // Interview 1: LeetCode 307, Range Sum Query - Mutable.
    static final class NumArray {
        private final int[] nums;
        private final FenwickTree tree;

        NumArray(int[] nums) {
            this.nums = nums.clone();
            tree = new FenwickTree(nums);
        }

        void update(int index, int val) {
            tree.add(index + 1, val - nums[index]);       // the tree only takes changes, so pass the difference
            nums[index] = val;
        }

        int sumRange(int left, int right) {
            return tree.rangeSum(left + 1, right + 1);    // LeetCode counts from 0, the tree from 1
        }
    }

    // Interview 2: LeetCode 1649, Create Sorted Array through Instructions.
    static int createSortedArray(int[] instructions) {
        int maxValue = Arrays.stream(instructions).max().getAsInt();
        FenwickTree count = new FenwickTree(maxValue);    // count.prefix(v) = inserted values that are <= v
        long cost = 0;
        for (int i = 0; i < instructions.length; i++) {
            int x = instructions[i];
            int less = count.prefix(x - 1);
            int greater = i - count.prefix(x);            // i values are already inserted
            cost += Math.min(less, greater);
            count.add(x, 1);
        }
        return (int) (cost % 1_000_000_007);
    }

    // Interview 3: LeetCode 2179, Count Good Triplets in an Array.
    static long goodTriplets(int[] nums1, int[] nums2) {
        int n = nums1.length;
        int[] pos2 = new int[n];                          // pos2[v] = where value v sits in nums2
        for (int i = 0; i < n; i++) pos2[nums2[i]] = i;
        FenwickTree seen = new FenwickTree(n);            // marks the nums2 positions of values already passed in nums1
        long total = 0;
        for (int j = 0; j < n; j++) {
            int p = pos2[nums1[j]];
            long left = seen.prefix(p);                   // before j in nums1 and before p in nums2
            long right = (n - 1 - p) - (j - left);        // after p in nums2, minus those already passed in nums1
            total += left * right;
            seen.add(p + 1, 1);
        }
        return total;
    }

    static int steps(int i, boolean up, int n) {          // lab-only: how many boxes an operation touches
        int count = 0;
        while (i > 0 && i <= n) { count++; i = up ? i + lowbit(i) : i - lowbit(i); }
        return count;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] lows = new int[9];
        for (int i = 1; i <= 8; i++) lows[i] = lowbit(i);
        check(Arrays.equals(lows, new int[] {0, 1, 2, 1, 4, 1, 2, 1, 8}), "lowbit of 1..8");
        check((-6 & 0xFF) == 0b11111010 && (~6 & 0xFF) == 0b11111001 && lowbit(6) == 2, "6 & -6 in 8 bits");

        int[] a = {5, 8, 6, 3, 2, 7, 2, 6};
        FenwickTree fenwick = new FenwickTree(a);
        check(Arrays.equals(fenwick.tree, new int[] {0, 5, 13, 6, 22, 2, 9, 2, 39}), "boxes for positions 1..8");
        check(fenwick.prefix(7) == 33 && 2 + 9 + 22 == 33, "prefix(7) = tree[7] + tree[6] + tree[4]");
        check(steps(7, false, 8) == 3 && steps(8, false, 8) == 1, "prefix(7) reads 3 boxes, prefix(8) reads 1");
        check(fenwick.rangeSum(3, 6) == 18 && (9 + 22) - 13 == 18, "positions 3..6 = 31 - 13");
        check(fenwick.rangeSum(3, 7) == 20 && steps(7, false, 8) + steps(2, false, 8) == 4, "same range as the segment tree, 4 reads");

        fenwick.add(3, 4);
        check(fenwick.tree[3] == 10 && fenwick.tree[4] == 26 && fenwick.tree[8] == 43, "add(3, 4) climbs 3 -> 4 -> 8");
        check(steps(3, true, 8) == 3 && steps(1, true, 8) == 4, "update path lengths");
        check(fenwick.rangeSum(3, 6) == 22 && fenwick.prefix(8) == 43, "sums after the update");

        RangeAddFenwick ranges = new RangeAddFenwick(8);
        ranges.rangeAdd(2, 6, 2);
        ranges.rangeAdd(5, 8, 3);
        int[] values = new int[9];
        for (int i = 1; i <= 8; i++) values[i] = ranges.valueAt(i);
        check(Arrays.equals(values, new int[] {0, 0, 2, 2, 2, 5, 5, 3, 3}), "range adds via a difference tree");

        NumArray numArray = new NumArray(new int[] {1, 3, 5});
        check(numArray.sumRange(0, 2) == 9, "LC 307 before");
        numArray.update(1, 2);
        check(numArray.sumRange(0, 2) == 8 && numArray.sumRange(1, 2) == 7, "LC 307 after");

        check(createSortedArray(new int[] {1, 5, 6, 2}) == 1, "LC 1649");
        check(createSortedArray(new int[] {1, 2, 3, 6, 5, 4}) == 3, "LC 1649 example 2");
        check(createSortedArray(new int[] {1, 3, 3, 3, 2, 4, 2, 1, 2}) == 4, "LC 1649 example 3");

        check(goodTriplets(new int[] {2, 0, 1, 3}, new int[] {0, 1, 2, 3}) == 1, "LC 2179");
        check(goodTriplets(new int[] {4, 0, 1, 3, 2}, new int[] {4, 1, 0, 2, 3}) == 4, "LC 2179 example 2");
        System.out.println("OK");
    }
}
