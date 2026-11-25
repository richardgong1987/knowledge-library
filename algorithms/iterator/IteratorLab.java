import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.TreeSet;

// Run from this folder with: java IteratorLab.java
public class IteratorLab {

    // Level 1: the whole protocol is two methods and one piece of state.
    static final class ArrayIterator implements Iterator<Integer> {
        private final int[] items;
        private int cursor;                            // index of the next item to hand out

        ArrayIterator(int[] items) { this.items = items; }

        public boolean hasNext() { return cursor < items.length; }

        public Integer next() {
            if (!hasNext()) throw new NoSuchElementException();
            return items[cursor++];
        }
    }

    static int sum(Iterable<Integer> numbers) {
        int total = 0;
        Iterator<Integer> it = numbers.iterator();
        while (it.hasNext()) total += it.next();       // what for (int x : numbers) compiles to
        return total;
    }

    // Level 2: a lazy iterator computes each value only when asked, so it can even be infinite.
    static Iterator<Long> squares() {                  // 1, 4, 9, 16, ... forever; only a counter is stored
        return new Iterator<>() {
            private long n = 0;

            public boolean hasNext() { return true; }

            public Long next() { n++; return n * n; }
        };
    }

    // Level 3: a tree iterator keeps the unfinished part of the recursion on an explicit stack.
    static final class Node {
        final int key;
        final Node left, right;

        Node(int key, Node left, Node right) { this.key = key; this.left = left; this.right = right; }
    }

    static Node leaf(int key) {
        return new Node(key, null, null);
    }

    static final class PreorderIterator implements Iterator<Integer> {
        private final Deque<Node> stack = new ArrayDeque<>();

        PreorderIterator(Node root) { if (root != null) stack.push(root); }

        public boolean hasNext() { return !stack.isEmpty(); }

        public Integer next() {
            Node node = stack.pop();
            if (node.right != null) stack.push(node.right);   // pushed first, so it comes out after the left
            if (node.left != null) stack.push(node.left);
            return node.key;
        }
    }

    // Level 4: fail-fast iteration. Changing the list behind the iterator's back is a bug.
    static void removeEvensWrong(List<Integer> list) {
        for (Integer x : list) {
            if (x % 2 == 0) list.remove(x);            // wrong: changes the list behind the iterator's back
        }
    }

    static void removeEvens(List<Integer> list) {
        Iterator<Integer> it = list.iterator();
        while (it.hasNext()) {
            if (it.next() % 2 == 0) it.remove();       // removing through the iterator keeps it in step
        }
    }

    // Level 5: a ListIterator can also go backwards and replace or insert at its cursor.
    static void doubleEach(List<Integer> list) {
        for (ListIterator<Integer> it = list.listIterator(); it.hasNext(); ) {
            it.set(it.next() * 2);                     // set replaces the element next() just returned
        }
    }

    // Interview 1: LeetCode 284, Peeking Iterator.
    static final class PeekingIterator implements Iterator<Integer> {
        private final Iterator<Integer> source;
        private Integer nextValue;                     // always one step ahead; null when the source is used up

        PeekingIterator(Iterator<Integer> source) {
            this.source = source;
            nextValue = source.hasNext() ? source.next() : null;
        }

        public Integer peek() { return nextValue; }

        public boolean hasNext() { return nextValue != null; }

        public Integer next() {
            if (nextValue == null) throw new NoSuchElementException();
            Integer current = nextValue;
            nextValue = source.hasNext() ? source.next() : null;
            return current;
        }
    }

    // Interview 2: LeetCode 173, Binary Search Tree Iterator.
    static final class BSTIterator {
        private final Deque<Node> stack = new ArrayDeque<>();   // nodes whose key is still to come

        BSTIterator(Node root) { pushLeftPath(root); }

        private void pushLeftPath(Node node) {
            for (; node != null; node = node.left) stack.push(node);
        }

        boolean hasNext() { return !stack.isEmpty(); }

        int next() {
            Node node = stack.pop();
            pushLeftPath(node.right);                  // everything in the right subtree comes next
            return node.key;
        }
    }

    // Interview 3: LeetCode 341, Flatten Nested List Iterator. LeetCode supplies this interface.
    record NestedInteger(Integer value, List<NestedInteger> list) {
        boolean isInteger() { return value != null; }
        Integer getInteger() { return value; }
        List<NestedInteger> getList() { return list; }
    }

    static NestedInteger num(int value) {
        return new NestedInteger(value, null);
    }

    static NestedInteger nest(NestedInteger... items) {
        return new NestedInteger(null, List.of(items));
    }

    static final class NestedIterator implements Iterator<Integer> {
        private final Deque<Iterator<NestedInteger>> stack = new ArrayDeque<>();
        private Integer nextValue;                     // found by hasNext(), handed out by next()

        NestedIterator(List<NestedInteger> nestedList) { stack.push(nestedList.iterator()); }

        public boolean hasNext() {
            while (nextValue == null && !stack.isEmpty()) {
                if (!stack.peek().hasNext()) { stack.pop(); continue; }   // this list is finished
                NestedInteger item = stack.peek().next();
                if (item.isInteger()) nextValue = item.getInteger();
                else stack.push(item.getList().iterator());               // step into the inner list
            }
            return nextValue != null;
        }

        public Integer next() {
            if (!hasNext()) throw new NoSuchElementException();
            Integer value = nextValue;
            nextValue = null;
            return value;
        }
    }

    // Interview 4: LeetCode 632, Smallest Range Covering Elements from K Lists.
    static int[] smallestRange(List<List<Integer>> lists) {
        List<Iterator<Integer>> cursors = new ArrayList<>();
        PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0])); // {value, list}
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < lists.size(); i++) {
            cursors.add(lists.get(i).iterator());
            int value = cursors.get(i).next();
            heap.add(new int[] {value, i});
            max = Math.max(max, value);
        }
        int[] best = null;
        while (true) {
            int[] min = heap.poll();
            if (best == null || max - min[0] < best[1] - best[0]) best = new int[] {min[0], max};
            Iterator<Integer> cursor = cursors.get(min[1]);
            if (!cursor.hasNext()) return best;        // this list is used up: no later range can include it
            int value = cursor.next();
            heap.add(new int[] {value, min[1]});
            max = Math.max(max, value);
        }
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    static List<Integer> drain(Iterator<Integer> it) {
        List<Integer> out = new ArrayList<>();
        while (it.hasNext()) out.add(it.next());
        return out;
    }

    public static void main(String[] args) {
        // Level 1
        ArrayIterator three = new ArrayIterator(new int[] {4, 8, 15});
        check(three.hasNext() && three.hasNext() && three.next() == 4, "hasNext does not move the cursor");
        check(three.next() == 8 && three.next() == 15 && !three.hasNext(), "three items, then done");
        try {
            three.next();
            check(false, "next() past the end must throw");
        } catch (NoSuchElementException expected) {
            // the protocol's way of saying "there is nothing left"
        }
        check(sum(List.of(4, 8, 15)) == 27, "sum over a List");
        check(sum(new LinkedList<>(List.of(4, 8, 15))) == 27 && sum(new TreeSet<>(List.of(15, 4, 8))) == 27,
                "the same sum works for any Iterable");

        // Level 2
        Iterator<Long> sq = squares();
        check(sq.next() == 1 && sq.next() == 4 && sq.next() == 9 && sq.next() == 16, "the first four squares");
        check(4L * 1_000_000_000 == 4_000_000_000L, "a billion ints take 4 GB as a list");

        // Level 3: the tree 4 / (2: 1, 3) / (6: 5, 7)
        Node tree = new Node(4, new Node(2, leaf(1), leaf(3)), new Node(6, leaf(5), leaf(7)));
        PreorderIterator pre = new PreorderIterator(tree);
        List<Integer> preorder = new ArrayList<>();
        int largestStack = 0;
        while (pre.hasNext()) {
            preorder.add(pre.next());
            largestStack = Math.max(largestStack, pre.stack.size());
        }
        check(preorder.equals(List.of(4, 2, 1, 3, 6, 5, 7)), "pre-order");
        check(largestStack == 3, "the stack never holds more than h + 1 = 3 nodes");
        PreorderIterator twoSteps = new PreorderIterator(tree);
        twoSteps.next();
        twoSteps.next();
        check(twoSteps.stack.size() == 3, "after 4 and 2 the stack holds 1, 3, 6");

        // Level 4
        List<Integer> sneaky = new ArrayList<>(List.of(1, 2, 4));
        removeEvensWrong(sneaky);
        check(sneaky.equals(List.of(1, 4)), "removing 2 ends the loop early: 4 is never examined");
        List<Integer> loud = new ArrayList<>(List.of(2, 3, 4, 5));
        try {
            removeEvensWrong(loud);
            check(false, "expected ConcurrentModificationException");
        } catch (ConcurrentModificationException expected) {
            // modCount changed, so the next call to next() noticed
        }
        List<Integer> fixed = new ArrayList<>(List.of(1, 2, 4));
        removeEvens(fixed);
        check(fixed.equals(List.of(1)), "removing through the iterator");
        List<Integer> alsoFixed = new ArrayList<>(List.of(2, 3, 4, 5));
        removeEvens(alsoFixed);
        check(alsoFixed.equals(List.of(3, 5)), "removing through the iterator, second list");

        // Level 5
        List<Integer> doubled = new ArrayList<>(List.of(4, 8, 15));
        doubleEach(doubled);
        check(doubled.equals(List.of(8, 16, 30)), "ListIterator.set");

        // Interview 1
        PeekingIterator peeking = new PeekingIterator(List.of(1, 2, 3).iterator());
        check(peeking.next() == 1 && peeking.peek() == 2 && peeking.next() == 2, "LC 284: peek does not advance");
        check(peeking.next() == 3 && !peeking.hasNext() && peeking.peek() == null, "LC 284: done");
        check(!new PeekingIterator(List.<Integer>of().iterator()).hasNext(), "LC 284: empty source");

        // Interview 2: the tree 7 / 3, 15 / (15: 9, 20)
        BSTIterator bst = new BSTIterator(new Node(7, leaf(3), new Node(15, leaf(9), leaf(20))));
        int pushes = bst.stack.size(), pops = 0;
        check(pushes == 2, "the constructor pushes 7 and 3");
        List<Integer> inOrder = new ArrayList<>(), callCosts = new ArrayList<>();
        while (bst.hasNext()) {
            int before = bst.stack.size();
            inOrder.add(bst.next());
            int pushedNow = bst.stack.size() - before + 1;   // one pop, then the pushes
            pops++;
            pushes += pushedNow;
            callCosts.add(1 + pushedNow);
        }
        check(inOrder.equals(List.of(3, 7, 9, 15, 20)), "LC 173: sorted order");
        check(pushes == 5 && pops == 5 && (pushes + pops) / 5 == 2, "LC 173: every node pushed once and popped once");
        check(callCosts.equals(List.of(1, 3, 1, 2, 1)), "LC 173: stack operations per call");
        BSTIterator chain = new BSTIterator(new Node(3, new Node(2, leaf(1), null), null));
        check(chain.stack.size() == 3 && chain.next() == 1 && chain.next() == 2 && chain.next() == 3, "LC 173 left chain");

        // Interview 3
        List<NestedInteger> first = List.of(nest(num(1), num(1)), num(2), nest(num(1), num(1)));
        check(drain(new NestedIterator(first)).equals(List.of(1, 1, 2, 1, 1)), "LC 341 example 1");
        List<NestedInteger> second = List.of(num(1), nest(num(4), nest(num(6))));
        check(drain(new NestedIterator(second)).equals(List.of(1, 4, 6)), "LC 341 example 2");
        List<NestedInteger> empty = List.of(nest(), nest(nest()));
        NestedIterator none = new NestedIterator(empty);
        check(!none.hasNext() && !none.hasNext(), "LC 341: empty lists hold no integers");

        // Interview 4
        check(java.util.Arrays.equals(smallestRange(List.of(List.of(1, 5, 8), List.of(4, 12), List.of(7, 8, 10))),
                new int[] {4, 7}), "LC 632 small example");
        check(java.util.Arrays.equals(smallestRange(List.of(List.of(4, 10, 15, 24, 26), List.of(0, 9, 12, 20),
                List.of(5, 18, 22, 30))), new int[] {20, 24}), "LC 632 example 1");
        check(java.util.Arrays.equals(smallestRange(List.of(List.of(1, 2, 3), List.of(1, 2, 3), List.of(1, 2, 3))),
                new int[] {1, 1}), "LC 632 example 2");
        System.out.println("OK");
    }
}
