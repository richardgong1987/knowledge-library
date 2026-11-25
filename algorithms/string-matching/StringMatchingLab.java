import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// Run from this folder with: java StringMatchingLab.java
public class StringMatchingLab {

    // Level 1: the naive matcher tries every shift.
    static int naiveSearch(String text, String pattern) {
        for (int shift = 0; shift + pattern.length() <= text.length(); shift++) {
            int k = 0;
            while (k < pattern.length() && text.charAt(shift + k) == pattern.charAt(k)) k++;
            if (k == pattern.length()) return shift;
        }
        return -1;
    }

    // Level 2: the prefix function. pi[i] = longest proper border of p[0..i].
    static int[] prefixFunction(String p) {
        int[] pi = new int[p.length()];
        for (int i = 1, k = 0; i < p.length(); i++) {
            while (k > 0 && p.charAt(i) != p.charAt(k)) k = pi[k - 1];   // fall back to the next shorter border
            if (p.charAt(i) == p.charAt(k)) k++;
            pi[i] = k;
        }
        return pi;
    }

    // Level 3: Knuth-Morris-Pratt. The text index i never moves backwards.
    static int kmpSearch(String text, String pattern) {
        if (pattern.isEmpty()) return 0;
        int[] pi = prefixFunction(pattern);
        for (int i = 0, k = 0; i < text.length(); i++) {
            while (k > 0 && text.charAt(i) != pattern.charAt(k)) k = pi[k - 1];
            if (text.charAt(i) == pattern.charAt(k)) k++;
            if (k == pattern.length()) return i - k + 1;
        }
        return -1;
    }

    // Level 4: the Z-function. z[i] = longest common prefix of s and s[i..]; z[0] = n by convention.
    static int[] zFunction(String s) {
        int n = s.length();
        int[] z = new int[n];
        for (int i = 1, l = 0, r = 0; i < n; i++) {
            if (i < r) z[i] = Math.min(r - i, z[i - l]);   // reuse what the box [l, r) already matched
            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) z[i]++;
            if (i + z[i] > r) { l = i; r = i + z[i]; }
        }
        if (n > 0) z[0] = n;
        return z;
    }

    // Comparison counters for the cost claims; not shown in the lesson.
    static int naiveComparisons(String text, String pattern) {
        int count = 0;
        for (int shift = 0; shift + pattern.length() <= text.length(); shift++) {
            for (int k = 0; k < pattern.length(); k++) {
                count++;
                if (text.charAt(shift + k) != pattern.charAt(k)) break;
            }
        }
        return count;
    }

    static int kmpComparisons(String text, String pattern) {
        int[] pi = prefixFunction(pattern);
        int count = 0;
        for (int i = 0, k = 0; i < text.length(); i++) {
            while (k > 0 && text.charAt(i) != pattern.charAt(k)) {
                count++;                                   // a failed comparison, then a fallback
                k = pi[k - 1];
            }
            count++;                                       // the comparison that ends the loop
            if (text.charAt(i) == pattern.charAt(k)) k++;
            if (k == pattern.length()) k = pi[k - 1];   // keep going to count a full scan
        }
        return count;
    }

    // A deliberately wrong prefix function that falls back only once; used to check the "Common mistakes" row.
    static int[] prefixFunctionWithIf(String p) {
        int[] pi = new int[p.length()];
        for (int i = 1, k = 0; i < p.length(); i++) {
            if (k > 0 && p.charAt(i) != p.charAt(k)) k = pi[k - 1];
            if (p.charAt(i) == p.charAt(k)) k++;
            pi[i] = k;
        }
        return pi;
    }

    // Level 5: Horspool's bad-character shift, checked here and described in prose.
    static int horspoolWindows(String text, String pattern) {
        int m = pattern.length(), windows = 0;
        Map<Character, Integer> shift = new HashMap<>();
        for (int j = 0; j < m - 1; j++) shift.put(pattern.charAt(j), m - 1 - j);
        for (int s = 0; s + m <= text.length(); s += shift.getOrDefault(text.charAt(s + m - 1), m)) {
            windows++;
            if (text.startsWith(pattern, s)) return windows;
        }
        return windows;
    }

    // Interview 1: LeetCode 28, Find the Index of the First Occurrence in a String.
    static int strStr(String haystack, String needle) {
        int[] z = zFunction(needle + "\0" + haystack);   // "\0" never occurs in the input
        for (int i = needle.length() + 1; i < z.length; i++) {
            if (z[i] == needle.length()) return i - needle.length() - 1;
        }
        return -1;
    }

    // Interview 2: LeetCode 459, Repeated Substring Pattern.
    static boolean repeatedSubstringPattern(String s) {
        int n = s.length(), border = prefixFunction(s)[n - 1];
        return border > 0 && n % (n - border) == 0;
    }

    // Interview 3: LeetCode 1392, Longest Happy Prefix.
    static String longestPrefix(String s) {
        return s.substring(0, prefixFunction(s)[s.length() - 1]);
    }

    // Interview 4: LeetCode 214, Shortest Palindrome.
    static String shortestPalindrome(String s) {
        String reversed = new StringBuilder(s).reverse().toString();
        int[] pi = prefixFunction(s + "#" + reversed);   // "#" stops a border from crossing the middle
        int palindromePrefix = pi[pi.length - 1];
        return new StringBuilder(s.substring(palindromePrefix)).reverse() + s;
    }

    static boolean isPalindrome(String s) {
        return new StringBuilder(s).reverse().toString().equals(s);
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        // Level 1: the naive worst case.
        check(naiveSearch("aaaaaaab", "aaab") == 4, "naive finds aaab at 4");
        check(naiveComparisons("aaaaaaab", "aaab") == 20 && (8 - 4 + 1) * 4 == 20, "naive makes 20 comparisons");
        check(naiveSearch("abc", "d") == -1 && naiveSearch("abc", "") == 0, "naive edges");

        // Level 2: prefix function of abacabab.
        check(Arrays.equals(prefixFunction("abacabab"), new int[] {0, 0, 1, 0, 1, 2, 3, 2}), "pi(abacabab)");
        check(Arrays.equals(prefixFunction("aaaa"), new int[] {0, 1, 2, 3}), "pi(aaaa)");
        check(Arrays.equals(prefixFunction("aaab"), new int[] {0, 1, 2, 0}) && prefixFunctionWithIf("aaab")[3] == 1, "if instead of while");

        // Level 3: KMP.
        check(kmpSearch("abacabacabab", "abacabab") == 4, "KMP finds abacabab at 4");
        check(kmpSearch("aaaaaaab", "aaab") == 4 && kmpSearch("abc", "abd") == -1 && kmpSearch("", "a") == -1, "KMP edges");
        int kmpCount = kmpComparisons("aaaaaaab", "aaab");
        check(kmpCount == 12 && kmpCount <= 2 * 8, "KMP makes 8 + 4 = 12 comparisons, at most 2n = 16");

        // Level 4: Z-function.
        check(Arrays.equals(zFunction("aabxaab"), new int[] {7, 1, 0, 0, 3, 1, 0}), "z(aabxaab)");
        check(Arrays.equals(zFunction("aaaaa"), new int[] {5, 4, 3, 2, 1}), "z(aaaaa)");

        // Level 5: Horspool on xxxxabcd.
        check(horspoolWindows("xxxxabcd", "abcd") == 2, "Horspool checks two windows");
        check(horspoolWindows("abcd", "abcd") == 1, "Horspool immediate match");

        // Random cross-checks of the three matchers.
        java.util.Random rng = new java.util.Random(5);
        for (int trial = 0; trial < 2000; trial++) {
            StringBuilder text = new StringBuilder(), pattern = new StringBuilder();
            for (int i = 0, n = rng.nextInt(12); i < n; i++) text.append((char) ('a' + rng.nextInt(2)));
            for (int i = 0, m = 1 + rng.nextInt(4); i < m; i++) pattern.append((char) ('a' + rng.nextInt(2)));
            int expected = text.indexOf(pattern.toString());
            check(naiveSearch(text.toString(), pattern.toString()) == expected, "naive random");
            check(kmpSearch(text.toString(), pattern.toString()) == expected, "KMP random");
            check(strStr(text.toString(), pattern.toString()) == expected, "Z random");
        }

        // Interview problems.
        check(strStr("sadbutsad", "sad") == 0 && strStr("leetcode", "leeto") == -1, "LC 28");
        check(zFunction("sad\0sadbutsad")[4] == 3, "z value at the start of the haystack");
        check(repeatedSubstringPattern("abab") && !repeatedSubstringPattern("aba") && repeatedSubstringPattern("abcabcabc"), "LC 459");
        check(prefixFunction("abcabcabc")[8] == 6 && !repeatedSubstringPattern("a") && repeatedSubstringPattern("aa"), "LC 459 edges");
        check(!repeatedSubstringPattern("abaababaab".substring(0, 5)), "LC 459 border without full period");
        check(prefixFunction("ababab")[5] == 4 && longestPrefix("ababab").equals("abab"), "LC 1392 ababab");
        check(longestPrefix("level").equals("l") && longestPrefix("a").equals(""), "LC 1392 level");
        check(prefixFunction("aacecaaa#aaacecaa")[16] == 7, "pi at the end of s#reverse(s)");
        check(shortestPalindrome("aacecaaa").equals("aaacecaaa") && shortestPalindrome("abcd").equals("dcbabcd"), "LC 214");
        check(shortestPalindrome("").equals("") && shortestPalindrome("aa").equals("aa"), "LC 214 edges");
        check(prefixFunction("aaaa")[3] == 3, "without the separator, aa+aa gives a border longer than aa");
        for (int trial = 0; trial < 500; trial++) {
            StringBuilder s = new StringBuilder();
            for (int i = 0, n = rng.nextInt(9); i < n; i++) s.append((char) ('a' + rng.nextInt(3)));
            String answer = shortestPalindrome(s.toString());
            check(isPalindrome(answer) && answer.endsWith(s.toString()), "LC 214 random");
            for (int add = 0; add < answer.length() - s.length(); add++) {
                check(!isPalindrome(new StringBuilder(s.substring(s.length() - add)).reverse() + s.toString()), "LC 214 is shortest");
            }
        }
        System.out.println("OK");
    }
}
