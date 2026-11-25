import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

// Run from this folder with: java CountingSortLab.java
public class CountingSortLab {

    // Level 1: tally every key, then write each key out as many times as it was seen.
    static int[] countingSortKeys(int[] a, int k) {   // every key is in [0, k)
        int[] count = new int[k];
        for (int x : a) count[x]++;
        int[] out = new int[a.length];
        int pos = 0;
        for (int v = 0; v < k; v++) {
            for (int c = 0; c < count[v]; c++) out[pos++] = v;
        }
        return out;
    }

    // Level 2: prefix sums of the counts say where each key's block starts.
    static int[] startPositions(int[] a, int k) {   // start[v] = how many keys are smaller than v
        int[] count = new int[k];
        for (int x : a) count[x]++;
        int[] start = new int[k];
        for (int v = 1; v < k; v++) start[v] = start[v - 1] + count[v - 1];
        return start;
    }

    // Level 3: stable counting sort of records, placing them right to left.
    record Card(String name, int key) {}

    static Card[] countingSortStable(Card[] cards, int k) {
        int[] end = new int[k];
        for (Card c : cards) end[c.key()]++;
        for (int v = 1; v < k; v++) end[v] += end[v - 1];   // end[v] = how many keys are <= v
        Card[] out = new Card[cards.length];
        for (int i = cards.length - 1; i >= 0; i--) {        // right to left keeps equal keys in order
            Card c = cards[i];
            out[--end[c.key()]] = c;
        }
        return out;
    }

    // Level 4: any integer range, by shifting the keys down by the minimum.
    static int[] sortAnyRange(int[] a) {   // keys may be negative: shift them into [0, max - min]
        int min = Arrays.stream(a).min().orElse(0), max = Arrays.stream(a).max().orElse(0);
        int[] shifted = Arrays.stream(a).map(x -> x - min).toArray();
        int[] sorted = countingSortKeys(shifted, max - min + 1);
        return Arrays.stream(sorted).map(x -> x + min).toArray();
    }

    // Interview 1: LeetCode 1122, Relative Sort Array.
    static int[] relativeSortArray(int[] arr1, int[] arr2) {
        int[] count = new int[1001];                     // values are in [0, 1000]
        for (int x : arr1) count[x]++;
        int[] out = new int[arr1.length];
        int pos = 0;
        for (int x : arr2) {
            for (; count[x] > 0; count[x]--) out[pos++] = x;   // every copy of x, in arr2's order
        }
        for (int v = 0; v < count.length; v++) {
            for (; count[v] > 0; count[v]--) out[pos++] = v;   // the rest, ascending
        }
        return out;
    }

    // Interview 2: LeetCode 274, H-Index.
    static int hIndex(int[] citations) {
        int n = citations.length;
        int[] papers = new int[n + 1];                   // papers[c] = papers with c citations, c capped at n
        for (int c : citations) papers[Math.min(c, n)]++;
        int atLeast = 0;                                 // papers with at least h citations
        for (int h = n; h >= 0; h--) {
            atLeast += papers[h];
            if (atLeast >= h) return h;
        }
        return 0;
    }

    // Interview 3: LeetCode 1585, Check If String Is Transformable With Substring Sort Operations.
    static boolean isTransformable(String s, String t) {
        List<Deque<Integer>> positions = new ArrayList<>();   // positions.get(d) = where digit d sits in s, left to right
        for (int d = 0; d < 10; d++) positions.add(new ArrayDeque<>());
        for (int i = 0; i < s.length(); i++) positions.get(s.charAt(i) - '0').addLast(i);
        for (char ch : t.toCharArray()) {
            int d = ch - '0';
            if (positions.get(d).isEmpty()) return false;     // t uses more copies of d than s has
            int p = positions.get(d).pollFirst();             // the k-th d of t comes from the k-th d of s
            for (int smaller = 0; smaller < d; smaller++) {
                Deque<Integer> waiting = positions.get(smaller);
                if (!waiting.isEmpty() && waiting.peekFirst() < p) return false;   // a smaller digit blocks d
            }
        }
        return true;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] ratings = {3, 1, 4, 1, 0, 3, 2, 1};
        check(Arrays.equals(countingSortKeys(ratings, 5), new int[] {0, 1, 1, 1, 2, 3, 3, 4}), "tally and rewrite");
        check(Arrays.equals(startPositions(ratings, 5), new int[] {0, 1, 4, 5, 7}), "start positions 0, 1, 4, 5, 7");

        Card[] cards = {new Card("A", 3), new Card("B", 1), new Card("C", 4), new Card("D", 1),
                        new Card("E", 0), new Card("F", 3), new Card("G", 2), new Card("H", 1)};
        String order = String.join("", Arrays.stream(countingSortStable(cards, 5)).map(Card::name).toList());
        check(order.equals("EBDHGAFC"), "stable: B, D, H keep their order, and so do A, F");

        check(Arrays.equals(sortAnyRange(new int[] {3, -2, 0, -2, 1}), new int[] {-2, -2, 0, 1, 3}), "negative keys");
        check(Arrays.equals(sortAnyRange(new int[] {}), new int[] {}), "empty input");
        check(Math.round(1e6 * Math.log(1e6) / Math.log(2)) == 19_931_569, "n log2 n at n = 10^6");
        check((1L << 32) * 4 == 17_179_869_184L, "2^32 int counters take 16 GiB");

        check(Arrays.equals(relativeSortArray(new int[] {2, 3, 1, 3, 2, 4, 6, 7, 9, 2, 19}, new int[] {2, 1, 4, 3, 9, 6}),
                new int[] {2, 2, 2, 1, 4, 3, 3, 9, 6, 7, 19}), "LC 1122 example 1");
        check(Arrays.equals(relativeSortArray(new int[] {28, 6, 22, 8, 44, 17}, new int[] {22, 28, 8, 6}),
                new int[] {22, 28, 8, 6, 17, 44}), "LC 1122 example 2");

        check(hIndex(new int[] {3, 0, 6, 1, 5}) == 3, "LC 274 example 1");
        check(hIndex(new int[] {1, 3, 1}) == 1, "LC 274 example 2");
        check(hIndex(new int[] {0, 0}) == 0, "LC 274 no citations");
        check(hIndex(new int[] {100}) == 1, "LC 274 one famous paper");

        check(isTransformable("84532", "34852"), "LC 1585 example 1");
        check(isTransformable("34521", "23415"), "LC 1585 example 2");
        check(!isTransformable("12345", "12435"), "LC 1585 example 3");
        check(!isTransformable("1", "2"), "LC 1585 different digits");
        System.out.println("OK");
    }
}
