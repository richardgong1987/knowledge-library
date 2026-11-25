import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

// Run from this folder with: java OrderedSetLab.java
public class OrderedSetLab {

    // Level 1: a sorted array answers "ceiling" with binary search.
    static int ceilingIndex(int[] sorted, int x) {   // first index whose key is >= x
        int lo = 0, hi = sorted.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (sorted[mid] < x) lo = mid + 1;
            else hi = mid;
        }
        return lo;                                     // sorted.length when every key is < x
    }

    // Level 2: the same questions, asked of java.util.TreeSet.
    static void navigableSetDemo() {
        TreeSet<Integer> buses = new TreeSet<>(List.of(10, 20, 30, 40, 50));
        check(buses.ceiling(33) == 40 && buses.floor(33) == 30, "nearest keys around 33");
        check(buses.ceiling(30) == 30 && buses.higher(30) == 40, "ceiling may return x itself, higher may not");
        check(buses.floor(30) == 30 && buses.lower(30) == 20, "floor may return x itself, lower may not");
        check(buses.ceiling(55) == null && buses.lower(10) == null, "no answer is null, not an exception");
        check(buses.subSet(20, 40).equals(Set.of(20, 30)), "subSet(a, b) is [a, b)");
        check(buses.headSet(30).equals(Set.of(10, 20)), "headSet(b) is everything below b");
        check(buses.tailSet(30).equals(Set.of(30, 40, 50)), "tailSet(a) is everything from a up");
    }

    // Level 3: a plain binary search tree, the shape every ordered set is built on.
    static final class Node {
        final int key;
        Node left, right;
        boolean red;                                   // used only by the red-black tree in Level 4

        Node(int key) { this.key = key; }
    }

    static Node insert(Node node, int key) {
        if (node == null) return new Node(key);
        if (key < node.key) node.left = insert(node.left, key);
        else if (key > node.key) node.right = insert(node.right, key);
        return node;
    }

    static Integer ceiling(Node node, int x) {
        Integer best = null;
        while (node != null) {
            if (node.key == x) return x;
            if (node.key > x) { best = node.key; node = node.left; }   // a candidate; look for a smaller one
            else node = node.right;                                    // too small; only bigger keys can help
        }
        return best;
    }

    static int levels(Node node) {                     // keys on the longest path from node downwards
        return node == null ? 0 : 1 + Math.max(levels(node.left), levels(node.right));
    }

    // Level 4: Sedgewick's left-leaning red-black tree. Three local repairs keep it balanced.
    static boolean isRed(Node node) {
        return node != null && node.red;
    }

    static Node put(Node h, int key) {
        if (h == null) { Node fresh = new Node(key); fresh.red = true; return fresh; }
        if (key < h.key) h.left = put(h.left, key);
        else if (key > h.key) h.right = put(h.right, key);
        if (isRed(h.right) && !isRed(h.left)) h = rotateLeft(h);      // a red link leans right: turn it left
        if (isRed(h.left) && isRed(h.left.left)) h = rotateRight(h);  // two reds in a row: lift the middle one
        if (isRed(h.left) && isRed(h.right)) flipColors(h);           // a node with two red links: split it
        return h;
    }

    static Node rotateLeft(Node h) {
        Node x = h.right;
        h.right = x.left;
        x.left = h;
        x.red = h.red;
        h.red = true;
        return x;
    }

    static Node rotateRight(Node h) {
        Node x = h.left;
        h.left = x.right;
        x.right = h;
        x.red = h.red;
        h.red = true;
        return x;
    }

    static void flipColors(Node h) {
        h.red = true;
        h.left.red = false;
        h.right.red = false;
    }

    static Node redBlackInsert(Node root, int key) {
        root = put(root, key);
        root.red = false;                              // the root is always black
        return root;
    }

    // Checks the red-black rules; returns the black height, or -1 if a rule is broken.
    static int blackHeight(Node node) {
        if (node == null) return 1;                    // the empty NIL leaves count as black
        if (isRed(node) && (isRed(node.left) || isRed(node.right))) return -1;
        int left = blackHeight(node.left), right = blackHeight(node.right);
        if (left < 0 || left != right) return -1;
        return left + (isRed(node) ? 0 : 1);
    }

    // Level 5: TreeSet forgets duplicates, so a sorted multiset is a TreeMap from value to count.
    static void add(TreeMap<Integer, Integer> counts, int x) {
        counts.merge(x, 1, Integer::sum);
    }

    static void removeOne(TreeMap<Integer, Integer> counts, int x) {
        if (counts.merge(x, -1, Integer::sum) == 0) counts.remove(x);
    }

    // Interview 1: LeetCode 729, My Calendar I.
    static final class MyCalendar {
        final TreeMap<Integer, Integer> booked = new TreeMap<>();     // start -> end

        boolean book(int start, int end) {
            Map.Entry<Integer, Integer> before = booked.floorEntry(start);
            Integer after = booked.ceilingKey(start);
            if (before != null && before.getValue() > start) return false; // the earlier event runs into us
            if (after != null && after < end) return false;                // we run into the later event
            booked.put(start, end);
            return true;
        }
    }

    // Interview 2: LeetCode 220, Contains Duplicate III.
    static boolean containsNearbyAlmostDuplicate(int[] nums, int indexDiff, int valueDiff) {
        TreeSet<Long> window = new TreeSet<>();        // the last indexDiff values; long avoids overflow
        for (int i = 0; i < nums.length; i++) {
            long x = nums[i];
            Long nearest = window.ceiling(x - valueDiff);              // smallest value >= x - t
            if (nearest != null && nearest <= x + valueDiff) return true;
            window.add(x);
            if (i >= indexDiff) window.remove((long) nums[i - indexDiff]);
        }
        return false;
    }

    // Interview 3: LeetCode 715, Range Module.
    static final class RangeModule {
        final TreeMap<Integer, Integer> ranges = new TreeMap<>();     // start -> end, sorted, disjoint, not touching

        void addRange(int left, int right) {
            Map.Entry<Integer, Integer> e = ranges.floorEntry(left);
            if (e != null && e.getValue() >= left) { left = e.getKey(); right = Math.max(right, e.getValue()); }
            e = ranges.floorEntry(right);
            if (e != null && e.getValue() > right) right = e.getValue();
            ranges.subMap(left, true, right, true).clear();             // swallow everything in [left, right]
            ranges.put(left, right);
        }

        void removeRange(int left, int right) {
            Map.Entry<Integer, Integer> e = ranges.floorEntry(right);
            if (e != null && e.getValue() > right) ranges.put(right, e.getValue()); // keep the piece after right
            e = ranges.floorEntry(left);
            if (e != null && e.getValue() > left) ranges.put(e.getKey(), left);     // keep the piece before left
            ranges.subMap(left, true, right, false).clear();
        }

        boolean queryRange(int left, int right) {
            Map.Entry<Integer, Integer> e = ranges.floorEntry(left);
            return e != null && e.getValue() >= right;
        }
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    public static void main(String[] args) {
        // Level 1
        int[] sorted = {10, 20, 30, 40, 50};
        check(ceilingIndex(sorted, 33) == 3 && sorted[3] == 40, "ceiling of 33 is 40, at index 3");
        check(ceilingIndex(sorted, 30) == 2 && ceilingIndex(sorted, 55) == 5, "exact hit, and no ceiling");
        check(sorted.length - ceilingIndex(sorted, 35) == 2, "inserting 35 shifts 40 and 50");
        check(Math.ceil(log2(5 + 1)) == 3 && Math.ceil(log2(1_000_000 + 1)) == 20, "binary search steps");

        // Level 2
        navigableSetDemo();
        TreeSet<Integer> buses = new TreeSet<>(List.of(10, 20, 30, 40, 50));
        check(buses.headSet(40).size() - buses.headSet(20).size() == 2, "rank(40) - rank(20) = 3 - 1");

        // Level 3
        Node shuffled = null;
        for (int key : new int[] {30, 10, 50, 20, 40}) shuffled = insert(shuffled, key);
        check(ceiling(shuffled, 33) == 40 && ceiling(shuffled, 30) == 30 && ceiling(shuffled, 55) == null, "BST ceiling");
        Node chain = null, balanced = null;
        for (int key = 1; key <= 7; key++) chain = insert(chain, key);
        for (int key : new int[] {4, 2, 6, 1, 3, 5, 7}) balanced = insert(balanced, key);
        check(levels(chain) == 7 && levels(balanced) == 3, "7 keys: a chain of 7 levels or 3 levels");

        // Level 4
        Node rb = null;
        for (int key : sorted) rb = redBlackInsert(rb, key);
        check(rb.key == 40 && rb.left.key == 20 && rb.left.red && rb.right.key == 50, "LLRB shape after 10..50");
        check(rb.left.left.key == 10 && rb.left.right.key == 30 && blackHeight(rb) == 3, "black height 2 plus NIL");
        check(Math.abs(2 * log2(1_000_001) - 39.86) < 0.01, "height bound for a million keys");
        check(2 * log2(7 + 1) == 6, "height bound for 7 keys");
        Node big = null;
        for (int key = 1; key <= 1_000_000; key++) big = redBlackInsert(big, key);
        check(blackHeight(big) > 0 && levels(big) <= 39, "a million sorted inserts stay within the bound");
        check(levels(big) == 20, "in fact the tree is nearly perfect: 20 levels");
        Random random = new Random(42);
        Node randomTree = null;
        List<Integer> randomKeys = new ArrayList<>();
        for (int i = 0; i < 100_000; i++) randomKeys.add(random.nextInt());
        for (int key : randomKeys) randomTree = redBlackInsert(randomTree, key);
        check(blackHeight(randomTree) > 0 && levels(randomTree) <= 2 * log2(100_001), "random inserts obey the rules");
        check(levels(randomTree) == 23 && Math.ceil(log2(100_001)) == 17, "100,000 random keys: 23 levels, 17 at best");

        // Level 5
        TreeMap<Integer, Integer> counts = new TreeMap<>();
        for (int x : new int[] {5, 5, 7}) add(counts, x);
        check(counts.equals(Map.of(5, 2, 7, 1)), "multiset {5, 5, 7}");
        removeOne(counts, 5);
        removeOne(counts, 7);
        check(counts.equals(Map.of(5, 1)), "one 5 left, 7 gone");

        // Interview problems
        MyCalendar calendar = new MyCalendar();
        check(calendar.book(10, 20) && !calendar.book(15, 25) && calendar.book(20, 30), "LC 729");
        check(!calendar.book(5, 11) && calendar.book(5, 10) && !calendar.book(10, 20), "LC 729 edges");

        check(containsNearbyAlmostDuplicate(new int[] {1, 9, 5, 12}, 2, 3), "LC 220: 9 and 12");
        check(!containsNearbyAlmostDuplicate(new int[] {1, 9, 5, 12}, 1, 2), "LC 220: too far apart");
        check(containsNearbyAlmostDuplicate(new int[] {1, 2, 3, 1}, 3, 0), "LC 220 example 1");
        check(!containsNearbyAlmostDuplicate(new int[] {1, 5, 9, 1, 5, 9}, 2, 3), "LC 220 example 2");
        check(containsNearbyAlmostDuplicate(new int[] {-1_000_000_000, 1_000_000_000}, 1, 2_000_000_000), "LC 220 overflow");

        RangeModule module = new RangeModule();
        module.addRange(10, 20);
        module.removeRange(14, 16);
        check(module.ranges.equals(Map.of(10, 14, 16, 20)), "LC 715: [10, 20) minus [14, 16)");
        check(module.queryRange(10, 14) && !module.queryRange(13, 15) && module.queryRange(16, 17), "LC 715");
        module.addRange(14, 16);
        check(module.ranges.equals(Map.of(10, 20)), "LC 715: touching ranges merge back");
        module.removeRange(5, 25);
        check(module.ranges.isEmpty(), "LC 715: remove everything");
        System.out.println("OK");
    }
}
