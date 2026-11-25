import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

// Run from this folder with: java QueueLab.java
public class QueueLab {

    // Level 2 and Interview 2 (LeetCode 622, Design Circular Queue): a ring buffer.
    static final class CircularQueue {
        final int[] items;
        int head, size;                                // the front element is items[head]

        CircularQueue(int capacity) {
            items = new int[capacity];
        }

        boolean enQueue(int value) {
            if (size == items.length) return false;    // full
            items[(head + size) % items.length] = value;
            size++;
            return true;
        }

        boolean deQueue() {
            if (size == 0) return false;
            head = (head + 1) % items.length;          // the old front slot is simply reused later
            size--;
            return true;
        }

        int front() {
            return size == 0 ? -1 : items[head];
        }

        int rear() {
            return size == 0 ? -1 : items[(head + size - 1) % items.length];
        }

        boolean isEmpty() {
            return size == 0;
        }

        boolean isFull() {
            return size == items.length;
        }
    }

    // Level 3: java.util.Queue in use. Children in a circle pass a potato; every k-th child leaves.
    static int josephus(int n, int k) {
        Queue<Integer> circle = new ArrayDeque<>();
        for (int i = 1; i <= n; i++) circle.offer(i);
        while (circle.size() > 1) {
            for (int i = 1; i < k; i++) circle.offer(circle.poll());  // front child moves to the back
            circle.poll();                                            // the k-th child leaves
        }
        return circle.peek();
    }

    // Interview 1: LeetCode 232, Implement Queue using Stacks.
    static final class MyQueue {
        final Deque<Integer> inbox = new ArrayDeque<>(), outbox = new ArrayDeque<>();
        int moved;                                     // elements carried from inbox to outbox so far

        void push(int x) {
            inbox.push(x);
        }

        int pop() {
            peek();                                    // makes sure the oldest element is on top of outbox
            return outbox.pop();
        }

        int peek() {
            if (outbox.isEmpty()) {
                while (!inbox.isEmpty()) {             // reversing a stack puts the oldest on top
                    outbox.push(inbox.pop());
                    moved++;
                }
            }
            return outbox.peek();
        }

        boolean empty() {
            return inbox.isEmpty() && outbox.isEmpty();
        }
    }

    // Interview 3: LeetCode 862, Shortest Subarray with Sum at Least K.
    static int shortestSubarray(int[] nums, int k) {
        int n = nums.length, best = n + 1;
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + nums[i];
        Deque<Integer> starts = new ArrayDeque<>();    // candidate starts; their prefix sums increase
        for (int j = 0; j <= n; j++) {
            while (!starts.isEmpty() && prefix[j] - prefix[starts.peekFirst()] >= k) {
                best = Math.min(best, j - starts.pollFirst());   // no later end can do better for this start
            }
            while (!starts.isEmpty() && prefix[starts.peekLast()] >= prefix[j]) {
                starts.pollLast();                     // j starts later with a smaller or equal prefix
            }
            starts.offerLast(j);
        }
        return best <= n ? best : -1;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int n = 5, shifts = 0;
        for (int remaining = n; remaining > 0; remaining--) shifts += remaining - 1;
        check(shifts == 10 && n * (n - 1) / 2 == 10, "draining 5 by shifting moves 4 + 3 + 2 + 1 = 10");

        CircularQueue ring = new CircularQueue(5);
        for (int i = 1; i <= 3; i++) ring.enQueue(i);
        for (int i = 1; i <= 3; i++) ring.deQueue();
        ring.enQueue(10);
        ring.enQueue(20);
        ring.enQueue(30);
        check(ring.head == 3 && ring.items[3] == 10 && ring.items[4] == 20 && ring.items[0] == 30, "wraps from 4 to 0");
        check(ring.rear() == 30 && (3 + 3 - 1) % 5 == 0, "rear at (3 + 3 - 1) mod 5 = 0");
        ring.enQueue(40);
        check(ring.items[1] == 40 && (3 + 3) % 5 == 1, "next free slot (3 + 3) mod 5 = 1");
        check(ring.front() == 10, "front is items[head]");

        Queue<String> line = new ArrayDeque<>();
        check(line.poll() == null && line.peek() == null, "poll and peek return null when empty");
        try {
            line.remove();
            check(false, "remove on empty must throw");
        } catch (NoSuchElementException expected) {
        }
        try {
            line.element();
            check(false, "element on empty must throw");
        } catch (NoSuchElementException expected) {
        }
        check(line.offer("Ada") && line.offer("Ben") && line.poll().equals("Ada"), "first in, first out");
        Queue<Integer> linked = new LinkedList<>();
        linked.offer(null);
        check(linked.size() == 1 && linked.poll() == null, "LinkedList allows null, so poll's null is ambiguous");

        check(josephus(5, 2) == 3, "five children, every second leaves: child 3 remains");
        int survivor = 0;                              // J(1) = 0, J(m) = (J(m - 1) + k) mod m, 0-based
        for (int m = 2; m <= 5; m++) survivor = (survivor + 2) % m;
        check(survivor + 1 == 3, "the Josephus recurrence agrees");
        check(josephus(1, 3) == 1 && josephus(7, 3) == 4, "Josephus edge cases");

        MyQueue q = new MyQueue();
        q.push(1);
        q.push(2);
        q.push(3);
        check(q.pop() == 1 && q.moved == 3, "LC 232 first pop moves 3");
        q.push(4);
        check(q.pop() == 2 && q.pop() == 3 && q.moved == 3, "LC 232 next pops move nothing");
        check(q.peek() == 4 && q.moved == 4, "LC 232 moves 4 only when outbox is empty");
        check(q.pop() == 4 && q.empty(), "LC 232 empty");
        check(4 + 4 + 2 * q.moved == 16, "4 pushes + 4 pops + 2 per move = 16 stack operations");

        CircularQueue lc = new CircularQueue(3);
        check(lc.enQueue(1) && lc.enQueue(2) && lc.enQueue(3) && !lc.enQueue(4), "LC 622 full after 3");
        check(lc.rear() == 3 && lc.isFull(), "LC 622 rear");
        check(lc.deQueue() && lc.enQueue(4) && lc.rear() == 4 && lc.items[0] == 4, "LC 622 wraps to slot 0");
        check(lc.front() == 2, "LC 622 front");
        CircularQueue empty = new CircularQueue(2);
        check(empty.front() == -1 && empty.rear() == -1 && !empty.deQueue() && empty.isEmpty(), "LC 622 empty");

        check(shortestSubarray(new int[] {2, -1, 2}, 3) == 3, "LC 862");
        check(shortestSubarray(new int[] {1}, 1) == 1, "LC 862 single");
        check(shortestSubarray(new int[] {1, 2}, 4) == -1, "LC 862 impossible");
        check(shortestSubarray(new int[] {84, -37, 32, 40, 95}, 167) == 3, "LC 862 negative in the middle");
        System.out.println("OK");
    }
}
