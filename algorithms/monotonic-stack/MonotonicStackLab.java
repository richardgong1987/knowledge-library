import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

// Run from this folder with: java MonotonicStackLab.java
public class MonotonicStackLab {

    // Level 1: for each index, scan to the right until something is bigger.
    static int[] nextGreaterBrute(int[] a) {
        int[] next = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            next[i] = -1;
            for (int j = i + 1; j < a.length; j++) {
                if (a[j] > a[i]) { next[i] = j; break; }
            }
        }
        return next;
    }

    // Level 2: one left-to-right pass with a stack of indices still waiting for an answer.
    static int[] nextGreaterIndex(int[] a) {
        int[] next = new int[a.length];
        Arrays.fill(next, -1);                            // -1 = no greater value to the right
        Deque<Integer> waiting = new ArrayDeque<>();      // indices; their values decrease from bottom to top
        for (int j = 0; j < a.length; j++) {
            while (!waiting.isEmpty() && a[waiting.peek()] < a[j]) {
                next[waiting.pop()] = j;                  // a[j] is the first greater value for that index
            }
            waiting.push(j);
        }
        return next;
    }

    // Level 4: the nearest strictly smaller index on each side; the answer is read off the stack top.
    static int[] previousSmallerIndex(int[] a) {
        int[] left = new int[a.length];
        Deque<Integer> stack = new ArrayDeque<>();        // values increase from bottom to top
        for (int i = 0; i < a.length; i++) {
            while (!stack.isEmpty() && a[stack.peek()] >= a[i]) stack.pop();
            left[i] = stack.isEmpty() ? -1 : stack.peek();
            stack.push(i);
        }
        return left;
    }

    static int[] nextSmallerIndex(int[] a) {
        int[] right = new int[a.length];
        Deque<Integer> stack = new ArrayDeque<>();
        for (int i = a.length - 1; i >= 0; i--) {
            while (!stack.isEmpty() && a[stack.peek()] >= a[i]) stack.pop();
            right[i] = stack.isEmpty() ? a.length : stack.peek();
            stack.push(i);
        }
        return right;
    }

    // Level 5: a circular array (LeetCode 503). Walk it twice so the end can see the start.
    static int[] nextGreaterCircular(int[] a) {
        int n = a.length;
        int[] next = new int[n];
        Arrays.fill(next, -1);
        Deque<Integer> waiting = new ArrayDeque<>();
        for (int j = 0; j < 2 * n; j++) {
            while (!waiting.isEmpty() && a[waiting.peek()] < a[j % n]) next[waiting.pop()] = a[j % n];
            if (j < n) waiting.push(j);                   // the second lap only answers, it never waits
        }
        return next;
    }

    // Interview 1: LeetCode 496, Next Greater Element I.
    static int[] nextGreaterElement(int[] nums1, int[] nums2) {
        int[] next = nextGreaterIndex(nums2);
        Map<Integer, Integer> answerFor = new HashMap<>();
        for (int i = 0; i < nums2.length; i++) answerFor.put(nums2[i], next[i] == -1 ? -1 : nums2[next[i]]);
        int[] answer = new int[nums1.length];
        for (int i = 0; i < nums1.length; i++) answer[i] = answerFor.get(nums1[i]);
        return answer;
    }

    // Interview 2: LeetCode 739, Daily Temperatures.
    static int[] dailyTemperatures(int[] temperatures) {
        int[] next = nextGreaterIndex(temperatures);
        int[] wait = new int[temperatures.length];
        for (int i = 0; i < wait.length; i++) wait[i] = next[i] == -1 ? 0 : next[i] - i;
        return wait;
    }

    // Interview 3: LeetCode 84, Largest Rectangle in Histogram.
    static int largestRectangleArea(int[] heights) {
        int[] left = previousSmallerIndex(heights), right = nextSmallerIndex(heights);
        int best = 0;
        for (int i = 0; i < heights.length; i++) {
            best = Math.max(best, heights[i] * (right[i] - left[i] - 1)); // bar i is the shortest in its widest rectangle
        }
        return best;
    }

    // Interview 4: LeetCode 85, Maximal Rectangle.
    static int maximalRectangle(char[][] matrix) {
        int[] heights = new int[matrix[0].length];        // heights[c] = run of '1's ending at this row in column c
        int best = 0;
        for (char[] row : matrix) {
            for (int c = 0; c < row.length; c++) heights[c] = row[c] == '1' ? heights[c] + 1 : 0;
            best = Math.max(best, largestRectangleArea(heights));
        }
        return best;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] a = {2, 1, 5, 6, 2, 3};

        int[] next = nextGreaterIndex(a);
        check(Arrays.equals(next, new int[] {2, 2, 3, -1, 5, -1}), "next greater indices");
        check(Arrays.equals(next, nextGreaterBrute(a)), "stack agrees with brute force");
        long pops = Arrays.stream(next).filter(j -> j != -1).count();
        check(a.length + pops == 10 && 10 <= 2 * a.length, "6 pushes + 4 pops = 10 <= 12");
        check(a.length * (a.length - 1) / 2 == 15, "brute force compares up to 15 pairs");

        check(Arrays.equals(previousSmallerIndex(a), new int[] {-1, -1, 1, 2, 1, 4}), "previous smaller");
        check(Arrays.equals(nextSmallerIndex(a), new int[] {1, 6, 4, 4, 6, 6}), "next smaller");
        check(Arrays.equals(nextGreaterIndex(new int[] {2, 2}), new int[] {-1, -1}), "equal values are not greater");
        check(Arrays.equals(nextGreaterCircular(new int[] {1, 2, 1}), new int[] {2, -1, 2}), "circular");
        check(Arrays.equals(nextGreaterCircular(new int[] {5, 4, 3}), new int[] {-1, 5, 5}), "circular wraps");

        check(Arrays.equals(nextGreaterElement(new int[] {4, 1, 2}, new int[] {1, 3, 4, 2}), new int[] {-1, 3, -1}), "LC 496");
        check(Arrays.equals(nextGreaterElement(new int[] {2, 4}, new int[] {1, 2, 3, 4}), new int[] {3, -1}), "LC 496 example 2");
        check(Arrays.equals(dailyTemperatures(new int[] {73, 74, 75, 71, 69, 72, 76, 73}),
                new int[] {1, 1, 4, 2, 1, 1, 0, 0}), "LC 739");
        check(Arrays.equals(nextGreaterIndex(new int[] {1, 3, 4, 2}), new int[] {1, 2, -1, -1}), "LC 496 next on nums2");
        check(Arrays.equals(nextGreaterIndex(new int[] {6, 5, 4, 3, 2, 1}), new int[] {-1, -1, -1, -1, -1, -1}), "falling array");
        check(Arrays.equals(dailyTemperatures(new int[] {30, 40, 50, 60}), new int[] {1, 1, 1, 0}), "LC 739 rising");

        check(largestRectangleArea(a) == 10, "LC 84");
        check(5 * (4 - 1 - 1) == 10 && 2 * (6 - 1 - 1) == 8 && 1 * (6 - (-1) - 1) == 6, "LC 84 areas per bar");
        check(largestRectangleArea(new int[] {2, 2}) == 4, "LC 84 equal bars");
        check(largestRectangleArea(new int[] {7}) == 7, "LC 84 one bar");

        char[][] matrix = {
            "10100".toCharArray(),
            "10111".toCharArray(),
            "11111".toCharArray(),
            "10010".toCharArray(),
        };
        check(maximalRectangle(matrix) == 6, "LC 85");
        check(largestRectangleArea(new int[] {1, 0, 1, 0, 0}) == 1 && largestRectangleArea(new int[] {2, 0, 2, 1, 1}) == 3
                && largestRectangleArea(new int[] {4, 0, 0, 3, 0}) == 4, "LC 85 other rows");
        check(largestRectangleArea(new int[] {3, 1, 3, 2, 2}) == 6, "LC 85 third-row histogram");
        check(maximalRectangle(new char[][] {"0".toCharArray()}) == 0, "LC 85 single zero");
        System.out.println("OK");
    }
}
