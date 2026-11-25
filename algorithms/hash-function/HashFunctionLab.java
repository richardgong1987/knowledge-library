import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

// Run from this folder with: java HashFunctionLab.java
public class HashFunctionLab {

    // Level 2: the division method.
    static int divisionHash(int k, int m) {
        return Math.floorMod(k, m);
    }

    // Level 3: the multiplication method, in floating point and in integer form.
    static final double A = (Math.sqrt(5) - 1) / 2;   // 0.6180339887...

    static int multiplicationHash(double k, int m) {
        double fraction = k * A - Math.floor(k * A);
        return (int) Math.floor(m * fraction);
    }

    static int fibonacciHash(int k, int p) {   // a table of m = 2^p slots
        return (k * 0x9E3779B9) >>> (32 - p);   // 0x9E3779B9 = floor(2^32 * A)
    }

    // Level 4: MurmurHash3's 32-bit finaliser, a mixer with good avalanche.
    static int mix(int h) {
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }

    // Level 5: a universal family. Choose a and b at random once, when the table is created.
    record UniversalHash(long a, long b, long p, int m) {
        static UniversalHash random(long p, int m, Random rng) {
            return new UniversalHash(1 + rng.nextLong(p - 1), rng.nextLong(p), p, m);
        }

        int of(long k) {
            return (int) ((a * k + b) % p % m);
        }
    }

    // Level 6: equal objects must have equal hash codes.
    static final class Point {
        final int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Point p && p.x == x && p.y == y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);   // 31 * (31 * 1 + x) + y
        }
    }

    record Cell(int row, int col) {}   // equals and hashCode are generated from both fields

    // Level 7: a cryptographic hash from the JDK.
    static String sha256Hex(String text) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    static int differingBits(byte[] x, byte[] y) {
        int count = 0;
        for (int i = 0; i < x.length; i++) count += Integer.bitCount((x[i] ^ y[i]) & 0xFF);
        return count;
    }

    static double fiftyPercentCollisionKeys(double m) {   // birthday bound: n = sqrt(2 m ln 2)
        return Math.sqrt(2 * m * Math.log(2));
    }

    // Interview 1: LeetCode 49, Group Anagrams.
    static List<List<String>> groupAnagrams(String[] words) {
        Map<String, List<String>> groups = new HashMap<>();
        for (String word : words) {
            char[] letters = word.toCharArray();
            Arrays.sort(letters);   // anagrams share one sorted spelling
            groups.computeIfAbsent(new String(letters), key -> new ArrayList<>()).add(word);
        }
        return new ArrayList<>(groups.values());
    }

    // Interview 2: LeetCode 535, Encode and Decode TinyURL.
    static final class Codec {
        static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final Map<String, String> urlByCode = new HashMap<>();
        final Random random = new Random(42);

        String encode(String longUrl) {
            String code;
            do {
                StringBuilder letters = new StringBuilder();
                for (int i = 0; i < 6; i++) letters.append(ALPHABET.charAt(random.nextInt(62)));
                code = letters.toString();
            } while (urlByCode.containsKey(code));   // a collision: draw another code
            urlByCode.put(code, longUrl);
            return "http://tinyurl.com/" + code;
        }

        String decode(String shortUrl) {
            return urlByCode.get(shortUrl.substring(shortUrl.lastIndexOf('/') + 1));
        }
    }

    // Interview 3: LeetCode 1923, Longest Common Subpath.
    static final long BASE = 100_003, MOD1 = 1_000_000_007, MOD2 = 998_244_353;

    static int longestCommonSubpath(int n, int[][] paths) {
        int low = 0, high = Integer.MAX_VALUE;
        for (int[] path : paths) high = Math.min(high, path.length);
        while (low < high) {   // the largest length that every path shares
            int mid = (low + high + 1) / 2;
            if (allShare(paths, mid)) low = mid; else high = mid - 1;
        }
        return low;
    }

    static boolean allShare(int[][] paths, int length) {
        Set<Long> common = windowHashes(paths[0], length);
        for (int i = 1; i < paths.length && !common.isEmpty(); i++) common.retainAll(windowHashes(paths[i], length));
        return !common.isEmpty();
    }

    static Set<Long> windowHashes(int[] path, int length) {
        long power1 = 1, power2 = 1;   // BASE^length, the weight of the city that leaves the window
        for (int i = 0; i < length; i++) { power1 = power1 * BASE % MOD1; power2 = power2 * BASE % MOD2; }
        Set<Long> hashes = new HashSet<>();
        long h1 = 0, h2 = 0;
        for (int i = 0; i < path.length; i++) {
            h1 = (h1 * BASE + path[i]) % MOD1;
            h2 = (h2 * BASE + path[i]) % MOD2;
            if (i >= length) {
                h1 = Math.floorMod(h1 - path[i - length] * power1, MOD1);
                h2 = Math.floorMod(h2 - path[i - length] * power2, MOD2);
            }
            if (i >= length - 1) hashes.add(h1 * MOD2 + h2);   // two hashes packed into one 64-bit key
        }
        return hashes;
    }

    // Plain comparison of every subpath; only used to confirm the hashed answer on small inputs.
    static int bruteForceCommonSubpath(int[][] paths) {
        int best = 0;
        for (int length = 1; length <= paths[0].length; length++) {
            Set<List<Integer>> common = null;
            for (int[] path : paths) {
                Set<List<Integer>> windows = new HashSet<>();
                for (int i = 0; i + length <= path.length; i++) {
                    List<Integer> window = new ArrayList<>();
                    for (int j = i; j < i + length; j++) window.add(path[j]);
                    windows.add(window);
                }
                if (common == null) common = windows; else common.retainAll(windows);
            }
            if (!common.isEmpty()) best = length;
        }
        return best;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) throws Exception {
        // Level 2: keys that share a factor with m all collide.
        int[] aligned = {8, 16, 24, 32, 40};
        for (int k : aligned) check(divisionHash(k, 8) == 0, "multiples of 8 all land in slot 0 of 8");
        for (int i = 0; i < aligned.length; i++) check(divisionHash(aligned[i], 7) == i + 1, "m = 7 spreads them to 1..5");
        String[] words = {"ab", "ba", "bc", "zb"};
        int[] asNumbers = new int[words.length];
        for (int i = 0; i < words.length; i++) asNumbers[i] = words[i].charAt(0) * 256 + words[i].charAt(1);
        check(Arrays.equals(asNumbers, new int[] {24930, 25185, 25187, 31330}), "two letters read as a base-256 number");
        int[][] expected = {{98, 97, 99, 98}, {195, 195, 197, 220}, {1, 256, 1, 233}, {395, 650, 652, 486}};
        int[] sizes = {256, 255, 257, 701};
        for (int row = 0; row < sizes.length; row++) {
            for (int i = 0; i < words.length; i++) check(divisionHash(asNumbers[i], sizes[row]) == expected[row][i], "m = " + sizes[row]);
        }
        check(97 + 98 == 195 && 98 - 97 == 1 && 99 - 98 == 1, "255 sums the letters, 257 alternates their signs");
        check(divisionHash(-3, 8) == 5, "floorMod keeps negative keys in range");

        // Level 3: golden-ratio multiplication.
        check(Math.abs(A - 0.6180339887) < 1e-10, "Knuth's A");
        check(Math.abs(3 * A - 1.8541019662) < 1e-9, "3A");
        check((long) Math.floor(4294967296.0 * A) == 2654435769L && 0x9E3779B9 == (int) 2654435769L, "2^32 * A");
        int[] spread = new int[8];
        for (int k = 1; k <= 8; k++) spread[k - 1] = multiplicationHash(k, 8);
        check(Arrays.equals(spread, new int[] {4, 1, 6, 3, 0, 5, 2, 7}), "keys 1..8 fill all 8 slots");
        for (int k = 1; k <= 8; k++) check(fibonacciHash(k, 3) == spread[k - 1], "integer form agrees");
        check(multiplicationHash(8, 8) == 7 && multiplicationHash(16, 8) == 7, "structured keys can still collide");

        // Level 4: avalanche.
        check(Integer.bitCount(10 ^ 11) == 1, "10 and 11 differ in one bit");
        check(mix(10) == 0xe9250490 && mix(11) == 0x93a0fe04, "mixed values of 10 and 11");
        check(Integer.bitCount(mix(10) ^ mix(11)) == 17, "mixed, they differ in 17 of 32 bits");
        int totalFlips = 0;
        for (int k = 0; k < 10_000; k++) totalFlips += Integer.bitCount(mix(k) ^ mix(k ^ 1));
        check(Math.abs(totalFlips / 10_000.0 - 16) < 0.5, "on average about 16 of 32 output bits flip");

        // Level 5: universal hashing with p = 13, m = 4.
        check(new UniversalHash(5, 2, 13, 4).of(7) == 3 && (5 * 7 + 2) % 13 == 11, "h(7) with a = 5, b = 2");
        int worst = 0;
        for (int k = 0; k < 13; k++) {
            for (int l = k + 1; l < 13; l++) {
                int collisions = 0;
                for (int a = 1; a < 13; a++) {
                    for (int b = 0; b < 13; b++) {
                        if (new UniversalHash(a, b, 13, 4).of(k) == new UniversalHash(a, b, 13, 4).of(l)) collisions++;
                    }
                }
                worst = Math.max(worst, collisions);
            }
        }
        check(worst == 30 && 12 * 13 == 156 && 30.0 / 156 <= 1.0 / 4, "at most 30 of 156 functions collide on any pair");
        UniversalHash h = UniversalHash.random(1_000_000_007L, 16, new Random(1));
        check(h.of(12345) == h.of(12345) && h.of(99) >= 0 && h.of(99) < 16, "a chosen function is deterministic and in range");

        // Level 6: equals and hashCode.
        check(new Point(3, 4).equals(new Point(3, 4)) && new Point(3, 4).hashCode() == new Point(3, 4).hashCode(), "Point contract");
        check(Objects.hash(3, 4) == 1058 && 31 * (31 + 3) + 4 == 1058, "Objects.hash(3, 4)");
        check(new Cell(3, 4).equals(new Cell(3, 4)) && new Cell(3, 4).hashCode() == new Cell(3, 4).hashCode(), "record contract");
        Set<Point> points = new HashSet<>(List.of(new Point(3, 4)));
        check(points.contains(new Point(3, 4)), "a HashSet finds an equal Point");
        check("Aa".hashCode() == "BB".hashCode() && !"Aa".equals("BB"), "equal hash codes do not mean equal objects");
        int extreme = "polygenelubricants".hashCode();
        check(extreme == Integer.MIN_VALUE && Math.abs(extreme) % 7 == -2 && Math.floorMod(extreme, 7) == 5, "Math.abs trap");

        // Level 7: cryptographic hashes and the birthday bound.
        check(sha256Hex("cat").startsWith("77af778b51abd4a3"), "SHA-256 of cat");
        check(sha256Hex("cau").startsWith("bbcfcbdecdfec348"), "SHA-256 of cau");
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        check(differingBits(sha.digest("cat".getBytes(StandardCharsets.UTF_8)), sha.digest("cau".getBytes(StandardCharsets.UTF_8))) == 122,
                "one input bit changes 122 of 256 output bits");
        check(Math.round(fiftyPercentCollisionKeys(Math.pow(2, 32))) == 77163, "about 77,163 keys for a 50% collision in 32 bits");
        check((long) Math.pow(62, 6) == 56_800_235_584L && Math.round(fiftyPercentCollisionKeys(Math.pow(62, 6))) == 280610, "62^6 codes");

        // Interview problems.
        List<List<String>> groups = groupAnagrams(new String[] {"eat", "tea", "tan", "ate", "nat", "bat"});
        Set<Set<String>> grouped = new HashSet<>();
        for (List<String> group : groups) grouped.add(new HashSet<>(group));
        check(grouped.equals(Set.of(Set.of("eat", "tea", "ate"), Set.of("tan", "nat"), Set.of("bat"))), "LC 49");
        check(groupAnagrams(new String[] {""}).equals(List.of(List.of(""))), "LC 49 empty word");

        Codec codec = new Codec();
        String url = "https://leetcode.com/problems/design-tinyurl";
        String tiny = codec.encode(url);
        check(codec.decode(tiny).equals(url) && tiny.length() == "http://tinyurl.com/".length() + 6, "LC 535 round trip");
        for (int i = 0; i < 1000; i++) {
            String other = "https://example.com/" + i;
            check(codec.decode(codec.encode(other)).equals(other), "LC 535 many URLs");
        }

        int[][] example1 = {{0, 1, 2, 3, 4}, {2, 3, 4}, {4, 0, 1, 2, 3}};
        check(longestCommonSubpath(5, example1) == 2, "LC 1923 example 1");
        check(allShare(example1, 2) && !allShare(example1, 3), "length 2 is shared, length 3 is not");
        // The same windows read as base-10 numbers, as in the lesson's hand calculation.
        List<List<Set<Integer>>> byLength = new ArrayList<>();
        for (int length = 2; length <= 3; length++) {
            List<Set<Integer>> sets = new ArrayList<>();
            for (int[] path : example1) {
                Set<Integer> windows = new HashSet<>();
                for (int i = 0; i + length <= path.length; i++) {
                    int value = 0;
                    for (int j = i; j < i + length; j++) value = value * 10 + path[j];
                    windows.add(value);
                }
                sets.add(windows);
            }
            byLength.add(sets);
        }
        check(byLength.get(0).equals(List.of(Set.of(1, 12, 23, 34), Set.of(23, 34), Set.of(40, 1, 12, 23))), "base-10 windows of length 2");
        check(byLength.get(1).equals(List.of(Set.of(12, 123, 234), Set.of(234), Set.of(401, 12, 123))), "base-10 windows of length 3");
        check(longestCommonSubpath(3, new int[][] {{0}, {1}, {2}}) == 0, "LC 1923 example 2");
        check(longestCommonSubpath(5, new int[][] {{0, 1, 2, 3, 4}, {4, 3, 2, 1, 0}}) == 1, "LC 1923 example 3");
        Random rng = new Random(7);
        for (int trial = 0; trial < 200; trial++) {
            int[][] paths = new int[2 + rng.nextInt(3)][];
            for (int i = 0; i < paths.length; i++) {
                paths[i] = new int[1 + rng.nextInt(8)];
                for (int j = 0; j < paths[i].length; j++) paths[i][j] = rng.nextInt(3);
            }
            check(longestCommonSubpath(3, paths) == bruteForceCommonSubpath(paths), "LC 1923 random trial " + trial);
        }
        System.out.println("OK");
    }
}
