import java.util.ArrayList;
import java.util.List;

// Run from this folder with: java RecursionLab.java
public class RecursionLab {

    // Level 1: a base case and a smaller subproblem.
    static long factorial(int n) {
        if (n == 0) return 1;                  // base case: stops the recursion
        return n * factorial(n - 1);           // smaller subproblem: n - 1 < n
    }

    // Level 2: n frames are on the call stack at the deepest point.
    static long sumTo(int n) {                 // 1 + 2 + ... + n
        if (n == 0) return 0;
        return n + sumTo(n - 1);
    }

    static long sumToLoop(int n) {             // the same sum, using one frame
        long total = 0;
        for (int i = 1; i <= n; i++) total += i;
        return total;
    }

    // Level 3: recursion follows a nested structure of unknown depth.
    record Bag(int marbles, List<Bag> inside) {}

    static int countMarbles(Bag bag) {
        int total = bag.marbles();
        for (Bag inner : bag.inside()) total += countMarbles(inner);   // the same question, one level down
        return total;
    }

    // Level 4: two calls per call, so the calls form a tree.
    static long calls;

    static long fib(int n) {
        calls++;
        if (n <= 1) return n;                  // base cases: fib(0) = 0, fib(1) = 1
        return fib(n - 1) + fib(n - 2);
    }

    // Level 5: halve the problem each time.
    static int binaryDigits(int n) {           // n >= 1
        if (n == 1) return 1;
        return 1 + binaryDigits(n / 2);        // n / 2 drops the last binary digit
    }

    // Level 6: two halves plus linear work, evaluated directly. n is a power of two.
    static long work(int n) {                  // T(n) = 2 T(n / 2) + n, T(1) = 0
        if (n == 1) return 0;
        return 2 * work(n / 2) + n;
    }

    // Cost table check: T(n) = 2 T(n / 2) + 1, T(1) = 1, as when finding the maximum by halves.
    static long halvesWithConstantWork(int n) {
        return n == 1 ? 1 : 2 * halvesWithConstantWork(n / 2) + 1;
    }

    // Interview 1: LeetCode 21, Merge Two Sorted Lists.
    static final class ListNode {
        int val;
        ListNode next;

        ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }
    }

    static ListNode mergeTwoLists(ListNode a, ListNode b) {
        if (a == null) return b;                     // base cases: one list is empty
        if (b == null) return a;
        if (a.val <= b.val) {
            a.next = mergeTwoLists(a.next, b);       // a's head is the smallest; merge the rest behind it
            return a;
        }
        b.next = mergeTwoLists(a, b.next);
        return b;
    }

    // Interview 2: LeetCode 50, Pow(x, n).
    static double myPow(double x, int n) {
        long e = n;                                  // long: -(-2^31) does not fit in an int
        return e < 0 ? 1 / power(x, -e) : power(x, e);
    }

    static double power(double x, long e) {
        if (e == 0) return 1;
        double half = power(x, e / 2);               // one recursive call, used twice
        return e % 2 == 0 ? half * half : half * half * x;
    }

    // Interview 3: LeetCode 1106, Parsing A Boolean Expression.
    static final class BoolParser {
        private final String s;
        private int pos;                             // index of the next character to read

        BoolParser(String s) {
            this.s = s;
        }

        boolean parse() {
            char c = s.charAt(pos++);
            if (c == 't') return true;               // base cases: a single letter
            if (c == 'f') return false;
            pos++;                                   // skip '('
            List<Boolean> args = new ArrayList<>();
            do {
                args.add(parse());                   // each argument is a smaller expression
            } while (s.charAt(pos++) == ',');        // the loop ends after reading ')'
            if (c == '!') return !args.get(0);
            return c == '&' ? !args.contains(false) : args.contains(true);
        }
    }

    static boolean parseBoolExpr(String expression) {
        return new BoolParser(expression).parse();
    }

    static ListNode list(int... values) {
        ListNode head = null;
        for (int i = values.length - 1; i >= 0; i--) head = new ListNode(values[i], head);
        return head;
    }

    static List<Integer> values(ListNode head) {
        List<Integer> out = new ArrayList<>();
        for (ListNode n = head; n != null; n = n.next) out.add(n.val);
        return out;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        check(factorial(4) == 24 && factorial(0) == 1, "4! = 24 and 0! = 1");
        check(factorial(20) == 2_432_902_008_176_640_000L, "20! still fits in a long");
        check(factorial(21) < 0, "21! overflows a long and wraps negative");

        check(sumTo(3) == 6 && sumTo(1000) == 500_500, "1 + ... + 1000 = 500,500");
        boolean overflowed = false;
        try {
            sumTo(10_000_000);
        } catch (StackOverflowError e) {
            overflowed = true;
        }
        check(overflowed, "ten million frames do not fit on the default call stack");
        check(sumToLoop(10_000_000) == 50_000_005_000_000L, "the loop needs only one frame");

        Bag c = new Bag(4, List.of());
        Bag b = new Bag(1, List.of(c));
        Bag a = new Bag(3, List.of());
        Bag parcel = new Bag(2, List.of(a, b));
        check(countMarbles(parcel) == 10, "2 + 3 + 1 + 4 = 10 marbles");
        check(countMarbles(c) == 4, "a bag with no bags inside is the base case");

        calls = 0;
        check(fib(4) == 3 && calls == 9, "fib(4) = 3 after 9 calls");
        calls = 0;
        check(fib(5) == 5 && calls == 15, "fib(5) = 5 after 15 calls");
        calls = 0;
        check(fib(30) == 832_040 && calls == 2_692_537, "fib(30) after 2,692,537 calls");
        for (int n = 0; n <= 20; n++) {
            calls = 0;
            fib(n);
            long callsForN = calls;
            check(callsForN == 2 * fib(n + 1) - 1, "calls(n) = 2 fib(n + 1) - 1");
        }

        check(binaryDigits(13) == 4 && Integer.toBinaryString(13).equals("1101"), "13 = 1101 in binary");
        check(binaryDigits(1) == 1 && binaryDigits(1_000_000) == 20, "1 and a million");
        check(binaryDigits(Integer.MAX_VALUE) == 31, "2^31 - 1 has 31 binary digits");

        check(work(2) == 2 && work(4) == 8 && work(8) == 24, "T(8) = 24 = 8 log2 8");
        check(work(1024) == 10_240, "T(1024) = 1024 x 10");
        check(binaryDigits(8) == 4 && binaryDigits(1024) == 11, "cost table: halving");
        check(halvesWithConstantWork(8) == 15 && halvesWithConstantWork(1024) == 2047, "cost table: 2n - 1");
        calls = 0;
        fib(8);
        check(calls == 67, "cost table: 67 calls for fib(8)");
        java.math.BigInteger f = java.math.BigInteger.ZERO, next = java.math.BigInteger.ONE;
        for (int i = 0; i < 1025; i++) { java.math.BigInteger sum = f.add(next); f = next; next = sum; }
        java.math.BigInteger callsFor1024 = f.shiftLeft(1).subtract(java.math.BigInteger.ONE);   // 2 F(1025) - 1
        check(callsFor1024.compareTo(java.math.BigInteger.TEN.pow(214)) > 0, "fib(1024) would make over 10^214 calls");

        check(values(mergeTwoLists(list(1, 2, 4), list(1, 3, 4))).equals(List.of(1, 1, 2, 3, 4, 4)), "LC 21");
        check(mergeTwoLists(null, null) == null && values(mergeTwoLists(null, list(0))).equals(List.of(0)), "LC 21 empty");

        check(myPow(2, 13) == 8192 && 8 + 4 + 1 == 13, "2^13 = 2^8 2^4 2^1 = 8192");
        check(myPow(2, 10) == 1024 && myPow(2, -2) == 0.25, "LC 50 examples");
        check(Math.abs(myPow(2.1, 3) - 9.261) < 1e-9, "LC 50: 2.1^3");
        check(-Integer.MIN_VALUE == Integer.MIN_VALUE, "negating -2^31 overflows an int");
        check(myPow(1, Integer.MIN_VALUE) == 1 && myPow(2, Integer.MIN_VALUE) == 0, "LC 50 n = -2^31");

        check(!parseBoolExpr("|(&(t,f,t),!(t))"), "LC 1106: OR(AND(t, f, t), NOT(t)) = false");
        check(parseBoolExpr("&(|(t),!(f))") && !parseBoolExpr("&(|(f),!(f))"), "LC 1106 more");
        check(parseBoolExpr("|(f,f,f,t)") && parseBoolExpr("!(&(f,t))"), "LC 1106 more");
        check("|(&(t,f,t),!(t))".length() == 16, "16 characters");
        System.out.println("OK");
    }
}
