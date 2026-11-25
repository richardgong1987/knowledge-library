import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

// Run from this folder with: java GeometryLab.java
public class GeometryLab {

    // Level 1: points with integer coordinates, so every test below is exact.
    record Point(long x, long y) {}

    static long squaredDistance(Point a, Point b) {
        long dx = a.x() - b.x(), dy = a.y() - b.y();
        return dx * dx + dy * dy;                           // no square root, so it stays an exact integer
    }

    static Point at(int[] xy) {
        return new Point(xy[0], xy[1]);
    }

    // Level 2: the dot product of the vectors o->a and o->b.
    static long dot(Point o, Point a, Point b) {            // > 0: angle below 90 degrees; 0: right angle; < 0: above 90
        return (a.x() - o.x()) * (b.x() - o.x()) + (a.y() - o.y()) * (b.y() - o.y());
    }

    // Level 3: the cross product of the vectors o->a and o->b.
    static long cross(Point o, Point a, Point b) {          // > 0: o -> a -> b turns left; < 0: right; 0: straight
        return (a.x() - o.x()) * (b.y() - o.y()) - (a.y() - o.y()) * (b.x() - o.x());
    }

    // Level 4: do the closed segments ab and cd share a point?
    static boolean segmentsIntersect(Point a, Point b, Point c, Point d) {
        long d1 = cross(a, b, c), d2 = cross(a, b, d), d3 = cross(c, d, a), d4 = cross(c, d, b);
        if (Long.signum(d1) * Long.signum(d2) < 0 && Long.signum(d3) * Long.signum(d4) < 0) return true; // a proper crossing
        return (d1 == 0 && onSegment(a, b, c)) || (d2 == 0 && onSegment(a, b, d))
            || (d3 == 0 && onSegment(c, d, a)) || (d4 == 0 && onSegment(c, d, b));     // an endpoint touches the other segment
    }

    static boolean onSegment(Point a, Point b, Point p) {   // p is on the line ab; is it between a and b?
        return Math.min(a.x(), b.x()) <= p.x() && p.x() <= Math.max(a.x(), b.x())
            && Math.min(a.y(), b.y()) <= p.y() && p.y() <= Math.max(a.y(), b.y());
    }

    // Level 5: the shoelace formula.
    static long twiceArea(Point[] polygon) {                // positive when the corners go counter-clockwise
        long sum = 0;
        for (int i = 0; i < polygon.length; i++) {
            Point p = polygon[i], q = polygon[(i + 1) % polygon.length];
            sum += p.x() * q.y() - q.x() * p.y();
        }
        return sum;
    }

    // Level 6: ray casting. Count the edges crossed by a ray going right from the point.
    static boolean insidePolygon(Point[] polygon, Point point) {   // the point must not lie on the boundary
        boolean inside = false;
        for (int i = 0; i < polygon.length; i++) {
            Point p = polygon[i], q = polygon[(i + 1) % polygon.length];
            boolean straddles = (p.y() > point.y()) != (q.y() > point.y());          // the edge crosses the ray's height
            if (straddles && (cross(p, q, point) > 0) == (q.y() > p.y())) inside = !inside; // ... to the right of the point
        }
        return inside;
    }

    // Level 7: Andrew's monotone chain.
    static List<Point> convexHull(Point[] points, boolean keepCollinear) {
        if (points.length == 1) return List.of(points[0]);
        Point[] sorted = points.clone();
        Arrays.sort(sorted, Comparator.comparingLong(Point::x).thenComparingLong(Point::y));
        List<Point> hull = new ArrayList<>();
        for (Point p : sorted) addToChain(hull, p, 0, keepCollinear);                       // lower hull, left to right
        int upperStart = hull.size() - 1;
        for (int i = sorted.length - 2; i >= 0; i--) addToChain(hull, sorted[i], upperStart, keepCollinear); // upper hull, right to left
        hull.remove(hull.size() - 1);                       // the first point has come round again
        return hull;
    }

    static void addToChain(List<Point> chain, Point p, int chainStart, boolean keepCollinear) {
        while (chain.size() >= chainStart + 2) {
            long turn = cross(chain.get(chain.size() - 2), chain.get(chain.size() - 1), p);
            if (turn > 0 || (turn == 0 && keepCollinear)) break;    // a left turn, or a straight line we keep
            chain.remove(chain.size() - 1);                         // a right turn: the middle point is inside the fence
        }
        chain.add(p);
    }

    // Interview 1: LeetCode 1037, Valid Boomerang.
    static boolean isBoomerang(int[][] points) {
        return cross(at(points[0]), at(points[1]), at(points[2])) != 0; // zero area: on one line, or a repeated point
    }

    // Interview 2: LeetCode 593, Valid Square.
    static boolean validSquare(int[] p1, int[] p2, int[] p3, int[] p4) {
        Point[] p = {at(p1), at(p2), at(p3), at(p4)};
        long[] d = new long[6];
        int next = 0;
        for (int i = 0; i < 4; i++)
            for (int j = i + 1; j < 4; j++) d[next++] = squaredDistance(p[i], p[j]);
        Arrays.sort(d);                                     // four sides first, then two diagonals
        return d[0] > 0 && d[0] == d[3] && d[4] == d[5] && d[4] == 2 * d[0];
    }

    // Interview 3: LeetCode 587, Erect the Fence.
    static int[][] outerTrees(int[][] trees) {
        Point[] points = Arrays.stream(trees).map(GeometryLab::at).toArray(Point[]::new);
        return convexHull(points, true).stream()
            .distinct()                                     // a straight-line garden visits its points twice
            .map(p -> new int[] {(int) p.x(), (int) p.y()})
            .toArray(int[][]::new);
    }

    // Brute force for the checks: p is on the fence if some line through p has no point strictly on one side.
    static Set<Point> fenceByBruteForce(Point[] points) {
        Set<Point> fence = new HashSet<>();
        if (points.length == 1) fence.add(points[0]);
        for (Point p : points)
            for (Point q : points) {
                if (p.equals(q)) continue;
                boolean noneLeft = true, noneRight = true;
                for (Point r : points) {
                    if (cross(p, q, r) > 0) noneLeft = false;
                    if (cross(p, q, r) < 0) noneRight = false;
                }
                if (noneLeft || noneRight) fence.add(p);
            }
        return fence;
    }

    static Set<Point> asSet(int[][] xy) {
        Set<Point> set = new HashSet<>();
        for (int[] p : xy) set.add(at(p));
        return set;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        Point o = new Point(0, 0), a = new Point(4, 0), b = new Point(4, 3);

        // Level 1
        check(squaredDistance(o, b) == 25 && Math.sqrt(squaredDistance(o, b)) == 5, "a 3-4-5 triangle");
        check(0.1 + 0.2 != 0.3, "doubles round");
        check(0.1 * 2.1 - 0.7 * 0.3 != 0 && 1 * 21 - 7 * 3 == 0, "collinear in exact integers, not in doubles");

        // Level 2
        check(dot(o, a, new Point(3, 3)) == 12, "dot product (4,0).(3,3)");
        double cosine = 12 / (4 * Math.sqrt(18));
        check(Math.abs(cosine - 0.7071) < 1e-4 && Math.abs(Math.toDegrees(Math.acos(cosine)) - 45) < 1e-9, "45 degrees");
        check(dot(o, new Point(1, 2), new Point(-2, 1)) == 0, "perpendicular");
        check(dot(o, a, new Point(-1, 1)) < 0, "obtuse");

        // Level 3
        check(cross(o, a, b) == 12 && cross(o, b, a) == -12, "left turn and right turn");
        check(cross(new Point(1, 1), new Point(2, 2), new Point(3, 3)) == 0, "collinear");
        check(2L * 32_768 * 32_768 == 1L << 31 && 2 * 32_768 * 32_768 < 0, "int cross products overflow from differences of 32,768");
        long big = 2_000_000_000L;
        check(big * big + big * big == 8_000_000_000_000_000_000L && 8_000_000_000_000_000_000L < Long.MAX_VALUE, "cross products of 10^9 coordinates fit in a long");

        // Level 4
        check(segmentsIntersect(o, new Point(4, 4), new Point(0, 4), a), "the X crosses");
        check(cross(o, new Point(4, 4), new Point(0, 4)) == 16 && cross(o, new Point(4, 4), a) == -16, "opposite sides");
        check(!segmentsIntersect(o, new Point(4, 4), new Point(5, 0), new Point(5, 4)), "apart");
        check(cross(o, new Point(4, 4), new Point(5, 0)) == -20 && cross(o, new Point(4, 4), new Point(5, 4)) == -4, "same side");
        check(segmentsIntersect(o, new Point(4, 4), new Point(4, 4), new Point(6, 0)), "touching at an endpoint");
        check(segmentsIntersect(o, new Point(2, 0), new Point(1, 0), new Point(3, 0)), "overlapping on one line");
        check(!segmentsIntersect(o, new Point(1, 0), new Point(2, 0), new Point(3, 0)), "same line, apart");

        // Level 5
        Point[] triangle = {o, a, b};
        check(twiceArea(triangle) == 12, "shoelace: area 6");
        check(twiceArea(new Point[] {o, b, a}) == -12, "clockwise gives a negative sign");
        check(twiceArea(new Point[] {o, a, new Point(4, 4), new Point(0, 4)}) == 32, "4 x 4 square");
        int boundary = 4 + 3 + 1, interior = 0;              // gcd(4,0) + gcd(0,3) + gcd(4,3)
        for (int x = 0; x <= 4; x++) for (int y = 0; y <= 3; y++)
            if (insidePolygon(triangle, new Point(x, y)) && cross(o, b, new Point(x, y)) != 0 && y != 0 && x != 4) interior++;
        check(boundary == 8 && interior == 3 && 6 == interior + boundary / 2 - 1, "Pick's theorem");

        // Level 6
        check(insidePolygon(triangle, new Point(3, 1)) && !insidePolygon(triangle, new Point(1, 2)), "ray casting");
        Point[] lShape = {o, a, new Point(4, 2), new Point(2, 2), new Point(2, 4), new Point(0, 4)};
        check(insidePolygon(lShape, new Point(1, 3)) && !insidePolygon(lShape, new Point(3, 3)), "a non-convex polygon");
        check(twiceArea(lShape) == 24, "L shape has area 12");

        // Level 7
        Point[] garden = Arrays.stream(new int[][] {{1, 1}, {2, 2}, {2, 0}, {2, 4}, {3, 3}, {4, 2}}).map(GeometryLab::at).toArray(Point[]::new);
        check(convexHull(garden, false).equals(List.of(new Point(1, 1), new Point(2, 0), new Point(4, 2), new Point(2, 4))), "strict hull drops (3, 3)");
        check(convexHull(garden, true).equals(List.of(new Point(1, 1), new Point(2, 0), new Point(4, 2), new Point(3, 3), new Point(2, 4))), "hull keeping (3, 3)");
        check(cross(new Point(4, 2), new Point(3, 3), new Point(2, 4)) == 0, "(3, 3) lies on the edge from (4, 2) to (2, 4)");
        Point[] withEdgePoint = {o, new Point(2, 0), a, new Point(4, 4), new Point(0, 4), new Point(2, 2)};
        check(convexHull(withEdgePoint, false).size() == 4 && convexHull(withEdgePoint, true).size() == 5, "collinear edge point");

        // Interview problems
        check(isBoomerang(new int[][] {{1, 1}, {2, 3}, {3, 2}}) && !isBoomerang(new int[][] {{1, 1}, {2, 2}, {3, 3}}), "LC 1037");
        check(cross(new Point(1, 1), new Point(2, 3), new Point(3, 2)) == -3, "LC 1037 cross");
        check(!isBoomerang(new int[][] {{0, 0}, {0, 0}, {5, 7}}), "LC 1037 repeated point");
        check(validSquare(new int[] {0, 0}, new int[] {1, 1}, new int[] {1, 0}, new int[] {0, 1}), "LC 593 example 1");
        check(!validSquare(new int[] {0, 0}, new int[] {1, 1}, new int[] {1, 0}, new int[] {0, 12}), "LC 593 example 2");
        check(validSquare(new int[] {1, 0}, new int[] {-1, 0}, new int[] {0, 1}, new int[] {0, -1}), "LC 593 example 3");
        check(!validSquare(new int[] {0, 0}, new int[] {0, 0}, new int[] {0, 0}, new int[] {0, 0}), "LC 593 all equal");
        check(!validSquare(new int[] {0, 0}, new int[] {2, 1}, new int[] {3, 3}, new int[] {1, 2}), "LC 593 rhombus");
        check(asSet(outerTrees(new int[][] {{1, 1}, {2, 2}, {2, 0}, {2, 4}, {3, 3}, {4, 2}}))
            .equals(asSet(new int[][] {{1, 1}, {2, 0}, {4, 2}, {3, 3}, {2, 4}})), "LC 587 example 1");
        int[][] line = outerTrees(new int[][] {{1, 2}, {2, 2}, {4, 2}});
        check(line.length == 3 && asSet(line).equals(asSet(new int[][] {{1, 2}, {2, 2}, {4, 2}})), "LC 587 example 2");
        check(outerTrees(new int[][] {{3, 7}}).length == 1, "LC 587 one tree");

        Random random = new Random(7);
        for (int trial = 0; trial < 2000; trial++) {
            Set<Point> unique = new HashSet<>();
            int n = 1 + random.nextInt(9);
            while (unique.size() < n) unique.add(new Point(random.nextInt(5), random.nextInt(5)));
            Point[] points = unique.toArray(new Point[0]);
            int[][] trees = unique.stream().map(p -> new int[] {(int) p.x(), (int) p.y()}).toArray(int[][]::new);
            int[][] fence = outerTrees(trees);
            check(fence.length == asSet(fence).size() && asSet(fence).equals(fenceByBruteForce(points)), "LC 587 random " + unique);
        }
        System.out.println("OK");
    }
}
