import java.util.ArrayList;
import java.util.List;

// Run from this folder with: java BinarySearchTreeLab.java
public class BinarySearchTreeLab {

    // Level 1: a node holds one key and two links.
    static final class Node {
        int key;
        Node left, right;

        Node(int key, Node left, Node right) {
            this.key = key;
            this.left = left;
            this.right = right;
        }
    }

    // Level 2: search. Each comparison throws away one whole subtree.
    static Node search(Node node, int key) {
        while (node != null && node.key != key) {
            node = key < node.key ? node.left : node.right;
        }
        return node; // null when the key is absent
    }

    // Level 3: insert. The new key goes exactly where its search fell off the tree.
    static Node insert(Node node, int key) {
        if (node == null) return new Node(key, null, null);
        if (key < node.key) node.left = insert(node.left, key);
        else if (key > node.key) node.right = insert(node.right, key);
        return node; // a duplicate key changes nothing
    }

    // Level 4: in-order traversal (left, node, right) lists the keys in sorted order.
    static void inOrder(Node node, List<Integer> out) {
        if (node == null) return;
        inOrder(node.left, out);
        out.add(node.key);
        inOrder(node.right, out);
    }

    // Level 5: floor = the largest key <= x; ceiling = the smallest key >= x.
    static Integer floor(Node node, int x) {
        Integer best = null;
        while (node != null) {
            if (node.key == x) return x;
            if (node.key < x) { best = node.key; node = node.right; } // a candidate; look for a bigger one
            else node = node.left;
        }
        return best;
    }

    static Integer ceiling(Node node, int x) {
        Integer best = null;
        while (node != null) {
            if (node.key == x) return x;
            if (node.key > x) { best = node.key; node = node.left; }
            else node = node.right;
        }
        return best;
    }

    // Level 6: the in-order successor of a node with two children is the minimum of its right subtree.
    static Node min(Node node) {
        while (node.left != null) node = node.left;
        return node;
    }

    // Level 7: height counts edges on the longest root-to-leaf path.
    static int height(Node node) {
        if (node == null) return -1; // so that a single node has height 0
        return 1 + Math.max(height(node.left), height(node.right));
    }

    // Interview 1: LeetCode 98, Validate Binary Search Tree.
    static boolean isValid(Node node, long low, long high) {
        if (node == null) return true;
        if (node.key <= low || node.key >= high) return false;
        return isValid(node.left, low, node.key) && isValid(node.right, node.key, high);
    }

    // Interview 2: LeetCode 450, Delete Node in a BST.
    static Node delete(Node node, int key) {
        if (node == null) return null;                         // key not found
        if (key < node.key) node.left = delete(node.left, key);
        else if (key > node.key) node.right = delete(node.right, key);
        else if (node.left == null) return node.right;         // case 1 or 2: no left child
        else if (node.right == null) return node.left;         // case 2: only a left child
        else {                                                 // case 3: two children
            node.key = min(node.right).key;                    // copy the successor up
            node.right = delete(node.right, node.key);         // then remove the successor below
        }
        return node;
    }

    // Interview 3: LeetCode 1373, Maximum Sum BST in Binary Tree.
    record Summary(boolean isBst, int min, int max, int sum) {}

    static int bestSum;

    static Summary summarize(Node node) {
        if (node == null) return new Summary(true, Integer.MAX_VALUE, Integer.MIN_VALUE, 0);
        Summary left = summarize(node.left), right = summarize(node.right);
        if (left.isBst() && right.isBst() && left.max() < node.key && node.key < right.min()) {
            int sum = left.sum() + node.key + right.sum();
            bestSum = Math.max(bestSum, sum);
            return new Summary(true, Math.min(left.min(), node.key), Math.max(right.max(), node.key), sum);
        }
        return new Summary(false, 0, 0, 0); // min, max and sum are never read once a subtree fails
    }

    static int maxSumBST(Node root) {
        bestSum = 0; // the empty tree is a BST with sum 0
        summarize(root);
        return bestSum;
    }

    static Node build(int... keys) {
        Node root = null;
        for (int key : keys) root = insert(root, key);
        return root;
    }

    static List<Integer> keys(Node root) {
        List<Integer> out = new ArrayList<>();
        inOrder(root, out);
        return out;
    }

    static Node leaf(int key) {
        return new Node(key, null, null);
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        Node root = build(50, 30, 80, 20, 40, 70, 90, 60);
        check(root.key == 50 && root.right.left.left.key == 60, "60 sits under 70 under 80");
        check(search(root, 60) != null && search(root, 65) == null, "search hit and miss");
        check(keys(root).equals(List.of(20, 30, 40, 50, 60, 70, 80, 90)), "in-order is sorted");
        check(height(root) == 3 && height(null) == -1 && height(leaf(1)) == 0, "heights");

        check(floor(root, 65) == 60 && ceiling(root, 65) == 70, "floor and ceiling of 65");
        check(floor(root, 50) == 50 && floor(root, 10) == null && ceiling(root, 95) == null, "floor and ceiling edges");

        check(min(root.right).key == 60, "successor of 50 is 60");
        root = delete(root, 20);                                   // case 1: a leaf
        check(keys(root).equals(List.of(30, 40, 50, 60, 70, 80, 90)), "delete leaf 20");
        root = delete(root, 70);                                   // case 2: one child
        check(root.right.left.key == 60, "60 moves up to replace 70");
        root = delete(root, 50);                                   // case 3: two children
        check(root.key == 60 && keys(root).equals(List.of(30, 40, 60, 80, 90)), "60 replaces the root");
        check(isValid(root, Long.MIN_VALUE, Long.MAX_VALUE), "still a BST after deletes");
        check(delete(null, 5) == null && keys(delete(root, 999)).size() == 5, "deleting a missing key");

        check(height(build(1, 2, 3, 4, 5, 6, 7)) == 6, "sorted insertion degenerates into a path");
        check(height(build(4, 2, 6, 1, 3, 5, 7)) == 2, "middle-first insertion is perfectly balanced");
        check(Math.abs(2 * Math.log(1_000_000) - 27.63) < 0.01, "2 ln n for a million random keys");
        check(Math.abs(2 * Math.log(1_000_001) / Math.log(2) - 39.86) < 0.01, "red-black height bound");

        Node trap = new Node(5, leaf(4), new Node(6, leaf(3), leaf(7)));
        check(!isValid(trap, Long.MIN_VALUE, Long.MAX_VALUE), "3 is right of 5, so the tree is invalid");
        check(isValid(build(4, 2, 6, 1, 3, 5, 7), Long.MIN_VALUE, Long.MAX_VALUE), "a real BST is valid");
        check(isValid(leaf(Integer.MIN_VALUE), Long.MIN_VALUE, Long.MAX_VALUE), "long bounds accept int extremes");

        Node mixed = new Node(5, new Node(4, leaf(3), leaf(6)), new Node(8, leaf(7), leaf(9)));
        check(maxSumBST(mixed) == 24, "LC 1373: the subtree 7, 8, 9");
        check(maxSumBST(new Node(-4, leaf(-2), leaf(-5))) == 0, "LC 1373: all negative, take the empty BST");
        check(maxSumBST(build(4, 2, 6, 1, 3, 5, 7)) == 28, "LC 1373: a whole BST counts");
        System.out.println("OK");
    }
}
