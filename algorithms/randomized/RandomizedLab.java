import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;

// Run from this folder with: java RandomizedLab.java
public class RandomizedLab {

    // Level 1: a Las Vegas search and a Monte Carlo search for the letter 'a'.
    static int lasVegasFind(char[] letters, Random random) {
        while (true) {
            int i = random.nextInt(letters.length);
            if (letters[i] == 'a') return i;           // always correct; only the time is random
        }
    }

    static int monteCarloFind(char[] letters, int tries, Random random) {
        for (int t = 0; t < tries; t++) {
            int i = random.nextInt(letters.length);
            if (letters[i] == 'a') return i;
        }
        return -1;                                     // fixed time, but may wrongly say "none"
    }

    // Level 2: the Fisher–Yates shuffle, in Durstenfeld's in-place form.
    static void fisherYates(int[] a, Random random) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);             // 0 <= j <= i: only cards not yet placed
            swap(a, i, j);
        }
    }

    // Level 2: the tempting version. It looks fair but is not.
    static void naiveShuffle(int[] a, Random random) {
        for (int i = 0; i < a.length; i++) {
            swap(a, i, random.nextInt(a.length));      // any position, even ones already settled
        }
    }

    static void swap(int[] a, int i, int j) {
        int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    // Level 3: randomized quicksort. Returns the number of comparisons it made.
    static int quicksort(int[] a, int lo, int hi, Random random) {
        if (lo >= hi) return 0;
        swap(a, lo + random.nextInt(hi - lo + 1), hi); // a random pivot, parked at the end
        int store = lo;
        for (int i = lo; i < hi; i++) {
            if (a[i] < a[hi]) swap(a, i, store++);
        }
        swap(a, store, hi);                            // the pivot lands in its final place
        return (hi - lo) + quicksort(a, lo, store - 1, random) + quicksort(a, store + 1, hi, random);
    }

    // Level 3: the exact expected number of comparisons, summed over pairs of ranks i < j.
    static double expectedComparisons(int n) {
        double sum = 0;
        for (int i = 1; i <= n; i++) {
            for (int j = i + 1; j <= n; j++) sum += 2.0 / (j - i + 1);
        }
        return sum;
    }

    // Level 4: Freivalds' check of A·B = C. A "false" is always right; a "true" may be wrong.
    static boolean freivalds(long[][] a, long[][] b, long[][] c, Random random) {
        long[] r = new long[a.length];
        for (int i = 0; i < r.length; i++) r[i] = random.nextInt(2);   // a random 0/1 vector
        return Arrays.equals(times(a, times(b, r)), times(c, r));
    }

    static long[] times(long[][] m, long[] v) {
        long[] out = new long[m.length];
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < v.length; j++) out[i] += m[i][j] * v[j];
        }
        return out;
    }

    // Interview 1: LeetCode 384, Shuffle an Array. The Random is passed in so the lab is repeatable.
    static final class ShuffleArray {
        private final int[] original;
        private final Random random;

        ShuffleArray(int[] nums, Random random) {
            original = nums.clone();
            this.random = random;
        }

        int[] reset() {
            return original.clone();
        }

        int[] shuffle() {
            int[] copy = original.clone();
            fisherYates(copy, random);
            return copy;
        }
    }

    // Interview 2: LeetCode 380, Insert Delete GetRandom O(1).
    static final class RandomizedSet {
        private final List<Integer> values = new ArrayList<>();
        private final Map<Integer, Integer> indexOf = new HashMap<>();
        private final Random random;

        RandomizedSet(Random random) {
            this.random = random;
        }

        boolean insert(int val) {
            if (indexOf.containsKey(val)) return false;
            indexOf.put(val, values.size());
            values.add(val);
            return true;
        }

        boolean remove(int val) {
            Integer hole = indexOf.remove(val);
            if (hole == null) return false;
            int last = values.remove(values.size() - 1);   // removing the end shifts nothing
            if (hole < values.size()) {                     // val was not last: move last into the hole
                values.set(hole, last);
                indexOf.put(last, hole);
            }
            return true;
        }

        int getRandom() {
            return values.get(random.nextInt(values.size()));
        }
    }

    // Interview 3: LeetCode 381, Insert Delete GetRandom O(1) - Duplicates allowed.
    static final class RandomizedCollection {
        private final List<Integer> values = new ArrayList<>();
        private final Map<Integer, Set<Integer>> positions = new HashMap<>();
        private final Random random;

        RandomizedCollection(Random random) {
            this.random = random;
        }

        boolean insert(int val) {
            Set<Integer> at = positions.computeIfAbsent(val, k -> new HashSet<>());
            at.add(values.size());
            values.add(val);
            return at.size() == 1;                          // true when val was absent before
        }

        boolean remove(int val) {
            Set<Integer> at = positions.get(val);
            if (at == null || at.isEmpty()) return false;
            int hole = at.iterator().next();
            at.remove(hole);
            int last = values.remove(values.size() - 1);
            if (hole < values.size()) {
                values.set(hole, last);
                positions.get(last).remove(values.size());  // its old index was the old end
                positions.get(last).add(hole);
            }
            return true;
        }

        int getRandom() {
            return values.get(random.nextInt(values.size()));
        }
    }

    // Interview 4: LeetCode 710, Random Pick with Blacklist.
    static final class BlacklistPicker {
        private final Map<Integer, Integer> remap = new HashMap<>();
        private final int whiteCount;
        private final Random random;

        BlacklistPicker(int n, int[] blacklist, Random random) {
            this.random = random;
            whiteCount = n - blacklist.length;
            Set<Integer> black = new HashSet<>();
            for (int b : blacklist) black.add(b);
            int spare = whiteCount;                         // scans the top part [m, n) for white numbers
            for (int b : blacklist) {
                if (b >= whiteCount) continue;              // never drawn, so it needs no remap
                while (black.contains(spare)) spare++;
                remap.put(b, spare++);
            }
        }

        int pick() {
            int x = random.nextInt(whiteCount);
            return remap.getOrDefault(x, x);
        }
    }

    // A fake Random that replays fixed choices, so every path of a shuffle can be enumerated.
    static final class ScriptedRandom extends Random {
        private final int[] script;
        private int next;

        ScriptedRandom(int[] script) {
            this.script = script;
        }

        @Override
        public int nextInt(int bound) {
            return script[next++];
        }
    }

    // Runs a shuffle of [1, 2, 3] once for every possible sequence of random choices.
    static Map<String, Integer> countAllPaths(int[] bounds, BiConsumer<int[], Random> shuffler) {
        int paths = 1;
        for (int b : bounds) paths *= b;
        Map<String, Integer> counts = new TreeMap<>();
        for (int p = 0; p < paths; p++) {
            int[] script = new int[bounds.length];
            for (int s = 0, rest = p; s < bounds.length; s++, rest /= bounds[s - 1]) script[s] = rest % bounds[s];
            int[] a = {1, 2, 3};
            shuffler.accept(a, new ScriptedRandom(script));
            counts.merge(Arrays.toString(a), 1, Integer::sum);
        }
        return counts;
    }

    static double harmonic(int n) {
        double h = 0;
        for (int i = 1; i <= n; i++) h += 1.0 / i;
        return h;
    }

    static boolean near(double actual, double expected, double tolerance) {
        return Math.abs(actual - expected) <= tolerance;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        Random random = new Random(42);
        final int runs = 100_000;

        // Level 1: half the letters are 'a', so p = 1/2.
        char[] letters = "abbaabab".toCharArray();
        check(letters[lasVegasFind(letters, random)] == 'a', "Las Vegas always returns an 'a'");
        long totalTries = 0;
        for (int r = 0; r < runs; r++) {
            int tries = 1;
            while (letters[random.nextInt(letters.length)] != 'a') tries++;
            totalTries += tries;
        }
        check(near((double) totalTries / runs, 2.0, 0.03), "Las Vegas needs 1/p = 2 tries on average");
        int failures = 0;
        for (int r = 0; r < runs; r++) if (monteCarloFind(letters, 3, random) == -1) failures++;
        check(near((double) failures / runs, 0.125, 0.005), "Monte Carlo with 3 tries fails (1/2)^3 = 0.125 of the time");
        check(near(Math.pow(0.5, 10), 0.000977, 1e-6), "(1/2)^10 = 1/1024");
        check(Math.pow(0.5, 4) == 1.0 / 16, "stopping Las Vegas after 2E[T] = 4 grabs fails 1/16 of the time");

        // Level 2: enumerate every path.
        Map<String, Integer> naive = countAllPaths(new int[] {3, 3, 3}, RandomizedLab::naiveShuffle);
        check(naive.equals(Map.of("[1, 2, 3]", 4, "[1, 3, 2]", 5, "[2, 1, 3]", 5, "[2, 3, 1]", 5, "[3, 1, 2]", 4, "[3, 2, 1]", 4)),
                "naive shuffle: 27 paths split 4/5/5/5/4/4");
        Map<String, Integer> fair = countAllPaths(new int[] {3, 2}, RandomizedLab::fisherYates);
        check(fair.size() == 6 && fair.values().stream().allMatch(c -> c == 1), "Fisher–Yates: 6 paths, one per order");
        Map<String, Integer> offByOne = countAllPaths(new int[] {2, 1}, (a, r) -> {
            for (int i = a.length - 1; i > 0; i--) swap(a, i, r.nextInt(i));
        });
        check(offByOne.keySet().equals(Set.of("[2, 3, 1]", "[3, 1, 2]")), "nextInt(i) gives only the 2 cyclic orders");

        Map<String, Integer> shuffled = new HashMap<>();
        for (int r = 0; r < 60_000; r++) {
            int[] a = {1, 2, 3};
            fisherYates(a, random);
            shuffled.merge(Arrays.toString(a), 1, Integer::sum);
        }
        check(shuffled.size() == 6 && shuffled.values().stream().allMatch(c -> near(c, 10_000, 400)),
                "each of the 6 orders appears about 60,000 / 6 = 10,000 times");

        // Level 3: expected comparisons.
        check(near(expectedComparisons(3), 2.6667, 1e-4), "n = 3: 1 + 1 + 2/3");
        check(near(expectedComparisons(4), 4.8333, 1e-4), "n = 4: 3 + 4/3 + 1/2");
        check(near(expectedComparisons(8), 16.92, 0.005), "n = 8");
        for (int n = 2; n <= 20; n++) {
            check(near(expectedComparisons(n), 2 * (n + 1) * harmonic(n) - 4 * n, 1e-9), "pair sum = 2(n+1)H_n - 4n");
        }
        long totalComparisons = 0;
        for (int r = 0; r < 20_000; r++) {
            int[] sorted = {1, 2, 3, 4, 5, 6, 7, 8};
            totalComparisons += quicksort(sorted, 0, sorted.length - 1, random);
            check(Arrays.equals(sorted, new int[] {1, 2, 3, 4, 5, 6, 7, 8}), "quicksort sorts");
        }
        check(near(totalComparisons / 20_000.0, 16.92, 0.15), "sorted input of 8 costs about 16.92 comparisons, not 28");
        check(8 * 7 / 2 == 28, "a fixed last pivot on sorted input compares every pair: 28");
        int million = 1_000_000;
        check(near(2.0 * (million + 1) * harmonic(million) - 4.0 * million, 24.79e6, 0.01e6), "n = 10^6: exact about 24.8 million");
        check(near(2.0 * million * Math.log(million), 27.63e6, 0.01e6), "n = 10^6: 2 n ln n about 27.6 million");

        // Level 4: Freivalds on 2 x 2 matrices.
        long[][] a = {{1, 2}, {3, 4}};
        long[][] b = {{5, 6}, {7, 8}};
        long[][] right = {{19, 22}, {43, 50}};
        long[][] wrong = {{19, 22}, {43, 51}};
        int fooled = 0;
        for (int r = 0; r < 10_000; r++) {
            check(freivalds(a, b, right, random), "a correct product always passes");
            if (freivalds(a, b, wrong, random)) fooled++;
        }
        check(near(fooled / 10_000.0, 0.5, 0.03), "a wrong product slips through half the time");
        check(Arrays.equals(times(a, new long[] {0, 1}), new long[] {2, 4}), "matrix times vector");

        // Interview 1: LeetCode 384.
        ShuffleArray deck = new ShuffleArray(new int[] {1, 2, 3}, random);
        int[] once = deck.shuffle();
        Arrays.sort(once);
        check(Arrays.equals(once, new int[] {1, 2, 3}), "LC 384 shuffle keeps the same values");
        check(Arrays.equals(deck.reset(), new int[] {1, 2, 3}), "LC 384 reset");

        // Interview 2: LeetCode 380.
        RandomizedSet set = new RandomizedSet(random);
        check(set.insert(1) && set.insert(2) && set.insert(3) && !set.insert(2), "LC 380 insert");
        check(set.remove(1) && !set.remove(1), "LC 380 remove");
        check(set.values.equals(List.of(3, 2)) && set.indexOf.get(3) == 0, "LC 380: 3 moved into the hole at index 0");
        int threes = 0;
        for (int r = 0; r < runs; r++) if (set.getRandom() == 3) threes++;
        check(near((double) threes / runs, 0.5, 0.01), "LC 380 getRandom is uniform over {3, 2}");
        check(set.remove(2) && set.remove(3) && set.values.isEmpty(), "LC 380 remove the last element");

        // Interview 3: LeetCode 381.
        RandomizedCollection bag = new RandomizedCollection(random);
        check(bag.insert(1) && !bag.insert(1) && bag.insert(2), "LC 381 insert returns true only for a new value");
        int ones = 0;
        for (int r = 0; r < runs; r++) if (bag.getRandom() == 1) ones++;
        check(near((double) ones / runs, 2.0 / 3, 0.01), "LC 381 P(1) = 2/3");
        check(bag.remove(1) && bag.values.size() == 2, "LC 381 remove one copy");
        check(bag.remove(1) && !bag.remove(1) && bag.values.equals(List.of(2)), "LC 381 remove the other copy");
        check(bag.positions.get(2).equals(Set.of(0)), "LC 381 index sets stay correct");

        // Interview 4: LeetCode 710.
        BlacklistPicker picker = new BlacklistPicker(7, new int[] {2, 3, 5}, random);
        check(picker.remap.equals(Map.of(2, 4, 3, 6)), "LC 710 remaps 2 -> 4 and 3 -> 6");
        Map<Integer, Integer> picks = new HashMap<>();
        for (int r = 0; r < runs; r++) picks.merge(picker.pick(), 1, Integer::sum);
        check(picks.keySet().equals(Set.of(0, 1, 4, 6)), "LC 710 picks only white numbers");
        check(picks.values().stream().allMatch(c -> near((double) c / runs, 0.25, 0.01)), "LC 710 each white number 1/4");
        check(near(7.0 / 4, 1.75, 1e-12), "rejection would need 7/4 tries");

        System.out.println("OK");
    }
}
