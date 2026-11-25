import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Run from this folder with: java NumberTheoryLab.java
public class NumberTheoryLab {

    // Level 1: trial division. A composite n always has a divisor no bigger than sqrt(n).
    static boolean isPrime(int n) {
        if (n < 2) return false;
        for (int d = 2; (long) d * d <= n; d++) {
            if (n % d == 0) return false;                   // found a divisor no bigger than sqrt(n)
        }
        return true;
    }

    static List<Integer> primeFactors(int n) {              // the distinct primes that divide n
        List<Integer> primes = new ArrayList<>();
        for (int p = 2; (long) p * p <= n; p++) {
            if (n % p != 0) continue;
            primes.add(p);
            while (n % p == 0) n /= p;
        }
        if (n > 1) primes.add(n);                           // what is left is one prime above the square root
        return primes;
    }

    // Level 2: the sieve of Eratosthenes.
    static boolean[] sieve(int n) {
        boolean[] isPrime = new boolean[n + 1];
        for (int i = 2; i <= n; i++) isPrime[i] = true;
        for (int p = 2; (long) p * p <= n; p++) {
            if (!isPrime[p]) continue;
            for (int multiple = p * p; multiple <= n; multiple += p) isPrime[multiple] = false; // smaller multiples are already crossed out
        }
        return isPrime;
    }

    // Level 3: Euclid's algorithm.
    static long gcd(long a, long b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    static long lcm(long a, long b) {
        return a / gcd(a, b) * b;                           // divide first, so the intermediate value stays small
    }

    // Level 4: extended Euclid and the modular inverse.
    static long[] extendedGcd(long a, long b) {             // returns {g, x, y} with a*x + b*y = g
        if (b == 0) return new long[] {a, 1, 0};
        long[] next = extendedGcd(b, a % b);                // b*x' + (a % b)*y' = g
        return new long[] {next[0], next[2], next[1] - (a / b) * next[2]};
    }

    static long modInverse(long a, long m) {                // needs gcd(a, m) = 1
        long[] g = extendedGcd(a, m);
        if (g[0] != 1) throw new ArithmeticException(a + " has no inverse modulo " + m);
        return Math.floorMod(g[1], m);
    }

    // Level 5: Fermat's little theorem gives the inverse modulo a prime.
    static long power(long base, long exponent, long mod) { // fast power, from the Math lesson
        long result = 1;
        base %= mod;
        while (exponent > 0) {
            if ((exponent & 1) == 1) result = result * base % mod;
            base = base * base % mod;
            exponent >>= 1;
        }
        return result;
    }

    static long inverseModPrime(long a, long p) {           // p is prime and a is not a multiple of p
        return power(a, p - 2, p);                          // Fermat: a^(p-1) = 1, so a^(p-2) is the inverse
    }

    // Interview 1: LeetCode 204, Count Primes (primes strictly below n).
    static int countPrimes(int n) {
        if (n < 3) return 0;
        int count = 0;
        for (boolean prime : sieve(n - 1)) if (prime) count++;
        return count;
    }

    // Interview 2: LeetCode 365, Water and Jug Problem.
    static boolean canMeasureWater(int x, int y, int target) {
        return target <= x + y && target % gcd(x, y) == 0;  // Bezout: the reachable totals are the multiples of gcd(x, y)
    }

    // Interview 3: LeetCode 952, Largest Component Size by Common Factor.
    static int largestComponentSize(int[] nums) {
        int[] parent = new int[Arrays.stream(nums).max().getAsInt() + 1];
        for (int i = 0; i < parent.length; i++) parent[i] = i;
        for (int x : nums) {
            for (int p : primeFactors(x)) parent[find(parent, x)] = find(parent, p); // x joins the group of each of its primes
        }
        Map<Integer, Integer> groupSize = new HashMap<>();
        int best = 0;
        for (int x : nums) best = Math.max(best, groupSize.merge(find(parent, x), 1, Integer::sum));
        return best;
    }

    static int find(int[] parent, int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];                  // path halving keeps the trees shallow
            x = parent[x];
        }
        return x;
    }

    static int euclidSteps(long a, long b) {
        int steps = 0;
        while (b != 0) { long r = a % b; a = b; b = r; steps++; }
        return steps;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        // Level 1
        check(!isPrime(91) && 7 * 13 == 91 && isPrime(97) && !isPrime(1) && isPrime(2), "trial division");
        check(isPrime(2_147_483_647), "2^31 - 1 is prime, and (long) d * d keeps the loop safe");
        check(primeFactors(252).equals(List.of(2, 3, 7)) && 4 * 9 * 7 == 252, "252 = 2^2 * 3^2 * 7");
        check(primeFactors(105).equals(List.of(3, 5, 7)) && primeFactors(194).equals(List.of(2, 97)), "105 and 194");
        check(lcm(252, 105) == 1260 && 4 * 9 * 5 * 7 == 1260, "lcm by largest exponents");
        check(gcd(252, 105) * lcm(252, 105) == 252L * 105 && 252 * 105 == 26_460, "gcd * lcm = a * b");

        // Level 2: the sieve traced to 30
        boolean[] upTo30 = sieve(30);
        List<Integer> primes = new ArrayList<>();
        for (int i = 0; i <= 30; i++) if (upTo30[i]) primes.add(i);
        check(primes.equals(List.of(2, 3, 5, 7, 11, 13, 17, 19, 23, 29)), "primes up to 30");
        int crossings = 0;
        for (int p : List.of(2, 3, 5)) crossings += 30 / p - p + 1;
        check(crossings == 24 && 30 / 2 - 2 + 1 == 14 && 30 / 3 - 3 + 1 == 8 && 30 / 5 - 5 + 1 == 2, "24 crossings");
        check(Math.abs(30 * Math.log(Math.log(30)) - 36.7) < 0.1, "n ln ln n for n = 30");
        check(Math.abs(Math.log(Math.log(1e6)) - 2.626) < 1e-3, "ln ln 10^6");
        check(sieve(1).length == 2 && !sieve(1)[1] && sieve(0).length == 1, "tiny sieves");

        // Level 3
        check(gcd(252, 105) == 21 && 252 % 105 == 42 && 105 % 42 == 21 && 42 % 21 == 0, "gcd(252, 105) = 21");
        check(gcd(7, 0) == 7 && gcd(0, 7) == 7 && gcd(17, 5) == 1, "gcd edges");
        check(euclidSteps(252, 105) == 3 && euclidSteps(89, 55) == 9 && gcd(89, 55) == 1, "Euclid's step counts");

        // Level 4
        check(Arrays.equals(extendedGcd(252, 105), new long[] {21, -2, 5}) && 252 * -2 + 105 * 5 == 21, "Bezout for 252, 105");
        check(Arrays.equals(extendedGcd(3, 7), new long[] {1, -2, 1}), "Bezout for 3, 7");
        check(modInverse(3, 7) == 5 && 3 * 5 % 7 == 1, "inverse of 3 mod 7");
        boolean threw = false;
        try { modInverse(4, 8); } catch (ArithmeticException e) { threw = true; }
        check(threw, "4 has no inverse modulo 8");

        // Level 5
        long mod = 1_000_000_007;
        check(power(3, 6, 7) == 1 && 729 == 104 * 7 + 1 && inverseModPrime(3, 7) == 5 && 243 % 7 == 5, "Fermat with p = 7");
        check(inverseModPrime(2, mod) == 500_000_004 && inverseModPrime(3, mod) == 333_333_336, "inverses mod 1e9+7");
        check(10 * inverseModPrime(2, mod) % mod == 5 && 5_000_000_040L - 5 * mod == 5, "10 / 2 under the modulus");
        check(modInverse(3, mod) == inverseModPrime(3, mod), "both inverse methods agree");

        // Interview problems
        check(countPrimes(10) == 4 && countPrimes(0) == 0 && countPrimes(1) == 0 && countPrimes(3) == 1, "LC 204");
        check(countPrimes(5_000_000) == 348_513, "LC 204 at the largest input");
        check(canMeasureWater(3, 5, 4) && !canMeasureWater(2, 6, 5) && canMeasureWater(1, 2, 3), "LC 365");
        check(3 * 3 + 5 * -1 == 4 && !canMeasureWater(3, 5, 9), "LC 365 Bezout and the capacity limit");
        check(largestComponentSize(new int[] {4, 6, 15, 35}) == 4, "LC 952 example 1");
        check(largestComponentSize(new int[] {20, 50, 9, 63}) == 2, "LC 952 example 2");
        check(largestComponentSize(new int[] {2, 3, 6, 7, 4, 12, 21, 39}) == 8, "LC 952 example 3");
        check(largestComponentSize(new int[] {1}) == 1, "LC 952 single 1");
        check(2 * 3 * 5 * 7 * 11 * 13 == 30_030 && 30_030 * 17 > 100_000, "at most 6 distinct primes below 10^5");
        System.out.println("OK");
    }
}
