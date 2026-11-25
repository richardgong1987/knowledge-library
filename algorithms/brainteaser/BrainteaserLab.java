import java.util.Arrays;

// Run from this folder with: java BrainteaserLab.java
public class BrainteaserLab {

    // Level 1: solve small cases by brute force, then look for the pattern.
    static boolean[] nimTable(int max) {                  // win[n]: can the player to move win with n stones?
        boolean[] win = new boolean[max + 1];             // win[0] = false: no stones, no move, you lost
        for (int n = 1; n <= max; n++)
            for (int take = 1; take <= 3 && take <= n; take++)
                if (!win[n - take]) win[n] = true;        // leave the opponent a losing pile
        return win;
    }

    // Level 2: a colouring invariant. Every domino covers one dark and one light square.
    static int colourImbalance(int size) {                // opposite corners removed; both are the same colour
        int dark = 0, light = 0;
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++) {
                boolean isRemoved = (r == 0 && c == 0) || (r == size - 1 && c == size - 1);
                if (isRemoved) continue;
                if ((r + c) % 2 == 0) dark++; else light++;
            }
        return Math.abs(dark - light);
    }

    // Level 3: simulate the lockers, to check the pairing argument.
    static int openAfterToggling(int n) {                 // round r toggles every r-th locker
        boolean[] open = new boolean[n + 1];
        for (int r = 1; r <= n; r++)
            for (int locker = r; locker <= n; locker += r) open[locker] = !open[locker];
        int count = 0;
        for (int locker = 1; locker <= n; locker++) if (open[locker]) count++;
        return count;
    }

    // Level 4: the counting bound. t tests with k outcomes each tell apart at most k^t cases.
    static int minTests(long cases, int outcomes) {       // smallest t with outcomes^t >= cases
        int t = 0;
        for (long reach = 1; reach < cases; reach *= outcomes) t++;
        return t;
    }

    // Level 5: Bouton's theorem for Nim with several piles.
    static boolean firstPlayerWinsNim(int[] piles) {      // Bouton (1901): lose exactly when the XOR is 0
        int xor = 0;
        for (int pile : piles) xor ^= pile;
        return xor != 0;
    }

    // Interview 1: LeetCode 292, Nim Game.
    static boolean canWinNim(int n) {
        return n % 4 != 0;                                // a multiple of 4 is the only losing pile
    }

    // Interview 2: LeetCode 1025, Divisor Game.
    static boolean divisorGame(int n) {
        return n % 2 == 0;                                // even: take 1 and hand back an odd number
    }

    // Interview 3: LeetCode 319, Bulb Switcher.
    static int bulbSwitch(int n) {
        return (int) Math.sqrt(n);                        // bulbs left on are the squares 1, 4, 9, ...
    }

    // Interview 4: LeetCode 458, Poor Pigs.
    static int poorPigs(int buckets, int minutesToDie, int minutesToTest) {
        int states = minutesToTest / minutesToDie + 1;    // dies after round 1, 2, ..., or survives
        return minTests(buckets, states);
    }

    // Checks only: try every domino tiling of a board.
    static boolean canTile(boolean[][] free) {
        for (int r = 0; r < free.length; r++)
            for (int c = 0; c < free[0].length; c++) {
                if (!free[r][c]) continue;                // the first free square must be covered now
                free[r][c] = false;
                boolean ok = false;
                if (c + 1 < free[0].length && free[r][c + 1]) {
                    free[r][c + 1] = false;
                    ok = canTile(free);
                    free[r][c + 1] = true;
                }
                if (!ok && r + 1 < free.length && free[r + 1][c]) {
                    free[r + 1][c] = false;
                    ok = canTile(free);
                    free[r + 1][c] = true;
                }
                free[r][c] = true;
                return ok;
            }
        return true;
    }

    static boolean[][] board(int size, boolean removeCorners) {
        boolean[][] free = new boolean[size][size];
        for (boolean[] row : free) Arrays.fill(row, true);
        if (removeCorners) free[0][0] = free[size - 1][size - 1] = false;
        return free;
    }

    // Checks only: count domino tilings column by column; bit r of a mask = row r already covered.
    static long countTilings(int rows, int cols) {
        long[] ways = new long[1 << rows];
        ways[0] = 1;
        for (int c = 0; c < cols; c++) {
            long[] next = new long[1 << rows];
            for (int mask = 0; mask < (1 << rows); mask++)
                if (ways[mask] > 0) fillColumn(rows, 0, mask, 0, ways[mask], next);
            ways = next;
        }
        return ways[0];
    }

    static void fillColumn(int rows, int r, int mask, int nextMask, long count, long[] next) {
        if (r == rows) { next[nextMask] += count; return; }
        if ((mask >> r & 1) == 1) { fillColumn(rows, r + 1, mask, nextMask, count, next); return; }
        fillColumn(rows, r + 1, mask, nextMask | 1 << r, count, next);              // horizontal domino
        if (r + 1 < rows && (mask >> (r + 1) & 1) == 0) fillColumn(rows, r + 2, mask, nextMask, count, next);
    }

    static boolean isPrime(int n) {
        if (n < 2) return false;
        for (int d = 2; d * d <= n; d++) if (n % d == 0) return false;
        return true;
    }

    static int divisorCount(int n) {
        int count = 0;
        for (int d = 1; d <= n; d++) if (n % d == 0) count++;
        return count;
    }

    static boolean nimWinsByBruteForce(int a, int b, int c) {
        for (int take = 1; take <= a; take++) if (!nimWinsByBruteForce(a - take, b, c)) return true;
        for (int take = 1; take <= b; take++) if (!nimWinsByBruteForce(a, b - take, c)) return true;
        for (int take = 1; take <= c; take++) if (!nimWinsByBruteForce(a, b, c - take)) return true;
        return false;
    }

    static boolean[] divisorGameTable(int max) {
        boolean[] win = new boolean[max + 1];
        for (int n = 2; n <= max; n++)
            for (int x = 1; x < n; x++)
                if (n % x == 0 && !win[n - x]) win[n] = true;
        return win;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        boolean[] win = nimTable(1000);
        check(win[1] && win[2] && win[3] && !win[4] && win[5] && win[6] && win[7] && !win[8], "W W W L W W W L");
        for (int n = 1; n <= 1000; n++) check(win[n] == canWinNim(n), "n mod 4 rule at " + n);
        check(canWinNim(Integer.MAX_VALUE) && Integer.MAX_VALUE % 4 == 3, "2^31 - 1 leaves remainder 3");

        check(colourImbalance(8) == 2 && 64 - 2 == 62 && 62 / 2 == 31, "30 dark and 32 light squares");
        check(!canTile(board(4, true)) && canTile(board(4, false)), "4 x 4: no tiling once corners are removed");
        check(colourImbalance(4) == 2, "4 x 4 imbalance");
        check(countTilings(8, 8) == 12_988_816L && countTilings(2, 2) == 2 && countTilings(4, 4) == 36, "tilings");

        for (int n = 0; n < 40; n++) check(isPrime(n * n + n + 41), "Euler's polynomial is prime at " + n);
        check(40 * 40 + 40 + 41 == 1681 && 41 * 41 == 1681 && !isPrime(1681), "but not at n = 40");
        check(divisorCount(12) == 6 && divisorCount(16) == 5 && divisorCount(36) == 9, "divisor counts");
        check(openAfterToggling(100) == 10, "10 lockers open out of 100");
        int toggles = 0;
        for (int r = 1; r <= 100; r++) toggles += 100 / r;
        check(toggles == 482, "482 toggles for 100 lockers");
        for (int n = 0; n <= 300; n++) check(openAfterToggling(n) == bulbSwitch(n), "squares rule at " + n);
        check(bulbSwitch(3) == 1 && bulbSwitch(99) == 9 && bulbSwitch(1_000_000_000) == 31_622, "LC 319");
        check(31_622L * 31_622 <= 1_000_000_000L && 31_623L * 31_623 > 1_000_000_000L, "floor sqrt of 10^9");

        check(minTests(100, 2) == 7 && (1 << 6) == 64 && (1 << 7) == 128, "1 to 100 needs 7 yes/no questions");
        check(minTests(24, 3) == 3 && 3 * 3 == 9 && 3 * 3 * 3 == 27, "12 coins: 24 cases, 3 weighings");
        check(minTests(1, 2) == 0, "one case needs no test");

        check(firstPlayerWinsNim(new int[] {3, 4, 5}) && (3 ^ 4 ^ 5) == 2, "(3, 4, 5) has XOR 2");
        check(!firstPlayerWinsNim(new int[] {1, 4, 5}) && (3 ^ 2) == 1, "take 2 from the 3-pile: (1, 4, 5)");
        for (int a = 0; a <= 5; a++)
            for (int b = 0; b <= 5; b++)
                for (int c = 0; c <= 5; c++)
                    check(nimWinsByBruteForce(a, b, c) == firstPlayerWinsNim(new int[] {a, b, c}), "Bouton");

        boolean[] divisor = divisorGameTable(1000);
        for (int n = 1; n <= 1000; n++) check(divisor[n] == divisorGame(n), "parity rule at " + n);
        check(divisorGame(2) && !divisorGame(3) && !divisorGame(1), "LC 1025 examples");

        check(poorPigs(4, 15, 15) == 2 && poorPigs(4, 15, 30) == 2, "LC 458 examples");
        check(poorPigs(1000, 15, 60) == 5 && 5 * 5 * 5 * 5 == 625 && 5 * 5 * 5 * 5 * 5 == 3125, "1000 buckets, 4 rounds");
        check(poorPigs(1, 1, 1) == 0, "one bucket needs no pig");
        check(Integer.toString(738, 5).equals("10423") && 625 + 4 * 25 + 2 * 5 + 3 == 738, "bucket 738 in base 5");
        check(poorPigs(1000, 15, 15) == 10 && (1 << 9) == 512 && (1 << 10) == 1024, "one round: binary labels");
        System.out.println("OK");
    }
}
