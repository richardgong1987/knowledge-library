import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

// Run from this folder with: java CountingLab.java
public class CountingLab {

    // Level 1: a frequency array, one counter per letter.
    static int[] letterCounts(String word) {
        int[] count = new int[26];
        for (char c : word.toCharArray()) count[c - 'a']++;  // 'a' -> 0, 'b' -> 1, ..., 'z' -> 25
        return count;
    }

    static boolean isAnagram(String s, String t) {
        return Arrays.equals(letterCounts(s), letterCounts(t));
    }

    static long equalLetterPairs(String word) {             // pairs i < j with word[i] == word[j]
        long pairs = 0;
        for (int f : letterCounts(word)) pairs += (long) f * (f - 1) / 2;
        return pairs;
    }

    // Level 2: count the complement. Substrings with a vowel = all substrings - vowel-free ones.
    static long substringsWithVowel(String s) {
        long n = s.length(), withoutVowel = 0, run = 0;
        for (char c : s.toCharArray()) {
            run = "aeiou".indexOf(c) >= 0 ? 0 : run + 1;   // length of the vowel-free run ending here
            withoutVowel += run;                            // that many vowel-free substrings end here
        }
        return n * (n + 1) / 2 - withoutVowel;
    }

    // Level 3: inclusion-exclusion over every non-empty group of divisors.
    static long countDivisibleByAny(long n, int[] divisors) {   // divisors pairwise coprime, so the lcm is the product
        long total = 0;
        for (int mask = 1; mask < (1 << divisors.length); mask++) {
            long product = 1;
            for (int i = 0; i < divisors.length; i++) if ((mask >> i & 1) == 1) product *= divisors[i];
            total += (Integer.bitCount(mask) % 2 == 1 ? 1 : -1) * (n / product); // odd-sized groups add, even-sized subtract
        }
        return total;
    }

    // Level 4: the pigeonhole principle finds a subarray whose sum is a multiple of n.
    static int[] subarrayDivisibleByLength(int[] a) {       // returns {from, to}: a[from..to] sums to a multiple of n
        int n = a.length;
        int[] firstSeen = new int[n];                       // firstSeen[r] = first prefix length with remainder r
        Arrays.fill(firstSeen, -1);
        firstSeen[0] = 0;                                   // the empty prefix has sum 0
        long prefix = 0;
        for (int j = 1; j <= n; j++) {
            prefix += a[j - 1];
            int r = Math.floorMod(prefix, n);
            if (firstSeen[r] >= 0) return new int[] {firstSeen[r], j - 1}; // n + 1 prefixes, only n remainders
            firstSeen[r] = j;
        }
        throw new AssertionError("impossible by the pigeonhole principle");
    }

    // Level 5: the contribution technique.
    static long sumOfAllSubarraySums(int[] a) {
        long total = 0;
        for (int i = 0; i < a.length; i++) total += (long) a[i] * (i + 1) * (a.length - i); // a[i] is in (i+1)(n-i) subarrays
        return total;
    }

    // Interview 1: LeetCode 2063, Vowels of All Substrings.
    static long countVowels(String word) {
        long total = 0, n = word.length();
        for (int i = 0; i < n; i++) {
            if ("aeiou".indexOf(word.charAt(i)) >= 0) total += (i + 1) * (n - i); // this vowel is in (i+1)(n-i) substrings
        }
        return total;
    }

    // Interview 2: LeetCode 2262, Total Appeal of A String.
    static long appealSum(String s) {
        int[] lastSeen = new int[26];
        Arrays.fill(lastSeen, -1);
        long total = 0, n = s.length();
        for (int i = 0; i < n; i++) {
            int c = s.charAt(i) - 'a';
            total += (i - lastSeen[c]) * (n - i);           // substrings in which s[i] is the first copy of its letter
            lastSeen[c] = i;
        }
        return total;
    }

    // Interview 3: LeetCode 828, Count Unique Characters of All Substrings of a Given String.
    static int uniqueLetterString(String s) {
        int n = s.length();
        int[] previous = new int[n], next = new int[n], lastSeen = new int[26];
        Arrays.fill(lastSeen, -1);
        for (int i = 0; i < n; i++) {
            previous[i] = lastSeen[s.charAt(i) - 'A'];
            lastSeen[s.charAt(i) - 'A'] = i;
        }
        Arrays.fill(lastSeen, n);
        for (int i = n - 1; i >= 0; i--) {
            next[i] = lastSeen[s.charAt(i) - 'A'];
            lastSeen[s.charAt(i) - 'A'] = i;
        }
        long total = 0;
        for (int i = 0; i < n; i++) total += (long) (i - previous[i]) * (next[i] - i); // s[i] is the only copy of its letter
        return (int) total;
    }

    // Brute force for the checks: list every substring and look at it directly.
    static long vowelsByBruteForce(String s) {
        long total = 0;
        for (String t : allSubstrings(s)) for (char c : t.toCharArray()) if ("aeiou".indexOf(c) >= 0) total++;
        return total;
    }

    static long appealByBruteForce(String s) {
        long total = 0;
        for (String t : allSubstrings(s)) total += t.chars().distinct().count();
        return total;
    }

    static long uniqueLettersByBruteForce(String s) {
        long total = 0;
        for (String t : allSubstrings(s)) for (char c : t.toCharArray()) if (t.indexOf(c) == t.lastIndexOf(c)) total++;
        return total;
    }

    static List<String> allSubstrings(String s) {
        List<String> substrings = new ArrayList<>();
        for (int i = 0; i < s.length(); i++)
            for (int j = i + 1; j <= s.length(); j++) substrings.add(s.substring(i, j));
        return substrings;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        // Level 1
        check(isAnagram("listen", "silent") && !isAnagram("listen", "listens"), "anagrams");
        int[] banana = letterCounts("banana");
        check(banana[0] == 3 && banana['n' - 'a'] == 2 && banana[1] == 1, "banana counts");
        check(equalLetterPairs("banana") == 4, "3 + 1 equal pairs in banana");

        // Level 2
        check(substringsWithVowel("abc") == 3 && 3 * 4 / 2 - 3 == 3, "abc: 6 - 3");
        check(substringsWithVowel("aba") == 5 && substringsWithVowel("xyz") == 0, "aba and xyz");
        check(100_000L * 100_001 / 2 == 5_000_050_000L && 5_000_050_000L > Integer.MAX_VALUE, "n(n+1)/2 needs a long");

        // Level 3
        check(countDivisibleByAny(100, new int[] {2, 3}) == 67 && 50 + 33 - 16 == 67, "2 or 3");
        check(countDivisibleByAny(100, new int[] {2, 3, 5}) == 74 && 50 + 33 + 20 - 16 - 10 - 6 + 3 == 74, "2, 3 or 5");
        int none = 0;
        for (int k = 1; k <= 100; k++) if (k % 2 != 0 && k % 3 != 0 && k % 5 != 0) none++;
        check(none == 26 && 100 - 74 == 26, "26 numbers avoid 2, 3 and 5");
        check(100 / 24 == 4 && 100 / 12 == 8, "4 and 6 need the lcm 12, not the product 24");

        // Level 4
        check(Arrays.equals(subarrayDivisibleByLength(new int[] {3, 1, 4, 2}), new int[] {0, 1}), "3 + 1 = 4");
        Random random = new Random(1);
        for (int trial = 0; trial < 1000; trial++) {
            int[] a = random.ints(1 + random.nextInt(8), -20, 20).toArray();
            int[] range = subarrayDivisibleByLength(a);
            long sum = 0;
            for (int i = range[0]; i <= range[1]; i++) sum += a[i];
            check(Math.floorMod(sum, a.length) == 0, "pigeonhole always finds one");
        }
        check((int) Math.ceil(10 / 3.0) == 4, "10 socks in 3 drawers: some drawer has 4");

        // Level 5
        check(sumOfAllSubarraySums(new int[] {1, 2, 3}) == 20 && 1 + 2 + 3 + 3 + 5 + 6 == 20, "sum of subarray sums");
        check(1 * 3 + 2 * 2 + 3 * 1 == 10 && 1 + 1 + 1 + 2 + 2 + 3 == 10, "substring lengths of a 3-letter string");

        // Interview problems
        check(countVowels("aba") == 6 && countVowels("abc") == 3 && countVowels("ltcd") == 0, "LC 2063");
        check(appealSum("abbca") == 28 && appealSum("code") == 20, "LC 2262");
        check(5 + 8 + 3 + 8 + 4 == 28 && 4 + 6 + 6 + 4 == 20, "LC 2262 contributions");
        check(uniqueLetterString("ABC") == 10 && uniqueLetterString("ABA") == 8 && uniqueLetterString("LEETCODE") == 92, "LC 828");
        check(2 + 4 + 2 == 8, "LC 828 contributions for ABA");
        String[] samples = {"a", "aba", "abbca", "leetcode", "aaaa", "abcabcab", "zebra"};
        for (String s : samples) {
            check(countVowels(s) == vowelsByBruteForce(s), "LC 2063 brute force " + s);
            check(appealSum(s) == appealByBruteForce(s), "LC 2262 brute force " + s);
            check(uniqueLetterString(s.toUpperCase()) == uniqueLettersByBruteForce(s), "LC 828 brute force " + s);
        }
        check(countVowels("a".repeat(100_000)) == 166_671_666_700_000L, "LC 2063 needs a long");
        System.out.println("OK");
    }
}
