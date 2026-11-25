import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Run from this folder with: java MatrixLab.java
public class MatrixLab {

    // Level 1: a rows x cols grid stored row after row in one flat array.
    static int flatIndex(int row, int col, int cols) {
        return row * cols + col;
    }

    // Level 2: cells with the same row + col lie on one anti-diagonal.
    static int[] antiDiagonalSums(int[][] m) {
        int[] sums = new int[m.length + m[0].length - 1];
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) sums[i + j] += m[i][j];
        }
        return sums;
    }

    // Level 3: rotation is two flips.
    static void transpose(int[][] m) {                  // square matrices only
        for (int i = 0; i < m.length; i++) {
            for (int j = i + 1; j < m.length; j++) {    // above the diagonal, so each pair swaps once
                int t = m[i][j];
                m[i][j] = m[j][i];
                m[j][i] = t;
            }
        }
    }

    static void reverseEachRow(int[][] m) {
        for (int[] row : m) {
            for (int l = 0, r = row.length - 1; l < r; l++, r--) {
                int t = row[l];
                row[l] = row[r];
                row[r] = t;
            }
        }
    }

    // Level 4: matrix multiplication.
    static int[][] multiply(int[][] a, int[][] b) {
        int n = a.length, m = b.length, p = b[0].length;
        int[][] c = new int[n][p];
        for (int i = 0; i < n; i++)
            for (int k = 0; k < m; k++)                 // i-k-j order reads b and c row by row
                for (int j = 0; j < p; j++)
                    c[i][j] += a[i][k] * b[k][j];
        return c;
    }

    // Level 5: 2D prefix sums.
    static int[][] prefixSums(int[][] a) {
        int rows = a.length, cols = a[0].length;
        int[][] p = new int[rows + 1][cols + 1];        // row 0 and column 0 stay 0
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                p[i + 1][j + 1] = a[i][j] + p[i][j + 1] + p[i + 1][j] - p[i][j];
        return p;
    }

    static int regionSum(int[][] p, int r1, int c1, int r2, int c2) {   // corners included
        return p[r2 + 1][c2 + 1] - p[r1][c2 + 1] - p[r2 + 1][c1] + p[r1][c1];
    }

    // Interview 1: LeetCode 48, Rotate Image.
    static void rotate(int[][] m) {                     // 90 degrees clockwise, in place
        transpose(m);
        reverseEachRow(m);
    }

    // Interview 2: LeetCode 54, Spiral Matrix.
    static List<Integer> spiralOrder(int[][] m) {
        List<Integer> out = new ArrayList<>();
        int top = 0, bottom = m.length - 1, left = 0, right = m[0].length - 1;
        while (top <= bottom && left <= right) {
            for (int j = left; j <= right; j++) out.add(m[top][j]);
            for (int i = top + 1; i <= bottom; i++) out.add(m[i][right]);
            if (top < bottom && left < right) {         // a lone row or column has no way back
                for (int j = right - 1; j >= left; j--) out.add(m[bottom][j]);
                for (int i = bottom - 1; i > top; i--) out.add(m[i][left]);
            }
            top++;
            bottom--;
            left++;
            right--;
        }
        return out;
    }

    // Interview 3: LeetCode 1074, Number of Submatrices That Sum to Target.
    static int numSubmatrixSumTarget(int[][] matrix, int target) {
        int rows = matrix.length, cols = matrix[0].length, count = 0;
        for (int top = 0; top < rows; top++) {
            int[] columnSums = new int[cols];           // sums of rows top..bottom, per column
            for (int bottom = top; bottom < rows; bottom++) {
                for (int c = 0; c < cols; c++) columnSums[c] += matrix[bottom][c];
                count += subarraysWithSum(columnSums, target);
            }
        }
        return count;
    }

    static int subarraysWithSum(int[] a, int target) {  // LeetCode 560 on one strip
        Map<Integer, Integer> prefixCount = new HashMap<>(Map.of(0, 1));
        int prefix = 0, count = 0;
        for (int x : a) {
            prefix += x;
            count += prefixCount.getOrDefault(prefix - target, 0);
            prefixCount.merge(prefix, 1, Integer::sum);
        }
        return count;
    }

    static int[][] grid() {
        return new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        check(flatIndex(2, 1, 4) == 9 && 9 / 4 == 2 && 9 % 4 == 1, "(2, 1) in a 3 x 4 grid is cell 9, and back");
        check(flatIndex(0, 0, 4) == 0 && flatIndex(2, 3, 4) == 11, "first and last of 12 cells");
        check(1 * 3 + 2 == 5, "column-major: (2, 1) in a 3 x 4 grid is 1 * 3 + 2 = 5");
        int[][] jagged = {{1}, {2, 3}, {4, 5, 6}};
        check(jagged[2].length == 3 && jagged[0].length == 1, "Java 2D arrays are arrays of rows");

        check(Arrays.equals(antiDiagonalSums(grid()), new int[] {1, 6, 15, 14, 9}), "five anti-diagonals");

        int[][] t = grid();
        transpose(t);
        check(Arrays.deepEquals(t, new int[][] {{1, 4, 7}, {2, 5, 8}, {3, 6, 9}}), "transpose");
        reverseEachRow(t);
        check(Arrays.deepEquals(t, new int[][] {{7, 4, 1}, {8, 5, 2}, {9, 6, 3}}), "then reverse each row");
        int[][] original = grid();
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++) check(t[i][j] == original[3 - 1 - j][i], "R(i, j) = A(n - 1 - j, i)");

        check(Arrays.deepEquals(multiply(new int[][] {{1, 2}, {3, 4}}, new int[][] {{5, 6}, {7, 8}}),
                new int[][] {{19, 22}, {43, 50}}), "2 x 2 product");
        check(Arrays.deepEquals(multiply(new int[][] {{1, 2, 3}}, new int[][] {{4}, {5}, {6}}), new int[][] {{32}}),
                "1 x 3 times 3 x 1");

        int[][] p = prefixSums(grid());
        check(p[3][3] == 45 && p[1][3] == 6 && p[3][1] == 12 && p[1][1] == 1 && p[2][2] == 12, "prefix table");
        check(regionSum(p, 1, 1, 2, 2) == 28, "bottom-right 2 x 2 block: 45 - 6 - 12 + 1 = 28");
        check(regionSum(p, 0, 0, 0, 0) == 1 && regionSum(p, 0, 0, 2, 2) == 45, "single cell and whole grid");

        int[][] image = grid();
        rotate(image);
        check(Arrays.deepEquals(image, new int[][] {{7, 4, 1}, {8, 5, 2}, {9, 6, 3}}), "LC 48");
        int[][] four = {{5, 1, 9, 11}, {2, 4, 8, 10}, {13, 3, 6, 7}, {15, 14, 12, 16}};
        rotate(four);
        check(Arrays.deepEquals(four, new int[][] {{15, 13, 2, 5}, {14, 3, 4, 1}, {12, 6, 8, 9}, {16, 7, 10, 11}}), "LC 48 4 x 4");

        int[][] wide = {{1, 2, 3, 4}, {5, 6, 7, 8}, {9, 10, 11, 12}};
        check(spiralOrder(wide).equals(List.of(1, 2, 3, 4, 8, 12, 11, 10, 9, 5, 6, 7)), "LC 54");
        check(spiralOrder(grid()).equals(List.of(1, 2, 3, 6, 9, 8, 7, 4, 5)), "LC 54 square");
        check(spiralOrder(new int[][] {{1}, {2}, {3}}).equals(List.of(1, 2, 3)), "LC 54 single column");
        check(2 * (3 + 4) - 4 == 10, "outer ring of a 3 x 4 grid has 10 cells");

        check(subarraysWithSum(new int[] {0, 0}, 0) == 3, "strip [0, 0] has 3 zero-sum pieces");
        check(numSubmatrixSumTarget(new int[][] {{1, -1}, {-1, 1}}, 0) == 5, "LC 1074");
        check(numSubmatrixSumTarget(new int[][] {{0, 1, 0}, {1, 1, 1}, {0, 1, 0}}, 0) == 4, "LC 1074 example 1");
        check(numSubmatrixSumTarget(new int[][] {{904}}, 0) == 0, "LC 1074 single cell");
        System.out.println("OK");
    }
}
