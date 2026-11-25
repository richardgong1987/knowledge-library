import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

// Run from this folder with: java SegmentTreeLab.java
public class SegmentTreeLab {

    // Levels 2-4: range sums with point updates. Node 1 is the root; node v has children 2v and 2v + 1.
    static final class SegmentTree {
        final int n;
        final int[] tree;                                 // tree[node] = sum of the range that node covers

        SegmentTree(int[] a) {
            n = a.length;
            tree = new int[4 * n];                        // 4n slots always fit every node index
            build(a, 1, 0, n - 1);
        }

        void build(int[] a, int node, int l, int r) {
            if (l == r) { tree[node] = a[l]; return; }
            int m = (l + r) / 2;
            build(a, 2 * node, l, m);
            build(a, 2 * node + 1, m + 1, r);
            tree[node] = tree[2 * node] + tree[2 * node + 1];
        }

        int query(int node, int l, int r, int ql, int qr) {
            if (qr < l || r < ql) return 0;               // no overlap
            if (ql <= l && r <= qr) return tree[node];    // the node lies fully inside the query
            int m = (l + r) / 2;
            return query(2 * node, l, m, ql, qr) + query(2 * node + 1, m + 1, r, ql, qr);
        }

        void update(int node, int l, int r, int index, int value) {
            if (l == r) { tree[node] = value; return; }
            int m = (l + r) / 2;
            if (index <= m) update(2 * node, l, m, index, value);
            else update(2 * node + 1, m + 1, r, index, value);
            tree[node] = tree[2 * node] + tree[2 * node + 1];
        }

        int sum(int ql, int qr) {
            return query(1, 0, n - 1, ql, qr);
        }

        void set(int index, int value) {
            update(1, 0, n - 1, index, value);
        }
    }

    // Level 5: range add and range sum. A node that is fully covered keeps the add as a pending note.
    static final class LazySegmentTree {
        final int n;
        final int[] sum, pending;                         // pending[node] = an add its children have not seen yet

        LazySegmentTree(int[] a) {
            n = a.length;
            sum = new int[4 * n];
            pending = new int[4 * n];
            build(a, 1, 0, n - 1);
        }

        void build(int[] a, int node, int l, int r) {
            if (l == r) { sum[node] = a[l]; return; }
            int m = (l + r) / 2;
            build(a, 2 * node, l, m);
            build(a, 2 * node + 1, m + 1, r);
            sum[node] = sum[2 * node] + sum[2 * node + 1];
        }

        void add(int node, int l, int r, int ql, int qr, int v) {
            if (qr < l || r < ql) return;
            if (ql <= l && r <= qr) { apply(node, l, r, v); return; }   // stop here; children hear about it later
            pushDown(node, l, r);
            int m = (l + r) / 2;
            add(2 * node, l, m, ql, qr, v);
            add(2 * node + 1, m + 1, r, ql, qr, v);
            sum[node] = sum[2 * node] + sum[2 * node + 1];
        }

        void apply(int node, int l, int r, int v) {
            sum[node] += v * (r - l + 1);                 // each of the r - l + 1 values grows by v
            pending[node] += v;
        }

        void pushDown(int node, int l, int r) {
            int m = (l + r) / 2;
            apply(2 * node, l, m, pending[node]);
            apply(2 * node + 1, m + 1, r, pending[node]);
            pending[node] = 0;
        }

        int query(int node, int l, int r, int ql, int qr) {
            if (qr < l || r < ql) return 0;
            if (ql <= l && r <= qr) return sum[node];
            pushDown(node, l, r);
            int m = (l + r) / 2;
            return query(2 * node, l, m, ql, qr) + query(2 * node + 1, m + 1, r, ql, qr);
        }
    }

    // Interview 1: LeetCode 307, Range Sum Query - Mutable.
    static final class NumArray {
        private final SegmentTree tree;

        NumArray(int[] nums) {
            tree = new SegmentTree(nums);
        }

        void update(int index, int val) {
            tree.set(index, val);
        }

        int sumRange(int left, int right) {
            return tree.sum(left, right);
        }
    }

    // Interview 2: LeetCode 699, Falling Squares. "Raise a range to at least v" and "max of a range".
    // Raising never lowers anything, so a tag can stay on the node it was put on instead of being pushed down.
    static final class RaiseMaxTree {
        final int[] best, tag;                            // best = max inside the node's range; tag = a raise covering all of it

        RaiseMaxTree(int size) {
            best = new int[4 * size];
            tag = new int[4 * size];
        }

        void raise(int node, int l, int r, int ql, int qr, int v) {
            if (qr < l || r < ql) return;
            best[node] = Math.max(best[node], v);         // some point of this range now reaches v
            if (ql <= l && r <= qr) { tag[node] = Math.max(tag[node], v); return; }
            int m = (l + r) / 2;
            raise(2 * node, l, m, ql, qr, v);
            raise(2 * node + 1, m + 1, r, ql, qr, v);
        }

        int max(int node, int l, int r, int ql, int qr) {
            if (qr < l || r < ql) return 0;
            if (ql <= l && r <= qr) return best[node];
            int m = (l + r) / 2;
            int below = Math.max(max(2 * node, l, m, ql, qr), max(2 * node + 1, m + 1, r, ql, qr));
            return Math.max(tag[node], below);            // a tag here covers every point below it
        }
    }

    static List<Integer> fallingSquares(int[][] positions) {
        TreeSet<Integer> edges = new TreeSet<>();
        for (int[] p : positions) { edges.add(p[0]); edges.add(p[0] + p[1] - 1); }
        List<Integer> xs = new ArrayList<>(edges);        // compressed coordinates: index -> x
        int last = xs.size() - 1;
        RaiseMaxTree heights = new RaiseMaxTree(xs.size());
        List<Integer> answer = new ArrayList<>();
        int tallest = 0;
        for (int[] p : positions) {
            int l = Collections.binarySearch(xs, p[0]), r = Collections.binarySearch(xs, p[0] + p[1] - 1);
            int top = heights.max(1, 0, last, l, r) + p[1];   // lands on the highest point under it
            heights.raise(1, 0, last, l, r, top);
            tallest = Math.max(tallest, top);
            answer.add(tallest);
        }
        return answer;
    }

    // Interview 3: LeetCode 732, My Calendar III. A sparse tree over the times 0..10^9: nodes exist only once touched.
    static final class MyCalendarThree {
        private final Map<Long, Integer> max = new HashMap<>();   // node -> most bookings at one time in its range
        private final Map<Long, Integer> tag = new HashMap<>();   // node -> bookings that cover its whole range

        int book(int startTime, int endTime) {
            add(1, 0, 1_000_000_000, startTime, endTime - 1);     // [start, end) is the times start..end-1
            return max.getOrDefault(1L, 0);
        }

        private void add(long node, int l, int r, int ql, int qr) {
            if (qr < l || r < ql) return;
            if (ql <= l && r <= qr) {
                max.merge(node, 1, Integer::sum);
                tag.merge(node, 1, Integer::sum);
                return;
            }
            int m = l + (r - l) / 2;
            add(2 * node, l, m, ql, qr);
            add(2 * node + 1, m + 1, r, ql, qr);
            int below = Math.max(max.getOrDefault(2 * node, 0), max.getOrDefault(2 * node + 1, 0));
            max.put(node, tag.getOrDefault(node, 0) + below);
        }
    }

    // Lab-only helpers that measure the tree shape quoted on the page.
    static int maxNodeIndex(int node, int l, int r) {
        if (l == r) return node;
        int m = (l + r) / 2;
        return Math.max(maxNodeIndex(2 * node, l, m), maxNodeIndex(2 * node + 1, m + 1, r));
    }

    static int countNodes(int l, int r) {
        if (l == r) return 1;
        int m = (l + r) / 2;
        return 1 + countNodes(l, m) + countNodes(m + 1, r);
    }

    static int countVisits(int l, int r, int ql, int qr) {
        if (qr < l || r < ql || (ql <= l && r <= qr)) return 1;
        int m = (l + r) / 2;
        return 1 + countVisits(l, m, ql, qr) + countVisits(m + 1, r, ql, qr);
    }

    static int levels(int n) {                             // ceil(log2 n) + 1
        int depth = 0;
        while ((1 << depth) < n) depth++;
        return depth + 1;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] a = {5, 8, 6, 3, 2, 7, 2, 6};

        SegmentTree st = new SegmentTree(a);
        check(st.tree[1] == 39 && st.tree[2] == 22 && st.tree[3] == 17, "root and its children");
        check(st.tree[4] == 13 && st.tree[5] == 9 && st.tree[6] == 9 && st.tree[7] == 8, "level 2 sums");
        check(countNodes(0, 7) == 15 && countNodes(0, 5) == 11, "2n - 1 nodes");
        check(maxNodeIndex(1, 0, 7) == 15 && maxNodeIndex(1, 0, 5) == 13 && 13 < 16 && 16 <= 4 * 6, "node indices fit");
        for (int n = 1; n <= 1000; n++) check(maxNodeIndex(1, 0, n - 1) < 4 * n, "4n is enough for n = " + n);

        check(st.sum(2, 6) == 20 && st.sum(0, 7) == 39 && st.sum(4, 4) == 2, "range sums");
        check(countVisits(0, 7, 2, 6) == 9, "query [2, 6] visits 9 nodes");
        check(java.util.Arrays.stream(a, 2, 7).max().getAsInt() == 7, "range max of [2, 6] is 7");
        for (int n = 1; n <= 100; n++) {
            for (int ql = 0; ql < n; ql++) {
                for (int qr = ql; qr < n; qr++) check(countVisits(0, n - 1, ql, qr) <= 4 * levels(n), "at most 4 per level");
            }
        }

        st.set(3, 10);
        check(st.tree[11] == 10 && st.tree[5] == 16 && st.tree[2] == 29 && st.tree[1] == 46, "update path 11, 5, 2, 1");
        check(st.sum(2, 6) == 27, "query after the update");

        LazySegmentTree lazy = new LazySegmentTree(a);
        lazy.add(1, 0, 7, 2, 6, 2);
        check(lazy.sum[1] == 49 && lazy.sum[2] == 26 && lazy.sum[3] == 23, "range add reaches the root");
        check(lazy.sum[5] == 13 && lazy.pending[5] == 2 && lazy.sum[6] == 13 && lazy.pending[6] == 2, "covered nodes keep a note");
        check(lazy.sum[10] == 6 && lazy.sum[11] == 3, "children of node 5 are not touched yet");
        check(lazy.query(1, 0, 7, 3, 3) == 5, "a[3] = 3 + 2");
        check(lazy.sum[10] == 8 && lazy.sum[11] == 5 && lazy.pending[5] == 0, "the note was pushed down");
        check(lazy.query(1, 0, 7, 2, 6) == 30 && lazy.query(1, 0, 7, 0, 7) == 49, "range sums after the add");

        NumArray numArray = new NumArray(new int[] {1, 3, 5});
        check(numArray.sumRange(0, 2) == 9, "LC 307 before");
        numArray.update(1, 2);
        check(numArray.sumRange(0, 2) == 8 && numArray.sumRange(1, 1) == 2, "LC 307 after");

        check(fallingSquares(new int[][] {{1, 2}, {2, 3}, {6, 1}}).equals(List.of(2, 5, 5)), "LC 699");
        check(fallingSquares(new int[][] {{100, 100}, {200, 100}}).equals(List.of(100, 100)), "LC 699 side by side");
        check(fallingSquares(new int[][] {{1, 2}, {1, 3}, {2, 1}}).equals(List.of(2, 5, 6)), "LC 699 stacking");

        MyCalendarThree calendar = new MyCalendarThree();
        int[][] bookings = {{10, 20}, {50, 60}, {10, 40}, {5, 15}, {5, 10}, {25, 55}};
        int[] expected = {1, 1, 2, 3, 3, 3};
        for (int i = 0; i < bookings.length; i++) {
            check(calendar.book(bookings[i][0], bookings[i][1]) == expected[i], "LC 732 booking " + i);
        }
        check(levels(1_000_000_001) == 31, "the calendar tree has 31 levels");
        System.out.println("OK");
    }
}
