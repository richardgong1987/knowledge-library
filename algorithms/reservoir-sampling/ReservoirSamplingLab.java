import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// Run from this folder with: java ReservoirSamplingLab.java
public class ReservoirSamplingLab {

    // Level 1: keep one item from a stream, replacing it with the i-th item with probability 1/i.
    static int pickOne(int[] stream, Random random) {
        int kept = 0;
        for (int i = 1; i <= stream.length; i++) {
            if (random.nextInt(i) == 0) kept = stream[i - 1];   // true with probability 1/i
        }
        return kept;
    }

    // Level 2: Algorithm R, a uniform sample of k items.
    static int[] algorithmR(int[] stream, int k, Random random) {
        int[] reservoir = Arrays.copyOf(stream, k);             // the first k items fill the reservoir
        for (int i = k + 1; i <= stream.length; i++) {
            int j = random.nextInt(i);                           // uniform in 0..i-1
            if (j < k) reservoir[j] = stream[i - 1];             // happens with probability k/i
        }
        return reservoir;
    }

    // Level 3: how many times Algorithm R writes into the reservoir after it is full, on average.
    static double expectedReplacements(int n, int k) {
        double sum = 0;
        for (int i = k + 1; i <= n; i++) sum += (double) k / i;
        return sum;
    }

    // Level 4: the Efraimidis–Spirakis key. Keeping the largest keys gives a weighted sample.
    static double weightedKey(double weight, Random random) {
        return Math.pow(random.nextDouble(), 1.0 / weight);
    }

    // Interview 1: LeetCode 382, Linked List Random Node.
    static final class ListNode {
        final int val;
        final ListNode next;

        ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }
    }

    static final class LinkedListRandom {
        private final ListNode head;
        private final Random random;

        LinkedListRandom(ListNode head, Random random) {
            this.head = head;
            this.random = random;
        }

        int getRandom() {
            int chosen = 0, seen = 0;
            for (ListNode node = head; node != null; node = node.next) {
                seen++;
                if (random.nextInt(seen) == 0) chosen = node.val;   // keep this node with probability 1/seen
            }
            return chosen;
        }
    }

    // Interview 2: LeetCode 398, Random Pick Index.
    static int pickIndex(int[] nums, int target, Random random) {
        int chosen = -1, matches = 0;
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] != target) continue;
            matches++;
            if (random.nextInt(matches) == 0) chosen = i;       // the c-th match wins with probability 1/c
        }
        return chosen;
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
        Random random = new Random(7);
        final int runs = 100_000;

        // Level 1: k = 1, n = 4.
        check(near(1.0 / 2 * 2.0 / 3 * 3.0 / 4, 0.25, 1e-12), "item 2 survives: 1/2 * 2/3 * 3/4 = 1/4");
        Map<Integer, Integer> one = new HashMap<>();
        for (int r = 0; r < runs; r++) one.merge(pickOne(new int[] {10, 20, 30, 40}, random), 1, Integer::sum);
        check(one.size() == 4 && one.values().stream().allMatch(c -> near((double) c / runs, 0.25, 0.01)),
                "each of 4 items is kept about 1/4 of the time");

        // Level 2: k = 2, n = 5.
        check(near(2.0 / 3 * 3.0 / 4 * 4.0 / 5, 0.4, 1e-12), "item 3 survives: 2/3 * 3/4 * 4/5 = 2/5");
        Map<Integer, Integer> two = new HashMap<>();
        for (int r = 0; r < runs; r++) {
            for (int x : algorithmR(new int[] {1, 2, 3, 4, 5}, 2, random)) two.merge(x, 1, Integer::sum);
        }
        check(two.size() == 5 && two.values().stream().allMatch(c -> near((double) c / runs, 0.4, 0.01)),
                "each of 5 items is in the sample about 2/5 of the time");
        check(Arrays.equals(algorithmR(new int[] {1, 2}, 2, random), new int[] {1, 2}), "n = k keeps everything");

        // Level 3: replacements.
        check(near(expectedReplacements(4, 1), 1.0833, 1e-4), "k = 1, n = 4: 1/2 + 1/3 + 1/4");
        long writes = 0;
        for (int r = 0; r < runs; r++) {
            for (int i = 2; i <= 4; i++) if (random.nextInt(i) < 1) writes++;
        }
        check(near((double) writes / runs, 1.0833, 0.01), "simulated replacements for k = 1, n = 4");
        check(near(expectedReplacements(1_000_000, 10), 114.6, 0.05), "k = 10, n = 10^6: about 114.6 writes");
        check(near(10 * (harmonic(1_000_000) - harmonic(10)), 114.6, 0.05), "the same as 10 (H_n - H_k)");
        check(near(10 * Math.log(1_000_000 / 10.0), 115.1, 0.05), "k ln(n / k) = 115.1");

        // Level 4: weights 1 and 3, sample size 1.
        int heavyWins = 0;
        for (int r = 0; r < runs; r++) if (weightedKey(3, random) > weightedKey(1, random)) heavyWins++;
        check(near((double) heavyWins / runs, 0.75, 0.01), "the weight-3 item wins 3/4 of the time");

        int productWins = 0;
        for (int r = 0; r < runs; r++) if (3 * random.nextDouble() > random.nextDouble()) productWins++;
        check(near((double) productWins / runs, 5.0 / 6, 0.01), "the wrong key u * w gives the heavy item 5/6");

        // Common mistake: evicting the oldest resident. k = 2, n = 4: item 1 survives only if items 3 and 4 stay out.
        int firstSurvives = 0;
        for (int r = 0; r < runs; r++) {
            boolean thirdEnters = random.nextInt(3) < 2, fourthEnters = random.nextInt(4) < 2;
            if (!thirdEnters && !fourthEnters) firstSurvives++;
        }
        check(near((double) firstSurvives / runs, 1.0 / 6, 0.01), "FIFO eviction keeps item 1 only 1/6 of the time");

        // Interview 1: LeetCode 382 on the list 1 -> 2 -> 3.
        LinkedListRandom list = new LinkedListRandom(new ListNode(1, new ListNode(2, new ListNode(3, null))), random);
        Map<Integer, Integer> nodes = new HashMap<>();
        for (int r = 0; r < runs; r++) nodes.merge(list.getRandom(), 1, Integer::sum);
        check(nodes.size() == 3 && nodes.values().stream().allMatch(c -> near((double) c / runs, 1.0 / 3, 0.01)),
                "LC 382: each node 1/3");
        check(new LinkedListRandom(new ListNode(5, null), random).getRandom() == 5, "LC 382 single node");

        // Interview 2: LeetCode 398.
        int[] nums = {1, 2, 3, 3, 3};
        Map<Integer, Integer> picks = new HashMap<>();
        for (int r = 0; r < runs; r++) picks.merge(pickIndex(nums, 3, random), 1, Integer::sum);
        check(picks.keySet().equals(java.util.Set.of(2, 3, 4)), "LC 398 returns only indices of 3");
        check(picks.values().stream().allMatch(c -> near((double) c / runs, 1.0 / 3, 0.01)), "LC 398: each index 1/3");
        check(pickIndex(nums, 1, random) == 0, "LC 398 single match");

        System.out.println("OK");
    }
}
