import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// Run from this folder with: java RejectionSamplingLab.java
public class RejectionSamplingLab {

    // Level 1: a number from 1 to 5 using a six-sided die.
    static int oneToFive(Random random) {
        while (true) {
            int roll = 1 + random.nextInt(6);          // propose: a fair six-sided die
            if (roll <= 5) return roll;                // accept; a 6 is thrown away
        }
    }

    // Level 2: von Neumann's trick, a fair coin from a biased one.
    static boolean fairFromBiased(double pHeads, Random random) {
        while (true) {
            boolean first = random.nextDouble() < pHeads, second = random.nextDouble() < pHeads;
            if (first != second) return first;         // HT and TH both have probability p(1 - p)
        }
    }

    // Level 3: a sample from the density f(x) = 2x on [0, 1], proposing from the uniform density.
    static double triangular(Random random) {
        while (true) {
            double x = random.nextDouble();            // proposal with density g(x) = 1
            double u = random.nextDouble();
            if (u <= x) return x;                      // accept with probability f(x) / (M g(x)) = 2x / 2
        }
    }

    // Level 4: volume of the unit ball in d dimensions, V_0 = 1, V_1 = 2, V_d = V_{d-2} * 2π / d.
    static double ballVolume(int d) {
        if (d == 0) return 1;
        if (d == 1) return 2;
        return ballVolume(d - 2) * 2 * Math.PI / d;
    }

    static double acceptanceInCube(int d) {
        return ballVolume(d) / Math.pow(2, d);         // the ball's share of the cube [-1, 1]^d
    }

    // Interview 1: LeetCode 470, Implement Rand10() Using Rand7().
    static int rand10(Random random) {
        while (true) {
            int index = 7 * (rand7(random) - 1) + rand7(random);   // uniform on 1..49
            if (index <= 40) return 1 + (index - 1) % 10;          // 40 = four full rounds of 1..10
        }
    }

    static int rand7(Random random) {
        return 1 + random.nextInt(7);
    }

    // Interview 2: LeetCode 478, Generate Random Point in a Circle.
    static final class CirclePoints {
        private final double radius, xCenter, yCenter;
        private final Random random;

        CirclePoints(double radius, double xCenter, double yCenter, Random random) {
            this.radius = radius;
            this.xCenter = xCenter;
            this.yCenter = yCenter;
            this.random = random;
        }

        double[] randPoint() {
            while (true) {
                double x = 2 * random.nextDouble() - 1, y = 2 * random.nextDouble() - 1;   // the square [-1, 1)^2
                if (x * x + y * y <= 1) return new double[] {xCenter + radius * x, yCenter + radius * y};
            }
        }
    }

    // A Random that counts how many numbers it hands out, to measure the cost of rejection.
    static final class CountingRandom extends Random {
        long calls;

        CountingRandom(long seed) {
            super(seed);
        }

        @Override
        public int nextInt(int bound) {
            calls++;
            return super.nextInt(bound);
        }

        @Override
        public double nextDouble() {
            calls++;
            return super.nextDouble();
        }
    }

    static boolean near(double actual, double expected, double tolerance) {
        return Math.abs(actual - expected) <= tolerance;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        final int runs = 100_000;

        // Level 1: p = 5/6, so E[rolls] = 6/5 = 1.2.
        CountingRandom die = new CountingRandom(1);
        Map<Integer, Integer> faces = new HashMap<>();
        for (int r = 0; r < runs; r++) faces.merge(oneToFive(die), 1, Integer::sum);
        check(faces.size() == 5 && faces.values().stream().allMatch(c -> near((double) c / runs, 0.2, 0.01)),
                "each of 1..5 comes up 1/5 of the time");
        check(near((double) die.calls / runs, 1.2, 0.01), "1.2 rolls per accepted number");
        check(near(Math.pow(1.0 / 6, 2) * 5.0 / 6, 0.0231, 1e-4), "exactly 3 rolls: (1/6)^2 * 5/6");

        // Level 2: p = 0.3, accept with 2 * 0.3 * 0.7 = 0.42.
        CountingRandom coin = new CountingRandom(2);
        int heads = 0;
        for (int r = 0; r < runs; r++) if (fairFromBiased(0.3, coin)) heads++;
        check(near((double) heads / runs, 0.5, 0.01), "the output coin is fair");
        check(near((double) coin.calls / 2 / runs, 1 / 0.42, 0.03), "about 2.38 pairs per fair flip");
        check(near(2 / 0.42, 4.76, 0.005), "about 4.76 biased flips per fair flip");

        // Level 3: f(x) = 2x, M = 2.
        CountingRandom darts = new CountingRandom(3);
        double sum = 0;
        int belowHalf = 0;
        for (int r = 0; r < runs; r++) {
            double x = triangular(darts);
            sum += x;
            if (x <= 0.5) belowHalf++;
        }
        check(near(sum / runs, 2.0 / 3, 0.005), "the mean of f(x) = 2x is 2/3");
        check(near((double) belowHalf / runs, 0.25, 0.01), "P(X <= 1/2) = 1/4");
        check(near((double) darts.calls / 2 / runs, 2.0, 0.03), "M = 2 proposals per sample");

        // Level 4: the curse of dimension.
        check(near(acceptanceInCube(2), Math.PI / 4, 1e-12) && near(acceptanceInCube(2), 0.785, 0.001), "d = 2: π/4");
        check(near(acceptanceInCube(3), 0.5236, 1e-4), "d = 3: π/6");
        check(near(acceptanceInCube(10), 0.00249, 1e-5), "d = 10: 0.00249");
        check(near(1 / acceptanceInCube(10), 401.5, 0.1), "d = 10: about 401.5 tries");

        // Interview 1: LeetCode 470.
        CountingRandom seven = new CountingRandom(4);
        Map<Integer, Integer> tens = new HashMap<>();
        for (int r = 0; r < runs; r++) tens.merge(rand10(seven), 1, Integer::sum);
        check(tens.size() == 10 && tens.values().stream().allMatch(c -> near((double) c / runs, 0.1, 0.005)),
                "LC 470: each of 1..10 has probability 1/10");
        check(near((double) seven.calls / runs, 2 * 49.0 / 40, 0.02), "LC 470: 2 * 49 / 40 = 2.45 calls");
        double recycled = (2 + 9.0 / 49 + 9.0 / 49 * 3.0 / 63) / (1 - 9.0 / 49 * 3.0 / 63 / 21);
        check(near(recycled, 2.193, 0.001), "recycling rejected values: about 2.19 calls");

        // Interview 2: LeetCode 478.
        CountingRandom plane = new CountingRandom(5);
        CirclePoints unit = new CirclePoints(1, 0, 0, plane);
        int inner = 0;
        for (int r = 0; r < runs; r++) {
            double[] p = unit.randPoint();
            double distance = Math.hypot(p[0], p[1]);
            check(distance <= 1, "LC 478: the point is inside the circle");
            if (distance <= 0.5) inner++;
        }
        check(near((double) plane.calls / 2 / runs, 4 / Math.PI, 0.01), "LC 478: 4/π = 1.27 tries per point");
        check(near(4 / Math.PI, 1.273, 0.001), "4/π");
        check(near((double) inner / runs, 0.25, 0.01), "LC 478: a quarter of the points within half the radius");
        double[] shifted = new CirclePoints(2, 3, -1, plane).randPoint();
        check(Math.hypot(shifted[0] - 3, shifted[1] + 1) <= 2, "LC 478 with radius 2 and centre (3, -1)");
        Random polar = new Random(6);
        int wrongInner = 0;
        for (int r = 0; r < runs; r++) if (polar.nextDouble() <= 0.5) wrongInner++;   // r = R * u puts half inside R/2
        check(near((double) wrongInner / runs, 0.5, 0.01), "the wrong polar method puts 1/2 within half the radius");

        System.out.println("OK");
    }
}
