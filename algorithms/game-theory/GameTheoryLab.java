import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

// Run from this folder with: java GameTheoryLab.java
public class GameTheoryLab {

    // Level 1: a take-away game. A move removes m stones for some m in moves; whoever cannot move loses.
    static boolean[] winTable(int maxStones, int[] moves) {
        boolean[] win = new boolean[maxStones + 1];      // win[n]: the player to move with n stones can force a win
        for (int n = 1; n <= maxStones; n++) {
            for (int m : moves) {
                if (m <= n && !win[n - m]) {             // this move leaves the opponent in a losing position
                    win[n] = true;
                    break;
                }
            }
        }
        return win;
    }

    // Level 2: Nim. XOR all the heap sizes.
    static int nimSum(int... heaps) {
        int x = 0;
        for (int h : heaps) x ^= h;
        return x;
    }

    record NimMove(int heap, int newSize) {}

    static Optional<NimMove> winningNimMove(int[] heaps) {
        int x = nimSum(heaps);
        for (int i = 0; i < heaps.length; i++) {
            int target = heaps[i] ^ x;                   // the size that would make the nim-sum 0
            if (target < heaps[i]) return Optional.of(new NimMove(i, target));
        }
        return Optional.empty();                         // nim-sum 0: every move hands the opponent a win
    }

    // Level 2 check: brute-force Nim on three heaps, to confirm the XOR rule.
    static boolean nimWinsByBruteForce(int a, int b, int c, Boolean[][][] memo) {
        if (memo[a][b][c] != null) return memo[a][b][c];
        boolean win = false;
        for (int k = 1; k <= a && !win; k++) win = !nimWinsByBruteForce(a - k, b, c, memo);
        for (int k = 1; k <= b && !win; k++) win = !nimWinsByBruteForce(a, b - k, c, memo);
        for (int k = 1; k <= c && !win; k++) win = !nimWinsByBruteForce(a, b, c - k, memo);
        return memo[a][b][c] = win;
    }

    // Level 3: Sprague-Grundy values. g(n) = mex of the values one move away.
    static int[] grundyTable(int maxStones, int[] moves) {
        int[] g = new int[maxStones + 1];
        for (int n = 1; n <= maxStones; n++) {
            Set<Integer> reachable = new HashSet<>();
            for (int m : moves) if (m <= n) reachable.add(g[n - m]);
            int mex = 0;
            while (reachable.contains(mex)) mex++;       // the smallest value no move reaches
            g[n] = mex;
        }
        return g;
    }

    // Level 3 check: two piles of the take-away game played together, by brute force.
    static boolean twoPileWins(int a, int b, int[] moves, Boolean[][] memo) {
        if (memo[a][b] != null) return memo[a][b];
        boolean win = false;
        for (int m : moves) {
            if (m <= a && !twoPileWins(a - m, b, moves, memo)) win = true;
            if (m <= b && !twoPileWins(a, b - m, moves, memo)) win = true;
        }
        return memo[a][b] = win;
    }

    // Interview 1: LeetCode 877, Stone Game.
    static boolean stoneGame(int[] piles) {
        return true;                                     // even pile count, odd total: Alice takes the richer colour
    }

    // Interview 1 check: the full score-difference DP, to confirm that Alice always wins.
    static int bestLead(int[] a) {
        int n = a.length;
        int[][] lead = new int[n][n];
        for (int i = n - 1; i >= 0; i--) {
            lead[i][i] = a[i];
            for (int j = i + 1; j < n; j++) lead[i][j] = Math.max(a[i] - lead[i + 1][j], a[j] - lead[i][j - 1]);
        }
        return lead[0][n - 1];
    }

    // Level 4: scores instead of wins. Each turn takes 1 to maxTake stones from the front of the row.
    static int bestMargin(int[] values, int maxTake) {
        int n = values.length;
        int[] lead = new int[n + 1];                     // lead[i] = best (mine - theirs) when stones i.. remain and I move
        for (int i = n - 1; i >= 0; i--) {
            lead[i] = Integer.MIN_VALUE;
            int taken = 0;
            for (int k = 1; k <= maxTake && i + k <= n; k++) {
                taken += values[i + k - 1];
                lead[i] = Math.max(lead[i], taken - lead[i + k]); // I gain taken, then the opponent leads by lead[i + k]
            }
        }
        return lead[0];
    }

    // Interview 2: LeetCode 1406, Stone Game III.
    static String stoneGameIII(int[] values) {
        int lead = bestMargin(values, 3);
        return lead > 0 ? "Alice" : lead < 0 ? "Bob" : "Tie";
    }

    // Interview 3: LeetCode 913, Cat and Mouse.
    static final int DRAW = 0, MOUSE_WINS = 1, CAT_WINS = 2;
    static final int MOUSE_TURN = 0, CAT_TURN = 1;

    static int[][][] countMoves(int[][] graph) {
        int n = graph.length;
        int[][][] movesLeft = new int[n][n][2];          // moves not yet known to lose, for the player to move
        for (int m = 0; m < n; m++) {
            for (int c = 1; c < n; c++) {
                movesLeft[m][c][MOUSE_TURN] = graph[m].length;
                movesLeft[m][c][CAT_TURN] = graph[c].length;
                for (int next : graph[c]) if (next == 0) movesLeft[m][c][CAT_TURN]--; // the cat may not enter the hole
            }
        }
        return movesLeft;
    }

    static int catMouseGame(int[][] graph) {
        int n = graph.length;
        int[][][] result = new int[n][n][2];             // result[mouse][cat][turn]; DRAW until proved otherwise
        int[][][] movesLeft = countMoves(graph);
        Deque<int[]> solved = new ArrayDeque<>();
        for (int c = 1; c < n; c++) {
            for (int turn = 0; turn < 2; turn++) {
                result[0][c][turn] = MOUSE_WINS;         // the mouse is in the hole
                result[c][c][turn] = CAT_WINS;           // the cat caught the mouse
                solved.add(new int[] {0, c, turn});
                solved.add(new int[] {c, c, turn});
            }
        }
        while (!solved.isEmpty()) {
            int[] s = solved.poll();
            int m = s[0], c = s[1], turn = s[2], outcome = result[m][c][turn];
            int prevTurn = 1 - turn;                     // the player who moved into state s
            for (int from : prevTurn == MOUSE_TURN ? graph[m] : graph[c]) {
                int pm = prevTurn == MOUSE_TURN ? from : m, pc = prevTurn == CAT_TURN ? from : c;
                if (pc == 0 || result[pm][pc][prevTurn] != DRAW) continue;
                boolean moverWins = outcome == (prevTurn == MOUSE_TURN ? MOUSE_WINS : CAT_WINS);
                if (moverWins || --movesLeft[pm][pc][prevTurn] == 0) { // one winning move, or every move loses
                    result[pm][pc][prevTurn] = outcome;
                    solved.add(new int[] {pm, pc, prevTurn});
                }
            }
        }
        return result[1][2][MOUSE_TURN];
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        boolean[] twoMoves = winTable(9, new int[] {1, 2});
        for (int n = 0; n <= 9; n++) check(twoMoves[n] == (n % 3 != 0), "take 1 or 2: lose exactly on multiples of 3");

        boolean[] win = winTable(16, new int[] {1, 3, 4});
        int[] losing = IntStream.rangeClosed(0, 16).filter(n -> !win[n]).toArray();
        check(Arrays.equals(losing, new int[] {0, 2, 7, 9, 14, 16}), "take 1, 3 or 4: P-positions 0, 2, 7, 9, 14, 16");

        check(nimSum(3, 4, 5) == 2, "3 xor 4 xor 5 = 2");
        check(nimSum(1, 2, 3) == 0, "1 xor 2 xor 3 = 0");
        check(winningNimMove(new int[] {3, 4, 5}).equals(Optional.of(new NimMove(0, 1))), "shrink the 3-heap to 1");
        check(nimSum(1, 4, 5) == 0, "after the move the nim-sum is 0");
        check(winningNimMove(new int[] {1, 4, 5}).isEmpty(), "no winning move from nim-sum 0");
        Boolean[][][] nimMemo = new Boolean[8][8][8];
        for (int a = 0; a < 8; a++) {
            for (int b = 0; b < 8; b++) {
                for (int c = 0; c < 8; c++) {
                    check(nimWinsByBruteForce(a, b, c, nimMemo) == (nimSum(a, b, c) != 0), "Bouton's theorem on heaps up to 7");
                }
            }
        }

        int[] g = grundyTable(10, new int[] {1, 3, 4});
        check(Arrays.equals(g, new int[] {0, 1, 0, 1, 2, 3, 2, 0, 1, 0, 1}), "Grundy values for moves 1, 3, 4");
        Boolean[][] pairMemo = new Boolean[11][11];
        for (int a = 0; a <= 10; a++) {
            for (int b = 0; b <= 10; b++) {
                check(twoPileWins(a, b, new int[] {1, 3, 4}, pairMemo) == ((g[a] ^ g[b]) != 0), "Sprague-Grundy on two piles");
            }
        }
        check((g[4] ^ g[6]) == 0 && (g[5] ^ g[6]) == 1, "piles 4 and 6 lose; piles 5 and 6 win");

        check(stoneGame(new int[] {5, 3, 4, 5}) && bestLead(new int[] {5, 3, 4, 5}) == 1, "LC 877: Alice wins 9 to 8");
        int oddRows = 0;
        for (int code = 0; code < 625; code++) {
            int[] piles = {1 + code % 5, 1 + code / 5 % 5, 1 + code / 25 % 5, 1 + code / 125 % 5};
            if (Arrays.stream(piles).sum() % 2 == 0) continue;
            oddRows++;
            check(bestLead(piles) > 0, "LC 877: Alice always wins");
        }
        check(oddRows == 312, "312 four-pile rows with an odd total");

        check(bestMargin(new int[] {4, -1, 2}, 2) == 3, "take the 4, then the opponent nets 1");
        check(bestMargin(new int[] {1, 2, 3, 7}, 3) == -1, "LC 1406 example 1 margin");
        check(stoneGameIII(new int[] {1, 2, 3, 7}).equals("Bob"), "LC 1406 example 1");
        check(stoneGameIII(new int[] {1, 2, 3, -9}).equals("Alice"), "LC 1406 example 2");
        check(stoneGameIII(new int[] {1, 2, 3, 6}).equals("Tie"), "LC 1406 example 3");

        check(catMouseGame(new int[][] {{2, 5}, {3}, {0, 4, 5}, {1, 4, 5}, {2, 3}, {0, 2, 3}}) == DRAW, "LC 913 example 1");
        check(catMouseGame(new int[][] {{1, 3}, {0}, {3}, {0, 2}}) == MOUSE_WINS, "LC 913 example 2");
        check(catMouseGame(new int[][] {{2}, {2}, {0, 1}}) == CAT_WINS, "LC 913: the only path to the hole runs past the cat");
        System.out.println("OK");
    }
}
