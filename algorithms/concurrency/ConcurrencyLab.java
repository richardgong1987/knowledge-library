import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.ReentrantLock;

// Run from this folder with: java ConcurrencyLab.java
// Every check joins its threads first, so the result is the same on every run.
public class ConcurrencyLab {

    // Level 1: run every interleaving of two "x++" operations, each split into read, add and write.
    static Map<Integer, Integer> finalValues() {
        Map<Integer, Integer> counts = new TreeMap<>();
        for (int mask = 0; mask < 1 << 6; mask++) {
            if (Integer.bitCount(mask) != 3) continue;          // bit s set: step s belongs to thread A
            int x = 0;
            int[] register = new int[2], step = new int[2];
            for (int s = 0; s < 6; s++) {
                int t = (mask >> s & 1) == 1 ? 0 : 1;
                switch (step[t]++) {
                    case 0 -> register[t] = x;                  // read the shared value
                    case 1 -> register[t]++;                    // add one in the thread's own register
                    default -> x = register[t];                 // write it back
                }
            }
            counts.merge(x, 1, Integer::sum);
        }
        return counts;
    }

    // Level 2: synchronized makes read-add-write one indivisible step.
    static final class SafeCounter {
        private int value;

        synchronized void increment() {
            value++;
        }

        synchronized int get() {
            return value;
        }
    }

    static int countWithTwoThreads(int perThread) throws InterruptedException {
        SafeCounter counter = new SafeCounter();
        Runnable work = () -> {
            for (int i = 0; i < perThread; i++) counter.increment();
        };
        Thread a = new Thread(work), b = new Thread(work);
        a.start();
        b.start();
        a.join();                                               // everything a did happens-before join returns
        b.join();
        return counter.get();
    }

    // Level 3: a volatile flag publishes the data written before it.
    static final class Mailbox {
        private int message;                                    // plain field
        private volatile boolean ready;                         // the volatile write is the "send"

        void send(int value) {
            message = value;
            ready = true;
        }

        int receive() {
            while (!ready) Thread.onSpinWait();                 // without volatile this loop may never end
            return message;                                     // guaranteed to see the value sent
        }
    }

    // Level 4: a semaphore with k permits lets at most k threads in at once.
    static int maxInside(int permits, int threads) throws InterruptedException {
        Semaphore gate = new Semaphore(permits);
        AtomicInteger inside = new AtomicInteger(), maxSeen = new AtomicInteger();
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread worker = new Thread(() -> {
                gate.acquireUninterruptibly();
                try {
                    maxSeen.accumulateAndGet(inside.incrementAndGet(), Math::max);
                    Thread.yield();
                    inside.decrementAndGet();
                } finally {
                    gate.release();
                }
            });
            workers.add(worker);
            worker.start();
        }
        for (Thread worker : workers) worker.join();
        return maxSeen.get();
    }

    // Level 5: lock ordering prevents deadlock.
    static final class Account {
        final int id;
        int balance;

        Account(int id, int balance) {
            this.id = id;
            this.balance = balance;
        }
    }

    static void transfer(Account from, Account to, int amount) {
        Account first = from.id < to.id ? from : to;            // always lock the lower id first,
        Account second = from.id < to.id ? to : from;           // so no cycle of waiting threads can form
        synchronized (first) {
            synchronized (second) {
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }

    // Interview 1: LeetCode 1114, Print in Order.
    static final class Foo {
        private final CountDownLatch firstDone = new CountDownLatch(1);
        private final CountDownLatch secondDone = new CountDownLatch(1);

        public void first(Runnable printFirst) throws InterruptedException {
            printFirst.run();
            firstDone.countDown();
        }

        public void second(Runnable printSecond) throws InterruptedException {
            firstDone.await();
            printSecond.run();
            secondDone.countDown();
        }

        public void third(Runnable printThird) throws InterruptedException {
            secondDone.await();
            printThird.run();
        }
    }

    // Interview 2: LeetCode 1115, Print FooBar Alternately.
    static final class FooBar {
        private final int n;
        private final Semaphore fooTurn = new Semaphore(1), barTurn = new Semaphore(0);

        FooBar(int n) {
            this.n = n;
        }

        public void foo(Runnable printFoo) throws InterruptedException {
            for (int i = 0; i < n; i++) {
                fooTurn.acquire();
                printFoo.run();
                barTurn.release();                              // hand the turn to bar
            }
        }

        public void bar(Runnable printBar) throws InterruptedException {
            for (int i = 0; i < n; i++) {
                barTurn.acquire();
                printBar.run();
                fooTurn.release();                              // hand the turn back to foo
            }
        }
    }

    // Interview 3: LeetCode 1117, Building H2O.
    static final class H2O {
        private final Semaphore hydrogenSlots = new Semaphore(2);
        private final Semaphore hydrogenReady = new Semaphore(0);

        public void hydrogen(Runnable releaseHydrogen) throws InterruptedException {
            hydrogenSlots.acquire();
            releaseHydrogen.run();
            hydrogenReady.release();
        }

        public void oxygen(Runnable releaseOxygen) throws InterruptedException {
            hydrogenReady.acquire(2);                           // wait for this molecule's two hydrogens
            releaseOxygen.run();
            hydrogenSlots.release(2);                           // open the next molecule
        }
    }

    // Interview 4: LeetCode 1226, The Dining Philosophers. Fork i is philosopher i's left fork.
    static final class DiningPhilosophers {
        private final ReentrantLock[] forks = new ReentrantLock[5];

        DiningPhilosophers() {
            for (int i = 0; i < 5; i++) forks[i] = new ReentrantLock();
        }

        public void wantsToEat(int philosopher, Runnable pickLeftFork, Runnable pickRightFork, Runnable eat,
                               Runnable putLeftFork, Runnable putRightFork) throws InterruptedException {
            int left = philosopher, right = (philosopher + 1) % 5;
            ReentrantLock first = forks[Math.min(left, right)], second = forks[Math.max(left, right)];
            first.lock();                                       // lower-numbered fork first: no circular wait
            second.lock();
            try {
                pickLeftFork.run();
                pickRightFork.run();
                eat.run();
                putLeftFork.run();
                putRightFork.run();
            } finally {
                second.unlock();
                first.unlock();
            }
        }
    }

    interface Task {
        void run() throws InterruptedException;
    }

    // Starts one thread per task, waits for all of them, and fails instead of hanging on a deadlock.
    static void runAll(List<Task> tasks) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for (Task task : tasks) {
            Thread thread = new Thread(() -> {
                try {
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join(10_000);
            check(!thread.isAlive(), "a thread did not finish within 10 s: deadlock?");
        }
    }

    static long binomial(int n, int k) {
        long result = 1;
        for (int i = 1; i <= k; i++) result = result * (n - k + i) / i;
        return result;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) throws InterruptedException {
        // Level 1.
        check(binomial(6, 3) == 20, "C(6, 3) = 20 interleavings");
        check(finalValues().equals(Map.of(1, 18, 2, 2)), "18 interleavings lose an update, 2 give x = 2");
        check(binomial(9, 3) * binomial(6, 3) == 1680, "three threads of 3 steps: 9! / (3!)^3 = 1680");

        // Level 2.
        check(countWithTwoThreads(10_000) == 20_000, "synchronized: 2 x 10,000 = 20,000");

        // Level 3.
        for (int round = 0; round < 100; round++) {
            Mailbox mailbox = new Mailbox();
            int[] received = new int[1];
            runAll(List.of(() -> received[0] = mailbox.receive(), () -> mailbox.send(42)));
            check(received[0] == 42, "the reader sees 42");
        }

        // Level 4.
        for (int round = 0; round < 20; round++) check(maxInside(2, 5) <= 2, "at most 2 of 5 threads inside");
        check(maxInside(1, 3) == 1, "a semaphore with 1 permit is a lock");

        // Level 5: opposite transfers would deadlock without the ordering.
        Account alice = new Account(1, 1_000), bob = new Account(2, 1_000);
        runAll(List.of(
                () -> { for (int i = 0; i < 10_000; i++) transfer(alice, bob, 1); },
                () -> { for (int i = 0; i < 10_000; i++) transfer(bob, alice, 1); }));
        check(alice.balance + bob.balance == 2_000 && alice.balance == 1_000, "money is conserved");

        // Interview 1: LeetCode 1114. Threads start in the worst order: third, second, first.
        for (int round = 0; round < 100; round++) {
            Foo foo = new Foo();
            StringBuffer out = new StringBuffer();
            runAll(List.of(
                    () -> foo.third(() -> out.append("third")),
                    () -> foo.second(() -> out.append("second")),
                    () -> foo.first(() -> out.append("first"))));
            check(out.toString().equals("firstsecondthird"), "LC 1114");
        }

        // Interview 2: LeetCode 1115.
        for (int round = 0; round < 20; round++) {
            FooBar fooBar = new FooBar(2);
            StringBuffer out = new StringBuffer();
            runAll(List.of(() -> fooBar.bar(() -> out.append("bar")), () -> fooBar.foo(() -> out.append("foo"))));
            check(out.toString().equals("foobarfoobar"), "LC 1115 n = 2");
        }

        // Interview 3: LeetCode 1117, with input "OOHHHH".
        for (int round = 0; round < 50; round++) {
            H2O water = new H2O();
            StringBuffer out = new StringBuffer();
            List<Task> atoms = new ArrayList<>();
            for (char atom : "OOHHHH".toCharArray()) {
                if (atom == 'H') atoms.add(() -> water.hydrogen(() -> out.append('H')));
                else atoms.add(() -> water.oxygen(() -> out.append('O')));
            }
            runAll(atoms);
            check(out.toString().equals("HHOHHO"), "LC 1117: every molecule is H, H, then O");
        }

        // Interview 4: LeetCode 1226. 5 philosophers eat 60 times each; neighbours never eat together.
        DiningPhilosophers table = new DiningPhilosophers();
        AtomicIntegerArray eating = new AtomicIntegerArray(5);
        AtomicInteger meals = new AtomicInteger();
        AtomicBoolean clash = new AtomicBoolean();
        AtomicInteger eatingNow = new AtomicInteger(), mostEating = new AtomicInteger();
        List<Task> philosophers = new ArrayList<>();
        for (int p = 0; p < 5; p++) {
            int id = p;
            Runnable nothing = () -> { };
            Runnable eat = () -> {
                eating.set(id, 1);
                mostEating.accumulateAndGet(eatingNow.incrementAndGet(), Math::max);
                if (eating.get((id + 1) % 5) == 1 || eating.get((id + 4) % 5) == 1) clash.set(true);
                meals.incrementAndGet();
                eatingNow.decrementAndGet();
                eating.set(id, 0);
            };
            philosophers.add(() -> {
                for (int meal = 0; meal < 60; meal++) table.wantsToEat(id, nothing, nothing, eat, nothing, nothing);
            });
        }
        runAll(philosophers);
        check(meals.get() == 300 && !clash.get(), "LC 1226: 5 x 60 = 300 meals, no deadlock, no shared fork");
        check(Math.min(4, (4 + 1) % 5) == 0, "philosopher 4 picks fork 0 first");
        check(mostEating.get() <= 2, "5 forks: at most 2 philosophers eat at once");

        // Why it works: Amdahl's law with s = 0.1 and p = 8.
        double speedUp = 1 / (0.1 + 0.9 / 8);
        check(Math.abs(speedUp - 4.7) < 0.01 && 1 / 0.1 == 10, "Amdahl: about 4.7 on 8 processors, below 10 on any number");

        System.out.println("OK");
    }
}
