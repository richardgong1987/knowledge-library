import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Run from this folder with: java BacktrackingLab.java
public class BacktrackingLab {

    static long nodes;   // nodes of the state-space tree visited by the last search

    // Level 1: every subset (LeetCode 78). Level k of the tree decides x_{k+1}: take items[k] or not.
    static void subsets(int[] items, int k, List<Integer> chosen, List<List<Integer>> out) {
        nodes++;                                        // every call is one node of the tree
        if (k == items.length) { out.add(new ArrayList<>(chosen)); return; } // a leaf: all n decided
        chosen.add(items[k]);                           // choose: x_{k+1} = 1, take items[k]
        subsets(items, k + 1, chosen, out);             // explore
        chosen.remove(chosen.size() - 1);               // un-choose
        subsets(items, k + 1, chosen, out);             // x_{k+1} = 0, skip items[k]
    }

    // Level 2: every ordering (LeetCode 46). Level k chooses which unused item goes in position k + 1.
    static void permute(int[] items, boolean[] used, List<Integer> path, List<List<Integer>> out) {
        nodes++;
        if (path.size() == items.length) { out.add(new ArrayList<>(path)); return; }
        for (int i = 0; i < items.length; i++) {
            if (used[i]) continue;                      // each item at most once
            used[i] = true;                             // choose
            path.add(items[i]);
            permute(items, used, path, out);            // explore
            path.remove(path.size() - 1);               // un-choose
            used[i] = false;
        }
    }

    // Level 3: subsets of positive items that sum to a target, with and without pruning.
    static final class SubsetSum {
        final int[] items;
        final int[] restSum;                            // restSum[k] = items[k] + ... + items[n - 1]
        final boolean prune;
        final List<Integer> chosen = new ArrayList<>();
        final List<List<Integer>> solutions = new ArrayList<>();
        long nodes;

        SubsetSum(int[] items, int target, boolean prune) {
            this.items = items;
            this.prune = prune;
            restSum = new int[items.length + 1];
            for (int k = items.length - 1; k >= 0; k--) restSum[k] = restSum[k + 1] + items[k];
            search(0, target);
        }

        void search(int k, int remaining) {
            nodes++;
            if (prune && (remaining < 0 || remaining > restSum[k])) return; // no completion can work
            if (k == items.length) {
                if (remaining == 0) solutions.add(new ArrayList<>(chosen));
                return;
            }
            chosen.add(items[k]);
            search(k + 1, remaining - items[k]);
            chosen.remove(chosen.size() - 1);
            search(k + 1, remaining);
        }
    }

    // Interview 1: LeetCode 39, Combination Sum. Candidates are sorted, and each may be reused.
    static void combinationSum(int[] sorted, int start, int remaining, List<Integer> path, List<List<Integer>> out) {
        nodes++;
        if (remaining == 0) { out.add(new ArrayList<>(path)); return; }
        for (int i = start; i < sorted.length; i++) {
            if (sorted[i] > remaining) break;           // sorted: every later candidate is too big as well
            path.add(sorted[i]);
            combinationSum(sorted, i, remaining - sorted[i], path, out);   // i, not i + 1: reuse allowed
            path.remove(path.size() - 1);
        }
    }

    static List<List<Integer>> combinationSum(int[] candidates, int target) {
        int[] sorted = candidates.clone();
        Arrays.sort(sorted);
        List<List<Integer>> out = new ArrayList<>();
        combinationSum(sorted, 0, target, new ArrayList<>(), out);
        return out;
    }

    // Interview 1 check: the same search without the break, which only stops after overshooting.
    static void combinationSumWithoutBreak(int[] sorted, int start, int remaining) {
        nodes++;
        if (remaining <= 0) return;
        for (int i = start; i < sorted.length; i++) combinationSumWithoutBreak(sorted, i, remaining - sorted[i]);
    }

    // Interview 2: LeetCode 51, N-Queens. Two queens attack each other exactly when this is true.
    static boolean attacks(int r1, int c1, int r2, int c2) {
        return c1 == c2 || r1 - c1 == r2 - c2 || r1 + c1 == r2 + c2;
    }

    static final class NQueens {
        final int n;
        final int[] queenCol;                           // queenCol[r] = x_{r+1}, the column used in row r
        final boolean[] col, diag, anti;                // taken columns, r - c diagonals, r + c diagonals
        final List<List<String>> boards = new ArrayList<>();
        long nodes;

        NQueens(int n) {
            this.n = n;
            queenCol = new int[n];
            col = new boolean[n];
            diag = new boolean[2 * n - 1];
            anti = new boolean[2 * n - 1];
            place(0);
        }

        void place(int r) {
            nodes++;
            if (r == n) { boards.add(draw()); return; }
            for (int c = 0; c < n; c++) {
                if (col[c] || diag[r - c + n - 1] || anti[r + c]) continue;   // attacked: prune
                queenCol[r] = c;
                col[c] = diag[r - c + n - 1] = anti[r + c] = true;           // choose
                place(r + 1);                                                // explore
                col[c] = diag[r - c + n - 1] = anti[r + c] = false;          // un-choose
            }
        }

        List<String> draw() {
            List<String> board = new ArrayList<>();
            for (int r = 0; r < n; r++) {
                char[] row = ".".repeat(n).toCharArray();
                row[queenCol[r]] = 'Q';
                board.add(new String(row));
            }
            return board;
        }
    }

    // Interview 3: LeetCode 37, Sudoku Solver. Stops at the first complete grid.
    static boolean solveSudoku(char[][] board) {
        nodes++;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] != '.') continue;
                for (char d = '1'; d <= '9'; d++) {
                    if (!canPlace(board, r, c, d)) continue;   // prune: d is already in the row, column or box
                    board[r][c] = d;                           // choose
                    if (solveSudoku(board)) return true;       // explore; true means solved, so stop
                    board[r][c] = '.';                         // un-choose
                }
                return false;                                  // no digit fits this cell: go back
            }
        }
        return true;                                           // no empty cell is left
    }

    static boolean canPlace(char[][] board, int r, int c, char d) {
        for (int i = 0; i < 9; i++) {
            if (board[r][i] == d || board[i][c] == d) return false;
            if (board[3 * (r / 3) + i / 3][3 * (c / 3) + i % 3] == d) return false;  // the 3 x 3 box
        }
        return true;
    }

    // Mathematical Abstraction check: count the prefixes (x_1, ..., x_k) whose queens never attack each other.
    static long[] safePrefixesPerLevel(int n) {
        long[] perLevel = new long[n + 1];
        countSafePrefixes(n, new int[n], 0, perLevel);
        return perLevel;
    }

    static void countSafePrefixes(int n, int[] x, int k, long[] perLevel) {
        perLevel[k]++;
        if (k == n) return;
        for (int c = 0; c < n; c++) {
            boolean safe = true;
            for (int r = 0; r < k; r++) if (attacks(r, x[r], k, c)) safe = false;
            if (safe) { x[k] = c; countSafePrefixes(n, x, k + 1, perLevel); }
        }
    }

    static long binomial(int n, int k) {
        long result = 1;
        for (int i = 1; i <= k; i++) result = result * (n - k + i) / i;
        return result;
    }

    static long factorial(int n) {
        return n == 0 ? 1 : n * factorial(n - 1);
    }

    static long permutationTreeNodes(int n) {           // sum over k of n! / (n - k)!
        long total = 0, level = 1;
        for (int k = 0; k <= n; k++) {
            total += level;
            level *= n - k;
        }
        return total;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        List<List<Integer>> all = new ArrayList<>();
        nodes = 0;
        subsets(new int[] {1, 2, 3}, 0, new ArrayList<>(), all);
        check(all.size() == 8 && nodes == 15, "3 items: 8 subsets, 2^4 - 1 = 15 nodes");
        check(all.get(0).equals(List.of(1, 2, 3)) && all.get(7).equals(List.of()), "take-first order");
        all.clear();
        nodes = 0;
        subsets(new int[] {1, 2, 3, 4}, 0, new ArrayList<>(), all);
        check(all.size() == 16 && nodes == 31, "4 items: 16 subsets, 31 nodes");

        List<List<Integer>> orders = new ArrayList<>();
        nodes = 0;
        permute(new int[] {1, 2, 3}, new boolean[3], new ArrayList<>(), orders);
        check(orders.size() == 6 && nodes == 16, "3 items: 6 orders, 1 + 3 + 6 + 6 = 16 nodes");
        check(orders.get(0).equals(List.of(1, 2, 3)) && orders.get(5).equals(List.of(3, 2, 1)), "order of output");
        orders.clear();
        nodes = 0;
        permute(new int[] {1, 2, 3, 4}, new boolean[4], new ArrayList<>(), orders);
        check(orders.size() == 24 && nodes == 65 && permutationTreeNodes(4) == 65, "4 items: 65 nodes");
        check(permutationTreeNodes(10) == 9_864_101, "10 items: 9,864,101 nodes");
        for (int n = 1; n <= 10; n++) {
            check(permutationTreeNodes(n) == (long) Math.floor(Math.E * factorial(n)), "nodes = floor(e n!)");
            check(permutationTreeNodes(n) == 1 + n * permutationTreeNodes(n - 1), "N(n) = 1 + n N(n - 1)");
        }

        int[] items = {3, 5, 6, 7};
        SubsetSum plain = new SubsetSum(items, 15, false);
        SubsetSum pruned = new SubsetSum(items, 15, true);
        check(plain.solutions.equals(List.of(List.of(3, 5, 7))) && pruned.solutions.equals(plain.solutions), "3 + 5 + 7");
        check(plain.nodes == 31, "without pruning: the whole tree of 31 nodes");
        check(pruned.nodes == 21, "with pruning: 21 nodes, and 10 are never created");
        check(new SubsetSum(items, 30, true).nodes == 1, "target 30 > 21 = everything: the root is cut");

        nodes = 0;
        check(combinationSum(new int[] {2, 3, 6, 7}, 7).equals(List.of(List.of(2, 2, 3), List.of(7))), "LC 39");
        check(nodes == 10, "LC 39 with the break: 10 nodes");
        nodes = 0;
        combinationSumWithoutBreak(new int[] {2, 3, 6, 7}, 0, 7);
        check(nodes == 28, "LC 39 without the break: 28 nodes");
        check(combinationSum(new int[] {2}, 1).isEmpty(), "LC 39 impossible");

        check(attacks(0, 1, 2, 3) && attacks(1, 3, 3, 1) && attacks(0, 2, 3, 2), "diagonal, anti-diagonal, column");
        check(!attacks(1, 3, 2, 0) && !attacks(0, 1, 1, 3), "safe pairs");
        NQueens four = new NQueens(4);
        check(four.boards.equals(List.of(List.of(".Q..", "...Q", "Q...", "..Q."), List.of("..Q.", "Q...", "...Q", ".Q.."))),
                "n = 4: two boards");
        check(four.nodes == 17, "n = 4: 17 nodes with pruning");
        NQueens eight = new NQueens(8);
        check(eight.boards.size() == 92 && eight.nodes == 2057, "n = 8: 92 solutions, 2057 nodes");
        check(Arrays.equals(safePrefixesPerLevel(4), new long[] {1, 4, 6, 4, 2}), "n = 4: 1 + 4 + 6 + 4 + 2 = 17");
        check(Arrays.stream(safePrefixesPerLevel(8)).sum() == 2057, "n = 8: the formula also gives 2057");
        check(binomial(16, 4) == 1820 && binomial(64, 8) == 4_426_165_368L, "ways to drop 4 and 8 queens on a board");
        check(new NQueens(1).boards.size() == 1 && new NQueens(2).boards.isEmpty() && new NQueens(3).boards.isEmpty(), "tiny n");
        check(permutationTreeNodes(8) == 109_601, "one queen per row and column, no diagonal check: 109,601 nodes");
        check((Math.pow(8, 9) - 1) / 7 == 19_173_961, "one queen per row, no checks at all: 19,173,961 nodes");

        String[] puzzle = {
            "53..7....", "6..195...", ".98....6.", "8...6...3", "4..8.3..1",
            "7...2...6", ".6....28.", "...419..5", "....8..79"};
        char[][] board = new char[9][];
        int empty = 0;
        for (int r = 0; r < 9; r++) {
            board[r] = puzzle[r].toCharArray();
            for (char ch : board[r]) if (ch == '.') empty++;
        }
        check(empty == 51, "51 empty cells");
        nodes = 0;
        check(solveSudoku(board), "LC 37 solved");
        check(new String(board[0]).equals("534678912") && new String(board[8]).equals("345286179"), "LC 37 rows");
        check(nodes == 4209, "LC 37: 4,209 calls");
        check(Math.abs(Math.pow(9, 51) / 4.64e48 - 1) < 0.01, "9^51 is about 4.64 x 10^48");
        check(3 * (4 / 3) == 3 && 3 * (7 / 3) == 6, "cell (4, 7) is in the box whose top-left corner is (3, 6)");
        System.out.println("OK");
    }
}
