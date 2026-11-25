import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Run from this folder with: java StringLab.java
public class StringLab {

    // Level 2: a code point above U+FFFF becomes two UTF-16 code units (a surrogate pair).
    static char[] surrogatePair(int codePoint) {
        int offset = codePoint - 0x10000;      // 20 bits remain
        char high = (char) (0xD800 + (offset >> 10));
        char low = (char) (0xDC00 + (offset & 0x3FF));
        return new char[] {high, low};
    }

    // Level 3: += builds a brand-new string every time.
    static int charactersCopiedByPlusEquals(int n) {
        String s = "";
        int copied = 0;
        for (int i = 0; i < n; i++) {
            s += "a";                          // a new string of length i + 1, every character copied
            copied += s.length();
        }
        return copied;
    }

    static String repeatWithBuilder(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);  // writes into spare room; grows about 2x when full
        return sb.toString();
    }

    // Level 4: a palindrome reads the same from both ends.
    static boolean isPalindrome(String s) {
        for (int i = 0, j = s.length() - 1; i < j; i++, j--) {
            if (s.charAt(i) != s.charAt(j)) return false;
        }
        return true;
    }

    // Level 5: anagrams have the same letter counts.
    static int[] letterCounts(String s) {
        int[] count = new int[26];
        for (char c : s.toCharArray()) count[c - 'a']++;  // 'a' -> 0, 'b' -> 1, ..., 'z' -> 25
        return count;
    }

    static boolean isAnagram(String s, String t) {
        return Arrays.equals(letterCounts(s), letterCounts(t));
    }

    // Interview 1: LeetCode 49, Group Anagrams.
    static List<List<String>> groupAnagrams(String[] words) {
        Map<String, List<String>> groups = new HashMap<>();
        for (String w : words) {
            String key = Arrays.toString(letterCounts(w));    // equal counts, equal key
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(w);
        }
        return new ArrayList<>(groups.values());
    }

    // Interview 2: LeetCode 5, Longest Palindromic Substring.
    static String longestPalindrome(String s) {
        int start = 0, end = 0;                // the best palindrome so far is s[start..end)
        for (int c = 0; c < s.length(); c++) {
            int length = Math.max(expand(s, c, c), expand(s, c, c + 1));  // odd and even centres
            if (length > end - start) {
                start = c - (length - 1) / 2;
                end = start + length;
            }
        }
        return s.substring(start, end);
    }

    static int expand(String s, int lo, int hi) {      // length of the palindrome grown from s[lo..hi]
        while (lo >= 0 && hi < s.length() && s.charAt(lo) == s.charAt(hi)) {
            lo--;
            hi++;
        }
        return hi - lo - 1;
    }

    // Interview 3: LeetCode 32, Longest Valid Parentheses.
    static int longestValidParentheses(String s) {
        String reversed = new StringBuilder(s).reverse().toString();
        return Math.max(scan(s, '('), scan(reversed, ')'));
    }

    static int scan(String s, char open) {     // longest balanced run, reading left to right
        int opens = 0, closes = 0, best = 0;
        for (char ch : s.toCharArray()) {
            if (ch == open) opens++;
            else closes++;
            if (opens == closes) best = Math.max(best, 2 * closes);
            else if (closes > opens) opens = closes = 0;   // an unmatched closer: start afresh
        }
        return best;
    }

    static Set<Set<String>> asSets(List<List<String>> groups) {
        Set<Set<String>> sets = new HashSet<>();
        for (List<String> g : groups) sets.add(new HashSet<>(g));
        return sets;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        String word = "java";
        check(word.length() * (word.length() + 1) / 2 == 10, "java has 10 non-empty substrings");
        Set<String> substrings = new HashSet<>();
        for (int i = 0; i < 4; i++) for (int j = i + 1; j <= 4; j++) substrings.add(word.substring(i, j));
        check(substrings.size() == 9, "java has 10 substrings but only 9 distinct ones (a repeats)");

        String upper = word.toUpperCase();
        check(word.equals("java") && upper.equals("JAVA"), "toUpperCase returns a new string");
        check('a' == 97 && 'z' - 'a' == 25 && 'A' == 65, "characters are numbers");

        String smile = "😀";        // U+1F600, grinning face
        check(smile.length() == 2 && smile.codePointCount(0, smile.length()) == 1, "emoji: 2 code units, 1 code point");
        check(smile.codePointAt(0) == 0x1F600 && smile.charAt(0) == 0xD83D && smile.charAt(1) == 0xDE00, "surrogates");
        char[] pair = surrogatePair(0x1F600);
        check(pair[0] == 0xD83D && pair[1] == 0xDE00, "surrogate formula");
        check(new StringBuilder("a" + smile).reverse().toString().equals(smile + "a"), "reverse keeps pairs together");

        check(charactersCopiedByPlusEquals(5) == 15, "5 appends copy 1 + 2 + 3 + 4 + 5 = 15");
        check(charactersCopiedByPlusEquals(10_000) == 50_005_000, "10,000 appends copy 50,005,000 characters");
        check(repeatWithBuilder('a', 10_000).equals("a".repeat(10_000)), "StringBuilder builds the same string");
        int capacity = 16, builderCopies = 0;
        for (int size = 0; size < 10_000; size++) {
            if (size == capacity) { builderCopies += size; capacity = 2 * capacity + 2; }
        }
        check(builderCopies == 16 + 34 + 70 + 142 + 286 + 574 + 1150 + 2302 + 4606 + 9214 && builderCopies == 18_394,
                "StringBuilder copies 18,394 characters for 10,000 appends");

        check(isPalindrome("racecar") && isPalindrome("abba") && isPalindrome("") && !isPalindrome("rocket"), "palindromes");
        check(isAnagram("listen", "silent") && !isAnagram("rat", "car"), "anagrams");
        int[] counts = letterCounts("listen");
        check(counts['e' - 'a'] == 1 && counts['l' - 'a'] == 1 && counts['z' - 'a'] == 0, "letter counts");

        check("apple".compareTo("apply") == -20 && "app".compareTo("apple") == -2, "compareTo is a difference");
        String built = new StringBuilder("ja").append("va").toString();
        check(built.equals("java") && built != "java", "equals compares letters, == compares objects");

        Set<Set<String>> expected = Set.of(Set.of("bat"), Set.of("nat", "tan"), Set.of("ate", "eat", "tea"));
        check(asSets(groupAnagrams(new String[] {"eat", "tea", "tan", "ate", "nat", "bat"})).equals(expected), "LC 49");
        check(groupAnagrams(new String[] {""}).size() == 1, "LC 49 empty word");

        check(longestPalindrome("babad").equals("bab"), "LC 5 odd centre");
        check(longestPalindrome("cbbd").equals("bb"), "LC 5 even centre");
        check(longestPalindrome("a").equals("a"), "LC 5 single letter");

        check(longestValidParentheses(")()())") == 4, "LC 32");
        check(longestValidParentheses("(()") == 2, "LC 32 needs the right-to-left pass");
        check(scan("(()", '(') == 0, "LC 32 left-to-right pass alone misses (()");
        check(longestValidParentheses("") == 0 && longestValidParentheses("()(())") == 6, "LC 32 edges");
        System.out.println("OK");
    }
}
