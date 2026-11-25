import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;

// Run from this folder with: java InteractiveLab.java
public class InteractiveLab {

    // Level 1: the judge hides a number. guess(num) is -1 if num is too high, 1 if too low, 0 if right.
    interface GuessJudge {
        int guess(int num);
    }

    static final class HonestJudge implements GuessJudge {
        private final int pick;
        int calls;

        HonestJudge(int pick) {
            this.pick = pick;
        }

        public int guess(int num) {
            calls++;
            return Integer.compare(pick, num);
        }
    }

    // Level 1: the obvious strategy asks 1, 2, 3, ... and needs n questions in the worst case.
    static int guessByCounting(int n, GuessJudge judge) {
        for (int num = 1; num <= n; num++) {
            if (judge.guess(num) == 0) return num;
        }
        throw new IllegalStateException("the judge broke its own rules");
    }

    // Level 2: the fewest three-way questions that always find a number in 1..n: ceil(log2(n + 1)).
    static int minGuesses(int n) {
        return 32 - Integer.numberOfLeadingZeros(n);       // the number of bits in n
    }

    // Level 3: an adversary that never commits to a number; it keeps the larger consistent range.
    static final class AdversaryJudge implements GuessJudge {
        private int low, high;                             // every number in [low, high] fits all past answers
        int calls;

        AdversaryJudge(int n) {
            low = 1;
            high = n;
        }

        public int guess(int num) {
            calls++;
            if (num < low) return 1;
            if (num > high) return -1;
            if (low == high) return 0;                     // only num is left, so it must admit it
            if (num - low >= high - num) {                 // keep the bigger side: say "too high"
                high = num - 1;
                return -1;
            }
            low = num + 1;
            return 1;
        }
    }

    // Level 4: the worst case of a query is its largest group of candidates that give the same answer.
    static int worstCase(List<String> candidates, String query) {
        int[] groupSize = new int[7];                      // an answer is 0..6 matching letters
        for (String word : candidates) groupSize[matches(word, query)]++;
        return Arrays.stream(groupSize).max().getAsInt();
    }

    static int matches(String a, String b) {
        int same = 0;
        for (int i = 0; i < a.length(); i++) if (a.charAt(i) == b.charAt(i)) same++;
        return same;
    }

    // Interview 1: LeetCode 374, Guess Number Higher or Lower.
    static int guessNumber(int n, GuessJudge judge) {
        int lo = 1, hi = n;
        while (true) {
            int mid = lo + (hi - lo) / 2;                  // (lo + hi) / 2 would overflow near 2^31
            int answer = judge.guess(mid);
            if (answer == 0) return mid;
            if (answer < 0) hi = mid - 1;
            else lo = mid + 1;
        }
    }

    // Interview 2: LeetCode 278, First Bad Version.
    static int firstBadVersion(int n, IntPredicate isBadVersion) {
        int lo = 1, hi = n;                                // the first bad version is always in [lo, hi]
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (isBadVersion.test(mid)) hi = mid;          // mid is bad: the first bad one is mid or earlier
            else lo = mid + 1;
        }
        return lo;
    }

    // Interview 3: LeetCode 1095, Find in Mountain Array.
    interface MountainArray {
        int get(int index);

        int length();
    }

    static int findInMountainArray(int target, MountainArray mountain) {
        int peak = peakIndex(mountain);
        int left = search(mountain, target, 0, peak, true);
        return left != -1 ? left : search(mountain, target, peak + 1, mountain.length() - 1, false);
    }

    static int peakIndex(MountainArray mountain) {
        int lo = 0, hi = mountain.length() - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (mountain.get(mid) < mountain.get(mid + 1)) lo = mid + 1;   // still climbing: the peak is right of mid
            else hi = mid;
        }
        return lo;
    }

    static int search(MountainArray mountain, int target, int lo, int hi, boolean ascending) {
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            int value = mountain.get(mid);
            if (value == target) return mid;
            if ((value < target) == ascending) lo = mid + 1;
            else hi = mid - 1;
        }
        return -1;
    }

    static final class CountingMountain implements MountainArray {
        private final int[] values;
        int calls;

        CountingMountain(int[] values) {
            this.values = values;
        }

        public int get(int index) {
            calls++;
            return values[index];
        }

        public int length() {
            return values.length;
        }
    }

    // Interview 4: LeetCode 843, Guess the Word.
    interface Master {
        int guess(String word);
    }

    static void findSecretWord(String[] words, Master master) {
        List<String> candidates = new ArrayList<>(List.of(words));
        while (!candidates.isEmpty()) {
            String best = candidates.get(0);
            for (String word : candidates) {
                if (worstCase(candidates, word) < worstCase(candidates, best)) best = word;   // minimax choice
            }
            int answer = master.guess(best);
            if (answer == 6) return;
            String guessed = best;
            candidates.removeIf(word -> matches(word, guessed) != answer);   // keep words that agree with the reply
        }
    }

    static final class CountingMaster implements Master {
        private final String secret;
        private final List<String> words;
        int calls;
        boolean found;

        CountingMaster(String secret, String[] words) {
            this.secret = secret;
            this.words = List.of(words);
        }

        public int guess(String word) {
            calls++;
            if (!words.contains(word)) return -1;
            if (word.equals(secret)) found = true;
            return matches(word, secret);
        }
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        // Level 1.
        HonestJudge counting = new HonestJudge(6);
        check(guessByCounting(10, counting) == 6 && counting.calls == 6, "counting up finds 6 after 6 questions");
        AdversaryJudge stubborn = new AdversaryJudge(10);
        check(guessByCounting(10, stubborn) == 10 && stubborn.calls == 10, "against an adversary, counting needs all 10");

        // Level 2.
        check(minGuesses(10) == 4 && Math.ceil(Math.log(11) / Math.log(2)) == 4, "n = 10: ceil(log2 11) = 4");
        check(minGuesses(100) == 7, "n = 100: 7 questions");
        check(minGuesses(Integer.MAX_VALUE) == 31, "n = 2^31 - 1: 31 questions");
        check(Math.ceil(Math.log(5) / Math.log(2)) == 3, "yes/no questions for n = 5: ceil(log2 5) = 3");
        check((1L << 31) - 1 == Integer.MAX_VALUE, "2^31 - 1");

        // Level 3: binary search meets the lower bound against the adversary, never better.
        for (int n : new int[] {1, 2, 3, 10, 100, 1_000, Integer.MAX_VALUE}) {
            AdversaryJudge adversary = new AdversaryJudge(n);
            guessNumber(n, adversary);
            check(adversary.calls == minGuesses(n), "binary search needs exactly ceil(log2(n + 1)) against the adversary, n = " + n);
        }

        // Level 4 and Interview 4: minimax on six words.
        String[] words = {"garden", "harden", "warden", "pardon", "golden", "burden"};
        List<String> all = List.of(words);
        check(matches("garden", "harden") == 5 && matches("harden", "golden") == 3, "match counts");
        check(worstCase(all, "garden") == 3 && worstCase(all, "harden") == 2 && worstCase(all, "pardon") == 3,
                "garden leaves up to 3 candidates, harden only 2");
        check(Math.ceil(Math.log(100) / Math.log(7)) == 3, "7 possible answers: at least ceil(log7 100) = 3 guesses");
        int worst = 0;
        for (String secret : words) {
            CountingMaster master = new CountingMaster(secret, words);
            findSecretWord(words, master);
            check(master.found, "LC 843 finds " + secret);
            worst = Math.max(worst, master.calls);
        }
        check(worst == 3, "LC 843: every secret in at most 3 guesses");
        CountingMaster burden = new CountingMaster("burden", words);
        findSecretWord(words, burden);
        check(burden.calls == 3, "burden: harden (4), pardon (3), burden (6)");
        check(new CountingMaster("garden", words).guess("zzzzzz") == -1, "a word outside the list gets -1");

        // Interview 1: LeetCode 374.
        HonestJudge six = new HonestJudge(6);
        check(guessNumber(10, six) == 6 && six.calls == 3, "LC 374: 5, 8, 6");
        HonestJudge big = new HonestJudge(Integer.MAX_VALUE);
        check(guessNumber(Integer.MAX_VALUE, big) == Integer.MAX_VALUE && big.calls <= 31, "LC 374 at the top, no overflow");
        HonestJudge one = new HonestJudge(1);
        check(guessNumber(1, one) == 1 && one.calls == 1, "LC 374 n = 1");

        // Interview 2: LeetCode 278.
        int[] calls = {0};
        check(firstBadVersion(5, v -> { calls[0]++; return v >= 4; }) == 4 && calls[0] == 2, "LC 278: 3 good, 4 bad");
        calls[0] = 0;
        check(firstBadVersion(Integer.MAX_VALUE, v -> { calls[0]++; return v >= Integer.MAX_VALUE; }) == Integer.MAX_VALUE
                && calls[0] <= 31, "LC 278 at the top: at most 31 calls");
        check(firstBadVersion(1, v -> true) == 1, "LC 278 n = 1");

        // Interview 3: LeetCode 1095.
        CountingMountain small = new CountingMountain(new int[] {1, 3, 5, 7, 6, 5, 2});
        check(findInMountainArray(5, small) == 2 && small.calls == 8, "LC 1095: 5 at index 2 (also at 5), 8 calls");
        check(findInMountainArray(2, new CountingMountain(new int[] {1, 3, 5, 7, 6, 5, 2})) == 6, "LC 1095 right side");
        check(findInMountainArray(4, new CountingMountain(new int[] {1, 3, 5, 7, 6, 5, 2})) == -1, "LC 1095 absent");
        int[] mountain = new int[10_000];
        for (int i = 0; i < mountain.length; i++) mountain[i] = i <= 6_000 ? i : 12_000 - i;
        int maxCalls = 0;
        for (int target = -1; target <= 6_001; target++) {
            CountingMountain judge = new CountingMountain(mountain);
            int index = findInMountainArray(target, judge);
            int expected = target >= 0 && target <= 6_000 ? target : -1;
            check(index == expected, "LC 1095 finds the smallest index of " + target);
            maxCalls = Math.max(maxCalls, judge.calls);
        }
        check(minGuesses(10_000) == 14 && 2 * 14 + 14 + 14 == 56 && maxCalls == 50, "LC 1095: at most 56 calls in theory, 50 here, both <= 100");

        System.out.println("OK");
    }
}
