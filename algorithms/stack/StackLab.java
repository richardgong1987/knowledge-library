import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

// Run from this folder with: java StackLab.java
public class StackLab {

    // Level 1: a stack is an array plus a count; the top is items[size - 1].
    static final class IntStack {
        int[] items = new int[4];
        int size;

        void push(int x) {
            if (size == items.length) items = Arrays.copyOf(items, 2 * size);  // amortised O(1)
            items[size++] = x;
        }

        int pop() {
            if (size == 0) throw new NoSuchElementException("stack is empty");
            return items[--size];
        }

        int peek() {
            if (size == 0) throw new NoSuchElementException("stack is empty");
            return items[size - 1];
        }
    }

    // Level 3: which pop orders can a stack produce from the pushes 1, 2, ..., n?
    static boolean canProduce(int[] popOrder) {
        Deque<Integer> stack = new ArrayDeque<>();
        int next = 1;
        for (int wanted : popOrder) {
            while (next <= wanted) stack.push(next++); // push until wanted is on the stack
            if (stack.pop() != wanted) return false;   // wanted is buried under a later number
        }
        return true;
    }

    // Level 4: brackets. The stack holds the closers we still owe.
    static boolean isValid(String s) {
        Deque<Character> owed = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            if (c == '(') owed.push(')');
            else if (c == '[') owed.push(']');
            else if (c == '{') owed.push('}');
            else if (owed.isEmpty() || owed.pop() != c) return false;
        }
        return owed.isEmpty();
    }

    // Level 5: postfix (reverse Polish) evaluation.
    static int evalRPN(String[] tokens) {
        Deque<Integer> stack = new ArrayDeque<>();
        for (String t : tokens) {
            switch (t) {
                case "+" -> stack.push(stack.pop() + stack.pop());
                case "*" -> stack.push(stack.pop() * stack.pop());
                case "-" -> {
                    int right = stack.pop(), left = stack.pop();   // popped in reverse order
                    stack.push(left - right);
                }
                case "/" -> {
                    int right = stack.pop(), left = stack.pop();
                    stack.push(left / right);
                }
                default -> stack.push(Integer.parseInt(t));
            }
        }
        return stack.pop();
    }

    // Interview 1: LeetCode 155, Min Stack.
    static final class MinStack {
        final Deque<int[]> stack = new ArrayDeque<>();   // {value, smallest value at or below it}

        void push(int x) {
            int min = stack.isEmpty() ? x : Math.min(x, stack.peek()[1]);
            stack.push(new int[] {x, min});
        }

        void pop() {
            stack.pop();
        }

        int top() {
            return stack.peek()[0];
        }

        int getMin() {
            return stack.peek()[1];
        }
    }

    // Interview 2: LeetCode 735, Asteroid Collision.
    static int[] asteroidCollision(int[] asteroids) {
        Deque<Integer> survivors = new ArrayDeque<>();
        for (int a : asteroids) {
            boolean alive = true;
            while (alive && a < 0 && !survivors.isEmpty() && survivors.peek() > 0) {
                int top = survivors.peek();
                if (top <= -a) survivors.pop();        // the right-mover explodes
                if (top >= -a) alive = false;          // a explodes (both do when equal)
            }
            if (alive) survivors.push(a);
        }
        int[] out = new int[survivors.size()];
        for (int i = out.length - 1; i >= 0; i--) out[i] = survivors.pop();
        return out;
    }

    // Interview 3: LeetCode 224, Basic Calculator.
    static int calculate(String s) {
        Deque<Integer> saved = new ArrayDeque<>();     // for each open '(': the result and sign before it
        int result = 0, sign = 1, number = 0;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                number = number * 10 + (c - '0');
            } else if (c == '+' || c == '-') {
                result += sign * number;
                number = 0;
                sign = (c == '+') ? 1 : -1;
            } else if (c == '(') {
                saved.push(result);
                saved.push(sign);
                result = 0;
                sign = 1;
            } else if (c == ')') {
                result += sign * number;
                number = 0;
                result = saved.pop() * result + saved.pop();   // sign before '(' times inside, plus outside
            }
        }
        return result + sign * number;
    }

    static int countProducible(int n) {
        return countProducible(new int[n], new boolean[n + 1], 0);
    }

    static int countProducible(int[] order, boolean[] used, int filled) {   // try every permutation
        if (filled == order.length) return canProduce(order) ? 1 : 0;
        int count = 0;
        for (int v = 1; v < used.length; v++) {
            if (used[v]) continue;
            used[v] = true;
            order[filled] = v;
            count += countProducible(order, used, filled + 1);
            used[v] = false;
        }
        return count;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        IntStack s = new IntStack();
        s.push(1);
        s.push(2);
        s.push(3);
        check(s.pop() == 3, "last in, first out");
        s.push(4);
        check(s.pop() == 4 && s.pop() == 2 && s.peek() == 1 && s.size == 1, "pops 3, 4, 2; 1 is left");
        for (int i = 0; i < 10; i++) s.push(i);
        check(s.items.length == 16 && s.size == 11, "grows 4 -> 8 -> 16");
        try {
            new IntStack().pop();
            check(false, "pop on empty must throw");
        } catch (NoSuchElementException expected) {
        }

        Deque<Integer> deque = new ArrayDeque<>();
        Stack<Integer> legacy = new Stack<>();
        for (int x = 1; x <= 3; x++) {
            deque.push(x);
            legacy.push(x);
        }
        check(new ArrayList<>(deque).equals(List.of(3, 2, 1)), "ArrayDeque iterates from the top");
        check(new ArrayList<>(legacy).equals(List.of(1, 2, 3)), "java.util.Stack iterates from the bottom");
        check(deque.peekFirst() == 3 && deque.pollFirst() == 3 && deque.pop() == 2, "push/pop are addFirst/removeFirst");

        check(canProduce(new int[] {1, 2, 3}) && canProduce(new int[] {1, 3, 2}) && canProduce(new int[] {2, 1, 3})
                && canProduce(new int[] {2, 3, 1}) && canProduce(new int[] {3, 2, 1}), "five possible orders");
        check(!canProduce(new int[] {3, 1, 2}), "3, 1, 2 is impossible");
        check(countProducible(3) == 5 && countProducible(4) == 14 && countProducible(5) == 42, "Catalan 5, 14, 42");

        check(isValid("([]{})") && isValid("()") && isValid(""), "balanced");
        check(!isValid("([)]") && !isValid("((") && !isValid(")"), "unbalanced");

        check(evalRPN(new String[] {"2", "1", "+", "3", "*"}) == 9, "(2 + 1) * 3");
        check(evalRPN(new String[] {"4", "13", "5", "/", "+"}) == 6, "4 + 13 / 5");
        check(evalRPN(new String[] {"10", "3", "-"}) == 7, "order of operands for minus");

        MinStack min = new MinStack();
        min.push(-2);
        min.push(0);
        min.push(-3);
        check(min.getMin() == -3, "LC 155 min of -2, 0, -3");
        min.pop();
        check(min.top() == 0 && min.getMin() == -2, "LC 155 after pop");
        MinStack repeats = new MinStack();
        repeats.push(1);
        repeats.push(1);
        repeats.pop();
        check(repeats.getMin() == 1, "LC 155 duplicate minimum");

        check(Arrays.equals(asteroidCollision(new int[] {5, 10, -5}), new int[] {5, 10}), "LC 735");
        check(asteroidCollision(new int[] {8, -8}).length == 0, "LC 735 equal sizes");
        check(Arrays.equals(asteroidCollision(new int[] {10, 2, -5}), new int[] {10}), "LC 735 chain");
        check(Arrays.equals(asteroidCollision(new int[] {-2, -1, 1, 2}), new int[] {-2, -1, 1, 2}), "LC 735 moving apart");

        check(calculate("1 + 1") == 2 && calculate(" 2-1 + 2 ") == 3, "LC 224 simple");
        check(calculate("(1+(4+5+2)-3)+(6+8)") == 23, "LC 224 nested");
        check(calculate("1 - (2 - 3)") == 2, "LC 224 minus before a parenthesis");
        check(calculate("-(2+3)") == -5 && calculate("2147483647") == Integer.MAX_VALUE, "LC 224 unary minus, big number");
        System.out.println("OK");
    }
}
