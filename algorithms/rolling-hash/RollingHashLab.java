import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Run from this folder with: java RollingHashLab.java
public class RollingHashLab {

    // Level 2: slide a window of digits along a number. Base 10, no modulus.
    static List<Integer> slidingNumbers(String digits, int length) {
        int top = (int) Math.pow(10, length - 1), h = 0;   // top = 10^(L-1), the place value of the leftmost digit
        List<Integer> windows = new ArrayList<>();
        for (int i = 0; i < digits.length(); i++) {
            if (i >= length) h -= (digits.charAt(i - length) - '0') * top;   // drop the leftmost digit
            h = h * 10 + (digits.charAt(i) - '0');                          // shift left, append the new digit
            if (i >= length - 1) windows.add(h);
        }
        return windows;
    }

    // Level 3: letters, a base B and a prime modulus M.
    static int value(char c) {
        return c - 'a' + 1;   // a = 1, ..., z = 26; never 0, so "a" and "aa" differ
    }

    record PolyHash(long base, long mod) {
        long of(String s) {
            long h = 0;
            for (char c : s.toCharArray()) h = (h * base + value(c)) % mod;   // Horner's rule
            return h;
        }

        long slide(long h, char out, char in, long topPower) {   // topPower = B^(L-1) mod M
            long dropped = Math.floorMod(h - value(out) * topPower, mod);
            return (dropped * base + value(in)) % mod;
        }

        long power(int exponent) {   // B^exponent mod M
            long p = 1;
            for (int i = 0; i < exponent; i++) p = p * base % mod;
            return p;
        }
    }

    // Level 4: Rabin-Karp. Compare hashes first, characters only when the hashes agree.
    static List<Integer> rabinKarp(String text, String pattern, PolyHash hasher) {
        int n = text.length(), length = pattern.length();
        List<Integer> matches = new ArrayList<>();
        if (length == 0 || length > n) return matches;
        long target = hasher.of(pattern), h = hasher.of(text.substring(0, length));
        long top = hasher.power(length - 1);
        for (int i = 0; ; i++) {
            if (h == target && text.startsWith(pattern, i)) matches.add(i);   // equal hashes can still be different strings
            if (i + length == n) return matches;
            h = hasher.slide(h, text.charAt(i), text.charAt(i + length), top);
        }
    }

    // Counts the windows whose hash equals the pattern's: true matches plus spurious hits.
    static int hashHits(String text, String pattern, PolyHash hasher) {
        int hits = 0;
        for (int i = 0; i + pattern.length() <= text.length(); i++) {
            if (hasher.of(text.substring(i, i + pattern.length())) == hasher.of(pattern)) hits++;
        }
        return hits;
    }

    // Level 5: prefix hashes give the hash of any substring in O(1).
    static final class PrefixHash {
        final long[] prefix, power;
        final long mod;

        PrefixHash(String s, long base, long mod) {
            this.mod = mod;
            prefix = new long[s.length() + 1];
            power = new long[s.length() + 1];
            power[0] = 1;
            for (int i = 0; i < s.length(); i++) {
                prefix[i + 1] = (prefix[i] * base + value(s.charAt(i))) % mod;
                power[i + 1] = power[i] * base % mod;
            }
        }

        long of(int from, int to) {   // the hash of s[from, to)
            return Math.floorMod(prefix[to] - prefix[from] * power[to - from], mod);
        }
    }

    // Level 6: two independent hashes packed into one 64-bit key.
    record DoubleHash(PrefixHash first, PrefixHash second) {
        DoubleHash(String s) {
            this(new PrefixHash(s, 131, 1_000_000_007L), new PrefixHash(s, 137, 998_244_353L));
        }

        long of(int from, int to) {
            return first.of(from, to) * second.mod + second.of(from, to);
        }
    }

    // Interview 1: LeetCode 187, Repeated DNA Sequences.
    static List<String> findRepeatedDnaSequences(String s) {
        Set<Integer> seen = new HashSet<>(), reported = new HashSet<>();
        List<String> repeated = new ArrayList<>();
        int h = 0;
        for (int i = 0; i < s.length(); i++) {
            h = ((h << 2) | "ACGT".indexOf(s.charAt(i))) & 0xFFFFF;   // keep the last 10 letters: 20 bits
            if (i >= 9 && !seen.add(h) && reported.add(h)) repeated.add(s.substring(i - 9, i + 1));
        }
        return repeated;
    }

    // Interview 2: LeetCode 1044, Longest Duplicate Substring.
    static String longestDupSubstring(String s) {
        DoubleHash hash = new DoubleHash(s);
        int low = 0, high = s.length() - 1, bestStart = 0;
        while (low < high) {   // the largest length that occurs twice
            int mid = (low + high + 1) / 2;
            int start = repeatedStart(hash, s.length(), mid);
            if (start >= 0) { low = mid; bestStart = start; } else high = mid - 1;
        }
        return s.substring(bestStart, bestStart + low);
    }

    static int repeatedStart(DoubleHash hash, int n, int length) {   // a window seen earlier, or -1
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i + length <= n; i++) {
            if (!seen.add(hash.of(i, i + length))) return i;
        }
        return -1;
    }

    // Interview 3: LeetCode 1316, Distinct Echo Substrings.
    static int distinctEchoSubstrings(String text) {
        DoubleHash hash = new DoubleHash(text);
        Set<Long> echoes = new HashSet<>();
        int n = text.length();
        for (int half = 1; 2 * half <= n; half++) {
            for (int i = 0; i + 2 * half <= n; i++) {
                if (hash.of(i, i + half) == hash.of(i + half, i + 2 * half)) echoes.add(hash.of(i, i + 2 * half));
            }
        }
        return echoes.size();
    }

    // Plain string versions, only used to confirm the hashed answers on small inputs.
    static String bruteLongestDup(String s) {
        for (int length = s.length() - 1; length > 0; length--) {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i + length <= s.length(); i++) {
                if (!seen.add(s.substring(i, i + length))) return s.substring(i, i + length);
            }
        }
        return "";
    }

    static int bruteEchoes(String s) {
        Set<String> echoes = new HashSet<>();
        for (int i = 0; i < s.length(); i++) {
            for (int half = 1; i + 2 * half <= s.length(); half++) {
                if (s.substring(i, i + half).equals(s.substring(i + half, i + 2 * half))) echoes.add(s.substring(i, i + 2 * half));
            }
        }
        return echoes.size();
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        // Levels 1 and 2: digits with base 10.
        check(3 * 100 + 1 * 10 + 4 == 314, "314 = 3*100 + 1*10 + 4");
        check(slidingNumbers("314159", 3).equals(List.of(314, 141, 415, 159)), "windows of 314159");
        check((314 - 3 * 100) * 10 + 1 == 141 && (141 - 1 * 100) * 10 + 5 == 415 && (415 - 4 * 100) * 10 + 9 == 159, "each slide");

        // Level 3: B = 31, M = 101 by hand.
        PolyHash small = new PolyHash(31, 101);
        check(1 * 961 + 2 * 31 + 3 == 1026 && small.of("abc") == 16, "H(abc) = 1026 mod 101 = 16");
        check(small.power(2) == 52 && 961 - 909 == 52, "31^2 mod 101 = 52");
        check(16 - 52 == -36 && Math.floorMod(-36, 101) == 65 && 65 * 31 + 4 == 2019 && 2019 % 101 == 100, "slide by hand");
        check(small.slide(16, 'a', 'd', 52) == 100 && small.of("bcd") == 100 && 2 * 961 + 3 * 31 + 4 == 2019, "slide agrees with direct");
        PolyHash big = new PolyHash(31, 1_000_000_007L);
        String text = "thequickbrownfox";
        long h = big.of(text.substring(0, 5)), top = big.power(4);
        for (int i = 1; i + 5 <= text.length(); i++) {
            h = big.slide(h, text.charAt(i - 1), text.charAt(i + 4), top);
            check(h == big.of(text.substring(i, i + 5)), "rolling equals recomputing at " + i);
        }

        // Level 4: Rabin-Karp and a spurious hit.
        check(small.of("map") == 16 && 13 * 961 + 1 * 31 + 16 == 12540 && 12540 % 101 == 16, "map collides with abc mod 101");
        check(small.of("apa") == 44 && small.of("pab") == 57, "the other windows of mapabc");
        check(rabinKarp("mapabc", "abc", small).equals(List.of(3)), "Rabin-Karp rejects the spurious hit");
        check(hashHits("mapabc", "abc", small) == 2, "two hash hits, one real");
        check(rabinKarp("abababa", "aba", big).equals(List.of(0, 2, 4)), "overlapping matches");
        check(rabinKarp("abc", "abcd", big).isEmpty() && rabinKarp("aaa", "a", big).equals(List.of(0, 1, 2)), "edges");

        // Level 5: prefix hashes on "abcd".
        PrefixHash prefixes = new PrefixHash("abcd", 31, 101);
        check(java.util.Arrays.equals(prefixes.prefix, new long[] {0, 1, 33, 16, 96}), "prefix hashes of abcd");
        check(prefixes.power[3] == 97 && 29791 % 101 == 97, "31^3 mod 101 = 97");
        check(prefixes.of(1, 4) == 100 && Math.floorMod(96 - 97, 101) == 100, "H(bcd) from prefixes");
        check(prefixes.of(0, 3) == 16, "H(abc) from prefixes");

        // Level 6: how many collisions to expect.
        check(Math.abs(1e10 / (2 * 1_000_000_007.0) - 5.0) < 0.01, "10^5 windows, one modulus: about 5 collisions");
        check(1e10 / (2 * 1_000_000_007.0 * 998_244_353.0) < 6e-9, "two moduli: about 5 * 10^-9");
        check(1_000_000_007L * 998_244_353L < Long.MAX_VALUE / 9, "the packed key fits in a long");
        check(small.of("elk") == 37 && small.of("sun") == 37, "another collision mod 101");
        check(new DoubleHash("elksun").of(0, 3) != new DoubleHash("elksun").of(3, 6), "double hashing separates them");

        // Interview problems.
        check(341 == 256 + 64 + 16 + 4 + 1 && 341 * 1024 == 349_184, "AAAAACCCCC and CCCCCAAAAA as base-4 numbers");
        check(findRepeatedDnaSequences("AAAAACCCCCAAAAACCCCCCAAAAAGGGTTT").equals(List.of("AAAAACCCCC", "CCCCCAAAAA")), "LC 187");
        check(findRepeatedDnaSequences("AAAAAAAAAAAAA").equals(List.of("AAAAAAAAAA")), "LC 187 example 2");
        check(findRepeatedDnaSequences("ACGT").isEmpty(), "LC 187 short input");
        check(longestDupSubstring("banana").equals("ana") && longestDupSubstring("abcd").equals(""), "LC 1044");
        check(repeatedStart(new DoubleHash("banana"), 6, 3) == 3 && repeatedStart(new DoubleHash("banana"), 6, 4) == -1, "banana lengths 3 and 4");
        check(distinctEchoSubstrings("abcabcabc") == 3 && distinctEchoSubstrings("leetcodeleetcode") == 2, "LC 1316");
        check(distinctEchoSubstrings("a") == 0 && distinctEchoSubstrings("aaaa") == 2, "LC 1316 edges");
        java.util.Random rng = new java.util.Random(3);
        for (int trial = 0; trial < 300; trial++) {
            StringBuilder s = new StringBuilder();
            int length = 2 + rng.nextInt(12);
            for (int i = 0; i < length; i++) s.append((char) ('a' + rng.nextInt(2)));
            String word = s.toString();
            check(longestDupSubstring(word).length() == bruteLongestDup(word).length(), "LC 1044 random " + word);
            check(distinctEchoSubstrings(word) == bruteEchoes(word), "LC 1316 random " + word);
        }
        System.out.println("OK");
    }
}
