import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

// Run from this folder with: java DesignLab.java
public class DesignLab {

    // Level 1: a library desk. Each operation is one HashSet call, so each is O(1) on average.
    static final class LibraryDesk {
        private final Set<String> borrowed = new HashSet<>();

        boolean borrow(String book) {
            return borrowed.add(book);                    // false if the book is already out
        }

        boolean giveBack(String book) {
            return borrowed.remove(book);
        }

        boolean isOut(String book) {
            return borrowed.contains(book);
        }

        int outCount() {
            return borrowed.size();
        }
    }

    // Level 2: keep the aggregate, not the history.
    static final class RunningAverage {
        private long sum;
        private int count;

        void add(int value) {
            sum += value;
            count++;
        }

        double average() {
            return (double) sum / count;
        }
    }

    // Level 3: two views of the same counts, kept in sync by one private method.
    static final class CountIndex {
        private final Map<String, Integer> countOf = new HashMap<>();
        private final TreeMap<Integer, Set<String>> keysWithCount = new TreeMap<>();

        void inc(String key) {
            setCount(key, countOf.getOrDefault(key, 0) + 1);
        }

        void dec(String key) {
            setCount(key, countOf.get(key) - 1);
        }

        String maxKey() {
            return keysWithCount.isEmpty() ? "" : keysWithCount.lastEntry().getValue().iterator().next();
        }

        private void setCount(String key, int count) {   // the only place that changes either view
            Integer old = countOf.remove(key);
            if (old != null) {
                keysWithCount.get(old).remove(key);
                if (keysWithCount.get(old).isEmpty()) keysWithCount.remove(old);
            }
            if (count == 0) return;
            countOf.put(key, count);
            keysWithCount.computeIfAbsent(count, c -> new LinkedHashSet<>()).add(key);
        }
    }

    // Level 4: the t newest posts across k lists, each list stored oldest first.
    record Post(int time, int id) {}

    static List<Post> newest(List<List<Post>> lists, int t) {
        record Head(List<Post> list, int index) {
            Post post() {
                return list.get(index);
            }
        }
        PriorityQueue<Head> heap = new PriorityQueue<>((a, b) -> b.post().time() - a.post().time());
        for (List<Post> list : lists) {
            if (!list.isEmpty()) heap.add(new Head(list, list.size() - 1));   // each list's newest post
        }
        List<Post> result = new ArrayList<>();
        while (!heap.isEmpty() && result.size() < t) {
            Head head = heap.poll();
            result.add(head.post());
            if (head.index() > 0) heap.add(new Head(head.list(), head.index() - 1));
        }
        return result;
    }

    // Interview 1: LeetCode 355, Design Twitter.
    static final class Twitter {
        private final Map<Integer, List<Post>> posts = new HashMap<>();
        private final Map<Integer, Set<Integer>> following = new HashMap<>();
        private int clock;

        void postTweet(int userId, int tweetId) {
            posts.computeIfAbsent(userId, u -> new ArrayList<>()).add(new Post(clock++, tweetId));
        }

        List<Integer> getNewsFeed(int userId) {
            List<List<Post>> sources = new ArrayList<>();
            sources.add(posts.getOrDefault(userId, List.of()));
            for (int followee : following.getOrDefault(userId, Set.of())) {
                sources.add(posts.getOrDefault(followee, List.of()));
            }
            List<Integer> feed = new ArrayList<>();
            for (Post post : newest(sources, 10)) feed.add(post.id());
            return feed;
        }

        void follow(int followerId, int followeeId) {
            if (followerId != followeeId) following.computeIfAbsent(followerId, u -> new HashSet<>()).add(followeeId);
        }

        void unfollow(int followerId, int followeeId) {
            following.getOrDefault(followerId, new HashSet<>()).remove(followeeId);
        }
    }

    // Interview 2: LeetCode 1396, Design Underground System.
    static final class UndergroundSystem {
        private record CheckIn(String station, int time) {}
        private record Route(String from, String to) {}

        private final Map<Integer, CheckIn> travelling = new HashMap<>();     // card id -> where and when it entered
        private final Map<Route, RunningAverage> trips = new HashMap<>();

        void checkIn(int id, String stationName, int t) {
            travelling.put(id, new CheckIn(stationName, t));
        }

        void checkOut(int id, String stationName, int t) {
            CheckIn start = travelling.remove(id);
            trips.computeIfAbsent(new Route(start.station(), stationName), r -> new RunningAverage()).add(t - start.time());
        }

        double getAverageTime(String startStation, String endStation) {
            return trips.get(new Route(startStation, endStation)).average();
        }
    }

    // Interview 3: LeetCode 432, All O`one Data Structure.
    static final class AllOne {
        private static final class Bucket {
            final int count;
            final Set<String> keys = new LinkedHashSet<>();   // O(1) to read its first key
            Bucket prev, next;

            Bucket(int count) {
                this.count = count;
            }
        }

        private final Bucket head = new Bucket(0), tail = new Bucket(0);   // sentinels: no null checks
        private final Map<String, Bucket> bucketOf = new HashMap<>();

        AllOne() {
            head.next = tail;
            tail.prev = head;
        }

        void inc(String key) {
            Bucket from = bucketOf.getOrDefault(key, head);
            Bucket to = from.next.count == from.count + 1 ? from.next : insertAfter(from, from.count + 1);
            to.keys.add(key);
            bucketOf.put(key, to);
            leave(from, key);
        }

        void dec(String key) {
            Bucket from = bucketOf.get(key);
            if (from.count == 1) {
                bucketOf.remove(key);
            } else {
                Bucket to = from.prev.count == from.count - 1 ? from.prev : insertAfter(from.prev, from.count - 1);
                to.keys.add(key);
                bucketOf.put(key, to);
            }
            leave(from, key);
        }

        String getMaxKey() {
            return tail.prev == head ? "" : tail.prev.keys.iterator().next();
        }

        String getMinKey() {
            return head.next == tail ? "" : head.next.keys.iterator().next();
        }

        private Bucket insertAfter(Bucket left, int count) {
            Bucket bucket = new Bucket(count);
            bucket.prev = left;
            bucket.next = left.next;
            left.next.prev = bucket;
            left.next = bucket;
            return bucket;
        }

        private void leave(Bucket bucket, String key) {   // remove key from its old bucket; unlink the bucket if empty
            if (bucket == head) return;
            bucket.keys.remove(key);
            if (bucket.keys.isEmpty()) {
                bucket.prev.next = bucket.next;
                bucket.next.prev = bucket.prev;
            }
        }
    }

    // Interview 4: LeetCode 2296, Design a Text Editor.
    static final class TextEditor {
        private final StringBuilder left = new StringBuilder();    // text before the cursor
        private final StringBuilder right = new StringBuilder();   // text after the cursor, stored reversed

        void addText(String text) {
            left.append(text);
        }

        int deleteText(int k) {
            int deleted = Math.min(k, left.length());
            left.setLength(left.length() - deleted);
            return deleted;
        }

        String cursorLeft(int k) {
            for (int i = 0; i < k && left.length() > 0; i++) moveLastChar(left, right);
            return lastTen();
        }

        String cursorRight(int k) {
            for (int i = 0; i < k && right.length() > 0; i++) moveLastChar(right, left);
            return lastTen();
        }

        private static void moveLastChar(StringBuilder from, StringBuilder to) {
            to.append(from.charAt(from.length() - 1));
            from.setLength(from.length() - 1);
        }

        private String lastTen() {
            return left.substring(Math.max(0, left.length() - 10));
        }
    }

    static boolean near(double actual, double expected, double tolerance) {
        return Math.abs(actual - expected) <= tolerance;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        // Level 1.
        LibraryDesk desk = new LibraryDesk();
        check(desk.borrow("Matilda") && desk.borrow("Holes") && !desk.borrow("Matilda"), "a book can be out only once");
        check(desk.isOut("Holes") && desk.giveBack("Holes") && !desk.isOut("Holes") && desk.outCount() == 1, "give back");
        check(1_000 * 500 == 500_000, "1,000 questions against a list of 500 books");
        check(10_000 * 1_000 == 10_000_000, "the late board: 10,000 questions scanning 1,000 children");
        check(near(1_000 * Math.log(1_000) / Math.log(2), 9_966, 1), "sorting 1,000 children: about 10,000 comparisons");

        // Level 2.
        RunningAverage average = new RunningAverage();
        for (int minutes : new int[] {10, 12, 14}) average.add(minutes);
        check(average.average() == 12.0, "(10 + 12 + 14) / 3 = 12");

        // Level 3.
        CountIndex counts = new CountIndex();
        counts.inc("a");
        counts.inc("b");
        counts.inc("b");
        check(counts.maxKey().equals("b") && counts.keysWithCount.equals(Map.of(1, Set.of("a"), 2, Set.of("b"))),
                "views agree: a has 1, b has 2");
        counts.dec("b");
        counts.dec("b");
        check(counts.maxKey().equals("a") && !counts.countOf.containsKey("b"), "a count of 0 removes the key");
        counts.dec("a");
        check(counts.maxKey().equals("") && counts.keysWithCount.isEmpty(), "empty");
        check(near(Math.log(1_000_000) / Math.log(2), 19.93, 0.01), "log2 of 10^6 is about 20");

        // Level 4.
        List<Post> mine = List.of(new Post(1, 101), new Post(5, 105));
        List<Post> anna = List.of(new Post(2, 202), new Post(3, 203), new Post(6, 206));
        List<Post> ben = List.of(new Post(4, 304));
        check(newest(List.of(mine, anna, ben), 4).stream().map(Post::id).toList().equals(List.of(206, 105, 304, 203)),
                "the 4 newest posts across 3 lists");
        check(newest(List.of(mine, List.of()), 10).size() == 2, "fewer than t posts");
        check(near(300 * Math.log(300) / Math.log(2), 2468.6, 0.1), "sorting 300 posts: about 2,469 comparisons");
        check(near(23 * Math.log(3) / Math.log(2), 36.5, 0.1), "the merge: about 37 comparisons");
        check(near(510 * Math.log(500) / Math.log(2), 4_573, 1), "a feed over 500 sources: about 4,600 comparisons");

        // Interview 1: LeetCode 355.
        Twitter twitter = new Twitter();
        twitter.postTweet(1, 5);
        check(twitter.getNewsFeed(1).equals(List.of(5)), "LC 355 own tweet");
        twitter.follow(1, 2);
        twitter.postTweet(2, 6);
        check(twitter.getNewsFeed(1).equals(List.of(6, 5)), "LC 355 followee's newer tweet first");
        twitter.unfollow(1, 2);
        check(twitter.getNewsFeed(1).equals(List.of(5)), "LC 355 unfollow");
        for (int i = 0; i < 12; i++) twitter.postTweet(3, 100 + i);
        twitter.follow(1, 3);
        check(twitter.getNewsFeed(1).size() == 10 && twitter.getNewsFeed(1).get(0) == 111, "LC 355 feed has at most 10");

        // Interview 2: LeetCode 1396.
        UndergroundSystem tube = new UndergroundSystem();
        tube.checkIn(45, "Leyton", 3);
        tube.checkIn(27, "Leyton", 10);
        tube.checkOut(45, "Waterloo", 15);
        tube.checkOut(27, "Waterloo", 20);
        check(tube.getAverageTime("Leyton", "Waterloo") == 11.0, "LC 1396: (12 + 10) / 2 = 11");
        tube.checkIn(10, "Leyton", 24);
        tube.checkOut(10, "Waterloo", 38);
        check(tube.getAverageTime("Leyton", "Waterloo") == 12.0, "LC 1396: (12 + 10 + 14) / 3 = 12");

        // Interview 3: LeetCode 432.
        AllOne allOne = new AllOne();
        check(allOne.getMaxKey().equals("") && allOne.getMinKey().equals(""), "LC 432 empty");
        allOne.inc("a");
        allOne.inc("b");
        allOne.inc("b");
        check(allOne.getMaxKey().equals("b") && allOne.getMinKey().equals("a"), "LC 432 [1:{a}] <-> [2:{b}]");
        allOne.inc("a");
        allOne.inc("a");
        check(allOne.getMaxKey().equals("a") && allOne.getMinKey().equals("b"), "LC 432 [2:{b}] <-> [3:{a}]");
        allOne.dec("b");
        check(allOne.getMinKey().equals("b") && allOne.bucketOf.get("b").count == 1, "LC 432 [1:{b}] <-> [3:{a}]");
        allOne.dec("b");
        check(!allOne.bucketOf.containsKey("b") && allOne.getMinKey().equals("a"), "LC 432 b removed at 0");
        allOne.dec("a");
        allOne.dec("a");
        allOne.dec("a");
        check(allOne.getMaxKey().equals("") && allOne.head.next == allOne.tail, "LC 432 back to empty");

        // Interview 4: LeetCode 2296.
        TextEditor editor = new TextEditor();
        editor.addText("hello world");
        check(editor.cursorLeft(6).equals("hello"), "LC 2296 cursorLeft(6)");
        check(editor.right.toString().equals("dlrow "), "the right side is stored reversed");
        check(editor.deleteText(2) == 2, "LC 2296 deleteText(2)");
        editor.addText("lo, my");
        check(editor.cursorRight(10).equals(", my world"), "LC 2296 cursorRight moves only 6 and shows the last 10");
        check(editor.deleteText(100) == 15 && editor.cursorLeft(3).equals(""), "LC 2296 delete stops at the start");
        check(10_000L * 20_000 == 200_000_000 && 40 * 20_000 == 800_000, "copying cost versus cursor cost");

        System.out.println("OK");
    }
}
