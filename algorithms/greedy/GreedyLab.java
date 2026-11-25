import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

// Run from this folder with: java GreedyLab.java
public class GreedyLab {

    // Level 1: the cashier's rule. Coins must be listed largest first.
    static List<Integer> greedyChange(int[] coinsLargestFirst, int amount) {
        List<Integer> used = new ArrayList<>();
        for (int coin : coinsLargestFirst) {
            while (amount >= coin) {
                used.add(coin);
                amount -= coin;
            }
        }
        return used;
    }

    // Level 5: the true optimum, by dynamic programming, to test the greedy rule against.
    static int fewestCoins(int[] coins, int amount) {
        int[] fewest = new int[amount + 1];
        Arrays.fill(fewest, amount + 1);
        fewest[0] = 0;
        for (int a = 1; a <= amount; a++) {
            for (int c : coins) if (c <= a) fewest[a] = Math.min(fewest[a], fewest[a - c] + 1);
        }
        return fewest[amount];
    }

    static int firstGreedyFailure(int[] coinsLargestFirst, int limit) {
        for (int amount = 1; amount <= limit; amount++) {
            if (greedyChange(coinsLargestFirst, amount).size() != fewestCoins(coinsLargestFirst, amount)) return amount;
        }
        return -1;                                       // greedy was optimal for every amount up to limit
    }

    // Level 2: interval scheduling. Keep the meeting that ends first, then repeat.
    record Meeting(String name, int start, int end) {}

    static List<Meeting> pickByEarliestEnd(List<Meeting> meetings) {
        List<Meeting> byEnd = new ArrayList<>(meetings);
        byEnd.sort(Comparator.comparingInt(Meeting::end));
        List<Meeting> chosen = new ArrayList<>();
        int freeFrom = Integer.MIN_VALUE;
        for (Meeting m : byEnd) {
            if (m.start() >= freeFrom) {                 // fits after the last meeting we kept
                chosen.add(m);
                freeFrom = m.end();
            }
        }
        return chosen;
    }

    // Level 2: any other greedy order, for comparison. Keeps a meeting if it overlaps none already kept.
    static List<Meeting> pickInOrder(List<Meeting> meetings, Comparator<Meeting> order) {
        List<Meeting> sorted = new ArrayList<>(meetings);
        sorted.sort(order);
        List<Meeting> chosen = new ArrayList<>();
        for (Meeting m : sorted) {
            boolean fits = chosen.stream().allMatch(c -> m.end() <= c.start() || c.end() <= m.start());
            if (fits) chosen.add(m);
        }
        return chosen;
    }

    // Level 3: fractional knapsack. Best value per kilogram first; the last item may be cut.
    record Item(String name, int weight, int value) {
        double perKg() {
            return (double) value / weight;
        }
    }

    static double fractionalKnapsack(List<Item> items, int capacity) {
        List<Item> best = new ArrayList<>(items);
        best.sort(Comparator.comparingDouble(Item::perKg).reversed());
        double total = 0;
        int room = capacity;
        for (Item it : best) {
            int take = Math.min(it.weight(), room);      // the whole item, or the slice that still fits
            total += it.perKg() * take;
            room -= take;
        }
        return total;
    }

    static int greedyZeroOneKnapsack(List<Item> items, int capacity) {
        List<Item> best = new ArrayList<>(items);
        best.sort(Comparator.comparingDouble(Item::perKg).reversed());
        int total = 0, room = capacity;
        for (Item it : best) {
            if (it.weight() <= room) {
                total += it.value();
                room -= it.weight();
            }
        }
        return total;
    }

    // Level 4: Huffman coding. Total bits = the sum of every merged weight.
    static int huffmanBits(int... frequencies) {
        PriorityQueue<Integer> heap = new PriorityQueue<>();
        for (int f : frequencies) heap.add(f);
        int bits = 0;
        while (heap.size() > 1) {
            int merged = heap.poll() + heap.poll();      // join the two rarest symbols
            bits += merged;
            heap.add(merged);
        }
        return bits;
    }

    // Interview 1: LeetCode 55, Jump Game.
    static boolean canJump(int[] nums) {
        int reach = 0;                                   // farthest index reachable so far
        for (int i = 0; i < nums.length; i++) {
            if (i > reach) return false;                 // a gap: index i can never be reached
            reach = Math.max(reach, i + nums[i]);
        }
        return true;
    }

    // Interview 2: LeetCode 435, Non-overlapping Intervals.
    static int eraseOverlapIntervals(int[][] intervals) {
        int[][] byEnd = intervals.clone();
        Arrays.sort(byEnd, Comparator.comparingInt((int[] iv) -> iv[1]));
        int kept = 0, freeFrom = Integer.MIN_VALUE;
        for (int[] iv : byEnd) {
            if (iv[0] >= freeFrom) {                     // the same rule as pickByEarliestEnd
                kept++;
                freeFrom = iv[1];
            }
        }
        return intervals.length - kept;
    }

    // Interview 3: LeetCode 135, Candy.
    static int candy(int[] ratings) {
        int n = ratings.length;
        int[] fromLeft = new int[n], fromRight = new int[n];
        Arrays.fill(fromLeft, 1);
        Arrays.fill(fromRight, 1);
        for (int i = 1; i < n; i++) if (ratings[i] > ratings[i - 1]) fromLeft[i] = fromLeft[i - 1] + 1;
        for (int i = n - 2; i >= 0; i--) if (ratings[i] > ratings[i + 1]) fromRight[i] = fromRight[i + 1] + 1;
        int total = 0;
        for (int i = 0; i < n; i++) total += Math.max(fromLeft[i], fromRight[i]); // satisfy both neighbours
        return total;
    }

    // Interview 4: LeetCode 630, Course Schedule III.
    static int scheduleCourse(int[][] courses) {
        int[][] byDeadline = courses.clone();
        Arrays.sort(byDeadline, Comparator.comparingInt((int[] c) -> c[1]));
        PriorityQueue<Integer> taken = new PriorityQueue<>(Comparator.reverseOrder()); // longest course on top
        int time = 0;
        for (int[] course : byDeadline) {
            time += course[0];
            taken.add(course[0]);
            if (time > course[1]) time -= taken.poll(); // over the deadline: drop the longest course so far
        }
        return taken.size();
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] usCoins = {25, 10, 5, 1};
        check(greedyChange(usCoins, 63).equals(List.of(25, 25, 10, 1, 1, 1)), "63 cents greedily");
        check(fewestCoins(usCoins, 63) == 6 && firstGreedyFailure(usCoins, 1000) == -1, "greedy is optimal for US coins");
        int[] oddCoins = {4, 3, 1};
        check(greedyChange(oddCoins, 6).equals(List.of(4, 1, 1)) && fewestCoins(oddCoins, 6) == 2, "6 with coins 4, 3, 1");
        check(firstGreedyFailure(oddCoins, 1000) == 6, "6 is the smallest amount where greedy fails");

        List<Meeting> day = List.of(new Meeting("A", 1, 4), new Meeting("B", 3, 5), new Meeting("C", 0, 6),
                new Meeting("D", 5, 7), new Meeting("E", 5, 9), new Meeting("F", 8, 10));
        check(pickByEarliestEnd(day).stream().map(Meeting::name).toList().equals(List.of("A", "D", "F")), "earliest end keeps A, D, F");
        check(pickInOrder(day, Comparator.comparingInt(Meeting::end)).size() == 3, "both versions agree");
        check(pickInOrder(day, Comparator.comparingInt(Meeting::start)).stream().map(Meeting::name).toList()
                .equals(List.of("C", "F")), "earliest start keeps only C, F");
        List<Meeting> trap = List.of(new Meeting("long1", 1, 5), new Meeting("short", 4, 6), new Meeting("long2", 5, 10));
        check(pickInOrder(trap, Comparator.comparingInt(m -> m.end() - m.start())).size() == 1, "shortest first keeps 1");
        check(pickByEarliestEnd(trap).size() == 2, "earliest end keeps 2");

        List<Item> items = List.of(new Item("A", 1, 1), new Item("B", 3, 4), new Item("C", 4, 5));
        check(Math.abs(fractionalKnapsack(items, 5) - 6.5) < 1e-9, "fractional knapsack: 4 + 2.5");
        check(greedyZeroOneKnapsack(items, 5) == 5, "greedy 0/1 knapsack takes B and A for 5, not the best 6");

        check(huffmanBits(5, 2, 2, 1, 1) == 23, "Huffman for abracadabra: 2 + 4 + 6 + 11 = 23 bits");
        check(5 * 1 + 2 * 3 + 2 * 3 + 1 * 3 + 1 * 3 == 23 && 11 * 3 == 33, "code lengths 1, 3, 3, 3, 3 against 3 bits each");
        check(huffmanBits(5) == 0, "a single symbol needs no merges");

        check(canJump(new int[] {2, 3, 1, 1, 4}), "LC 55 example 1");
        check(!canJump(new int[] {3, 2, 1, 0, 4}), "LC 55 example 2");
        check(canJump(new int[] {0}), "LC 55 already at the end");

        check(eraseOverlapIntervals(new int[][] {{1, 2}, {2, 3}, {3, 4}, {1, 3}}) == 1, "LC 435 example 1");
        check(eraseOverlapIntervals(new int[][] {{1, 2}, {1, 2}, {1, 2}}) == 2, "LC 435 example 2");
        check(eraseOverlapIntervals(new int[][] {{1, 2}, {2, 3}}) == 0, "LC 435 touching is fine");

        check(candy(new int[] {1, 0, 2}) == 5 && candy(new int[] {1, 2, 2}) == 4, "LC 135 examples");
        check(candy(new int[] {5, 3, 1, 4}) == 8, "LC 135: 3 + 2 + 1 + 2");

        check(scheduleCourse(new int[][] {{100, 200}, {200, 1300}, {1000, 1250}, {2000, 3200}}) == 3, "LC 630 example 1");
        check(scheduleCourse(new int[][] {{1, 2}}) == 1 && scheduleCourse(new int[][] {{3, 2}, {4, 3}}) == 0, "LC 630 small");
        check(scheduleCourse(new int[][] {{5, 5}, {4, 6}, {2, 6}}) == 2, "LC 630: swap the 5-day course for a shorter one");
        System.out.println("OK");
    }
}
