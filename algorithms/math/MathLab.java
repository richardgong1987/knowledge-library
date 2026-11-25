// Run from this folder with: java MathLab.java
public class MathLab {

    // Level 1: LeetCode 7, Reverse Integer. The checked methods throw instead of wrapping around.
    static int reverse(int x) {
        int reversed = 0;
        while (x != 0) {
            try {
                reversed = Math.addExact(Math.multiplyExact(reversed, 10), x % 10);
            } catch (ArithmeticException overflow) {
                return 0;                                   // LeetCode 7 asks for 0 when the answer does not fit
            }
            x /= 10;
        }
        return reversed;
    }

    // Level 2: reduce after every step, so no intermediate value can overflow.
    static final long MOD = 1_000_000_007;

    static long mulMod(long a, long b) {
        return a % MOD * (b % MOD) % MOD;                   // both factors are below 1e9+7, so the product is below 2^63
    }

    // Level 3: fast exponentiation, one squaring per bit of the exponent.
    static long power(long base, long exponent, long mod) {
        long result = 1;
        base %= mod;
        while (exponent > 0) {
            if ((exponent & 1) == 1) result = result * base % mod; // this bit of the exponent is 1
            base = base * base % mod;                               // base^1, base^2, base^4, base^8, ...
            exponent >>= 1;
        }
        return result;
    }

    // Level 4: integer square root by Newton's method.
    static long isqrtNewton(long x) {
        if (x < 2) return x;
        long r = x;
        while (r * r > x) r = (r + x / r) / 2;              // each step lands closer to the root, never below it
        return r;
    }

    // Level 5: floor division and a remainder that is never negative.
    static int dayOfWeek(int dayNumber) {                   // day 0 is a Monday; negative days are in the past
        return Math.floorMod(dayNumber, 7);
    }

    // Interview 1: LeetCode 69, Sqrt(x), by binary search on the answer.
    static int mySqrt(int x) {
        long low = 0, high = x;
        while (low < high) {
            long mid = (low + high + 1) / 2;                // round up, so low = mid always makes progress
            if (mid * mid <= x) low = mid; else high = mid - 1;
        }
        return (int) low;
    }

    // Interview 2: LeetCode 43, Multiply Strings. Column k collects every digit pair with i + j = k.
    static String multiply(String num1, String num2) {
        int[] column = new int[num1.length() + num2.length()];
        for (int i = 0; i < num1.length(); i++)
            for (int j = 0; j < num2.length(); j++)
                column[i + j] += digitFromRight(num1, i) * digitFromRight(num2, j);
        StringBuilder digits = new StringBuilder();
        int carry = 0;
        for (int sum : column) {
            digits.append((sum + carry) % 10);
            carry = (sum + carry) / 10;
        }
        while (digits.length() > 1 && digits.charAt(digits.length() - 1) == '0') digits.deleteCharAt(digits.length() - 1);
        return digits.reverse().toString();
    }

    static int digitFromRight(String number, int i) {
        return number.charAt(number.length() - 1 - i) - '0';
    }

    // Interview 3: LeetCode 829, Consecutive Numbers Sum. n = k*x + k(k-1)/2 with x >= 1.
    static int consecutiveNumbersSum(int n) {
        int ways = 0;
        for (long k = 1; k * (k - 1) / 2 < n; k++) {
            if ((n - k * (k - 1) / 2) % k == 0) ways++;      // the first term x is a whole number
        }
        return ways;
    }

    // Interview 4: LeetCode 233, Number of Digit One. Count the 1s place by place.
    static int countDigitOne(int n) {
        long ones = 0;
        for (long p = 1; p <= n; p *= 10) {                 // long: for n near 10^9, p * 10 passes the int limit
            long high = n / (p * 10), digit = n / p % 10, low = n % p;
            ones += high * p;                               // full cycles of 0..9 at this place
            if (digit == 1) ones += low + 1;
            else if (digit > 1) ones += p;
        }
        return (int) ones;
    }

    static int countDigitOneSlowly(int n) {
        int ones = 0;
        for (int i = 1; i <= n; i++) for (char c : String.valueOf(i).toCharArray()) if (c == '1') ones++;
        return ones;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        // Level 1: overflow wraps around by 2^32.
        int big = 2_000_000_000;
        check(big + big == -294_967_296, "2e9 + 2e9 wraps to -294,967,296");
        check(4_000_000_000L - 4_294_967_296L == -294_967_296L, "wrapped value = true value - 2^32");
        check(Integer.MAX_VALUE == 2_147_483_647 && Integer.MAX_VALUE + 1 == Integer.MIN_VALUE, "int limits");
        check(Math.abs(Integer.MIN_VALUE) < 0, "abs(MIN_VALUE) is still negative");
        boolean threw = false;
        try { Math.addExact(big, big); } catch (ArithmeticException e) { threw = true; }
        check(threw, "addExact throws on overflow");
        int low = 1_500_000_000, high = 2_000_000_000;
        check((low + high) / 2 < 0 && low + (high - low) / 2 == 1_750_000_000, "the binary-search midpoint bug");
        check(reverse(123) == 321 && reverse(-123) == -321 && reverse(120) == 21, "LC 7 examples");
        check(reverse(1_534_236_469) == 0 && 9_646_324_351L > Integer.MAX_VALUE, "LC 7 overflow returns 0");
        check(reverse(-2_147_483_648) == 0 && reverse(0) == 0, "LC 7 edges");

        // Level 2: modular rules with m = 7, then with 1e9+7.
        check((15 + 20) % 7 == 0 && (15 % 7 + 20 % 7) % 7 == 0, "sum rule");
        check(15 * 20 % 7 == 6 && (15 % 7) * (20 % 7) % 7 == 6, "product rule");
        check((3 - 5) % 7 == -2 && Math.floorMod(3 - 5, 7) == 5, "difference needs floorMod");
        check((MOD - 1) * (MOD - 1) > 0 && (MOD - 1) * (MOD - 1) == 1_000_000_012_000_000_036L, "product of two remainders fits in long");
        check(mulMod(1_000_000_000_000L, 1_000_000_000_000L) == power(1_000_000_000_000L % MOD, 2, MOD), "mulMod reduces first");
        check(mulMod(123_456_789, 987_654_321) == 259_106_859, "mulMod example");
        check(123_456_789L * 987_654_321L == 121_932_631_112_635_269L, "the exact product");

        // Level 3: 3^13 with 13 = 1101 in binary.
        check(Integer.toBinaryString(13).equals("1101"), "13 in binary");
        check(power(3, 13, 1_000_000_000) == 1_594_323 && 6561L * 81 * 3 == 1_594_323, "3^13 = 3^8 * 3^4 * 3^1");
        check(power(3, 13, 1000) == 323, "3^13 mod 1000");
        check(power(2, 10, MOD) == 1024 && power(5, 0, 7) == 1, "small powers");
        check(power(2, 30, MOD) == 73_741_817, "2^30 mod 1e9+7");
        check(64 - Long.numberOfLeadingZeros(1_000_000_000L) == 30, "10^9 has 30 bits");

        // Level 4: Newton from r = 8 goes 8, 4, 3, 2.
        check(isqrtNewton(8) == 2 && isqrtNewton(9) == 3 && isqrtNewton(0) == 0 && isqrtNewton(1) == 1, "Newton small");
        check(isqrtNewton(2_147_395_600L) == 46_340 && 46_340L * 46_340 == 2_147_395_600L, "46,340 squared");
        check(isqrtNewton(Integer.MAX_VALUE) == 46_340 && 46_341 * 46_341 < 0, "46,341 squared overflows int");
        for (int x = 0; x <= 10_000; x++) check(isqrtNewton(x) == mySqrt(x), "Newton agrees with binary search at " + x);

        // Level 5: truncation versus floor.
        check(-7 / 2 == -3 && -7 % 2 == -1, "Java truncates toward zero");
        check(Math.floorDiv(-7, 2) == -4 && Math.floorMod(-7, 2) == 1, "floor division");
        check(2 * -4 + 1 == -7 && 2 * -3 + -1 == -7, "both satisfy a = b*q + r");
        check(dayOfWeek(-1) == 6 && -1 % 7 == -1 && dayOfWeek(10) == 3, "day numbers");

        // Interview problems.
        check(mySqrt(4) == 2 && mySqrt(8) == 2 && mySqrt(0) == 0 && mySqrt(1) == 1, "LC 69");
        check(mySqrt(Integer.MAX_VALUE) == 46_340, "LC 69 at the int limit");
        check(multiply("123", "456").equals("56088") && 123 * 456 == 56_088, "LC 43");
        check(multiply("2", "3").equals("6") && multiply("0", "52").equals("0") && multiply("99", "99").equals("9801"), "LC 43 edges");
        check(consecutiveNumbersSum(5) == 2 && consecutiveNumbersSum(9) == 3 && consecutiveNumbersSum(15) == 4, "LC 829");
        check(consecutiveNumbersSum(1) == 1 && consecutiveNumbersSum(16) == 1, "LC 829 powers of two have one way");
        check(countDigitOne(13) == 6 && countDigitOne(0) == 0, "LC 233");
        check(countDigitOne(213) == 146 && countDigitOneSlowly(213) == 146, "LC 233 for 213");
        for (int n = 0; n <= 3000; n++) check(countDigitOne(n) == countDigitOneSlowly(n), "LC 233 brute force at " + n);
        check(countDigitOne(1_000_000_000) == 900_000_001, "LC 233 at 10^9");
        System.out.println("OK");
    }
}
