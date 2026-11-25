import java.util.HashMap;
import java.util.Map;

// Run from this folder with: java SlidingWindowLab.java
public class SlidingWindowLab {

    // Level 1: a fixed window of k elements. Each move adds one element and removes one.
    static int maxSumOfK(int[] a, int k) {
        int sum = 0;
        for (int i = 0; i < k; i++) sum += a[i];
        int best = sum;
        for (int right = k; right < a.length; right++) {
            sum += a[right] - a[right - k];     // a[right] enters, a[right - k] leaves
            best = Math.max(best, sum);
        }
        return best;
    }

    // Level 2: a variable window that grows on the right and shrinks on the left (LeetCode 209).
    static int shortestWithSumAtLeast(int[] a, int target) {   // every a[i] > 0
        int left = 0, sum = 0, best = Integer.MAX_VALUE;
        for (int right = 0; right < a.length; right++) {
            sum += a[right];
            while (sum >= target) {                     // valid: record it, then try a shorter one
                best = Math.min(best, right - left + 1);
                sum -= a[left++];
            }
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    // Level 3: the window keeps counts of what is inside it.
    static int longestWithAtMostKDistinct(String s, int k) {
        Map<Character, Integer> count = new HashMap<>();
        int left = 0, best = 0;
        for (int right = 0; right < s.length(); right++) {
            count.merge(s.charAt(right), 1, Integer::sum);
            while (count.size() > k) {                  // too many kinds: drop letters from the left
                char out = s.charAt(left++);
                if (count.merge(out, -1, Integer::sum) == 0) count.remove(out);
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    // Level 3 check: how many times the two ends move in total.
    static int pointerMoves(String s, int k) {
        Map<Character, Integer> count = new HashMap<>();
        int left = 0, moves = 0;
        for (int right = 0; right < s.length(); right++) {
            moves++;
            count.merge(s.charAt(right), 1, Integer::sum);
            while (count.size() > k) {
                moves++;
                char out = s.charAt(left++);
                if (count.merge(out, -1, Integer::sum) == 0) count.remove(out);
            }
        }
        return moves;
    }

    // Level 4: count every valid window, not just the best one.
    static int countAtMostKDistinct(int[] a, int k) {
        Map<Integer, Integer> count = new HashMap<>();
        int left = 0, total = 0;
        for (int right = 0; right < a.length; right++) {
            count.merge(a[right], 1, Integer::sum);
            while (count.size() > k) {
                int out = a[left++];
                if (count.merge(out, -1, Integer::sum) == 0) count.remove(out);
            }
            total += right - left + 1;                  // the windows a[i..right] for i = left..right
        }
        return total;
    }

    // Interview 1: LeetCode 3, Longest Substring Without Repeating Characters.
    static int lengthOfLongestSubstring(String s) {
        Map<Character, Integer> lastSeen = new HashMap<>();
        int left = 0, best = 0;
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            if (lastSeen.getOrDefault(c, -1) >= left) left = lastSeen.get(c) + 1; // jump past the copy
            lastSeen.put(c, right);
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    // Interview 2: LeetCode 76, Minimum Window Substring.
    static String minWindow(String s, String t) {
        int[] need = new int[128];                          // negative = more than needed
        for (char c : t.toCharArray()) need[c]++;
        int missing = t.length(), left = 0, bestStart = 0, bestLen = Integer.MAX_VALUE;
        for (int right = 0; right < s.length(); right++) {
            if (need[s.charAt(right)]-- > 0) missing--;     // this letter was still needed
            while (missing == 0) {                          // valid: shrink from the left
                if (right - left + 1 < bestLen) { bestStart = left; bestLen = right - left + 1; }
                if (++need[s.charAt(left++)] > 0) missing++; // dropped a letter we needed
            }
        }
        return bestLen == Integer.MAX_VALUE ? "" : s.substring(bestStart, bestStart + bestLen);
    }

    // Interview 3: LeetCode 992, Subarrays with K Different Integers.
    static int subarraysWithKDistinct(int[] nums, int k) {
        return countAtMostKDistinct(nums, k) - countAtMostKDistinct(nums, k - 1);
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] visitors = {2, 1, 5, 1, 3, 2};
        check(maxSumOfK(visitors, 3) == 9, "the best 3-day stretch is 5 + 1 + 3 = 9");
        check(maxSumOfK(visitors, 6) == 14 && maxSumOfK(visitors, 1) == 5, "window as long as the array, and of size 1");

        check(shortestWithSumAtLeast(new int[] {2, 3, 1, 2, 4, 3}, 7) == 2, "LC 209: [4, 3]");
        check(shortestWithSumAtLeast(new int[] {1, 1, 1}, 7) == 0, "LC 209: impossible");

        check(longestWithAtMostKDistinct("eceba", 2) == 3, "ece has two kinds of letter");
        check(pointerMoves("eceba", 2) == 8, "5 right moves and 3 left moves");
        check(longestWithAtMostKDistinct("aaaa", 1) == 4 && longestWithAtMostKDistinct("", 2) == 0, "edge cases");

        check(countAtMostKDistinct(new int[] {1, 2, 1, 2, 3}, 2) == 12, "12 windows with at most 2 kinds");
        check(countAtMostKDistinct(new int[] {1, 2, 1, 2, 3}, 1) == 5, "5 windows with at most 1 kind");
        check(countAtMostKDistinct(new int[] {'e', 'c', 'e', 'b', 'a'}, 2) == 10, "eceba has 10 windows with at most 2 kinds");

        check(lengthOfLongestSubstring("abcabcbb") == 3, "LC 3: abc");
        check(lengthOfLongestSubstring("pwwkew") == 3 && lengthOfLongestSubstring("bbbbb") == 1, "LC 3 more");
        check(lengthOfLongestSubstring("abba") == 2 && lengthOfLongestSubstring("") == 0, "LC 3: left never moves back");

        check(minWindow("ADOBECODEBANC", "ABC").equals("BANC"), "LC 76");
        check(minWindow("a", "aa").equals("") && minWindow("a", "a").equals("a"), "LC 76 edge cases");
        check(minWindow("aab", "aab").equals("aab"), "LC 76 repeated letters in t");

        check(subarraysWithKDistinct(new int[] {1, 2, 1, 2, 3}, 2) == 7, "LC 992: 12 - 5 = 7");
        check(subarraysWithKDistinct(new int[] {1, 2, 1, 3, 4}, 3) == 3, "LC 992 second example");
        System.out.println("OK");
    }
}
