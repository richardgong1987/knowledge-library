import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/** One order queue: strong reachability first, weak observation second. Java 17+. */
public class GcLab {
    static final class Order {
        final int id;
        final byte[] payload = new byte[1024];
        Order(int id) { this.id = id; }
    }

    public static void main(String[] args) {
        List<Order> pending = new ArrayList<>();
        Order order = new Order(101);
        pending.add(order);                        // Queue keeps a strong reference
        WeakReference<Order> observer = new WeakReference<>(order);
        System.out.println("Pending: " + pending.size());
        System.out.println("Still reachable: " + (observer.get() != null));

        pending.clear();                          // Release queue's strong reference
        order = null;                             // Release local strong reference
        // The object is now eligible for collection; not guaranteed collected.
        System.gc();                              // Hint, not a contract
        System.out.println("After hint: " +
                (observer.get() == null ? "cleared" : "not yet cleared"));
        System.out.println("Do not assert the timing of garbage collection.");
    }
}
