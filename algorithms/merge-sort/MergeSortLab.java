import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Run from this folder with: java MergeSortLab.java
public class MergeSortLab {

    static int comparisons;   // counted only so the lab can check the numbers in the lesson

    // Level 1: merge two sorted runs into one.
    static int[] merge(int[] left, int[] right) {
        int[] out = new int[left.length + right.length];
        int i = 0, j = 0, k = 0;
        while (i < left.length && j < right.length) {
            comparisons++;
            out[k++] = left[i] <= right[j] ? left[i++] : right[j++];   // <= takes the left key on a tie: stable
        }
        while (i < left.length) out[k++] = left[i++];
        while (j < right.length) out[k++] = right[j++];
        return out;
    }

    // Level 2: top-down merge sort.
    static int[] mergeSort(int[] a) {
        if (a.length <= 1) return a;
        int mid = a.length / 2;
        int[] left = mergeSort(Arrays.copyOfRange(a, 0, mid));
        int[] right = mergeSort(Arrays.copyOfRange(a, mid, a.length));
        return merge(left, right);
    }

    static int mergeWork(int n) {   // T(n) = 2T(n/2) + n, T(1) = 0, for n a power of two
        return n <= 1 ? 0 : 2 * mergeWork(n / 2) + n;
    }

    // Level 4: bottom-up merge sort, no recursion. Returns the number of passes.
    static int bottomUpSort(int[] a) {
        int n = a.length, passes = 0;
        for (int width = 1; width < n; width *= 2, passes++) {
            for (int lo = 0; lo + width < n; lo += 2 * width) {
                int mid = lo + width, hi = Math.min(lo + 2 * width, n);
                int[] merged = merge(Arrays.copyOfRange(a, lo, mid), Arrays.copyOfRange(a, mid, hi));
                System.arraycopy(merged, 0, a, lo, merged.length);
            }
        }
        return passes;
    }

    // Level 5: count inversions while merging.
    static long sortAndCount(int[] a, int lo, int hi) {   // sorts a[lo, hi) and returns its inversions
        if (hi - lo <= 1) return 0;
        int mid = (lo + hi) / 2;
        long count = sortAndCount(a, lo, mid) + sortAndCount(a, mid, hi);
        int[] left = Arrays.copyOfRange(a, lo, mid);
        int i = 0, j = mid, k = lo;
        while (i < left.length) {
            if (j < hi && a[j] < left[i]) {
                count += left.length - i;   // a[j] jumps over every left key still waiting
                a[k++] = a[j++];
            } else {
                a[k++] = left[i++];
            }
        }
        return count;
    }

    // Interview 1: LeetCode 148, Sort List.
    static final class ListNode {
        int val;
        ListNode next;

        ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }
    }

    static ListNode sortList(ListNode head) {
        if (head == null || head.next == null) return head;
        ListNode slow = head, fast = head.next;   // fast moves two steps for each step of slow
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        ListNode second = slow.next;
        slow.next = null;                         // cut the list into two halves
        return mergeLists(sortList(head), sortList(second));
    }

    static ListNode mergeLists(ListNode a, ListNode b) {
        ListNode dummy = new ListNode(0, null), tail = dummy;
        while (a != null && b != null) {
            if (a.val <= b.val) { tail.next = a; a = a.next; }
            else { tail.next = b; b = b.next; }
            tail = tail.next;
        }
        tail.next = (a != null) ? a : b;          // relink the leftover run, no copying
        return dummy.next;
    }

    // Interview 2: LeetCode 315, Count of Smaller Numbers After Self.
    static List<Integer> countSmaller(int[] nums) {
        int n = nums.length;
        int[] index = new int[n], smaller = new int[n];
        for (int i = 0; i < n; i++) index[i] = i;
        sortIndices(nums, index, smaller, 0, n);
        List<Integer> result = new ArrayList<>();
        for (int count : smaller) result.add(count);
        return result;
    }

    static void sortIndices(int[] nums, int[] index, int[] smaller, int lo, int hi) {   // sorts index[lo, hi) by value
        if (hi - lo <= 1) return;
        int mid = (lo + hi) / 2;
        sortIndices(nums, index, smaller, lo, mid);
        sortIndices(nums, index, smaller, mid, hi);
        int[] left = Arrays.copyOfRange(index, lo, mid);
        int i = 0, j = mid, k = lo;
        while (i < left.length) {
            if (j < hi && nums[index[j]] < nums[left[i]]) {
                index[k++] = index[j++];          // a smaller right key passes the waiting left keys
            } else {
                smaller[left[i]] += j - mid;      // j - mid smaller right keys have passed this one
                index[k++] = left[i++];
            }
        }
    }

    // Interview 3: LeetCode 493, Reverse Pairs.
    static int reversePairs(int[] nums) {
        return sortAndCountReverse(nums.clone(), 0, nums.length);
    }

    static int sortAndCountReverse(int[] a, int lo, int hi) {   // sorts a[lo, hi), returns its reverse pairs
        if (hi - lo <= 1) return 0;
        int mid = (lo + hi) / 2;
        int count = sortAndCountReverse(a, lo, mid) + sortAndCountReverse(a, mid, hi);
        for (int i = lo, j = mid; i < mid; i++) {
            while (j < hi && a[i] > 2L * a[j]) j++;   // 2L: doubling an int can overflow
            count += j - mid;
        }
        mergeHalves(a, lo, mid, hi);
        return count;
    }

    static void mergeHalves(int[] a, int lo, int mid, int hi) {   // a[lo, mid) and a[mid, hi) are sorted
        int[] left = Arrays.copyOfRange(a, lo, mid);
        int i = 0, j = mid, k = lo;
        while (i < left.length) {
            if (j < hi && a[j] < left[i]) a[k++] = a[j++];
            else a[k++] = left[i++];
        }
    }

    static ListNode listOf(int... values) {
        ListNode head = null;
        for (int i = values.length - 1; i >= 0; i--) head = new ListNode(values[i], head);
        return head;
    }

    static int[] valuesOf(ListNode head) {
        List<Integer> values = new ArrayList<>();
        for (ListNode node = head; node != null; node = node.next) values.add(node.val);
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        comparisons = 0;
        check(Arrays.equals(merge(new int[] {1, 3, 5, 8}, new int[] {2, 4, 6, 7}), new int[] {1, 2, 3, 4, 5, 6, 7, 8})
                && comparisons == 7, "interleaved runs: n - 1 = 7 comparisons");
        comparisons = 0;
        merge(new int[] {1, 2, 3, 4}, new int[] {5, 6, 7, 8});
        check(comparisons == 4, "one run before the other: only 4 comparisons");

        int[] example = {8, 3, 5, 1, 7, 2, 6, 4};
        comparisons = 0;
        check(Arrays.equals(mergeSort(example), new int[] {1, 2, 3, 4, 5, 6, 7, 8}), "merge sort sorts");
        check(comparisons == 17, "4*1 + 2*3 + 7 = 17 comparisons, the worst case for n = 8");
        check(8 * 3 - 8 + 1 == 17, "n log2 n - n + 1");
        check((1 << 15) < 40320 && 40320 <= (1 << 16), "lower bound for n = 8: ceil(log2 8!) = 16");
        comparisons = 0;
        mergeSort(new int[] {1, 2, 3, 4, 5, 6, 7, 8});
        check(comparisons == 12, "sorted input: 4 + 4 + 4 = 12 comparisons");
        check(mergeWork(8) == 24 && mergeWork(1024) == 10240, "T(n) = n log2 n");

        int[] bottomUp = example.clone();
        check(bottomUpSort(bottomUp) == 3 && Arrays.equals(bottomUp, new int[] {1, 2, 3, 4, 5, 6, 7, 8}), "3 passes for 8 keys");
        int[] odd = {5, 1, 4, 2, 3};
        check(bottomUpSort(odd) == 3 && Arrays.equals(odd, new int[] {1, 2, 3, 4, 5}), "3 passes for 5 keys");

        check(sortAndCount(new int[] {2, 4, 1, 3, 5}, 0, 5) == 3, "[2, 4, 1, 3, 5] has 3 inversions");
        check(sortAndCount(example.clone(), 0, 8) == 16, "the example has 16 inversions");
        check(sortAndCount(new int[] {8, 7, 6, 5, 4, 3, 2, 1}, 0, 8) == 28, "reversed: 8*7/2 = 28");

        check(Arrays.equals(valuesOf(sortList(listOf(4, 2, 1, 3))), new int[] {1, 2, 3, 4}), "LC 148");
        check(Arrays.equals(valuesOf(sortList(listOf(-1, 5, 3, 4, 0))), new int[] {-1, 0, 3, 4, 5}), "LC 148 odd length");
        check(sortList(null) == null, "LC 148 empty list");

        check(countSmaller(new int[] {5, 2, 6, 1}).equals(List.of(2, 1, 1, 0)), "LC 315");
        check(countSmaller(new int[] {-1, -1}).equals(List.of(0, 0)), "LC 315 equal keys are not smaller");

        check(reversePairs(new int[] {1, 3, 2, 3, 1}) == 2, "LC 493 example 1");
        check(reversePairs(new int[] {2, 4, 3, 5, 1}) == 3, "LC 493 example 2");
        check(reversePairs(new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE}) == 2, "LC 493 overflow");
        System.out.println("OK");
    }
}
