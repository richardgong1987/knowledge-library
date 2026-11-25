import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// Run from this folder with: java SortingLab.java
public class SortingLab {

    // Level 1: insertion sort, the way you sort a hand of cards. Returns how many single-step moves it made.
    static int insertionSort(int[] a) {
        int shifts = 0;
        for (int i = 1; i < a.length; i++) {
            int card = a[i];
            int j = i - 1;
            while (j >= 0 && a[j] > card) {   // strict >, so equal keys never pass each other
                a[j + 1] = a[j];
                j--;
                shifts++;
            }
            a[j + 1] = card;
        }
        return shifts;
    }

    static int countInversions(int[] a) {   // pairs i < j with a[i] > a[j], checked one by one
        int count = 0;
        for (int i = 0; i < a.length; i++) {
            for (int j = i + 1; j < a.length; j++) {
                if (a[i] > a[j]) count++;
            }
        }
        return count;
    }

    // Level 2: the fewest comparisons that any comparison sort needs in its worst case.
    static int comparisonLowerBound(int n) {   // smallest h with 2^h >= n!, exact for n <= 20
        long factorial = 1;
        for (int k = 2; k <= n; k++) factorial *= k;
        int h = 0;
        while ((1L << h) < factorial) h++;
        return h;
    }

    static double log2Factorial(int n) {
        double sum = 0;
        for (int k = 2; k <= n; k++) sum += Math.log(k) / Math.log(2);
        return sum;
    }

    // Level 3: stability.
    record Student(String name, int score) {}

    static void selectionSortByScore(Student[] s) {
        for (int i = 0; i < s.length; i++) {
            int min = i;
            for (int j = i + 1; j < s.length; j++) {
                if (s[j].score() < s[min].score()) min = j;
            }
            Student moved = s[i];   // this long-distance swap can jump over an equal score
            s[i] = s[min];
            s[min] = moved;
        }
    }

    // Level 4: quicksort with the last element as pivot. Returns the number of comparisons.
    static int quickSort(int[] a, int lo, int hi) {   // sorts a[lo..hi], both ends included
        if (lo >= hi) return 0;
        int pivot = a[hi], small = lo;                 // a[lo..small) holds the keys below the pivot
        for (int i = lo; i < hi; i++) {
            if (a[i] < pivot) swap(a, i, small++);
        }
        swap(a, small, hi);                            // the pivot lands in its final place
        return (hi - lo) + quickSort(a, lo, small - 1) + quickSort(a, small + 1, hi);
    }

    static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    static double expectedQuickSortComparisons(int n) {   // 2(n + 1)H(n) - 4n for a random pivot
        double harmonic = 0;
        for (int k = 1; k <= n; k++) harmonic += 1.0 / k;
        return 2 * (n + 1) * harmonic - 4 * n;
    }

    // Level 5: count the comparisons Java's object sort (TimSort) makes.
    static int countComparisons(Integer[] a) {
        int[] count = {0};
        Arrays.sort(a, (x, y) -> {
            count[0]++;
            return Integer.compare(x, y);
        });
        return count[0];
    }

    // Level 6: one comparator built from two keys.
    static final Comparator<Student> BY_SCORE_DESC_THEN_NAME =
            Comparator.comparingInt(Student::score).reversed().thenComparing(Student::name);

    // Interview 1: LeetCode 75, Sort Colors.
    static void sortColors(int[] nums) {
        int low = 0, i = 0, high = nums.length - 1;   // [0, low) = 0s, [low, i) = 1s, (high, end] = 2s
        while (i <= high) {
            if (nums[i] == 0) swap(nums, i++, low++);
            else if (nums[i] == 2) swap(nums, i, high--);   // keep i: the value swapped in is still unseen
            else i++;
        }
    }

    // Interview 2: LeetCode 56, Merge Intervals.
    static int[][] mergeIntervals(int[][] intervals) {
        int[][] sorted = intervals.clone();
        Arrays.sort(sorted, Comparator.comparingInt(interval -> interval[0]));
        List<int[]> merged = new ArrayList<>();
        for (int[] next : sorted) {
            if (!merged.isEmpty() && next[0] <= merged.getLast()[1]) {
                merged.getLast()[1] = Math.max(merged.getLast()[1], next[1]);   // overlap: stretch the block
            } else {
                merged.add(new int[] {next[0], next[1]});
            }
        }
        return merged.toArray(new int[0][]);
    }

    // Interview 3: LeetCode 179, Largest Number.
    static String largestNumber(int[] nums) {
        String[] parts = new String[nums.length];
        for (int i = 0; i < nums.length; i++) parts[i] = String.valueOf(nums[i]);
        Arrays.sort(parts, (a, b) -> (b + a).compareTo(a + b));   // a goes first when a+b beats b+a
        if (parts[0].equals("0")) return "0";                     // every number is zero
        return String.join("", parts);
    }

    // Interview 4: LeetCode 1665, Minimum Initial Energy to Finish Tasks.
    static int minimumEffort(int[][] tasks) {
        int[][] order = tasks.clone();
        Arrays.sort(order, (p, q) -> Integer.compare(q[1] - q[0], p[1] - p[0]));   // largest m - a first
        int spent = 0, need = 0;
        for (int[] task : order) {
            need = Math.max(need, spent + task[1]);   // energy needed to start this task
            spent += task[0];
        }
        return need;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] scores = {4, 2, 5, 1, 3};
        check(countInversions(scores) == 6, "[4, 2, 5, 1, 3] has 6 inversions");
        int[] cards = scores.clone();
        check(insertionSort(cards) == 6 && Arrays.equals(cards, new int[] {1, 2, 3, 4, 5}), "6 shifts, one per inversion");
        check(insertionSort(new int[] {5, 4, 3, 2, 1}) == 10, "reversed 5 keys: 5*4/2 = 10 shifts");
        check(insertionSort(new int[] {8, 7, 6, 5, 4, 3, 2, 1}) == 28, "reversed 8 keys: 28 shifts");
        check(insertionSort(new int[] {1, 2, 3, 4, 5}) == 0, "sorted input: no shifts");

        check(comparisonLowerBound(3) == 3, "3! = 6 leaves need height 3");
        check(comparisonLowerBound(4) == 5, "4! = 24, 2^5 = 32");
        check(comparisonLowerBound(5) == 7, "5! = 120, 2^7 = 128");
        check(comparisonLowerBound(10) == 22, "10! = 3,628,800, 2^22 = 4,194,304");
        check(Math.abs(log2Factorial(1000) - 8529.4) < 0.1, "log2(1000!) ~ 8,529.4");

        Student[] byScore = {new Student("Ava", 90), new Student("Ben", 85), new Student("Cal", 90), new Student("Dee", 85)};
        Student[] stable = byScore.clone();
        Arrays.sort(stable, Comparator.comparingInt(Student::score));
        check(Arrays.equals(Arrays.stream(stable).map(Student::name).toArray(), new String[] {"Ben", "Dee", "Ava", "Cal"}),
                "Arrays.sort on objects is stable");
        Student[] unstable = byScore.clone();
        selectionSortByScore(unstable);
        check(Arrays.equals(Arrays.stream(unstable).map(Student::name).toArray(), new String[] {"Ben", "Dee", "Cal", "Ava"}),
                "selection sort puts Cal before Ava");
        Student[] chained = byScore.clone();
        Arrays.sort(chained, BY_SCORE_DESC_THEN_NAME);
        check(Arrays.equals(Arrays.stream(chained).map(Student::name).toArray(), new String[] {"Ava", "Cal", "Ben", "Dee"}),
                "score descending, then name");

        int[] quick = scores.clone();
        check(quickSort(quick, 0, quick.length - 1) == 6 && Arrays.equals(quick, new int[] {1, 2, 3, 4, 5}), "quicksort: 4 + 1 + 1");
        check(quickSort(new int[] {1, 2, 3, 4, 5}, 0, 4) == 10, "sorted input is quicksort's worst case: 10");
        check(Math.round(expectedQuickSortComparisons(1000)) == 10986, "2(n+1)H(n) - 4n at n = 1000");
        check(Math.abs(expectedQuickSortComparisons(3) - 8.0 / 3) < 1e-9, "n = 3: 8/3");
        check(1000 * 999 / 2 == 499_500, "quicksort worst case at n = 1000");
        check(Math.round(2 * 1000 * Math.log(1000) / Math.log(2)) == 19932, "heapsort bound 2 n log2 n");

        check(countComparisons(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8}) == 7, "TimSort: sorted run of 8 costs 7");
        check(countComparisons(new Integer[] {8, 7, 6, 5, 4, 3, 2, 1}) == 7, "TimSort: descending run is reversed");

        int big = 2_000_000_000, negative = -1_000_000_000;
        check(big - negative == -1_294_967_296, "a - b overflows");
        check(Integer.compare(big, negative) > 0, "Integer.compare does not");

        int[] colors = {2, 0, 2, 1, 1, 0};
        sortColors(colors);
        check(Arrays.equals(colors, new int[] {0, 0, 1, 1, 2, 2}), "LC 75");
        check(Arrays.deepEquals(mergeIntervals(new int[][] {{1, 3}, {2, 6}, {8, 10}, {15, 18}}),
                new int[][] {{1, 6}, {8, 10}, {15, 18}}), "LC 56");
        check(Arrays.deepEquals(mergeIntervals(new int[][] {{4, 7}, {1, 4}}), new int[][] {{1, 7}}), "LC 56 touching");
        check(largestNumber(new int[] {3, 30, 34, 5, 9}).equals("9534330"), "LC 179");
        check(largestNumber(new int[] {10, 2}).equals("210"), "LC 179 small");
        check(largestNumber(new int[] {0, 0}).equals("0"), "LC 179 zeros");
        check(minimumEffort(new int[][] {{1, 2}, {2, 4}, {4, 8}}) == 8, "LC 1665 example 1");
        check(minimumEffort(new int[][] {{1, 3}, {2, 4}, {10, 11}, {10, 12}, {8, 9}}) == 32, "LC 1665 example 2");
        check(minimumEffort(new int[][] {{1, 7}, {2, 8}, {3, 9}, {4, 10}, {5, 11}, {6, 12}}) == 27, "LC 1665 example 3");
        System.out.println("OK");
    }
}
