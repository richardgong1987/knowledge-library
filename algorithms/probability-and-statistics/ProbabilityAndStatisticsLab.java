import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// Run from this folder with: java ProbabilityAndStatisticsLab.java
public class ProbabilityAndStatisticsLab {

    // Level 1: count favourable outcomes in a sample space of equally likely outcomes.
    static double probabilityOfSum(int target) {            // two fair dice: 36 equally likely outcomes
        int favourable = 0;
        for (int a = 1; a <= 6; a++)
            for (int b = 1; b <= 6; b++) if (a + b == target) favourable++;
        return favourable / 36.0;
    }

    // Level 2: Bayes' rule.
    static double bayes(double prior, double truePositiveRate, double falsePositiveRate) {
        double positive = truePositiveRate * prior + falsePositiveRate * (1 - prior); // every way to test positive
        return truePositiveRate * prior / positive;
    }

    // Level 3: linearity of expectation, checked by simulation.
    static double averageFixedPoints(int n, int trials, Random random) {
        List<Integer> cards = new ArrayList<>();
        for (int i = 0; i < n; i++) cards.add(i);
        int total = 0;
        for (int t = 0; t < trials; t++) {
            Collections.shuffle(cards, random);
            for (int i = 0; i < n; i++) if (cards.get(i) == i) total++; // child i got their own card back
        }
        return (double) total / trials;
    }

    // Level 4: mean and variance of equally likely values.
    static double[] meanAndVariance(double[] values) {
        double sum = 0, sumOfSquares = 0;
        for (double v : values) { sum += v; sumOfSquares += v * v; }
        double mean = sum / values.length;
        return new double[] {mean, sumOfSquares / values.length - mean * mean}; // E[X^2] - (E[X])^2
    }

    // Level 5: the coupon collector, a sum of geometric waiting times.
    static double expectedRollsToSeeAllFaces(int faces) {
        double expected = 0;
        for (int seen = 0; seen < faces; seen++) expected += (double) faces / (faces - seen); // a new face has chance (faces - seen) / faces
        return expected;
    }

    // Interview 1: LeetCode 528, Random Pick with Weight.
    static final class WeightedPicker {
        final int[] prefix;                                 // prefix[i] = w[0] + ... + w[i]
        final Random random;

        WeightedPicker(int[] w, Random random) {
            prefix = new int[w.length];
            for (int i = 0; i < w.length; i++) prefix[i] = w[i] + (i > 0 ? prefix[i - 1] : 0);
            this.random = random;
        }

        int pickIndex() {
            return indexForTicket(random.nextInt(prefix[prefix.length - 1]) + 1); // a ticket from 1 to W
        }

        int indexForTicket(int ticket) {                    // the first index whose prefix sum reaches the ticket
            int low = 0, high = prefix.length - 1;
            while (low < high) {
                int mid = (low + high) / 2;
                if (prefix[mid] >= ticket) high = mid; else low = mid + 1;
            }
            return low;
        }
    }

    // Interview 2: LeetCode 837, New 21 Game.
    static double new21Game(int n, int k, int maxPts) {
        if (k == 0 || n >= k - 1 + maxPts) return 1.0;      // she never draws, or can never pass n
        double[] reach = new double[n + 1];                 // reach[s] = probability that the score is ever exactly s
        reach[0] = 1;
        double window = 1, answer = 0;                      // window = sum of reach[t] for scores t that still draw and can reach s
        for (int s = 1; s <= n; s++) {
            reach[s] = window / maxPts;
            if (s < k) window += reach[s]; else answer += reach[s];  // below k she draws again; from k on she stops
            if (s - maxPts >= 0 && s - maxPts < k) window -= reach[s - maxPts]; // too far back to reach s + 1
        }
        return answer;
    }

    // Interview 3: LeetCode 688, Knight Probability in Chessboard.
    static double knightProbability(int n, int k, int row, int column) {
        int[][] moves = {{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}};
        double[][] chance = new double[n][n];               // chance[r][c] = probability of standing on (r, c) now
        chance[row][column] = 1;
        for (int step = 0; step < k; step++) {
            double[][] next = new double[n][n];
            for (int r = 0; r < n; r++)
                for (int c = 0; c < n; c++)
                    for (int[] m : moves) {
                        int nr = r + m[0], nc = c + m[1];
                        if (nr >= 0 && nr < n && nc >= 0 && nc < n) next[nr][nc] += chance[r][c] / 8; // each move has chance 1/8
                    }
            chance = next;
        }
        return Arrays.stream(chance).flatMapToDouble(Arrays::stream).sum();
    }

    // Interview 4: LeetCode 1467, Probability of a Two Boxes Having The Same Number of Distinct Balls.
    static double getProbability(int[] balls) {
        int half = Arrays.stream(balls).sum() / 2;
        return favourableSplits(balls, 0, half, 0) / choose(2 * half, half);
    }

    // Weighted count of ways to put `left` more balls into box 1 from colours i, i+1, ...
    // balance = (colours seen in box 1) - (colours seen in box 2) so far.
    static double favourableSplits(int[] balls, int i, int left, int balance) {
        if (i == balls.length) return left == 0 && balance == 0 ? 1 : 0;
        double ways = 0;
        for (int take = 0; take <= Math.min(balls[i], left); take++) {
            int change = (take > 0 ? 1 : 0) - (take < balls[i] ? 1 : 0); // is this colour in box 1? in box 2?
            ways += choose(balls[i], take) * favourableSplits(balls, i + 1, left - take, balance + change);
        }
        return ways;
    }

    static long choose(int n, int k) {                      // from the Combinatorics lesson
        if (k < 0 || k > n) return 0;
        k = Math.min(k, n - k);
        long ways = 1;
        for (int i = 1; i <= k; i++) ways = ways * (n - k + i) / i;
        return ways;
    }

    static double logFactorial(int n) {                 // ln(n!)
        double total = 0;
        for (int i = 2; i <= n; i++) total += Math.log(i);
        return total;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    static boolean near(double a, double b, double tolerance) {
        return Math.abs(a - b) <= tolerance;
    }

    public static void main(String[] args) {
        Random random = new Random(2024);

        // Level 1
        check(near(probabilityOfSum(7), 6 / 36.0, 1e-12) && near(probabilityOfSum(2), 1 / 36.0, 1e-12), "sums of two dice");
        double atLeastTen = probabilityOfSum(10) + probabilityOfSum(11) + probabilityOfSum(12);
        check(near(atLeastTen, 6 / 36.0, 1e-12), "sum at least 10");
        check(near(1 - Math.pow(5 / 6.0, 2), 11 / 36.0, 1e-12), "at least one six in two rolls");
        double all = 0;
        for (int s = 2; s <= 12; s++) all += probabilityOfSum(s);
        check(near(all, 1, 1e-12), "probabilities add to 1");

        // Level 2
        check(near((3 / 36.0) / (6 / 36.0), 0.5, 1e-12), "P(sum >= 10 given first die is 6)");
        check(near(bayes(0.01, 0.9, 0.05), 90.0 / 585, 1e-12) && near(90.0 / 585, 0.1538, 1e-4), "Bayes: about 15%");
        check(near(0.9 * 0.01 + 0.05 * 0.99, 0.0585, 1e-12), "P(positive)");
        check(10_000 / 100 == 100 && 100 * 9 / 10 == 90 && 9_900 * 5 / 100 == 495 && 90 + 495 == 585, "the 10,000-person count");
        check(near(bayes(0.5, 0.9, 0.05), 0.9473, 1e-4), "a higher prior changes everything");

        // Level 3
        check(near(meanAndVariance(new double[] {1, 2, 3, 4, 5, 6})[0], 3.5, 1e-12), "E[one die] = 3.5");
        int[] fixedCounts = {3, 1, 1, 0, 0, 1};             // fixed points of 123, 132, 213, 231, 312, 321
        check(Arrays.stream(fixedCounts).sum() == 6, "3! permutations of 3 have 6 fixed points in total");
        check(near(Math.exp(logFactorial(30)), 2.65e32, 0.01e32), "30! is about 2.65 x 10^32");
        check(near(averageFixedPoints(30, 200_000, random), 1.0, 0.01), "30 children: on average 1 gets their own card");
        check(near(averageFixedPoints(3, 200_000, random), 1.0, 0.01), "3 children");

        // Level 4
        double[] die = meanAndVariance(new double[] {1, 2, 3, 4, 5, 6});
        check(near(die[1], 35.0 / 12, 1e-12) && near(Math.sqrt(die[1]), 1.708, 1e-3) && near(91.0 / 6, 15.1667, 1e-4), "Var(die) = 35/12");
        double[] twoDice = new double[36];
        for (int a = 1; a <= 6; a++) for (int b = 1; b <= 6; b++) twoDice[(a - 1) * 6 + b - 1] = a + b;
        check(near(meanAndVariance(twoDice)[0], 7, 1e-12) && near(meanAndVariance(twoDice)[1], 35.0 / 6, 1e-12), "two dice: mean 7, variance 35/6");
        double[] data = meanAndVariance(new double[] {2, 4, 4, 4, 5, 5, 7, 9});
        check(near(data[0], 5, 1e-12) && near(data[1], 4, 1e-12), "mean 5, variance 4");
        check(near(32.0 / 7, 4.571, 1e-3), "sample variance divides by n - 1");
        check(near(Math.sqrt(35.0 / 12) / Math.sqrt(10_000), 0.0171, 1e-4), "standard error of 10,000 rolls");
        double sum = 0;
        for (int i = 0; i < 10_000; i++) sum += random.nextInt(6) + 1;
        check(near(sum / 10_000, 3.5, 5 * 0.0171), "sample mean of 10,000 rolls is within 5 standard errors");

        // Level 5
        check(near(1 / (1 / 6.0), 6, 1e-12) && near(expectedRollsToSeeAllFaces(6), 14.7, 1e-12), "geometric and coupon collector");
        int totalRolls = 0;
        for (int t = 0; t < 100_000; t++) { int rolls = 1; while (random.nextInt(6) != 5) rolls++; totalRolls += rolls; }
        check(near(totalRolls / 100_000.0, 6, 0.05), "simulated wait for a six");
        check(near(Math.pow(5 / 6.0, 2) * (1 / 6.0), 25 / 216.0, 1e-12), "first six on roll 3");

        // Interview 1
        WeightedPicker picker = new WeightedPicker(new int[] {1, 3}, random);
        check(picker.indexForTicket(1) == 0 && picker.indexForTicket(2) == 1 && picker.indexForTicket(4) == 1, "tickets");
        int ones = 0;
        for (int t = 0; t < 100_000; t++) ones += picker.pickIndex();
        check(near(ones / 100_000.0, 0.75, 0.01), "index 1 is picked 3/4 of the time");
        WeightedPicker five = new WeightedPicker(new int[] {2, 5, 3}, random);
        check(five.indexForTicket(2) == 0 && five.indexForTicket(3) == 1 && five.indexForTicket(7) == 1 && five.indexForTicket(8) == 2, "prefix 2, 7, 10");

        // Interview 2
        check(near(new21Game(10, 1, 10), 1.0, 1e-5) && near(new21Game(6, 1, 10), 0.6, 1e-5), "LC 837 examples 1 and 2");
        check(near(new21Game(21, 17, 10), 0.73278, 1e-5), "LC 837 example 3");
        check(near(new21Game(2, 2, 2), 0.75, 1e-12) && near(new21Game(0, 0, 1), 1.0, 1e-12), "LC 837 small cases");

        // Interview 3
        check(near(knightProbability(3, 2, 0, 0), 0.0625, 1e-12) && near(knightProbability(1, 0, 0, 0), 1.0, 1e-12), "LC 688");
        check(near(knightProbability(3, 1, 0, 0), 0.25, 1e-12), "two of eight moves stay on a 3 x 3 board");

        // Interview 4
        check(near(getProbability(new int[] {1, 1}), 1.0, 1e-5) && near(getProbability(new int[] {2, 1, 1}), 4 / 6.0, 1e-5), "LC 1467");
        check(near(getProbability(new int[] {1, 2, 1, 2}), 0.6, 1e-5) && choose(4, 2) == 6, "LC 1467 example 3");
        check(near(favourableSplits(new int[] {2, 1, 1}, 0, 2, 0), 4, 1e-12), "4 of the 6 weighted splits are favourable");
        System.out.println("OK");
    }
}
