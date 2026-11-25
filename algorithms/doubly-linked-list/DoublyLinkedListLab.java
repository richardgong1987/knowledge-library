import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Run from this folder with: java DoublyLinkedListLab.java
public class DoublyLinkedListLab {

    // Level 1: every node links both ways.
    static final class Node {
        int key, value;
        int freq = 1;                                  // used only by the LFU cache
        Node prev, next;

        Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    // Level 2: sentinels at both ends, so no link is ever null.
    static final class DList {
        final Node head = new Node(0, 0), tail = new Node(0, 0);
        int size;

        DList() {
            head.next = tail;
            tail.prev = head;
        }

        void addFirst(Node x) {                        // right after the head sentinel
            x.prev = head;
            x.next = head.next;
            head.next.prev = x;
            head.next = x;
            size++;
        }

        // Level 3: a node knows both neighbours, so removing it is two writes.
        void remove(Node x) {
            x.prev.next = x.next;
            x.next.prev = x.prev;
            size--;
        }

        Node removeLast() {                            // callers make sure the list is not empty
            Node last = tail.prev;
            remove(last);
            return last;
        }

        void moveToFront(Node x) {
            remove(x);
            addFirst(x);
        }

        // Level 4: dancing links. A removed node still remembers its neighbours.
        void restore(Node x) {
            x.prev.next = x;
            x.next.prev = x;
            size++;
        }
    }

    // Level 5: java.util.LinkedHashMap in access order is an LRU cache.
    static <K, V> Map<K, V> lruMap(int capacity) {
        return new LinkedHashMap<>(16, 0.75f, true) {  // true = order by access, not insertion
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    // Interview 1: LeetCode 146, LRU Cache.
    static final class LRUCache {
        final int capacity;
        final Map<Integer, Node> nodes = new HashMap<>();
        final DList recency = new DList();             // most recently used first

        LRUCache(int capacity) {
            this.capacity = capacity;
        }

        int get(int key) {
            Node x = nodes.get(key);
            if (x == null) return -1;
            recency.moveToFront(x);
            return x.value;
        }

        void put(int key, int value) {
            Node x = nodes.get(key);
            if (x != null) {
                x.value = value;
                recency.moveToFront(x);
                return;
            }
            if (nodes.size() == capacity) nodes.remove(recency.removeLast().key);
            x = new Node(key, value);
            nodes.put(key, x);
            recency.addFirst(x);
        }
    }

    // Interview 2: LeetCode 460, LFU Cache.
    static final class LFUCache {
        final int capacity;
        final Map<Integer, Node> nodes = new HashMap<>();
        final Map<Integer, DList> byFreq = new HashMap<>();   // use count -> nodes, most recent first
        int minFreq;

        LFUCache(int capacity) {
            this.capacity = capacity;
        }

        int get(int key) {
            Node x = nodes.get(key);
            if (x == null) return -1;
            touch(x);
            return x.value;
        }

        void put(int key, int value) {
            Node x = nodes.get(key);
            if (x != null) {
                x.value = value;
                touch(x);
                return;
            }
            if (nodes.size() == capacity) {            // fewest uses first, then least recent
                Node victim = byFreq.get(minFreq).removeLast();
                nodes.remove(victim.key);
            }
            x = new Node(key, value);
            nodes.put(key, x);
            byFreq.computeIfAbsent(1, f -> new DList()).addFirst(x);
            minFreq = 1;
        }

        void touch(Node x) {                           // move x from list f to list f + 1
            DList list = byFreq.get(x.freq);
            list.remove(x);
            if (x.freq == minFreq && list.size == 0) minFreq++;
            x.freq++;
            byFreq.computeIfAbsent(x.freq, f -> new DList()).addFirst(x);
        }
    }

    static List<Integer> keysForward(DList list) {
        List<Integer> keys = new ArrayList<>();
        for (Node n = list.head.next; n != list.tail; n = n.next) keys.add(n.key);
        return keys;
    }

    static List<Integer> keysBackward(DList list) {
        List<Integer> keys = new ArrayList<>();
        for (Node n = list.tail.prev; n != list.head; n = n.prev) keys.add(0, n.key);
        return keys;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        DList list = new DList();
        check(list.head.next == list.tail && list.tail.prev == list.head, "empty list: head and tail hold hands");
        Node a = new Node(1, 0), b = new Node(2, 0), c = new Node(3, 0);
        list.addFirst(c);
        list.addFirst(b);
        list.addFirst(a);
        check(keysForward(list).equals(List.of(1, 2, 3)) && keysBackward(list).equals(List.of(1, 2, 3)), "A B C both ways");

        list.remove(b);
        check(keysForward(list).equals(List.of(1, 3)) && keysBackward(list).equals(List.of(1, 3)) && list.size == 2,
                "removing B is two writes");
        check(b.prev == a && b.next == c, "B still remembers its old neighbours");
        list.restore(b);
        check(keysForward(list).equals(List.of(1, 2, 3)) && keysBackward(list).equals(List.of(1, 2, 3)), "dancing links undo");

        list.moveToFront(c);
        check(keysForward(list).equals(List.of(3, 1, 2)) && list.removeLast() == b, "move to front, remove last");

        Map<Integer, String> lru = lruMap(2);
        lru.put(1, "one");
        lru.put(2, "two");
        lru.get(1);
        lru.put(3, "three");
        check(new ArrayList<>(lru.keySet()).equals(List.of(1, 3)), "LinkedHashMap evicts 2, the least recently used");

        LRUCache cache = new LRUCache(2);
        cache.put(1, 1);
        cache.put(2, 2);
        check(cache.get(1) == 1, "LC 146 get 1");
        cache.put(3, 3);
        check(cache.get(2) == -1, "LC 146 key 2 evicted");
        cache.put(4, 4);
        check(cache.get(1) == -1 && cache.get(3) == 3 && cache.get(4) == 4, "LC 146 key 1 evicted");
        LRUCache one = new LRUCache(1);
        one.put(2, 1);
        one.put(2, 2);
        check(one.get(2) == 2, "LC 146 update keeps one entry");
        one.put(3, 3);
        check(one.get(2) == -1 && one.get(3) == 3, "LC 146 capacity 1");

        LFUCache lfu = new LFUCache(2);
        lfu.put(1, 1);
        lfu.put(2, 2);
        check(lfu.get(1) == 1, "LC 460 get 1: freq(1) = 2");
        lfu.put(3, 3);
        check(lfu.get(2) == -1, "LC 460 key 2 evicted: lowest count");
        check(lfu.get(3) == 3, "LC 460 freq(3) = 2");
        lfu.put(4, 4);
        check(lfu.get(1) == -1, "LC 460 tie at count 2: key 1 is less recent, evicted");
        check(lfu.get(3) == 3 && lfu.get(4) == 4, "LC 460 3 and 4 remain");
        LFUCache single = new LFUCache(1);
        single.put(1, 1);
        single.get(1);
        single.put(2, 2);
        check(single.minFreq == 1 && single.get(1) == -1 && single.get(2) == 2, "LC 460 a new key resets minFreq to 1");
        System.out.println("OK");
    }
}
