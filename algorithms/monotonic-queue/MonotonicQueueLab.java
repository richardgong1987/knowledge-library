import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

// Run from this folder with: java MonotonicQueueLab.java
public class MonotonicQueueLab {

    // Level 1: look at every element of every window.
    static int[] windowMaxBrute(int[] a, int k) {
        int[] result = new int[a.length - k + 1];
        for (int start = 0; start + k <= a.length; start++) {
            int max = a[start];
            for (int i = start + 1; i < start + k; i++) max = Math.max(max, a[i]);
            result[start] = max;
        }
        return result;
    }

    // Level 2: a deque of indices whose values decrease from front to back; the front is the maximum.
    static int[] maxSlidingWindow(int[] a, int k) {
        int[] result = new int[a.length - k + 1];
        Deque<Integer> deque = new ArrayDeque<>();
        for (int i = 0; i < a.length; i++) {
            if (!deque.isEmpty() && deque.peekFirst() <= i - k) deque.pollFirst();       // front left the window
            while (!deque.isEmpty() && a[deque.peekLast()] <= a[i]) deque.pollLast();   // dominated by a[i]
            deque.addLast(i);
            if (i >= k - 1) result[i - k + 1] = a[deque.peekFirst()];
        }
        return result;
    }

    // Level 4: the deque inside a dynamic program (LeetCode 1696, Jump Game VI).
    static int maxJumpScore(int[] nums, int k) {
        int[] best = new int[nums.length];                 // best[i] = top score of a path that ends on i
        Deque<Integer> deque = new ArrayDeque<>();         // indices; best[] decreases from front to back
        for (int i = 0; i < nums.length; i++) {
            if (!deque.isEmpty() && deque.peekFirst() < i - k) deque.pollFirst();
            best[i] = nums[i] + (deque.isEmpty() ? 0 : best[deque.peekFirst()]);
            while (!deque.isEmpty() && best[deque.peekLast()] <= best[i]) deque.pollLast();
            deque.addLast(i);
        }
        return best[nums.length - 1];
    }

    // Interview 1: LeetCode 1438, Longest Continuous Subarray With Absolute Diff <= Limit.
    static int longestSubarray(int[] nums, int limit) {
        Deque<Integer> maxQ = new ArrayDeque<>(), minQ = new ArrayDeque<>();   // indices
        int left = 0, best = 0;
        for (int right = 0; right < nums.length; right++) {
            while (!maxQ.isEmpty() && nums[maxQ.peekLast()] <= nums[right]) maxQ.pollLast();
            while (!minQ.isEmpty() && nums[minQ.peekLast()] >= nums[right]) minQ.pollLast();
            maxQ.addLast(right);
            minQ.addLast(right);
            while (nums[maxQ.peekFirst()] - nums[minQ.peekFirst()] > limit) {
                left++;                                    // shrink from the left until max - min <= limit
                if (maxQ.peekFirst() < left) maxQ.pollFirst();
                if (minQ.peekFirst() < left) minQ.pollFirst();
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    // Interview 3: LeetCode 1499, Max Value of Equation.
    static int findMaxValueOfEquation(int[][] points, int k) {
        Deque<int[]> deque = new ArrayDeque<>();           // pairs {x, y - x}; y - x decreases from front to back
        int best = Integer.MIN_VALUE;
        for (int[] p : points) {
            int x = p[0], y = p[1];
            while (!deque.isEmpty() && x - deque.peekFirst()[0] > k) deque.pollFirst();
            if (!deque.isEmpty()) best = Math.max(best, x + y + deque.peekFirst()[1]);
            while (!deque.isEmpty() && deque.peekLast()[1] <= y - x) deque.pollLast();
            deque.addLast(new int[] {x, y - x});
        }
        return best;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        int[] a = {2, 7, 3, 1, 5, 4};

        check(Arrays.equals(windowMaxBrute(a, 3), new int[] {7, 7, 5, 5}), "brute force window maxima");
        check(Arrays.equals(maxSlidingWindow(a, 3), new int[] {7, 7, 5, 5}), "deque window maxima");
        check((a.length - 3 + 1) * (3 - 1) == 8, "brute force: 4 windows x 2 comparisons");
        check(Arrays.equals(maxSlidingWindow(a, 1), a), "k = 1 returns the array");
        check(Arrays.equals(maxSlidingWindow(a, 6), new int[] {7}), "k = n returns the overall max");
        check(Arrays.equals(maxSlidingWindow(new int[] {4, 4, 4}, 2), new int[] {4, 4}), "equal values");

        check(maxJumpScore(new int[] {1, -1, -2, 4, -7, 3}, 2) == 7, "LC 1696 jump game");
        check(maxJumpScore(new int[] {10, -5, -2, 4, 0, 3}, 3) == 17, "LC 1696 example 2");

        check(longestSubarray(new int[] {8, 2, 4, 7}, 4) == 2, "LC 1438");
        check(longestSubarray(new int[] {10, 1, 2, 4, 7, 2}, 5) == 4, "LC 1438 example 2");
        check(longestSubarray(new int[] {4, 2, 2, 2, 4, 4, 2, 2}, 0) == 3, "LC 1438 example 3");

        check(Arrays.equals(maxSlidingWindow(new int[] {1, 3, -1, -3, 5, 3, 6, 7}, 3), new int[] {3, 3, 5, 5, 6, 7}), "LC 239");
        check(Arrays.equals(maxSlidingWindow(new int[] {1}, 1), new int[] {1}), "LC 239 single");

        check(findMaxValueOfEquation(new int[][] {{1, 3}, {2, 0}, {5, 10}, {6, -10}}, 1) == 4, "LC 1499");
        check(findMaxValueOfEquation(new int[][] {{0, 0}, {3, 0}, {9, 2}}, 3) == 3, "LC 1499 example 2");
        check(3 - 1 + (0 + 2) == 4 && 10 - 5 + (-10 + 6) == 1, "LC 1499 split terms");
        System.out.println("OK");
    }
}
