import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Run from this folder with: java SuffixArrayLab.java
public class SuffixArrayLab {

    // Level 1: sort the suffixes directly.
    static Integer[] naiveSuffixArray(String s) {
        Integer[] sa = new Integer[s.length()];
        for (int i = 0; i < sa.length; i++) sa[i] = i;
        Arrays.sort(sa, (a, b) -> s.substring(a).compareTo(s.substring(b)));   // each comparison may read n characters
        return sa;
    }

    // Level 2: prefix doubling. Round k sorts the suffixes by their first 2k characters.
    static int[] suffixArray(String s) {
        int n = s.length();
        int[] rank = s.chars().toArray();   // round 0: rank by the first character
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        for (int k = 1; ; k *= 2) {
            rank = sortByPairs(order, rank, k);
            if (rank[order[n - 1]] == n - 1) break;   // every suffix has its own rank: sorted
        }
        int[] sa = new int[n];
        for (int i = 0; i < n; i++) sa[i] = order[i];
        return sa;
    }

    // Sorts order by the pair (rank[i], rank[i + k]) and returns the new ranks.
    static int[] sortByPairs(Integer[] order, int[] rank, int k) {
        int n = rank.length;
        long[] key = new long[n];   // the first half's rank in the high 32 bits, the second half's in the low 32
        for (int i = 0; i < n; i++) key[i] = ((long) rank[i] << 32) | (i + k < n ? rank[i + k] + 1 : 0);   // 0 = no second half
        Arrays.sort(order, Comparator.comparingLong(i -> key[i]));
        int[] newRank = new int[n];
        for (int j = 1; j < n; j++) newRank[order[j]] = newRank[order[j - 1]] + (key[order[j]] > key[order[j - 1]] ? 1 : 0);
        return newRank;
    }

    // Level 3: Kasai's LCP array. lcp[r] = common prefix of the suffixes at ranks r - 1 and r.
    static int[] lcpArray(String s, int[] sa) {
        int n = s.length();
        int[] rank = new int[n], lcp = new int[n];
        for (int r = 0; r < n; r++) rank[sa[r]] = r;
        for (int i = 0, h = 0; i < n; i++) {
            if (rank[i] == 0) { h = 0; continue; }
            int j = sa[rank[i] - 1];   // the suffix just before suffix i in sorted order
            while (i + h < n && j + h < n && s.charAt(i + h) == s.charAt(j + h)) h++;
            lcp[rank[i]] = h;
            if (h > 0) h--;            // Kasai: the next suffix keeps at least h - 1
        }
        return lcp;
    }

    // Level 4: count distinct substrings.
    static long distinctSubstrings(String s) {
        long n = s.length(), total = n * (n + 1) / 2;
        for (int common : lcpArray(s, suffixArray(s))) total -= common;   // prefixes already counted by the previous suffix
        return total;
    }

    // Level 5: the longest repeated substring.
    static String longestRepeatedSubstring(String s) {
        int[] sa = suffixArray(s), lcp = lcpArray(s, sa);
        int best = 0;
        for (int r = 1; r < lcp.length; r++) if (lcp[r] > lcp[best]) best = r;
        return s.substring(sa[best], sa[best] + lcp[best]);
    }

    // Level 6: every occurrence of a pattern is one block of the suffix array.
    static int firstAtLeast(String s, int[] sa, String pattern) {   // the first rank whose suffix is >= pattern
        int low = 0, high = sa.length;
        while (low < high) {
            int mid = (low + high) / 2;
            if (s.substring(sa[mid]).compareTo(pattern) < 0) low = mid + 1; else high = mid;
        }
        return low;
    }

    static List<Integer> occurrences(String s, int[] sa, String pattern) {
        List<Integer> starts = new ArrayList<>();
        for (int r = firstAtLeast(s, sa, pattern); r < sa.length && s.startsWith(pattern, sa[r]); r++) starts.add(sa[r]);
        return starts;
    }

    // Interview 1: LeetCode 1163, Last Substring in Lexicographical Order.
    static String lastSubstringBySuffixArray(String s) {
        return s.substring(suffixArray(s)[s.length() - 1]);   // the largest suffix is the last in sorted order
    }

    static String lastSubstring(String s) {
        int n = s.length(), i = 0, j = 1, k = 0;   // i = best start so far, j = challenger, k = letters matched
        while (j + k < n) {
            char a = s.charAt(i + k), b = s.charAt(j + k);
            if (a == b) { k++; continue; }
            if (a < b) { i = Math.max(i + k + 1, j); j = i + 1; }   // starts i .. i + k all lose to j .. j + k
            else j = j + k + 1;                                       // starts j .. j + k all lose to i .. i + k
            k = 0;
        }
        return s.substring(i);
    }

    // Interview 2: LeetCode 2223, Sum of Scores of Built Strings.
    static long sumScores(String s) {
        int n = s.length();
        int[] sa = suffixArray(s), lcp = lcpArray(s, sa);
        int home = 0;
        while (sa[home] != 0) home++;   // the rank of the whole string
        long total = n;                 // the whole string matches itself completely
        int common = n;
        for (int r = home + 1; r < n; r++) {
            common = Math.min(common, lcp[r]);   // LCP with the whole string = smallest lcp in between
            total += common;
        }
        common = n;
        for (int r = home - 1; r >= 0; r--) {
            common = Math.min(common, lcp[r + 1]);
            total += common;
        }
        return total;
    }

    // Plain versions, only used to confirm the answers on small inputs.
    static String bruteLastSubstring(String s) {
        String best = "";
        for (int i = 0; i < s.length(); i++) if (s.substring(i).compareTo(best) > 0) best = s.substring(i);
        return best;
    }

    static long bruteScores(String s) {
        long total = 0;
        for (int i = 0; i < s.length(); i++) {
            int k = 0;
            while (i + k < s.length() && s.charAt(k) == s.charAt(i + k)) k++;
            total += k;
        }
        return total;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        String banana = "banana";
        int[] sa = suffixArray(banana);
        check(Arrays.equals(sa, new int[] {5, 3, 1, 0, 4, 2}), "SA(banana)");
        check(Arrays.equals(naiveSuffixArray(banana), new Integer[] {5, 3, 1, 0, 4, 2}), "naive SA(banana)");
        check(6 * 7 / 2 == 21, "banana has 21 substrings counted with repeats");
        Integer[] order = {0, 1, 2, 3, 4, 5};
        int[] round1 = sortByPairs(order, banana.chars().toArray(), 1);
        check(Arrays.equals(round1, new int[] {2, 1, 3, 1, 3, 0}), "ranks by the first 2 letters");
        int[][] pairs = new int[6][];
        for (int i = 0; i < 6; i++) pairs[i] = new int[] {round1[i], i + 2 < 6 ? round1[i + 2] : -1};
        check(Arrays.deepEquals(pairs, new int[][] {{2, 3}, {1, 1}, {3, 3}, {1, 0}, {3, -1}, {0, -1}}), "round 2 pairs");
        int[] round2 = sortByPairs(order, round1, 2);
        check(Arrays.equals(round2, new int[] {3, 2, 5, 1, 4, 0}), "ranks by the first 4 letters, all different");
        check(Arrays.equals(order, new Integer[] {5, 3, 1, 0, 4, 2}), "two rounds sort banana");

        int[] lcp = lcpArray(banana, sa);
        check(Arrays.equals(lcp, new int[] {0, 1, 3, 0, 0, 2}) && Arrays.stream(lcp).sum() == 6, "LCP(banana)");
        check(distinctSubstrings(banana) == 15 && 21 - 6 == 15, "banana has 15 distinct substrings");
        Set<String> all = new HashSet<>();
        for (int i = 0; i < banana.length(); i++) for (int j = i + 1; j <= banana.length(); j++) all.add(banana.substring(i, j));
        check(all.size() == 15, "15 confirmed by listing them");
        check(longestRepeatedSubstring(banana).equals("ana"), "longest repeated substring of banana");
        check(occurrences(banana, sa, "ana").equals(List.of(3, 1)) && occurrences(banana, sa, "x").isEmpty(), "ana occurs at 3 and 1");
        check(firstAtLeast(banana, sa, "ana") == 1, "the block for ana starts at rank 1");
        check(Math.min(lcp[1], lcp[2]) == 1, "LCP(a, anana) is the smallest lcp between ranks 0 and 2");

        // Edge cases and random cross-checks of the fast suffix array.
        check(Arrays.equals(suffixArray("a"), new int[] {0}) && Arrays.equals(suffixArray("aaaa"), new int[] {3, 2, 1, 0}), "SA edges");
        check(distinctSubstrings("aaaa") == 4 && distinctSubstrings("abcd") == 10, "distinct substring edges");
        long n = 100_000;
        check(n * (n + 1) / 2 == 5_000_050_000L && 5_000_050_000L > Integer.MAX_VALUE, "n(n+1)/2 overflows an int");
        java.util.Random rng = new java.util.Random(11);
        for (int trial = 0; trial < 500; trial++) {
            StringBuilder text = new StringBuilder();
            for (int i = 0, len = 1 + rng.nextInt(12); i < len; i++) text.append((char) ('a' + rng.nextInt(3)));
            String s = text.toString();
            Integer[] naive = naiveSuffixArray(s);
            int[] fast = suffixArray(s);
            for (int i = 0; i < s.length(); i++) check(naive[i] == fast[i], "prefix doubling agrees with sorting: " + s);
            Set<String> distinct = new HashSet<>();
            for (int i = 0; i < s.length(); i++) for (int j = i + 1; j <= s.length(); j++) distinct.add(s.substring(i, j));
            check(distinctSubstrings(s) == distinct.size(), "distinct substrings: " + s);
            check(lastSubstring(s).equals(bruteLastSubstring(s)), "LC 1163 random: " + s);
            check(lastSubstringBySuffixArray(s).equals(bruteLastSubstring(s)), "LC 1163 by suffix array: " + s);
            check(sumScores(s) == bruteScores(s), "LC 2223 random: " + s);
        }

        // Interview problems.
        check(lastSubstring("abab").equals("bab") && lastSubstring("leetcode").equals("tcode"), "LC 1163");
        check(Arrays.equals(suffixArray("abab"), new int[] {2, 0, 3, 1}), "SA(abab)");
        check(lastSubstring("zzz").equals("zzz") && lastSubstring("a").equals("a"), "LC 1163 edges");
        check(Arrays.equals(suffixArray("babab"), new int[] {3, 1, 4, 2, 0}), "SA(babab)");
        check(Arrays.equals(lcpArray("babab", suffixArray("babab")), new int[] {0, 2, 0, 1, 3}), "LCP(babab)");
        check(sumScores("babab") == 9 && sumScores("azbazbzaz") == 14, "LC 2223");
        check(sumScores("a") == 1 && sumScores("aaaa") == 10, "LC 2223 edges");
        check(sumScores("a".repeat(100_000)) == 5_000_050_000L, "LC 2223 needs a long for 10^5 copies of a");
        System.out.println("OK");
    }
}
