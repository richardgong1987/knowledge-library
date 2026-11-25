import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Run from this folder with: java TrieLab.java
public class TrieLab {

    // Level 1: one node, with a door for each letter a to z.
    static final class Node {
        final Node[] children = new Node[26]; // children[0] is 'a', children[25] is 'z'
        String word;                          // the word that ends here, or null
        int passCount;                        // how many inserted words pass through here (Level 4)
    }

    static final class Trie {
        final Node root = new Node();

        // Level 2: insert walks one door per letter and builds the doors that are missing.
        void insert(String word) {
            Node node = root;
            node.passCount++;
            for (char c : word.toCharArray()) {
                int i = c - 'a';
                if (node.children[i] == null) node.children[i] = new Node();
                node = node.children[i];
                node.passCount++;
            }
            node.word = word;
        }

        // Level 3: follow a prefix letter by letter.
        Node walk(String prefix) {
            Node node = root;
            for (char c : prefix.toCharArray()) {
                node = node.children[c - 'a'];
                if (node == null) return null; // no stored word starts this way
            }
            return node;
        }

        // Interview 1: LeetCode 208, Implement Trie (Prefix Tree).
        boolean search(String word) {
            Node node = walk(word);
            return node != null && node.word != null;
        }

        boolean startsWith(String prefix) {
            return walk(prefix) != null;
        }

        // Level 4: how many inserted words begin with this prefix?
        int countWithPrefix(String prefix) {
            Node node = walk(prefix);
            return node == null ? 0 : node.passCount;
        }
    }

    // Level 5: count the nodes, the root included.
    static int countNodes(Node node) {
        int count = 1;
        for (Node child : node.children) {
            if (child != null) count += countNodes(child);
        }
        return count;
    }

    // Level 6: children in a hash map, for any alphabet.
    static final class MapNode {
        final Map<Character, MapNode> children = new HashMap<>();
        boolean isWord;
    }

    static void insert(MapNode root, String word) {
        MapNode node = root;
        for (char c : word.toCharArray()) node = node.children.computeIfAbsent(c, k -> new MapNode());
        node.isWord = true;
    }

    static boolean contains(MapNode root, String word) {
        MapNode node = root;
        for (char c : word.toCharArray()) {
            node = node.children.get(c);
            if (node == null) return false;
        }
        return node.isWord;
    }

    // Interview 2: LeetCode 211, Design Add and Search Words Data Structure. '.' matches any letter.
    static boolean matches(Node node, String pattern, int i) {
        if (i == pattern.length()) return node.word != null;
        char c = pattern.charAt(i);
        if (c != '.') {
            Node child = node.children[c - 'a'];
            return child != null && matches(child, pattern, i + 1);
        }
        for (Node child : node.children) {
            if (child != null && matches(child, pattern, i + 1)) return true;
        }
        return false;
    }

    // Interview 3: LeetCode 212, Word Search II.
    static List<String> findWords(char[][] board, String[] words) {
        Trie trie = new Trie();
        for (String w : words) trie.insert(w);
        List<String> found = new ArrayList<>();
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[0].length; c++) dfs(board, r, c, trie.root, found);
        }
        return found;
    }

    static void dfs(char[][] board, int r, int c, Node parent, List<String> found) {
        if (r < 0 || c < 0 || r >= board.length || c >= board[0].length) return;
        char letter = board[r][c];
        if (letter == '#' || parent.children[letter - 'a'] == null) return; // used cell, or no word goes this way
        Node node = parent.children[letter - 'a'];
        if (node.word != null) { found.add(node.word); node.word = null; } // report each word once
        board[r][c] = '#';                                                  // mark the cell as used on this path
        dfs(board, r + 1, c, node, found);
        dfs(board, r - 1, c, node, found);
        dfs(board, r, c + 1, node, found);
        dfs(board, r, c - 1, node, found);
        board[r][c] = letter;                                               // unmark when backtracking
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        Trie trie = new Trie();
        for (String w : new String[] {"car", "cat", "cart", "dog"}) trie.insert(w);

        check('c' - 'a' == 2 && 't' - 'a' == 19, "door numbers for c and t");
        check(trie.root.children[2].children[0].children[19].word.equals("cat"), "cat is root -> 2 -> 0 -> 19");
        check(trie.search("car") && trie.search("cart") && !trie.search("ca") && !trie.search("cow"), "search");
        check(trie.startsWith("ca") && trie.startsWith("do") && !trie.startsWith("cow"), "startsWith");
        check(trie.walk("cow") == null && trie.walk("") == trie.root, "walk");
        check(trie.countWithPrefix("ca") == 3 && trie.countWithPrefix("car") == 2 && trie.countWithPrefix("d") == 1,
                "words per prefix");
        check(trie.countWithPrefix("x") == 0 && trie.countWithPrefix("") == 4, "prefix count edges");
        check(countNodes(trie.root) == 9, "9 nodes: the root plus 8 letters");
        check(1 + 3 + 3 + 4 + 3 == 14, "the bound is 1 + 13 characters");
        check(Math.abs(8.0 / (26 * 9) - 0.034) < 0.001, "only 3.4% of the child slots are used");

        Trie fresh = new Trie();
        int[] newNodes = new int[4];
        String[] order = {"car", "cat", "cart", "dog"};
        for (int k = 0; k < order.length; k++) {
            int before = countNodes(fresh.root);
            fresh.insert(order[k]);
            newNodes[k] = countNodes(fresh.root) - before;
        }
        check(newNodes[0] == 3 && newNodes[1] == 1 && newNodes[2] == 1 && newNodes[3] == 3, "new nodes per insert");

        MapNode mapRoot = new MapNode();
        insert(mapRoot, "café");
        insert(mapRoot, "cat");
        check(contains(mapRoot, "café") && contains(mapRoot, "cat") && !contains(mapRoot, "ca"), "map children");
        check(mapRoot.children.size() == 1 && mapRoot.children.get('c').children.get('a').children.size() == 2,
                "the map trie shares c-a and branches into f and t");

        Trie dictionary = new Trie();
        for (String w : new String[] {"bad", "dad", "mad"}) dictionary.insert(w);
        check(matches(dictionary.root, ".ad", 0) && matches(dictionary.root, "b..", 0), "LC 211 wildcards");
        check(!matches(dictionary.root, "pad", 0) && !matches(dictionary.root, "ba", 0), "LC 211 misses");
        check(26 * 26 * 25 == 16_900, "LC 211 worst case with two dots");

        char[][] board = {"cat".toCharArray(), "dog".toCharArray()};
        check(findWords(board, new String[] {"cat", "dog", "goat", "cod"}).equals(List.of("cat", "dog", "goat")), "LC 212");
        check(new String(board[0]).equals("cat") && new String(board[1]).equals("dog"), "the board is restored");
        char[][] example = {"oaan".toCharArray(), "etae".toCharArray(), "ihkr".toCharArray(), "iflv".toCharArray()};
        check(findWords(example, new String[] {"oath", "pea", "eat", "rain"}).equals(List.of("oath", "eat")), "LC 212 example");
        check(findWords(new char[][] {"aa".toCharArray()}, new String[] {"a"}).equals(List.of("a")), "LC 212 reports once");
        check(12 * 12 * 4 * (int) Math.pow(3, 9) == 11_337_408, "LC 212 worst-case path count");
        System.out.println("OK");
    }
}
