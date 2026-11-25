import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Run from this folder with: java HashTableLab.java
public class HashTableLab {

    // Level 3a: separate chaining. Each bucket holds a linked list of the keys that landed there.
    static final class ChainedTable {
        static final class Node {
            final int key;
            String value;
            Node next;

            Node(int key, String value, Node next) {
                this.key = key;
                this.value = value;
                this.next = next;
            }
        }

        Node[] buckets = new Node[8];
        int size;

        int index(int key) {
            return Math.floorMod(key, buckets.length);
        }

        void put(int key, String value) {
            for (Node n = buckets[index(key)]; n != null; n = n.next) {
                if (n.key == key) { n.value = value; return; }
            }
            buckets[index(key)] = new Node(key, value, buckets[index(key)]); // add at the front of the chain
            if (++size > 0.75 * buckets.length) resize();
        }

        String get(int key) {
            for (Node n = buckets[index(key)]; n != null; n = n.next) {
                if (n.key == key) return n.value;
            }
            return null; // absent, exactly like java.util.Map.get
        }

        void resize() {
            Node[] old = buckets;
            buckets = new Node[old.length * 2];
            size = 0;
            for (Node head : old) {
                for (Node n = head; n != null; n = n.next) put(n.key, n.value);
            }
        }
    }

    // Level 3b: open addressing with linear probing. Every key lives directly in the array.
    static final class LinearProbingTable {
        final Integer[] keys;       // null = this slot was never used
        final String[] values;
        final boolean[] tombstone;  // true = a key was deleted here; keep probing past it

        LinearProbingTable(int capacity) {
            keys = new Integer[capacity];
            values = new String[capacity];
            tombstone = new boolean[capacity];
        }

        int probe(int key, int i) {
            return Math.floorMod(key + i, keys.length);
        }

        void put(int key, String value) {
            for (int i = 0; i < keys.length; i++) {
                int slot = probe(key, i);
                if (keys[slot] == null || keys[slot] == key) { // a free slot, or this key's own slot
                    keys[slot] = key;
                    values[slot] = value;
                    tombstone[slot] = false;
                    return;
                }
            }
            throw new IllegalStateException("table is full");
        }

        String get(int key) {
            for (int i = 0; i < keys.length; i++) {
                int slot = probe(key, i);
                if (keys[slot] == null) return null; // an unused slot ends every probe sequence
                if (!tombstone[slot] && keys[slot] == key) return values[slot];
            }
            return null;
        }

        void remove(int key) {
            for (int i = 0; i < keys.length; i++) {
                int slot = probe(key, i);
                if (keys[slot] == null) return;
                if (!tombstone[slot] && keys[slot] == key) { tombstone[slot] = true; values[slot] = null; return; }
            }
        }
    }

    // Level 3c: two more probe sequences.
    static int quadraticSlot(int home, int i, int m) {   // m must be a power of two
        return (home + i * (i + 1) / 2) % m;
    }

    static int doubleHashSlot(int key, int i, int m) {   // m must be prime
        int h1 = Math.floorMod(key, m);
        int h2 = 1 + Math.floorMod(key, m - 1);           // never 0, so the probe always moves
        return (h1 + i * h2) % m;
    }

    // Level 4: the same two steps java.util.HashMap uses to pick a bucket.
    static int spread(Object key) {
        int h = key.hashCode();
        return h ^ (h >>> 16);                            // mix the high bits into the low bits
    }

    static int bucketIndex(Object key, int capacity) {   // capacity is a power of two
        return (capacity - 1) & spread(key);
    }

    // Level 2: how likely is at least one collision?
    static double noCollisionProbability(int n, int m) {
        double p = 1;
        for (int i = 0; i < n; i++) p *= (double) (m - i) / m;
        return p;
    }

    // Interview 1: LeetCode 128, Longest Consecutive Sequence.
    static int longestConsecutive(int[] nums) {
        Set<Integer> seen = new HashSet<>();
        for (int x : nums) seen.add(x);
        int best = 0;
        for (int start : seen) {
            if (seen.contains(start - 1)) continue;       // start is not the first number of its run
            int length = 1;
            while (seen.contains(start + length)) length++;
            best = Math.max(best, length);
        }
        return best;
    }

    // Interview 2: LeetCode 560, Subarray Sum Equals K.
    static int subarraySum(int[] nums, int k) {
        Map<Integer, Integer> prefixCount = new HashMap<>(Map.of(0, 1));
        int prefix = 0, count = 0;
        for (int x : nums) {
            prefix += x;
            count += prefixCount.getOrDefault(prefix - k, 0);
            prefixCount.merge(prefix, 1, Integer::sum);
        }
        return count;
    }

    // Interview 3: LeetCode 149, Max Points on a Line.
    static int maxPoints(int[][] points) {
        int best = 0;
        for (int i = 0; i < points.length; i++) {
            Map<String, Integer> directions = new HashMap<>();
            int most = 0;
            for (int j = i + 1; j < points.length; j++) {
                int dx = points[j][0] - points[i][0], dy = points[j][1] - points[i][1];
                int g = gcd(Math.abs(dx), Math.abs(dy));
                dx /= g;
                dy /= g;
                if (dx < 0 || (dx == 0 && dy < 0)) { dx = -dx; dy = -dy; } // one name per direction
                most = Math.max(most, directions.merge(dx + "/" + dy, 1, Integer::sum));
            }
            best = Math.max(best, most + 1);
        }
        return best;
    }

    static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] ids = {10, 22, 31, 4, 15, 28};

        ChainedTable chained = new ChainedTable();
        for (int id : ids) chained.put(id, "student " + id);
        check(chained.get(15).equals("student 15") && chained.get(99) == null, "chaining finds keys");
        check(chained.buckets.length == 8 && chained.buckets[7].key == 15 && chained.buckets[7].next.key == 31,
                "15 and 31 share bucket 7, newest first");
        for (int id = 100; id < 103; id++) chained.put(id, "x");
        check(chained.buckets.length == 16, "the 7th key pushes the load factor past 0.75 and doubles the table");

        LinearProbingTable linear = new LinearProbingTable(8);
        for (int id : ids) linear.put(id, "student " + id);
        check(linear.keys[0] == 15 && linear.keys[5] == 28, "15 wraps to slot 0, 28 moves to slot 5");
        linear.remove(31);
        check(linear.get(15).equals("student 15"), "a tombstone at slot 7 keeps 15 reachable");
        check(linear.get(31) == null, "31 is gone");

        Set<Integer> visited = new HashSet<>();
        for (int i = 0; i < 8; i++) visited.add(quadraticSlot(7, i, 8));
        check(visited.size() == 8, "triangular probing visits all 8 slots");
        check(doubleHashSlot(15, 0, 11) == 4 && doubleHashSlot(15, 1, 11) == 10 && doubleHashSlot(15, 2, 11) == 5,
                "double hashing jumps by 6");

        check("cat".hashCode() == 98262 && spread("cat") == 98263 && bucketIndex("cat", 16) == 7, "HashMap bucket for cat");
        check(Math.abs(noCollisionProbability(6, 8) - 0.0769) < 1e-4, "6 keys in 8 slots");
        check(Math.abs(noCollisionProbability(23, 365) - 0.4927) < 1e-4, "birthday paradox");

        check(longestConsecutive(new int[] {100, 4, 200, 1, 3, 2}) == 4, "LC 128");
        check(longestConsecutive(new int[] {}) == 0, "LC 128 empty");
        check(subarraySum(new int[] {1, 2, 3}, 3) == 2, "LC 560");
        check(subarraySum(new int[] {1, -1, 0}, 0) == 3, "LC 560 with negatives and zero");
        check(maxPoints(new int[][] {{1, 1}, {3, 2}, {5, 3}, {4, 1}, {2, 3}, {1, 4}}) == 4, "LC 149");
        check(maxPoints(new int[][] {{0, 0}}) == 1, "LC 149 single point");
        System.out.println("OK");
    }
}
