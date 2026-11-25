import java.util.List;

// Run from this folder with: java MinimaxLab.java
public class MinimaxLab {

    // Level 1: one move each. tree[a][b] is my score after my move a and their reply b.
    static int minimaxTwoPly(int[][] tree) {
        int best = Integer.MIN_VALUE;
        for (int[] replies : tree) {
            int worst = Integer.MAX_VALUE;
            for (int score : replies) worst = Math.min(worst, score); // they pick what hurts me most
            best = Math.max(best, worst);                             // I pick the move whose worst case is best
        }
        return best;
    }

    // Level 2: a game tree of any depth. Leaves hold scores for the maximising player.
    record Node(int score, List<Node> children) {
        static Node leaf(int score) {
            return new Node(score, List.of());
        }

        static Node of(Node... children) {
            return new Node(0, List.of(children));
        }

        boolean isLeaf() {
            return children.isEmpty();
        }
    }

    static int minimax(Node node, boolean maxToMove) {
        if (node.isLeaf()) return node.score();
        int best = maxToMove ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Node child : node.children()) {
            int value = minimax(child, !maxToMove);
            best = maxToMove ? Math.max(best, value) : Math.min(best, value);
        }
        return best;
    }

    // Level 3: pick from either end of a row. lead[i][j] = best (mine - theirs) on a[i..j] when I move.
    static int bestLead(int[] a) {
        int n = a.length;
        int[][] lead = new int[n][n];
        for (int i = n - 1; i >= 0; i--) {
            lead[i][i] = a[i];
            for (int j = i + 1; j < n; j++) {
                lead[i][j] = Math.max(a[i] - lead[i + 1][j], a[j] - lead[i][j - 1]); // take the left end or the right end
            }
        }
        return lead[0][n - 1];
    }

    static int greedyLead(int[] a) {                     // both players simply take the bigger end
        int i = 0, j = a.length - 1, lead = 0, sign = 1;
        while (i <= j) {
            int taken = a[i] >= a[j] ? a[i++] : a[j--];
            lead += sign * taken;
            sign = -sign;
        }
        return lead;
    }

    // Level 4: alpha-beta pruning. Same answer as minimax, fewer leaves looked at.
    static int leavesSeen;

    static int alphaBeta(Node node, boolean maxToMove, int alpha, int beta) {
        if (node.isLeaf()) {
            leavesSeen++;
            return node.score();
        }
        for (Node child : node.children()) {
            int value = alphaBeta(child, !maxToMove, alpha, beta);
            if (maxToMove) alpha = Math.max(alpha, value);
            else beta = Math.min(beta, value);
            if (alpha >= beta) break;                    // the other player will never allow this line, so stop
        }
        return maxToMove ? alpha : beta;
    }

    // Level 4 check: count leaves on a uniform tree with b moves per position and depth d.
    static long uniformLeaves;

    static int uniformMinimax(int b, int depth, boolean maxToMove) {
        if (depth == 0) {
            uniformLeaves++;
            return 0;
        }
        int best = maxToMove ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int k = 0; k < b; k++) {
            int value = uniformMinimax(b, depth - 1, !maxToMove);
            best = maxToMove ? Math.max(best, value) : Math.min(best, value);
        }
        return best;
    }

    // All leaves equal 0, so the first move tried is always a best move: the best case for alpha-beta.
    static int uniformAlphaBeta(int b, int depth, boolean maxToMove, int alpha, int beta) {
        if (depth == 0) {
            uniformLeaves++;
            return 0;
        }
        for (int k = 0; k < b; k++) {
            int value = uniformAlphaBeta(b, depth - 1, !maxToMove, alpha, beta);
            if (maxToMove) alpha = Math.max(alpha, value);
            else beta = Math.min(beta, value);
            if (alpha >= beta) break;
        }
        return maxToMove ? alpha : beta;
    }

    // Interview 1: LeetCode 486, Predict the Winner. Player 1 also wins a tie.
    static boolean predictTheWinner(int[] nums) {
        return bestLead(nums) >= 0;
    }

    // Interview 2: LeetCode 375, Guess Number Higher or Lower II.
    static int getMoneyAmount(int n) {
        int[][] cost = new int[n + 2][n + 2];            // cost[lo][hi] = money that guarantees a win when the number is in lo..hi
        for (int lo = n; lo >= 1; lo--) {
            for (int hi = lo + 1; hi <= n; hi++) {
                cost[lo][hi] = Integer.MAX_VALUE;
                for (int x = lo; x <= hi; x++) {         // I guess x; the answer sends me to the worse side
                    int worst = x + Math.max(cost[lo][x - 1], cost[x + 1][hi]);
                    cost[lo][hi] = Math.min(cost[lo][hi], worst);
                }
            }
        }
        return cost[1][n];
    }

    // Interview 3: LeetCode 1872, Stone Game VIII.
    static int stoneGameVIII(int[] stones) {
        int n = stones.length;
        int[] prefix = new int[n];
        prefix[0] = stones[0];
        for (int i = 1; i < n; i++) prefix[i] = prefix[i - 1] + stones[i];
        int best = prefix[n - 1];                        // D(n-1): the only move left takes everything
        for (int j = n - 2; j >= 1; j--) {
            best = Math.max(best, prefix[j] - best);     // D(j) = max(D(j+1), P[j] - D(j+1))
        }
        return best;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[][] small = {{4, 6, 5}, {7, 2, 9}, {3, 8, 1}};
        check(minimaxTwoPly(small) == 4, "worst cases 4, 2, 1; the best of them is 4");

        Node tree = Node.of(
                Node.of(Node.leaf(4), Node.leaf(6), Node.leaf(5)),
                Node.of(Node.leaf(7), Node.leaf(2), Node.leaf(9)),
                Node.of(Node.leaf(3), Node.leaf(8), Node.leaf(1)));
        check(minimax(tree, true) == 4, "the same tree as nodes");
        leavesSeen = 0;
        check(alphaBeta(tree, true, Integer.MIN_VALUE, Integer.MAX_VALUE) == 4 && leavesSeen == 6,
                "alpha-beta finds 4 after looking at 6 of the 9 leaves");
        Node deeper = Node.of(Node.of(Node.of(Node.leaf(3), Node.leaf(5)), Node.of(Node.leaf(6), Node.leaf(9))),
                Node.of(Node.of(Node.leaf(1), Node.leaf(2)), Node.of(Node.leaf(0), Node.leaf(-1))));
        check(minimax(deeper, true) == 5, "depth 3: max(min(5, 9), min(2, 0)) = 5");
        leavesSeen = 0;
        check(alphaBeta(deeper, true, Integer.MIN_VALUE, Integer.MAX_VALUE) == 5 && leavesSeen == 5, "depth 3 prunes 3 leaves");

        check(bestLead(new int[] {3, 9, 1, 2}) == 7, "minimax lead on 3, 9, 1, 2 is 7 (11 to 4)");
        check(greedyLead(new int[] {3, 9, 1, 2}) == -5, "greedy takes 3 first and loses 5 to 10");

        uniformLeaves = 0;
        uniformMinimax(10, 6, true);
        check(uniformLeaves == 1_000_000, "plain minimax: 10^6 leaves");
        uniformLeaves = 0;
        uniformAlphaBeta(10, 6, true, Integer.MIN_VALUE, Integer.MAX_VALUE);
        check(uniformLeaves == 1_999, "best-case alpha-beta: 10^3 + 10^3 - 1 leaves");
        uniformLeaves = 0;
        uniformAlphaBeta(3, 2, true, Integer.MIN_VALUE, Integer.MAX_VALUE);
        check(uniformLeaves == 5, "b = 3, d = 2: 3 + 3 - 1 = 5 leaves");

        check(!predictTheWinner(new int[] {1, 5, 2}) && bestLead(new int[] {1, 5, 2}) == -2, "LC 486 example 1");
        check(predictTheWinner(new int[] {1, 5, 233, 7}) && bestLead(new int[] {1, 5, 233, 7}) == 222, "LC 486 example 2");
        check(predictTheWinner(new int[] {1, 1}), "LC 486: a tie counts as a win for player 1");

        check(getMoneyAmount(1) == 0 && getMoneyAmount(2) == 1 && getMoneyAmount(3) == 2, "LC 375 small n");
        check(getMoneyAmount(4) == 4, "LC 375 n = 4");
        check(getMoneyAmount(10) == 16, "LC 375 n = 10");

        check(stoneGameVIII(new int[] {-1, 2, -3, 4, -5}) == 5, "LC 1872 example 1");
        check(stoneGameVIII(new int[] {7, -6, 5, 10, 5, -2, -6}) == 13, "LC 1872 example 2");
        check(stoneGameVIII(new int[] {-10, -12}) == -22, "LC 1872 example 3");
        System.out.println("OK");
    }
}
