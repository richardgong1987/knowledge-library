import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

// Run from this folder with: java SweepLineLab.java
public class SweepLineLab {

    // Levels 1-2: +1 at every start, -1 at every end; sort the events and keep a running count.
    static int maxOverlap(int[][] intervals) {             // half-open intervals [start, end)
        List<int[]> events = new ArrayList<>();
        for (int[] interval : intervals) {
            events.add(new int[] {interval[0], +1});
            events.add(new int[] {interval[1], -1});
        }
        events.sort(Comparator.<int[]>comparingInt(e -> e[0]).thenComparingInt(e -> e[1]));   // at a tie, -1 comes first
        int active = 0, best = 0;
        for (int[] e : events) {
            active += e[1];
            best = Math.max(best, active);
        }
        return best;
    }

    // Level 3: the active set itself, as a multiset (value -> how many copies are active).
    static void addOne(TreeMap<Integer, Integer> bag, int key) {
        bag.merge(key, 1, Integer::sum);
    }

    static void removeOne(TreeMap<Integer, Integer> bag, int key) {
        if (bag.merge(key, -1, Integer::sum) == 0) bag.remove(key);
    }

    // Level 4: total length covered by a set of intervals [start, end).
    static long unionLength(List<int[]> spans) {
        spans.sort(Comparator.comparingInt(s -> s[0]));
        long length = 0;
        int reach = Integer.MIN_VALUE;                     // right end of everything covered so far
        for (int[] s : spans) {
            if (s[1] > reach) {
                length += s[1] - Math.max(s[0], reach);    // only the part beyond reach is new
                reach = s[1];
            }
        }
        return length;
    }

    // Interview 1: LeetCode 1094, Car Pooling.
    static boolean carPooling(int[][] trips, int capacity) {
        int[] change = new int[1001];                      // locations are 0..1000
        for (int[] trip : trips) {
            change[trip[1]] += trip[0];                    // passengers get on at "from"
            change[trip[2]] -= trip[0];                    // and off at "to"
        }
        int onBoard = 0;
        for (int delta : change) {
            onBoard += delta;
            if (onBoard > capacity) return false;
        }
        return true;
    }

    // Interview 2: LeetCode 218, The Skyline Problem.
    static List<List<Integer>> getSkyline(int[][] buildings) {
        List<int[]> events = new ArrayList<>();
        for (int[] b : buildings) {
            events.add(new int[] {b[0], -b[2]});           // a start, stored as a negative height
            events.add(new int[] {b[1], b[2]});            // an end
        }
        // Same x: starts before ends, taller starts first, lower ends first.
        events.sort(Comparator.<int[]>comparingInt(e -> e[0]).thenComparingInt(e -> e[1]));
        TreeMap<Integer, Integer> active = new TreeMap<>(Map.of(0, 1));   // the ground, height 0, is always there
        List<List<Integer>> skyline = new ArrayList<>();
        int height = 0;
        for (int[] e : events) {
            if (e[1] < 0) addOne(active, -e[1]);
            else removeOne(active, e[1]);
            if (active.lastKey() != height) {              // the tallest active building changed
                height = active.lastKey();
                skyline.add(List.of(e[0], height));
            }
        }
        return skyline;
    }

    // Interview 3: LeetCode 850, Rectangle Area II.
    static int rectangleArea(int[][] rectangles) {
        TreeSet<Integer> xSet = new TreeSet<>();
        for (int[] r : rectangles) { xSet.add(r[0]); xSet.add(r[2]); }
        List<Integer> xs = new ArrayList<>(xSet);
        long area = 0;
        for (int i = 0; i + 1 < xs.size(); i++) {
            int left = xs.get(i), right = xs.get(i + 1);
            List<int[]> spans = new ArrayList<>();         // y-ranges of the rectangles that cover this whole slab
            for (int[] r : rectangles) if (r[0] <= left && right <= r[2]) spans.add(new int[] {r[1], r[3]});
            area += (right - left) * unionLength(spans);
        }
        return (int) (area % 1_000_000_007);
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[][] meetings = {{1, 4}, {2, 6}, {3, 5}, {7, 8}};
        check(maxOverlap(meetings) == 3, "three meetings overlap during [3, 4)");
        check(maxOverlap(new int[][] {{1, 3}, {3, 5}}) == 1, "half-open intervals that only touch");
        check(maxOverlap(new int[][] {}) == 0, "no intervals");
        check(4 * 3 / 2 == 6, "pairwise checks for 4 intervals");

        TreeMap<Integer, Integer> bag = new TreeMap<>();
        addOne(bag, 10);
        addOne(bag, 15);
        addOne(bag, 12);
        check(bag.lastKey() == 15, "tallest of 10, 15, 12");
        removeOne(bag, 15);
        check(bag.lastKey() == 12, "after 15 leaves");
        addOne(bag, 12);
        removeOne(bag, 12);
        check(bag.lastKey() == 12 && bag.get(12) == 1, "a second 12 keeps the first one alive");

        List<int[]> spans = new ArrayList<>(List.of(new int[] {1, 4}, new int[] {2, 6}, new int[] {3, 5}, new int[] {7, 8}));
        check(unionLength(spans) == 6, "covered length (6 - 1) + (8 - 7)");
        check(rectangleArea(new int[][] {{0, 0, 2, 2}, {1, 1, 3, 3}}) == 7, "two squares overlapping in one cell");

        check(!carPooling(new int[][] {{2, 1, 5}, {3, 3, 7}}, 4), "LC 1094 capacity 4");
        check(carPooling(new int[][] {{2, 1, 5}, {3, 3, 7}}, 5), "LC 1094 capacity 5");
        check(carPooling(new int[][] {{2, 1, 5}, {3, 5, 7}}, 3), "LC 1094 drop-off before pick-up");

        List<List<Integer>> skyline = getSkyline(new int[][] {{2, 9, 10}, {3, 7, 15}, {5, 12, 12}, {15, 20, 10}, {19, 24, 8}});
        check(skyline.equals(List.of(List.of(2, 10), List.of(3, 15), List.of(7, 12), List.of(12, 0),
                List.of(15, 10), List.of(20, 8), List.of(24, 0))), "LC 218");
        check(getSkyline(new int[][] {{0, 2, 3}, {2, 5, 3}}).equals(List.of(List.of(0, 3), List.of(5, 0))), "LC 218 equal heights touching");
        check(getSkyline(new int[][] {{1, 5, 3}, {1, 5, 7}}).equals(List.of(List.of(1, 7), List.of(5, 0))), "LC 218 same start and end");

        check(rectangleArea(new int[][] {{0, 0, 2, 2}, {1, 0, 2, 3}, {1, 0, 3, 1}}) == 6, "LC 850");
        check(rectangleArea(new int[][] {{0, 0, 1_000_000_000, 1_000_000_000}}) == 49, "LC 850 huge square");
        System.out.println("OK");
    }
}
