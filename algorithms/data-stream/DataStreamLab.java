import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

// Run from this folder with: java DataStreamLab.java
public class DataStreamLab {

    // Level 1: the running mean, one number of memory.
    static double[] runningMeans(double[] stream) {
        double[] means = new double[stream.length];
        double mean = 0;
        for (int n = 1; n <= stream.length; n++) {
            mean += (stream[n - 1] - mean) / n;   // nudge the old mean toward the new value
            means[n - 1] = mean;
        }
        return means;
    }

    // Level 2: the textbook variance formula, which cancels badly on large values.
    static double naiveVariance(double[] xs) {
        double sum = 0, sumOfSquares = 0;
        for (double x : xs) { sum += x; sumOfSquares += x * x; }
        double mean = sum / xs.length;
        return sumOfSquares / xs.length - mean * mean;
    }

    // Level 2: Welford's one-pass mean and variance.
    static final class RunningStats {
        long n;
        double mean, m2;   // m2 = sum of squared distances from the current mean

        void add(double x) {
            n++;
            double delta = x - mean;   // distance from the OLD mean
            mean += delta / n;
            m2 += delta * (x - mean);  // times the distance from the NEW mean
        }

        double populationVariance() { return m2 / n; }

        double sampleVariance() { return m2 / (n - 1); }
    }

    // Level 3: the mean of the last k values.
    static final class MovingAverage {
        final int k;
        final Deque<Integer> window = new ArrayDeque<>();
        long sum;

        MovingAverage(int k) { this.k = k; }

        double next(int x) {
            window.add(x);
            sum += x;
            if (window.size() > k) sum -= window.poll();   // the oldest value leaves the window
            return (double) sum / window.size();
        }
    }

    // Level 4: the k-th largest value so far, with only k values kept.
    static final class KthLargest {
        final int k;
        final PriorityQueue<Integer> topK = new PriorityQueue<>();   // min-heap of the k largest so far

        KthLargest(int k) { this.k = k; }

        int add(int x) {
            topK.add(x);
            if (topK.size() > k) topK.poll();   // the smallest of k + 1 values cannot be in the top k
            return topK.peek();
        }
    }

    // Interview 1: LeetCode 1352, Product of the Last K Numbers.
    static final class ProductOfNumbers {
        final List<Integer> prefix = new ArrayList<>(List.of(1));   // prefix products since the last zero

        void add(int x) {
            if (x == 0) { prefix.clear(); prefix.add(1); }   // every window that contains the zero is 0
            else prefix.add(prefix.get(prefix.size() - 1) * x);
        }

        int getProduct(int k) {
            int n = prefix.size();
            return k >= n ? 0 : prefix.get(n - 1) / prefix.get(n - 1 - k);
        }
    }

    // Interview 2: LeetCode 295, Find Median from Data Stream.
    static final class MedianFinder {
        final PriorityQueue<Integer> lower = new PriorityQueue<>(Comparator.reverseOrder());   // max-heap
        final PriorityQueue<Integer> upper = new PriorityQueue<>();                            // min-heap

        void addNum(int x) {
            lower.add(x);
            upper.add(lower.poll());                                    // the largest low value moves up
            if (upper.size() > lower.size()) lower.add(upper.poll());   // lower may hold one extra
        }

        double findMedian() {
            return lower.size() > upper.size() ? lower.peek() : (lower.peek() + upper.peek()) / 2.0;
        }
    }

    // Interview 3: LeetCode 352, Data Stream as Disjoint Intervals.
    static final class SummaryRanges {
        final TreeMap<Integer, Integer> intervals = new TreeMap<>();   // start -> end

        void addNum(int value) {
            int start = value, end = value;
            Map.Entry<Integer, Integer> left = intervals.floorEntry(value);
            if (left != null && left.getValue() >= value - 1) {   // the interval on the left touches or covers value
                start = left.getKey();
                end = Math.max(end, left.getValue());
            }
            Integer rightEnd = intervals.remove(end + 1);          // an interval starting just after end
            if (rightEnd != null) end = rightEnd;
            intervals.put(start, end);
        }

        int[][] getIntervals() {
            return intervals.entrySet().stream().map(e -> new int[] {e.getKey(), e.getValue()}).toArray(int[][]::new);
        }
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-4;
    }

    public static void main(String[] args) {
        double[] stream = {2, 4, 4, 4, 5, 5, 7, 9};

        // Level 1.
        double[] means = runningMeans(stream);
        check(near(means[1], 3) && near(means[2], 3.3333) && near(means[3], 3.5) && near(means[4], 3.8), "means 2..5");
        check(near(means[5], 4) && near(means[6], 4.4286) && near(means[7], 5), "means 6..8");
        check(1_000_000L * 1_000_001 / 2 == 500_000_500_000L, "recomputing from scratch costs n(n+1)/2 additions");

        // Level 2.
        RunningStats stats = new RunningStats();
        double[] m2After = {0, 2, 2.6667, 3, 4.8, 6, 13.7143, 32};
        for (int i = 0; i < stream.length; i++) {
            stats.add(stream[i]);
            check(near(stats.m2, m2After[i]), "M2 after " + (i + 1) + " values");
        }
        check(near(stats.mean, 5) && near(stats.populationVariance(), 4) && near(stats.sampleVariance(), 32.0 / 7), "Welford");
        double[] big = {1e9 + 4, 1e9 + 7, 1e9 + 13, 1e9 + 16};
        check(naiveVariance(big) == -128.0, "naive formula returns a negative variance");
        RunningStats bigStats = new RunningStats();
        for (double x : big) bigStats.add(x);
        check(bigStats.populationVariance() == 22.5, "Welford is exact here");
        check(naiveVariance(new double[] {4, 7, 13, 16}) == 22.5, "naive is fine on small values");

        // Level 3.
        MovingAverage moving = new MovingAverage(3);
        double[] averages = new double[stream.length];
        for (int i = 0; i < stream.length; i++) averages[i] = moving.next((int) stream[i]);
        check(near(averages[0], 2) && near(averages[1], 3) && near(averages[2], 3.3333) && near(averages[3], 4), "first windows");
        check(near(averages[6], 17.0 / 3) && near(averages[7], 7), "last windows: 17/3 then 21/3");

        // Level 4.
        KthLargest third = new KthLargest(3);
        int last = 0;
        for (double x : stream) last = third.add((int) x);
        check(last == 5 && third.topK.size() == 3, "3rd largest of the stream is 5, keeping 3 values");

        // Interview 1.
        ProductOfNumbers products = new ProductOfNumbers();
        for (int x : new int[] {3, 0, 2, 5, 4}) products.add(x);
        check(products.prefix.equals(List.of(1, 2, 10, 40)), "prefix products after the zero");
        check(products.getProduct(2) == 20 && products.getProduct(3) == 40 && products.getProduct(4) == 0, "LC 1352 queries");
        products.add(8);
        check(products.getProduct(2) == 32, "LC 1352 after adding 8");

        // Interview 2.
        MedianFinder median = new MedianFinder();
        double[] medians = new double[4];
        int[] values = {5, 15, 1, 3};
        for (int i = 0; i < values.length; i++) {
            median.addNum(values[i]);
            medians[i] = median.findMedian();
        }
        check(medians[0] == 5 && medians[1] == 10 && medians[2] == 5 && medians[3] == 4, "LC 295 medians 5, 10, 5, 4");
        check(median.lower.size() == 2 && median.upper.size() == 2 && median.lower.peek() == 3 && median.upper.peek() == 5,
                "heaps {1,3} and {5,15}");
        MedianFinder negatives = new MedianFinder();
        negatives.addNum(-1);
        negatives.addNum(-2);
        check(negatives.findMedian() == -1.5, "LC 295 negatives");

        // Interview 3.
        SummaryRanges ranges = new SummaryRanges();
        for (int x : new int[] {1, 3, 7, 2, 6}) ranges.addNum(x);
        int[][] result = ranges.getIntervals();
        check(result.length == 2 && result[0][0] == 1 && result[0][1] == 3 && result[1][0] == 6 && result[1][1] == 7,
                "LC 352 gives [1,3] and [6,7]");
        ranges.addNum(2);
        ranges.addNum(5);
        check(ranges.intervals.equals(Map.of(1, 3, 5, 7)), "a repeat changes nothing; 5 joins [6,7]");
        ranges.addNum(4);
        check(ranges.intervals.equals(Map.of(1, 7)), "4 bridges [1,3] and [5,7]");
        System.out.println("OK");
    }
}
