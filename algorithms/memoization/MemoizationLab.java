import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Run from this folder with: java MemoizationLab.java
public class MemoizationLab {

    static long calls;                                   // how many times a function below was entered

    // Level 1: plain recursion solves the same small problems again and again.
    static long fibNaive(int n) {
        calls++;
        if (n < 2) return n;
        return fibNaive(n - 1) + fibNaive(n - 2);
    }

    // Level 2: the same recursion, but every answer is written down the first time it is found.
    static long fibMemo(int n, long[] memo) {
        calls++;
        if (n < 2) return n;
        if (memo[n] != 0) return memo[n];                // solved before: read the note
        memo[n] = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
        return memo[n];
    }

    // Level 3: a function of two arguments needs both of them in the key.
    static final Map<Long, Long> chooseMemo = new HashMap<>();

    static long choose(int n, int k) {
        if (k == 0 || k == n) return 1;
        long key = (long) n << 32 | k;                   // pack (n, k) into one long
        Long known = chooseMemo.get(key);
        if (known != null) return known;
        long result = choose(n - 1, k - 1) + choose(n - 1, k);
        chooseMemo.put(key, result);
        return result;
    }

    static long chooseNaive(int n, int k) {
        calls++;
        if (k == 0 || k == n) return 1;
        return chooseNaive(n - 1, k - 1) + chooseNaive(n - 1, k);
    }

    // Level 5: computeIfAbsent must not be called recursively on the same HashMap.
    static final Map<Integer, Long> fibCache = new HashMap<>();

    static long fibComputeIfAbsent(int n) {
        if (n < 2) return n;
        return fibCache.computeIfAbsent(n, m -> fibComputeIfAbsent(m - 1) + fibComputeIfAbsent(m - 2));
    }

    // Interview 1: LeetCode 139, Word Break.
    static final class WordBreak {
        final String s;
        final Set<String> words;
        final Boolean[] memo;                            // memo[start] = can s[start..) be split? null = not known yet

        WordBreak(String s, List<String> words) {
            this.s = s;
            this.words = new HashSet<>(words);
            this.memo = new Boolean[s.length() + 1];
        }

        boolean canSplit(int start) {
            if (start == s.length()) return true;
            if (memo[start] != null) return memo[start];
            boolean ok = false;
            for (int end = start + 1; end <= s.length() && !ok; end++) {
                ok = words.contains(s.substring(start, end)) && canSplit(end);
            }
            return memo[start] = ok;
        }
    }

    static boolean wordBreak(String s, List<String> words) {
        return new WordBreak(s, words).canSplit(0);
    }

    // Interview 2: LeetCode 403, Frog Jump.
    static final class FrogJump {
        final int[] stones;
        final Map<Integer, Integer> indexOf = new HashMap<>(); // stone position -> its index
        final Map<Long, Boolean> memo = new HashMap<>();

        FrogJump(int[] stones) {
            this.stones = stones;
            for (int i = 0; i < stones.length; i++) indexOf.put(stones[i], i);
        }

        boolean canFinish(int i, int lastJump) {         // standing on stone i, having just jumped lastJump units
            if (i == stones.length - 1) return true;
            long key = (long) i << 32 | lastJump;        // the future depends on both, so the key holds both
            Boolean known = memo.get(key);
            if (known != null) return known;
            boolean ok = false;
            for (int jump = lastJump - 1; jump <= lastJump + 1 && !ok; jump++) {
                Integer next = jump > 0 ? indexOf.get(stones[i] + jump) : null;
                ok = next != null && canFinish(next, jump);
            }
            memo.put(key, ok);
            return ok;
        }
    }

    static boolean canCross(int[] stones) {
        return new FrogJump(stones).canFinish(0, 0);     // a last jump of 0 makes the first jump exactly 1
    }

    // Interview 3: LeetCode 87, Scramble String.
    static final class Scramble {
        final String a, b;
        final Boolean[][][] memo;                        // memo[i][j][len], null = not known yet

        Scramble(String a, String b) {
            this.a = a;
            this.b = b;
            int n = a.length();
            this.memo = new Boolean[n][n][n + 1];
        }

        boolean matches(int i, int j, int len) {         // is b[j..j+len) a scramble of a[i..i+len)?
            if (memo[i][j][len] != null) return memo[i][j][len];
            boolean ok = a.regionMatches(i, b, j, len);
            for (int k = 1; k < len && !ok; k++) {
                ok = (matches(i, j, k) && matches(i + k, j + k, len - k))              // halves kept in order
                        || (matches(i, j + len - k, k) && matches(i + k, j, len - k)); // halves swapped
            }
            return memo[i][j][len] = ok;
        }
    }

    static boolean isScramble(String a, String b) {
        return a.length() == b.length() && new Scramble(a, b).matches(0, 0, a.length());
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        calls = 0;
        check(fibNaive(5) == 5 && calls == 15, "fib(5) naive: 15 calls");
        calls = 0;
        check(fibNaive(30) == 832_040 && calls == 2_692_537, "fib(30) naive: 2,692,537 calls");
        calls = 0;
        check(fibMemo(5, new long[6]) == 5 && calls == 9, "fib(5) memoised: 9 calls");
        calls = 0;
        check(fibMemo(30, new long[31]) == 832_040 && calls == 59, "fib(30) memoised: 59 calls");
        calls = 0;
        check(fibMemo(90, new long[91]) == 2_880_067_194_370_816_120L && calls == 179, "fib(90) memoised: 179 calls");

        check(((long) 5 << 32 | 2) == 21_474_836_482L, "the key for (5, 2)");
        check(choose(5, 2) == 10 && chooseMemo.size() == 6, "C(5,2) = 10 with k(n-k) = 6 stored states");
        check(choose(30, 15) == 155_117_520 && chooseMemo.size() == 225, "C(30,15) with 15 * 15 = 225 stored states");
        calls = 0;
        check(chooseNaive(5, 2) == 10 && calls == 19, "C(5,2) naive: 2 * 10 - 1 calls");
        calls = 0;
        check(chooseNaive(30, 15) == 155_117_520 && calls == 310_235_039, "C(30,15) naive: 310,235,039 calls");

        check(2 * fibMemo(51, new long[52]) - 1 == 40_730_022_147L, "fib(50) naive would make 2F(51) - 1 calls");
        boolean overflowed = false;
        try {
            fibMemo(1_000_000, new long[1_000_001]);
        } catch (StackOverflowError e) {
            overflowed = true;
        }
        check(overflowed, "a million nested calls overflow the default stack");

        boolean threw = false;
        try {
            fibComputeIfAbsent(30);
        } catch (ConcurrentModificationException e) {
            threw = true;
        }
        check(threw, "recursive computeIfAbsent on a HashMap throws");

        check(wordBreak("leetcode", List.of("leet", "code")), "LC 139 leetcode");
        check(wordBreak("applepenapple", List.of("apple", "pen")), "LC 139 apple pen apple");
        check(!wordBreak("catsandog", List.of("cats", "dog", "sand", "and", "cat")), "LC 139 cats and og");

        int[] stones = {0, 1, 3, 5, 6, 8, 12, 17};
        FrogJump frog = new FrogJump(stones);
        check(frog.canFinish(0, 0), "LC 403 example 1");
        check(frog.memo.size() == 8, "only 8 of the 8 * 8 (stone, jump) pairs are ever stored");
        check(!canCross(new int[] {0, 1, 2, 3, 4, 8, 9, 11}), "LC 403 example 2: gap of 4 after a jump of 1");
        check(!canCross(new int[] {0, 2}), "LC 403 first jump must be 1");

        check(isScramble("great", "rgeat"), "LC 87 great / rgeat");
        check(!isScramble("abcde", "caebd"), "LC 87 abcde / caebd");
        check(isScramble("a", "a"), "LC 87 single letter");
        System.out.println("OK");
    }
}
