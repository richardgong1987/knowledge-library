/** One order class for inspecting class files and bytecode. Java 17+. */
public class OrderLab {
    static class Order {
        static { System.out.println("Order class initialised"); }
        final int id;
        Order(int id) { this.id = id; }
        int total(int quantity) { return id * quantity; }
    }

    static int process(int id) {
        Order order = new Order(id);
        return order.total(2);
    }

    public static void main(String[] args) {
        System.out.println("Preparing order 101");
        System.out.println("Result: " + process(101));
        int sum = 0;
        for (int id : new int[]{101, 102}) sum += id;
        System.out.println("Loop result: " + sum);
        System.out.println("Application loader: " + OrderLab.class.getClassLoader());
        System.out.println("String loader: " + String.class.getClassLoader());
    }
}
