import java.util.Arrays;

// Run from this folder with: java BitManipulationLab.java
public class BitManipulationLab {

    // Level 1: bit k of x is the digit in the 2^k column.
    static int bit(int x, int k) {
        return (x >> k) & 1;
    }

    // Level 2: two's complement. Flip every bit, then add one.
    static int negate(int x) {
        return ~x + 1;
    }

    // Level 3: one mask, four jobs.
    static boolean isSet(int x, int k) { return ((x >> k) & 1) == 1; }
    static int set(int x, int k) { return x | (1 << k); }
    static int clear(int x, int k) { return x & ~(1 << k); }
    static int toggle(int x, int k) { return x ^ (1 << k); }

    // Level 4: the lowest set bit.
    static int clearLowest(int x) {
        return x & (x - 1);
    }

    static int lowestBit(int x) {
        return x & -x;
    }

    static boolean isPowerOfTwo(int x) {
        return x > 0 && (x & (x - 1)) == 0;   // exactly one 1-bit
    }

    // Level 4 and LeetCode 191, Number of 1 Bits: one loop turn per 1-bit.
    static int countOnes(int x) {
        int count = 0;
        while (x != 0) {
            x &= x - 1;   // remove the lowest 1-bit
            count++;
        }
        return count;
    }

    // Level 6: the reflected binary (Gray) code.
    static int gray(int i) {
        return i ^ (i >> 1);
    }

    // Interview 1: LeetCode 136, Single Number.
    static int singleNumber(int[] nums) {
        int result = 0;
        for (int x : nums) result ^= x;   // pairs cancel: a ^ a = 0
        return result;
    }

    // Interview 2: LeetCode 338, Counting Bits.
    static int[] countBits(int n) {
        int[] ones = new int[n + 1];
        for (int i = 1; i <= n; i++) ones[i] = ones[i >> 1] + (i & 1);
        return ones;
    }

    // Interview 3: LeetCode 371, Sum of Two Integers.
    static int getSum(int a, int b) {
        while (b != 0) {
            int carry = (a & b) << 1;   // columns where both bits are 1 carry into the next column
            a ^= b;                     // add without carrying
            b = carry;
        }
        return a;
    }

    // Interview 4: LeetCode 1611, Minimum One Bit Operations to Make Integers Zero.
    static int minimumOneBitOperations(int n) {
        int steps = 0;
        for (; n != 0; n >>= 1) steps ^= n;   // inverse Gray code: n ^ (n >> 1) ^ (n >> 2) ^ ...
        return steps;
    }

    // Breadth-first search over the two allowed moves; only used to confirm the formula for small n.
    static int bruteForceOperations(int n) {
        int limit = 1 << 12;
        int[] distance = new int[limit];
        Arrays.fill(distance, -1);
        int[] queue = new int[limit];
        int head = 0, tail = 0;
        queue[tail++] = n;
        distance[n] = 0;
        while (head < tail) {
            int x = queue[head++];
            if (x == 0) return distance[x];
            int[] moves = {x ^ 1, x ^ (lowestBit(x) << 1)};   // rule 1 and rule 2
            for (int y : moves) {
                if (y < limit && distance[y] < 0) { distance[y] = distance[x] + 1; queue[tail++] = y; }
            }
        }
        return -1;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        // Level 1: place values.
        check(bit(13, 0) == 1 && bit(13, 1) == 0 && bit(13, 2) == 1 && bit(13, 3) == 1, "13 = 1101");
        check(Integer.toBinaryString(13).equals("1101") && 8 + 4 + 1 == 13, "13 = 8 + 4 + 1");
        check(Integer.parseInt("1101", 2) == 13, "parse 1101");

        // Level 2: two's complement in 8 bits.
        check(((byte) ~5 & 0xFF) == 0b11111010, "~5 = 11111010 in 8 bits");
        check(((byte) negate(5) & 0xFF) == 0b11111011, "-5 = 11111011 in 8 bits");
        check((byte) 0b11111011 == -5 && -128 + 64 + 32 + 16 + 8 + 2 + 1 == -5, "11111011 reads as -5");
        check(negate(5) == -5 && negate(-7) == 7 && negate(0) == 0, "negate");
        check(~5 == -6 && (~12 & 0xFF) == 0b11110011 && -128 + 64 + 32 + 16 + 4 == -12, "flip, then add one");
        check((byte) (127 + 1) == -128, "a byte wraps from 127 to -128");
        check(negate(Integer.MIN_VALUE) == Integer.MIN_VALUE, "MIN_VALUE has no positive partner");
        check(Byte.MIN_VALUE == -128 && Byte.MAX_VALUE == 127, "8-bit range");

        // Level 3: the operators on 12 = 1100 and 10 = 1010.
        check((12 & 10) == 8 && (12 | 10) == 14 && (12 ^ 10) == 6, "and, or, xor");
        check(~12 == -13 && (12 << 1) == 24 && (12 >> 2) == 3, "not, shifts");
        check((-16 >> 2) == -4 && (-16 >>> 28) == 15, "signed vs unsigned right shift");
        check((-5 >> 1) == -3 && -5 / 2 == -2, "shift floors, division truncates");
        check((1 << 31) == Integer.MIN_VALUE && (1 << 32) == 1 && (1L << 32) == 4294967296L, "shift distance is taken mod 32");
        check(isSet(13, 2) && !isSet(13, 1), "isSet");
        check(set(13, 1) == 15 && clear(13, 2) == 9 && toggle(13, 0) == 12, "set, clear, toggle");
        check(-3 % 2 == -1 && (-3 & 1) == 1, "odd test with % fails for negatives");

        // Level 4: lowest set bit.
        check(clearLowest(12) == 8 && 12 - 1 == 0b1011, "12 & 11 = 8");
        check(lowestBit(12) == 4 && (negate(12) & 0xFF) == 0b11110100, "12 & -12 = 4");
        check(countOnes(11) == 3 && countOnes(128) == 1 && countOnes(-1) == 32, "countOnes");
        check(countOnes(2147483645) == 30, "LC 191 example 3");
        check(Integer.bitCount(11) == 3 && Integer.bitCount(-1) == 32, "Integer.bitCount");
        check(isPowerOfTwo(64) && !isPowerOfTwo(12) && !isPowerOfTwo(0) && !isPowerOfTwo(Integer.MIN_VALUE), "power of two");
        check(Integer.lowestOneBit(12) == 4 && Integer.numberOfTrailingZeros(12) == 2, "JDK helpers");
        check(Integer.highestOneBit(12) == 8 && Integer.numberOfLeadingZeros(12) == 28, "highest bit");

        // Level 5: XOR facts.
        check((9 ^ 9) == 0 && (9 ^ 0) == 9 && ((4 ^ 1) ^ 2) == (4 ^ (1 ^ 2)), "xor facts");

        // Level 6: Gray code.
        int[] firstGrays = new int[8];
        for (int i = 0; i < 8; i++) firstGrays[i] = gray(i);
        check(Arrays.equals(firstGrays, new int[] {0, 1, 3, 2, 6, 7, 5, 4}), "Gray codes 0..7");
        for (int i = 0; i < 1000; i++) check(Integer.bitCount(gray(i) ^ gray(i + 1)) == 1, "neighbours differ in one bit");
        for (int i = 0; i < 1000; i++) check(minimumOneBitOperations(gray(i)) == i, "inverse Gray undoes Gray");

        // Cost: incrementing a binary counter from 0 to 8 flips 1+2+1+3+1+2+1+4 = 15 bits.
        int flips = 0;
        for (int i = 0; i < 8; i++) flips += Integer.bitCount(i ^ (i + 1));
        check(flips == 15 && flips < 2 * 8, "binary counter flips");
        int flipsTo1000 = 0;
        for (int i = 0; i < 1000; i++) flipsTo1000 += Integer.bitCount(i ^ (i + 1));
        check(flipsTo1000 == 1994 && flipsTo1000 < 2 * 1000, "1000 increments flip fewer than 2000 bits");

        // Interview problems.
        check(singleNumber(new int[] {4, 1, 2, 1, 2}) == 4, "LC 136");
        check((4 ^ 1) == 5 && (5 ^ 2) == 7 && (7 ^ 1) == 6 && (6 ^ 2) == 4, "LC 136 running XOR");
        check(singleNumber(new int[] {2, 2, 1}) == 1 && singleNumber(new int[] {-3}) == -3, "LC 136 edges");
        check(Arrays.equals(countBits(5), new int[] {0, 1, 1, 2, 1, 2}), "LC 338");
        check(Arrays.equals(countBits(0), new int[] {0}), "LC 338 n = 0");
        for (int i = 0; i <= 1000; i++) check(countBits(1000)[i] == Integer.bitCount(i), "LC 338 vs bitCount");
        check(getSum(2, 3) == 5 && getSum(1, 2) == 3, "LC 371");
        check(getSum(-1, 1) == 0 && getSum(-1000, 999) == -1 && getSum(-7, -8) == -15, "LC 371 negatives");
        check(minimumOneBitOperations(3) == 2 && minimumOneBitOperations(6) == 4, "LC 1611");
        check(minimumOneBitOperations(0) == 0 && minimumOneBitOperations(1) == 1, "LC 1611 small");
        check(minimumOneBitOperations(4) == 7 && minimumOneBitOperations(2) == 3, "f(2^k) = 2^(k+1) - 1");
        for (int n = 0; n < 512; n++) check(minimumOneBitOperations(n) == bruteForceOperations(n), "LC 1611 vs search " + n);
        check(minimumOneBitOperations(1_000_000_000) >= 0, "LC 1611 largest input");
        System.out.println("OK");
    }
}
