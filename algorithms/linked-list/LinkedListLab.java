import java.util.ArrayList;
import java.util.List;

// Run from this folder with: java LinkedListLab.java
public class LinkedListLab {

    // Level 1: a node holds one value and a link to the next node.
    static final class Node {
        int val;
        Node next;

        Node(int val, Node next) {
            this.val = val;
            this.next = next;
        }
    }

    static Node nodeAt(Node head, int index) {         // follows index links: O(index)
        Node n = head;
        for (int i = 0; i < index; i++) n = n.next;
        return n;
    }

    // Level 2: once you hold a node, changing the links around it is O(1).
    static void insertAfter(Node node, int val) {
        node.next = new Node(val, node.next);
    }

    static void removeAfter(Node node) {
        node.next = node.next.next;                    // the skipped node is left for the garbage collector
    }

    // Level 3: a dummy node in front means the real head is not a special case.
    static Node removeAll(Node head, int target) {
        Node dummy = new Node(0, head);
        for (Node n = dummy; n.next != null; ) {
            if (n.next.val == target) n.next = n.next.next;
            else n = n.next;
        }
        return dummy.next;
    }

    // Level 4: reverse by flipping one arrow per step.
    static Node reverse(Node head) {
        Node prev = null, curr = head;
        while (curr != null) {
            Node next = curr.next;                     // remember the rest before cutting the link
            curr.next = prev;
            prev = curr;
            curr = next;
        }
        return prev;
    }

    // Level 5: fast and slow pointers.
    static Node middle(Node head) {
        Node slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;                          // one step
            fast = fast.next.next;                     // two steps
        }
        return slow;
    }

    // Level 6: Floyd's cycle detection. Returns where the two runners meet, or null.
    static Node meetingPoint(Node head) {
        Node slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) return slow;
        }
        return null;
    }

    // Interview 1: LeetCode 21, Merge Two Sorted Lists.
    static Node mergeTwoLists(Node a, Node b) {
        Node dummy = new Node(0, null), tail = dummy;
        while (a != null && b != null) {
            if (a.val <= b.val) {
                tail.next = a;
                a = a.next;
            } else {
                tail.next = b;
                b = b.next;
            }
            tail = tail.next;
        }
        tail.next = (a != null) ? a : b;               // one list is used up; append the other
        return dummy.next;
    }

    // Interview 2: LeetCode 142, Linked List Cycle II.
    static Node detectCycle(Node head) {
        Node meet = meetingPoint(head);
        if (meet == null) return null;
        Node fromHead = head, fromMeet = meet;
        while (fromHead != fromMeet) {                 // both are mu steps from the cycle start
            fromHead = fromHead.next;
            fromMeet = fromMeet.next;
        }
        return fromHead;
    }

    // Interview 3: LeetCode 25, Reverse Nodes in k-Group.
    static Node reverseKGroup(Node head, int k) {
        Node dummy = new Node(0, head), groupPrev = dummy;
        while (true) {
            Node kth = groupPrev;
            for (int i = 0; i < k && kth != null; i++) kth = kth.next;
            if (kth == null) return dummy.next;        // fewer than k nodes left: keep their order
            Node groupNext = kth.next, first = groupPrev.next;
            Node prev = groupNext, curr = first;       // reverse the group; its new tail points at groupNext
            while (curr != groupNext) {
                Node next = curr.next;
                curr.next = prev;
                prev = curr;
                curr = next;
            }
            groupPrev.next = kth;                      // kth is the group's new head
            groupPrev = first;                         // first is the group's new tail
        }
    }

    static Node of(int... values) {
        Node dummy = new Node(0, null), tail = dummy;
        for (int v : values) tail = tail.next = new Node(v, null);
        return dummy.next;
    }

    static List<Integer> values(Node head) {
        List<Integer> out = new ArrayList<>();
        for (Node n = head; n != null; n = n.next) out.add(n.val);
        return out;
    }

    static Node withCycle(int length, int cycleStart) {   // values 0..length-1, last node links back
        int[] values = new int[length];
        for (int i = 0; i < length; i++) values[i] = i;
        Node head = of(values);
        nodeAt(head, length - 1).next = nodeAt(head, cycleStart);
        return head;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        Node list = of(10, 20, 30, 40);
        check(nodeAt(list, 3).val == 40 && nodeAt(list, 0) == list, "reaching index 3 takes 3 links");

        Node twenty = nodeAt(list, 1);
        insertAfter(twenty, 25);
        check(values(list).equals(List.of(10, 20, 25, 30, 40)), "insert after 20");
        removeAfter(twenty);
        check(values(list).equals(List.of(10, 20, 30, 40)), "remove after 20");

        check(values(removeAll(of(6, 1, 2, 6, 3, 6), 6)).equals(List.of(1, 2, 3)), "dummy head removes a 6 at the head");
        check(removeAll(of(6, 6), 6) == null, "every node removed");

        check(values(reverse(of(1, 2, 3, 4))).equals(List.of(4, 3, 2, 1)), "reverse");
        check(reverse(null) == null && values(reverse(of(7))).equals(List.of(7)), "reverse edge cases");

        check(middle(of(1, 2, 3, 4, 5)).val == 3 && middle(of(1, 2, 3, 4, 5, 6)).val == 4, "middle at index n / 2");

        Node rho = withCycle(10, 4);                   // tail mu = 4, cycle lambda = 6
        check(meetingPoint(rho).val == 6, "runners meet at node 6 after t = 6 steps");
        check(detectCycle(rho).val == 4, "walking 4 more steps from 6 reaches the cycle start 4");
        check(meetingPoint(of(1, 2, 3)) == null && detectCycle(of(1, 2, 3)) == null, "no cycle");

        check(values(mergeTwoLists(of(1, 2, 4), of(1, 3, 4))).equals(List.of(1, 1, 2, 3, 4, 4)), "LC 21");
        check(mergeTwoLists(null, null) == null && values(mergeTwoLists(null, of(0))).equals(List.of(0)), "LC 21 empty");

        Node example = of(3, 2, 0, -4);
        example.next.next.next.next = example.next;    // -4 links back to 2: pos = 1
        check(meetingPoint(example).val == -4 && detectCycle(example).val == 2, "LC 142");
        Node selfLoop = of(1);
        selfLoop.next = selfLoop;
        check(detectCycle(selfLoop) == selfLoop, "LC 142 a one-node cycle");
        for (int length = 1; length <= 12; length++) {
            for (int start = 0; start < length; start++) {
                check(detectCycle(withCycle(length, start)).val == start, "LC 142 every shape");
            }
        }

        check(values(reverseKGroup(of(1, 2, 3, 4, 5), 2)).equals(List.of(2, 1, 4, 3, 5)), "LC 25 k = 2");
        check(values(reverseKGroup(of(1, 2, 3, 4, 5), 3)).equals(List.of(3, 2, 1, 4, 5)), "LC 25 k = 3");
        check(values(reverseKGroup(of(1, 2, 3, 4), 1)).equals(List.of(1, 2, 3, 4)), "LC 25 k = 1");
        check(values(reverseKGroup(of(1, 2, 3, 4), 4)).equals(List.of(4, 3, 2, 1)), "LC 25 k = n");
        System.out.println("OK");
    }
}
