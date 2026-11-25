import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Queue;

// Run from this folder with: java BinaryTreeLab.java
public class BinaryTreeLab {

    // Level 1: a node with at most two children, shaped like LeetCode's TreeNode.
    static final class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int val) {
            this.val = val;
        }

        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    // The lesson's example tree:
    //
    //          1
    //         / \
    //        2   3
    //       / \   \
    //      4   5   6
    static TreeNode example() {
        return new TreeNode(1,
                new TreeNode(2, new TreeNode(4), new TreeNode(5)),
                new TreeNode(3, null, new TreeNode(6)));
    }

    static TreeNode perfect(int height) {
        if (height < 0) return null;
        return new TreeNode(height, perfect(height - 1), perfect(height - 1));
    }

    // Level 2: the three depth-first orders differ only in where the visit happens.
    static void preorder(TreeNode node, List<Integer> out) {
        if (node == null) return;
        out.add(node.val);           // visit before the children
        preorder(node.left, out);
        preorder(node.right, out);
    }

    static void inorder(TreeNode node, List<Integer> out) {
        if (node == null) return;
        inorder(node.left, out);
        out.add(node.val);           // visit between the children
        inorder(node.right, out);
    }

    static void postorder(TreeNode node, List<Integer> out) {
        if (node == null) return;
        postorder(node.left, out);
        postorder(node.right, out);
        out.add(node.val);           // visit after the children
    }

    // Level 3: f(node) = combine(f(left), f(right)).
    static int size(TreeNode node) {
        if (node == null) return 0;
        return size(node.left) + size(node.right) + 1;
    }

    static int height(TreeNode node) {
        if (node == null) return -1;                     // so that a single leaf has height 0
        return Math.max(height(node.left), height(node.right)) + 1;
    }

    // Level 4: level order, with a queue.
    static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> levels = new ArrayList<>();
        Queue<TreeNode> queue = new ArrayDeque<>();
        if (root != null) queue.add(root);
        while (!queue.isEmpty()) {
            List<Integer> level = new ArrayList<>();
            for (int i = queue.size(); i > 0; i--) {     // exactly the nodes of the current level
                TreeNode node = queue.poll();
                level.add(node.val);
                if (node.left != null) queue.add(node.left);
                if (node.right != null) queue.add(node.right);
            }
            levels.add(level);
        }
        return levels;
    }

    // Level 5: in-order with our own stack instead of the call stack.
    static List<Integer> inorderIterative(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode node = root;
        while (node != null || !stack.isEmpty()) {
            while (node != null) {                       // go left as far as possible
                stack.push(node);
                node = node.left;
            }
            node = stack.pop();
            out.add(node.val);
            node = node.right;
        }
        return out;
    }

    // Interview 1: LeetCode 236, Lowest Common Ancestor of a Binary Tree.
    static TreeNode lowestCommonAncestor(TreeNode node, TreeNode p, TreeNode q) {
        if (node == null || node == p || node == q) return node;
        TreeNode left = lowestCommonAncestor(node.left, p, q);
        TreeNode right = lowestCommonAncestor(node.right, p, q);
        if (left != null && right != null) return node;  // p and q are on different sides
        return left != null ? left : right;
    }

    // Interview 2: LeetCode 124, Binary Tree Maximum Path Sum.
    static int maxPathSum(TreeNode root) {
        int[] best = {Integer.MIN_VALUE};
        gain(root, best);
        return best[0];
    }

    // The best downward path that starts at node; also records the best path that bends at node.
    static int gain(TreeNode node, int[] best) {
        if (node == null) return 0;
        int left = Math.max(0, gain(node.left, best));   // a branch with a negative sum is left out
        int right = Math.max(0, gain(node.right, best));
        best[0] = Math.max(best[0], node.val + left + right);
        return node.val + Math.max(left, right);
    }

    // Interview 3: LeetCode 297, Serialize and Deserialize Binary Tree.
    static String serialize(TreeNode node) {
        if (node == null) return "#";
        return node.val + "," + serialize(node.left) + "," + serialize(node.right);
    }

    static TreeNode deserialize(String data) {
        return build(new ArrayDeque<>(Arrays.asList(data.split(","))));
    }

    static TreeNode build(Deque<String> tokens) {
        String token = tokens.poll();
        if (token.equals("#")) return null;
        TreeNode node = new TreeNode(Integer.parseInt(token));
        node.left = build(tokens);
        node.right = build(tokens);
        return node;
    }

    // Used only by the checks below.
    static TreeNode find(TreeNode node, int val) {
        if (node == null || node.val == val) return node;
        TreeNode inLeft = find(node.left, val);
        return inLeft != null ? inLeft : find(node.right, val);
    }

    static int countWithChildren(TreeNode node, int k) {
        if (node == null) return 0;
        int children = (node.left != null ? 1 : 0) + (node.right != null ? 1 : 0);
        return (children == k ? 1 : 0) + countWithChildren(node.left, k) + countWithChildren(node.right, k);
    }

    static int minHeight(int n) {                        // the smallest h with 2^(h+1) - 1 >= n
        int h = 0;
        while ((1L << (h + 1)) - 1 < n) h++;
        return h;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        TreeNode root = example();

        // Level 1: perfect trees, and the height range for n = 6 and n = 1,000,000
        for (int h = 0; h <= 10; h++) check(size(perfect(h)) == (1 << (h + 1)) - 1, "perfect tree of height " + h);
        check(size(perfect(2)) == 7 && size(perfect(3)) == 15, "7 and 15 nodes");
        check(size(root) == 6 && height(root) == 2, "the example has 6 nodes and height 2");
        check(minHeight(6) == 2 && minHeight(7) == 2 && minHeight(8) == 3, "ceil(log2(n + 1)) - 1");
        check(minHeight(1_000_000) == 19, "a million nodes need height at least 19");

        // Level 2
        List<Integer> pre = new ArrayList<>(), in = new ArrayList<>(), post = new ArrayList<>(), none = new ArrayList<>();
        preorder(root, pre);
        inorder(root, in);
        postorder(root, post);
        preorder(null, none);
        check(pre.equals(List.of(1, 2, 4, 5, 3, 6)), "pre-order");
        check(in.equals(List.of(4, 2, 5, 1, 3, 6)), "in-order");
        check(post.equals(List.of(4, 5, 2, 6, 3, 1)), "post-order");
        check(none.isEmpty(), "empty tree");

        // Level 3
        check(height(new TreeNode(7)) == 0 && height(null) == -1 && size(null) == 0, "base cases");
        check(height(root.left) == 1 && height(root.right) == 1 && size(root.left) == 3 && size(root.right) == 2,
                "the fairy's answers for the two subtrees");

        // The problem: reading row by row loses the shape
        TreeNode sixOnTheLeft = new TreeNode(1,
                new TreeNode(2, new TreeNode(4), new TreeNode(5)),
                new TreeNode(3, new TreeNode(6), null));
        check(levelOrder(sixOnTheLeft).equals(levelOrder(root)) && !serialize(sixOnTheLeft).equals(serialize(root)),
                "same rows, different trees");

        // Level 4
        check(levelOrder(root).equals(List.of(List.of(1), List.of(2, 3), List.of(4, 5, 6))), "level order");
        check(levelOrder(null).isEmpty(), "level order of an empty tree");

        // Level 5
        check(inorderIterative(root).equals(List.of(4, 2, 5, 1, 3, 6)), "iterative in-order");

        // Why it works: leaves = (nodes with two children) + 1
        check(countWithChildren(root, 0) == 3 && countWithChildren(root, 1) == 1 && countWithChildren(root, 2) == 2,
                "n0 = 3, n1 = 1, n2 = 2");

        // Interview 1
        check(lowestCommonAncestor(root, find(root, 4), find(root, 6)).val == 1, "LCA(4, 6) = 1");
        check(lowestCommonAncestor(root, find(root, 4), find(root, 5)).val == 2, "LCA(4, 5) = 2");
        check(lowestCommonAncestor(root, find(root, 2), find(root, 5)).val == 2, "LCA(2, 5) = 2");

        // Interview 2: LeetCode's examples and an all-negative tree
        TreeNode leetcode = new TreeNode(-10, new TreeNode(9), new TreeNode(20, new TreeNode(15), new TreeNode(7)));
        check(maxPathSum(leetcode) == 42, "LC 124: 15 + 20 + 7");
        check(gain(leetcode.right, new int[] {Integer.MIN_VALUE}) == 35, "gain(20) = 35");
        check(maxPathSum(new TreeNode(1, new TreeNode(2), new TreeNode(3))) == 6, "LC 124 example 1");
        check(maxPathSum(new TreeNode(-3)) == -3, "LC 124 single negative node");
        check(maxPathSum(root) == 17, "LC 124 on the example: 5 + 2 + 1 + 3 + 6");

        // Interview 3
        String text = serialize(root);
        check(text.equals("1,2,4,#,#,5,#,#,3,#,6,#,#"), "serialized example");
        check(text.split(",").length == 13 && text.chars().filter(c -> c == '#').count() == 7, "2n + 1 tokens, n + 1 markers");
        check(serialize(deserialize(text)).equals(text), "round trip");
        check(serialize(null).equals("#") && deserialize("#") == null, "empty tree");
        check(!serialize(new TreeNode(1, new TreeNode(2), null)).equals(serialize(new TreeNode(1, null, new TreeNode(2)))),
                "null markers tell left from right");
        check(serialize(new TreeNode(1, new TreeNode(2), null)).equals("1,2,#,#,#"), "2 as left child");
        check(serialize(new TreeNode(-7)).equals("-7,#,#"), "negative values");

        System.out.println("OK");
    }
}
