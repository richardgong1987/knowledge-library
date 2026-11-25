import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.UnaryOperator;

// Run from this folder with: java SimulationLab.java
public class SimulationLab {

    // Level 1: the state is (x, y, direction); each command changes it by a fixed rule.
    static int[] walk(String commands) {                 // returns {x, y, direction}
        int[] dx = {0, 1, 0, -1}, dy = {1, 0, -1, 0};    // north, east, south, west
        int x = 0, y = 0, d = 0;
        for (char c : commands.toCharArray()) {
            if (c == 'G') { x += dx[d]; y += dy[d]; }
            else if (c == 'R') d = (d + 1) % 4;
            else d = (d + 3) % 4;                        // left = three rights; avoids a negative index
        }
        return new int[] {x, y, d};
    }

    // Level 2: every cell reads yesterday's row, so the new row is written somewhere else.
    static String nextDay(String cells) {                // e.g. "01011001", one character per cell
        StringBuilder next = new StringBuilder("0");     // the first cell has only one neighbour
        for (int i = 1; i + 1 < cells.length(); i++)
            next.append(cells.charAt(i - 1) == cells.charAt(i + 1) ? '1' : '0');
        return next.append('0').toString();              // and so does the last
    }

    // Level 3: remember when each state was first seen; on a repeat, skip the whole laps.
    static <S> S stateAfter(S start, long steps, UnaryOperator<S> rule) {
        Map<S, Integer> firstSeen = new HashMap<>();
        List<S> history = new ArrayList<>();
        S state = start;
        for (int t = 0; t < steps; t++) {
            Integer mu = firstSeen.putIfAbsent(state, t);
            if (mu != null) return history.get(mu + (int) ((steps - mu) % (t - mu)));   // t - mu is the period
            history.add(state);
            state = rule.apply(state);
        }
        return state;
    }

    // Level 4: jump from event to event instead of ticking every minute.
    static int[] finishTimes(int[] arrive, int[] service, int tills) {
        PriorityQueue<Integer> freeAt = new PriorityQueue<>();   // when each till is next free
        for (int t = 0; t < tills; t++) freeAt.add(0);
        int[] finish = new int[arrive.length];
        for (int i = 0; i < arrive.length; i++) {               // customers in arrival order
            int start = Math.max(arrive[i], freeAt.poll());       // wait for the earliest free till
            finish[i] = start + service[i];
            freeAt.add(finish[i]);
        }
        return finish;
    }

    // Interview 1: LeetCode 289, Game of Life, in place.
    static void gameOfLife(int[][] board) {
        int rows = board.length, cols = board[0].length;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int live = liveNeighbours(board, r, c);
                boolean isAlive = (board[r][c] & 1) == 1;
                if (live == 3 || (isAlive && live == 2)) board[r][c] |= 2;   // bit 1 = alive tomorrow
            }
        }
        for (int[] row : board)
            for (int c = 0; c < cols; c++) row[c] >>= 1;                    // tomorrow becomes today
    }

    static int liveNeighbours(int[][] board, int r, int c) {   // reads bit 0 only: today's state
        int live = 0;
        for (int i = Math.max(0, r - 1); i <= Math.min(board.length - 1, r + 1); i++)
            for (int j = Math.max(0, c - 1); j <= Math.min(board[0].length - 1, c + 1); j++)
                if (i != r || j != c) live += board[i][j] & 1;
        return live;
    }

    // Interview 2: LeetCode 957, Prison Cells After N Days.
    static int[] prisonAfterNDays(int[] cells, int n) {
        StringBuilder start = new StringBuilder();
        for (int cell : cells) start.append(cell);
        String end = stateAfter(start.toString(), n, SimulationLab::nextDay);
        return end.chars().map(ch -> ch - '0').toArray();
    }

    // Interview 3: LeetCode 68, Text Justification.
    static List<String> fullJustify(String[] words, int maxWidth) {
        List<String> lines = new ArrayList<>();
        for (int i = 0, j; i < words.length; i = j) {
            int letters = words[i].length();
            for (j = i + 1; j < words.length && letters + words[j].length() + (j - i) <= maxWidth; j++)
                letters += words[j].length();                     // j - i single spaces are needed so far
            int gaps = j - i - 1, spaces = maxWidth - letters;
            boolean isLastLine = j == words.length;
            StringBuilder line = new StringBuilder(words[i]);
            for (int g = 0; g < gaps; g++) {
                int width = isLastLine ? 1 : spaces / gaps + (g < spaces % gaps ? 1 : 0);   // left gaps get the extra
                line.append(" ".repeat(width)).append(words[i + 1 + g]);
            }
            lines.add(line + " ".repeat(maxWidth - line.length()));   // pads the last line and one-word lines
        }
        return lines;
    }

    // Checks only: where the cycle starts and how long it is.
    static <S> int[] tailAndPeriod(S start, UnaryOperator<S> rule) {
        Map<S, Integer> firstSeen = new HashMap<>();
        S state = start;
        for (int t = 0; ; t++) {
            Integer mu = firstSeen.putIfAbsent(state, t);
            if (mu != null) return new int[] {mu, t - mu};
            state = rule.apply(state);
        }
    }

    static <S> S stepByStep(S start, long steps, UnaryOperator<S> rule) {
        S state = start;
        for (long t = 0; t < steps; t++) state = rule.apply(state);
        return state;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        check(Arrays.equals(walk("GGLLGG"), new int[] {0, 0, 2}), "GGLLGG returns home facing south");
        check(Arrays.equals(walk("GGRG"), new int[] {1, 2, 1}), "GGRG ends at (1, 2) facing east");
        check(Arrays.equals(walk("L"), new int[] {0, 0, 3}) && (0 + 3) % 4 == 3, "left from north is west");

        check(nextDay("01011001").equals("01100000"), "prison day 1");
        String day = "01011001";
        for (int d = 0; d < 7; d++) day = nextDay(day);
        check(day.equals("00110000"), "prison day 7");

        UnaryOperator<Integer> squarePlusOne = x -> (x * x + 1) % 11;
        check(stepByStep(0, 6, squarePlusOne) == 4 && stepByStep(0, 5, squarePlusOne) == 6, "0 1 2 5 4 6 4 ...");
        check(Arrays.equals(tailAndPeriod(0, squarePlusOne), new int[] {4, 2}), "tail 4, period 2");
        check(stateAfter(0, 1_000_000_000L, squarePlusOne) == 4, "s(10^9) = s(4) = 4");
        for (int t = 0; t < 50; t++) {
            check(stateAfter(0, t, squarePlusOne).equals(stepByStep(0, t, squarePlusOne)), "cycle skip at " + t);
        }

        check(Arrays.equals(finishTimes(new int[] {0, 1, 2, 3}, new int[] {5, 2, 4, 1}, 2), new int[] {5, 3, 7, 6}),
                "two tills");
        check(Arrays.equals(finishTimes(new int[] {0, 0}, new int[] {3, 3}, 1), new int[] {3, 6}), "one till queues");

        int[][] board = {{0, 1, 0}, {0, 0, 1}, {1, 1, 1}, {0, 0, 0}};
        check(liveNeighbours(board, 1, 0) == 3 && liveNeighbours(board, 0, 1) == 1, "neighbour counts");
        gameOfLife(board);
        check(Arrays.deepEquals(board, new int[][] {{0, 0, 0}, {1, 0, 1}, {0, 1, 1}, {0, 1, 0}}), "LC 289 example 1");
        int[][] block = {{1, 1}, {1, 0}};
        gameOfLife(block);
        check(Arrays.deepEquals(block, new int[][] {{1, 1}, {1, 1}}), "LC 289 example 2");

        check(Arrays.equals(prisonAfterNDays(new int[] {0, 1, 0, 1, 1, 0, 0, 1}, 7), new int[] {0, 0, 1, 1, 0, 0, 0, 0}),
                "LC 957 example 1");
        check(Arrays.equals(prisonAfterNDays(new int[] {1, 0, 0, 1, 0, 0, 1, 0}, 1_000_000_000),
                new int[] {0, 0, 1, 1, 1, 1, 1, 0}), "LC 957 example 2");
        check(Arrays.equals(tailAndPeriod("10010010", SimulationLab::nextDay), new int[] {1, 14}), "tail 1, period 14");
        check((1_000_000_000L - 1) % 14 == 5, "(10^9 - 1) mod 14 = 5");
        check(stepByStep("10010010", 6, SimulationLab::nextDay).equals("00111110"), "day 6 is the answer");
        for (int t = 0; t < 60; t++) {
            check(stateAfter("01011001", t, SimulationLab::nextDay).equals(stepByStep("01011001", t, SimulationLab::nextDay)),
                    "prison cycle skip at " + t);
        }

        check(fullJustify(new String[] {"This", "is", "an", "example", "of", "text", "justification."}, 16)
                .equals(List.of("This    is    an", "example  of text", "justification.  ")), "LC 68 example 1");
        check(fullJustify(new String[] {"What", "must", "be", "acknowledgment", "shall", "be"}, 16)
                .equals(List.of("What   must   be", "acknowledgment  ", "shall be        ")), "LC 68 example 2");
        check(fullJustify(new String[] {"a"}, 1).equals(List.of("a")), "LC 68 one word");
        System.out.println("OK");
    }
}
