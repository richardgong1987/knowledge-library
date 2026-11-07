import java.nio.ByteBuffer;

/** One order, progressively revealing JVM runtime memory areas. Java 17+. */
public class MemoryLab {
    static final String MARKET = "BONDS";

    static final class Order {
        final int id;
        Order(int id) { this.id = id; }
    }

    static Order process(int id) {
        int localId = id;                 // Local-variable slot in this method's frame
        Order order = new Order(localId); // Object ordinarily allocated on the heap
        return order;                     // Reference can outlive the frame
    }

    public static void main(String[] args) {
        Order order = process(101);
        System.out.println("Order ID: " + order.id);
        System.out.println("Literal reused: " + (MARKET == ("BON" + "DS")));
        System.out.println("New String differs: " + (MARKET == new String("BONDS")));
        ByteBuffer bytes = ByteBuffer.allocateDirect(8);
        bytes.putInt(order.id);
        bytes.flip();
        System.out.println("Direct buffer order ID: " + bytes.getInt());
    }
}
