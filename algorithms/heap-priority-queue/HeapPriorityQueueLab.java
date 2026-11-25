import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

// Run from this folder with: java HeapPriorityQueueLab.java
public class HeapPriorityQueueLab {

    // Level 1: a complete binary tree stored level by level in an array.
    static int parent(int i) {
        return (i - 1) / 2;
    }

    static int left(int i) {
        return 2 * i + 1;
    }

    static int right(int i) {
        return 2 * i + 2;
    }

    // Levels 2 to 4: a min-heap of ints. Every parent is <= its children.
    static final class MinHeap {
        int[] a;
        int size;
        int swaps;                                     // counted so the lab can check the analysis

        MinHeap() {
            a = new int[16];
        }

        MinHeap(int[] values) {                        // Level 4: bottom-up heapify
            a = values.clone();
            size = a.length;
            for (int i = size / 2 - 1; i >= 0; i--) siftDown(i);
        }

        void offer(int x) {
            if (size == a.length) a = Arrays.copyOf(a, 2 * size);
            a[size] = x;
            siftUp(size++);
        }

        void siftUp(int i) {
            while (i > 0 && a[i] < a[parent(i)]) {     // smaller than its parent: move up
                swap(i, parent(i));
                i = parent(i);
            }
        }

        int poll() {
            if (size == 0) throw new NoSuchElementException("heap is empty");
            int min = a[0];
            a[0] = a[--size];                          // the last leaf fills the hole at the root
            siftDown(0);
            return min;
        }

        void siftDown(int i) {
            while (left(i) < size) {
                int child = left(i);
                if (child + 1 < size && a[child + 1] < a[child]) child++;   // the smaller child
                if (a[i] <= a[child]) return;
                swap(i, child);
                i = child;
            }
        }

        void swap(int i, int j) {
            int t = a[i];
            a[i] = a[j];
            a[j] = t;
            swaps++;
        }

        int[] contents() {
            return Arrays.copyOf(a, size);
        }
    }

    // Level 6: keep the k largest values seen so far; the root is the k-th largest.
    static int kthLargest(int[] values, int k) {
        PriorityQueue<Integer> largest = new PriorityQueue<>();    // a min-heap of at most k values
        for (int v : values) {
            largest.offer(v);
            if (largest.size() > k) largest.poll();    // drop the smallest of the k + 1
        }
        return largest.peek();
    }

    // Interview 1: LeetCode 1642, Furthest Building You Can Reach.
    static int furthestBuilding(int[] heights, int bricks, int ladders) {
        PriorityQueue<Integer> ladderClimbs = new PriorityQueue<>();   // the largest climbs, smallest on top
        for (int i = 0; i + 1 < heights.length; i++) {
            int climb = heights[i + 1] - heights[i];
            if (climb <= 0) continue;
            ladderClimbs.offer(climb);
            if (ladderClimbs.size() > ladders) bricks -= ladderClimbs.poll();  // the smallest climb costs bricks
            if (bricks < 0) return i;
        }
        return heights.length - 1;
    }

    // Interview 2: LeetCode 23, Merge k Sorted Lists.
    static final class ListNode {
        int val;
        ListNode next;

        ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }
    }

    static ListNode mergeKLists(ListNode[] lists) {
        PriorityQueue<ListNode> heads = new PriorityQueue<>(Comparator.comparingInt(n -> n.val));
        for (ListNode head : lists) {
            if (head != null) heads.offer(head);
        }
        ListNode dummy = new ListNode(0, null), tail = dummy;
        while (!heads.isEmpty()) {
            ListNode smallest = heads.poll();
            tail = tail.next = smallest;
            if (smallest.next != null) heads.offer(smallest.next);  // its list's new head joins the heap
        }
        return dummy.next;
    }

    // Interview 3: LeetCode 502, IPO.
    static int findMaximizedCapital(int k, int w, int[] profits, int[] capital) {
        int n = profits.length;
        Integer[] byCapital = new Integer[n];
        for (int i = 0; i < n; i++) byCapital[i] = i;
        Arrays.sort(byCapital, Comparator.comparingInt(i -> capital[i]));
        PriorityQueue<Integer> affordable = new PriorityQueue<>(Collections.reverseOrder());  // largest profit on top
        int next = 0;
        for (int round = 0; round < k; round++) {
            while (next < n && capital[byCapital[next]] <= w) affordable.offer(profits[byCapital[next++]]);
            if (affordable.isEmpty()) break;           // nothing left that we can afford
            w += affordable.poll();
        }
        return w;
    }

    static ListNode of(int... values) {
        ListNode dummy = new ListNode(0, null), tail = dummy;
        for (int v : values) tail = tail.next = new ListNode(v, null);
        return dummy.next;
    }

    static List<Integer> values(ListNode head) {
        List<Integer> out = new ArrayList<>();
        for (ListNode n = head; n != null; n = n.next) out.add(n.val);
        return out;
    }

    static boolean isMinHeap(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[parent(i)] > a[i]) return false;
        }
        return true;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        check(parent(4) == 1 && left(4) == 9 && right(4) == 10, "node 4: parent 1, children 9 and 10");
        check(parent(1) == 0 && parent(2) == 0 && left(0) == 1 && right(0) == 2, "the root's children");
        check((int) (Math.log(10) / Math.log(2)) == 3, "10 nodes have height floor(log2 10) = 3");
        check(isMinHeap(new int[] {1, 3, 2, 7, 4, 5}) && !isMinHeap(new int[] {1, 3, 2, 0}), "heap property");

        MinHeap heap = new MinHeap(new int[] {1, 3, 2, 7, 4, 5});
        check(heap.swaps == 0, "already a heap: heapify swaps nothing");
        heap.offer(0);
        check(Arrays.equals(heap.contents(), new int[] {0, 3, 1, 7, 4, 5, 2}) && heap.swaps == 2, "sift-up: 2 swaps");
        check(heap.poll() == 0, "poll returns the minimum");
        check(Arrays.equals(heap.contents(), new int[] {1, 3, 2, 7, 4, 5}) && heap.swaps == 3, "sift-down: 1 swap");
        List<Integer> drained = new ArrayList<>();
        while (heap.size > 0) drained.add(heap.poll());
        check(drained.equals(List.of(1, 2, 3, 4, 5, 7)), "polling everything gives sorted order");
        try {
            heap.poll();
            check(false, "poll on empty must throw");
        } catch (NoSuchElementException expected) {
        }

        int[] descending = new int[15];
        for (int i = 0; i < 15; i++) descending[i] = 15 - i;
        MinHeap built = new MinHeap(descending);
        check(isMinHeap(built.contents()) && built.swaps == 11, "heapify 15 values: 4*1 + 2*2 + 1*3 = 11 swaps");
        MinHeap inserted = new MinHeap();
        for (int v : descending) inserted.offer(v);
        check(isMinHeap(inserted.contents()) && inserted.swaps == 34, "15 inserts: 2*1 + 4*2 + 8*3 = 34 swaps");
        for (int n = 1; n <= 2000; n++) {
            int heights = 0;                           // a node's height: how far it can sift down
            for (int i = 0; i < n; i++) for (int j = i; left(j) < n; j = left(j)) heights++;
            check(heights == n - Integer.bitCount(n), "heights add up to n - (number of 1 bits of n), n = " + n);
            int[] worst = new int[n];
            for (int i = 0; i < n; i++) worst[i] = n - i;
            check(new MinHeap(worst).swaps <= heights, "heapify swaps at most the sum of heights, n = " + n);
        }

        PriorityQueue<Integer> min = new PriorityQueue<>(List.of(5, 1, 9, 3));
        PriorityQueue<Integer> max = new PriorityQueue<>(Collections.reverseOrder());
        max.addAll(List.of(5, 1, 9, 3));
        check(min.peek() == 1 && max.peek() == 9, "min-heap and max-heap");
        check(Integer.MIN_VALUE - 1 > 0, "subtraction overflows: MIN_VALUE - 1 is positive");
        PriorityQueue<Integer> broken = new PriorityQueue<>((x, y) -> x - y);
        broken.addAll(List.of(1, Integer.MIN_VALUE));
        check(broken.peek() == 1, "(x, y) -> x - y puts 1 before MIN_VALUE");
        PriorityQueue<Integer> fixed = new PriorityQueue<>(Integer::compare);
        fixed.addAll(List.of(1, Integer.MIN_VALUE));
        check(fixed.peek() == Integer.MIN_VALUE, "Integer::compare is safe");
        PriorityQueue<int[]> byFirst = new PriorityQueue<>(Comparator.comparingInt(p -> p[0]));
        byFirst.add(new int[] {3, 100});
        byFirst.add(new int[] {1, 200});
        check(byFirst.peek()[1] == 200, "comparing by the first field");

        check(kthLargest(new int[] {5, 1, 9, 3, 7, 6}, 3) == 6, "third largest of 5 1 9 3 7 6 is 6");
        check(kthLargest(new int[] {4}, 1) == 4, "k = n = 1");

        check(furthestBuilding(new int[] {4, 2, 7, 6, 9, 14, 12}, 5, 1) == 4, "LC 1642");
        check(furthestBuilding(new int[] {4, 12, 2, 7, 3, 18, 20, 3, 19}, 10, 2) == 7, "LC 1642 example 2");
        check(furthestBuilding(new int[] {14, 3, 19, 3}, 17, 0) == 3, "LC 1642 bricks only");

        ListNode merged = mergeKLists(new ListNode[] {of(1, 4, 5), of(1, 3, 4), of(2, 6)});
        check(values(merged).equals(List.of(1, 1, 2, 3, 4, 4, 5, 6)), "LC 23");
        check(mergeKLists(new ListNode[] {}) == null && mergeKLists(new ListNode[] {null}) == null, "LC 23 empty");

        check(findMaximizedCapital(2, 0, new int[] {1, 2, 3}, new int[] {0, 1, 1}) == 4, "LC 502");
        check(findMaximizedCapital(3, 0, new int[] {1, 2, 3}, new int[] {0, 1, 2}) == 6, "LC 502 example 2");
        check(findMaximizedCapital(1, 0, new int[] {5}, new int[] {1}) == 0, "LC 502 nothing affordable");
        System.out.println("OK");
    }
}
