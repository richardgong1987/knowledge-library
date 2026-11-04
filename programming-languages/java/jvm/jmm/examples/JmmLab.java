import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One-file experiments for the Java Memory Model.
 * Compiles on Java 17+; specification references use Java SE 27.
 * javac JmmLab.java && java JmmLab all
 */
public final class JmmLab {
    private JmmLab() {}

    // One running scenario: an order-processing worker shares state with its controller.
    static final class PlainOrderDesk {
        boolean running = true; // intentionally racy for explanation, not production use
        int latestOrder;
        void submit(int id) { latestOrder = id; }
        void stop() { running = false; }
    }

    static final class VolatileOrderDesk {
        int latestOrder;
        volatile boolean hasOrder;
        volatile boolean running = true;

        void submit(int id) {
            latestOrder = id;
            hasOrder = true; // publish ordinary data via volatile
        }
        void stop() { running = false; }
    }

    static final class LockedOrderDesk {
        private int processed;
        private boolean closed;
        synchronized boolean process() {
            if (closed) return false;
            processed++;
            notifyAll();
            return true;
        }
        synchronized void close() { closed = true; notifyAll(); }
        synchronized int count() { return processed; }
        synchronized void awaitAtLeast(int minimum) throws InterruptedException {
            while (processed < minimum && !closed) wait(); // always recheck condition
        }
    }

    static final class OrderSnapshot {
        final int id;
        final String market;
        OrderSnapshot(int id, String market) {
            this.id = id;
            this.market = market;
        }
    }

    static final class AtomicOrderDesk {
        final AtomicInteger processed = new AtomicInteger();
        void process() { processed.incrementAndGet(); }
    }

    static final class HandleOrderDesk {
        int latestOrder;
        boolean ready;
        static final VarHandle READY;
        static {
            try {
                READY = MethodHandles.lookup().findVarHandle(
                        HandleOrderDesk.class, "ready", boolean.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        void submit(int id) {
            latestOrder = id;
            READY.setRelease(this, true);
        }
        int receive() {
            while (!(boolean) READY.getAcquire(this)) Thread.onSpinWait();
            return latestOrder;
        }
    }

    static void plainStopExplanation() {
        PlainOrderDesk desk = new PlainOrderDesk();
        desk.submit(101);
        desk.stop();
        System.out.println("Plain example: order=" + desk.latestOrder);
        System.out.println("A plain cross-thread spin loop is NOT guaranteed to terminate.");
        // Deliberately do not start an unbounded racy loop.
    }

    static void volatilePublication() throws InterruptedException {
        VolatileOrderDesk desk = new VolatileOrderDesk();
        Thread worker = new Thread(() -> {
            while (!desk.hasOrder) Thread.onSpinWait();
            System.out.println("Volatile publication: order=" + desk.latestOrder);
            while (desk.running) Thread.onSpinWait();
            System.out.println("Volatile stop observed");
        }, "order-worker");
        worker.start();
        desk.submit(102);
        desk.stop();
        worker.join();
    }

    static void lockAndWait() throws InterruptedException {
        LockedOrderDesk desk = new LockedOrderDesk();
        Thread worker = new Thread(() -> {
            for (int i = 0; i < 3; i++) desk.process();
        }, "locked-worker");
        worker.start();
        desk.awaitAtLeast(3);
        worker.join();
        System.out.println("Monitor + wait/notify: processed=" + desk.count());
        desk.close();
    }

    static void finalFieldPublication() throws InterruptedException {
        OrderSnapshot[] holder = new OrderSnapshot[1];
        CountDownLatch published = new CountDownLatch(1);
        Thread publisher = new Thread(() -> {
            holder[0] = new OrderSnapshot(103, "BONDS");
            published.countDown();
        });
        Thread reader = new Thread(() -> {
            try {
                published.await();
                OrderSnapshot snapshot = holder[0];
                System.out.println("Final snapshot: " + snapshot.id + " / " + snapshot.market);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        reader.start();
        publisher.start();
        publisher.join();
        reader.join();
    }

    static void atomicCounter() throws InterruptedException {
        AtomicOrderDesk desk = new AtomicOrderDesk();
        int workers = 4, each = 1000;
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < workers; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < each; j++) desk.process();
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();
        System.out.println("Atomic count=" + desk.processed.get()
                + " expected=" + (workers * each));
    }

    static void releaseAcquire() throws InterruptedException {
        HandleOrderDesk desk = new HandleOrderDesk();
        Thread reader = new Thread(() ->
                System.out.println("VarHandle release/acquire: order=" + desk.receive()));
        reader.start();
        desk.submit(104);
        reader.join();
    }

    static void startAndJoin() throws InterruptedException {
        int[] slot = {0};
        slot[0] = 105;
        Thread worker = new Thread(() -> slot[0] += 1);
        worker.start();
        worker.join();
        System.out.println("Thread start/join: " + slot[0]);
    }

    static void wordTearingExplanation() {
        System.out.println("Word tearing: different fields/elements are distinct variables.");
        System.out.println("Non-volatile long/double have a special historical rule.");
    }

    static void causalityExplanation() {
        System.out.println("Causality is about allowed executions, not one observed run.");
        System.out.println("For systematic testing, investigate OpenJDK jcstress.");
    }

    public static void main(String[] args) throws Exception {
        String demo = args.length == 0 ? "all" : args[0];
        switch (demo) {
            case "plain" -> plainStopExplanation();
            case "volatile" -> volatilePublication();
            case "locks" -> lockAndWait();
            case "final" -> finalFieldPublication();
            case "atomic" -> atomicCounter();
            case "varhandle" -> releaseAcquire();
            case "lifecycle" -> startAndJoin();
            case "word-tearing" -> wordTearingExplanation();
            case "causality" -> causalityExplanation();
            case "all" -> {
                plainStopExplanation();
                volatilePublication();
                lockAndWait();
                finalFieldPublication();
                atomicCounter();
                releaseAcquire();
                startAndJoin();
                wordTearingExplanation();
                causalityExplanation();
            }
            default -> throw new IllegalArgumentException("Unknown demo: " + demo);
        }
    }
}
