import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

// Run from this folder with: java ArrayLab.java
public class ArrayLab {

    // Level 1: where element i lives, counted in bytes from the start of the array.
    static long address(long base, int index, int elementSize) {
        return base + (long) index * elementSize;
    }

    // Level 2: make a gap at index by moving every later element one place right.
    static int insertAt(int[] a, int size, int index, int value) {
        int shifts = 0;
        for (int i = size; i > index; i--) {   // right to left, so nothing is overwritten
            a[i] = a[i - 1];
            shifts++;
        }
        a[index] = value;
        return shifts;
    }

    static int removeAt(int[] a, int size, int index) {
        int shifts = 0;
        for (int i = index; i < size - 1; i++) {
            a[i] = a[i + 1];
            shifts++;
        }
        return shifts;
    }

    // Level 3: a growable array of ints that grows by half, like java.util.ArrayList.
    static final class IntList {
        int[] items = new int[10];
        int size;
        int copies;                            // elements copied by all resizes so far

        void add(int value) {
            if (size == items.length) {
                items = Arrays.copyOf(items, size + (size >> 1));
                copies += size;
            }
            items[size++] = value;
        }

        int get(int index) {
            Objects.checkIndex(index, size);   // the array may be longer than size
            return items[index];
        }
    }

    // Level 4: delete every zero with one pass instead of one shift per zero.
    static int removeZeros(int[] a) {
        int write = 0;
        for (int read = 0; read < a.length; read++) {
            if (a[read] != 0) a[write++] = a[read];
        }
        return write;                          // the new length: a[0..write) holds the survivors
    }

    // Interview 1: LeetCode 448, Find All Numbers Disappeared in an Array.
    static List<Integer> findDisappearedNumbers(int[] nums) {
        for (int v : nums) {
            int slot = Math.abs(v) - 1;        // value v is recorded at index v - 1
            if (nums[slot] > 0) nums[slot] = -nums[slot];
        }
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] > 0) missing.add(i + 1);
        }
        return missing;
    }

    // Interview 2: LeetCode 238, Product of Array Except Self.
    static int[] productExceptSelf(int[] nums) {
        int n = nums.length;
        int[] answer = new int[n];
        int left = 1;
        for (int i = 0; i < n; i++) {          // answer[i] = product of everything left of i
            answer[i] = left;
            left *= nums[i];
        }
        int right = 1;
        for (int i = n - 1; i >= 0; i--) {     // times the product of everything right of i
            answer[i] *= right;
            right *= nums[i];
        }
        return answer;
    }

    // Interview 3: LeetCode 41, First Missing Positive.
    static int firstMissingPositive(int[] nums) {
        placeValuesHome(nums);
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] != i + 1) return i + 1;
        }
        return nums.length + 1;
    }

    static int placeValuesHome(int[] nums) {   // returns the number of swaps
        int swaps = 0;
        for (int i = 0; i < nums.length; i++) {
            while (nums[i] >= 1 && nums[i] <= nums.length && nums[nums[i] - 1] != nums[i]) {
                int home = nums[i] - 1;        // value v belongs at index v - 1
                int moved = nums[home];
                nums[home] = nums[i];
                nums[i] = moved;
                swaps++;
            }
        }
        return swaps;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        check(address(1000, 3, 4) == 1012 && address(1000, 3, 8) == 1024, "base + i * size");
        check(address(1000, 0, 4) == 1000, "index 0 is the base");

        int[] scores = {72, 85, 90, 64, 78, 95, 0};
        check(insertAt(scores, 6, 2, 80) == 4, "inserting at 2 of 6 shifts 4");
        check(Arrays.equals(scores, new int[] {72, 85, 80, 90, 64, 78, 95}), "80 lands at index 2");
        int totalShifts = 0;
        for (int k = 0; k <= 6; k++) totalShifts += insertAt(new int[7], 6, k, 1);
        check(totalShifts == 21, "inserting at k = 0..6 shifts 21 in total, 3 on average");
        check(removeAt(scores, 7, 2) == 4, "removing index 2 of 7 shifts 4");

        IntList list = new IntList();
        for (int i = 0; i < 50; i++) list.add(i);
        check(list.items.length == 73 && list.copies == 129, "10, 15, 22, 33, 49, 73: 129 copies for 50 adds");
        check(list.get(49) == 49, "get reads by index");
        try {
            list.get(50);
            check(false, "index 50 is past size");
        } catch (IndexOutOfBoundsException expected) {
        }
        IntList big = new IntList();
        for (int i = 0; i < 1000; i++) big.add(i);
        check(big.copies == 2456, "1000 adds copy 2456 elements");
        for (int n = 1; n <= 3000; n++) {
            IntList grow = new IntList();
            for (int i = 0; i < n; i++) grow.add(i);
            check(grow.copies <= 3 * n, "copies stay within 3n for n = " + n);
        }
        int fixedGrowthCopies = 0;
        for (int capacity = 10; capacity < 1000; capacity += 10) fixedGrowthCopies += capacity;
        check(fixedGrowthCopies == 49_500, "growing by a fixed 10 copies 49,500 for 1000 adds");

        int[] withZeros = {0, 1, 0, 3, 12};
        check(removeZeros(withZeros) == 3 && withZeros[0] == 1 && withZeros[1] == 3 && withZeros[2] == 12,
                "compaction keeps 1, 3, 12 in order");
        int[] front = {0, 0, 0, 0, 5, 6, 7, 8};
        int size = front.length, oneByOne = 0;
        while (front[0] == 0) oneByOne += removeAt(front, size--, 0);
        check(oneByOne == 22 && 4 * 8 - 4 * 5 / 2 == 22, "four deletes at the front shift 7 + 6 + 5 + 4 = 22");

        check(findDisappearedNumbers(new int[] {4, 3, 2, 7, 8, 2, 3, 1}).equals(List.of(5, 6)), "LC 448");
        int[] marked = {4, 3, 2, 7, 8, 2, 3, 1};
        findDisappearedNumbers(marked);
        check(Arrays.equals(marked, new int[] {-4, -3, -2, -7, 8, 2, -3, -1}), "LC 448 sign marks");
        check(findDisappearedNumbers(new int[] {1, 1}).equals(List.of(2)), "LC 448 duplicates");

        check(Arrays.equals(productExceptSelf(new int[] {1, 2, 3, 4}), new int[] {24, 12, 8, 6}), "LC 238");
        check(Arrays.equals(productExceptSelf(new int[] {-1, 1, 0, -3, 3}), new int[] {0, 0, 9, 0, 0}), "LC 238 with a zero");

        check(firstMissingPositive(new int[] {3, 4, -1, 1}) == 2, "LC 41");
        int[] placed = {3, 4, -1, 1};
        check(placeValuesHome(placed) == 3 && Arrays.equals(placed, new int[] {1, -1, 3, 4}), "LC 41 three swaps");
        check(firstMissingPositive(new int[] {1, 2, 0}) == 3, "LC 41 answer n + 1 when 1..n present");
        check(firstMissingPositive(new int[] {7, 8, 9, 11, 12}) == 1, "LC 41 nothing in range");
        check(firstMissingPositive(new int[] {1, 1}) == 2, "LC 41 duplicates");
        System.out.println("OK");
    }
}
