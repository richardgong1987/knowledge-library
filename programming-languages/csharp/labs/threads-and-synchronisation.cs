// Run from programming-languages/csharp/labs with: dotnet run threads-and-synchronisation.cs
using System.Collections.Concurrent;
using System.Threading.Channels;

const int Threads = 4;
const int Increments = 100_000;
const int Expected = Threads * Increments; // 400,000

// Level 1: a Thread is an operating-system thread you create; the pool lends you one.
int workerId = 0;
bool workerIsPooled = true;
var thread = new Thread(() =>
{
    workerId = Environment.CurrentManagedThreadId;
    workerIsPooled = Thread.CurrentThread.IsThreadPoolThread;
});
thread.Start();
thread.Join(); // wait for it to finish; its writes are visible after Join
Check(workerId != Environment.CurrentManagedThreadId && !workerIsPooled, "a dedicated thread");
Check(await Task.Run(() => Thread.CurrentThread.IsThreadPoolThread), "Task.Run borrows a pool thread");

// Level 2: count++ is read, add, write. Interleaved, increments get lost.
Check(RacyCount(Threads, Increments) <= Expected, "lost updates can only make the total smaller");

// Level 3: a lock lets one thread at a time into the critical section.
Check(LockedCount(Threads, Increments) == Expected, "lock: exactly 400,000");
Lock gate = new();
lock (gate)
{
    Check(gate.IsHeldByCurrentThread, "inside the lock statement this thread holds the Lock");
}
Check(!gate.IsHeldByCurrentThread, "released at the closing brace");

// Level 4: Interlocked makes one read-modify-write indivisible, without a lock.
Check(AtomicCount(Threads, Increments) == Expected, "Interlocked: exactly 400,000");
int highest = 0;
RunOnThreads(Threads, () =>
{
    for (int i = 0; i < Increments; i++)
        UpdateMax(ref highest, i);
});
Check(highest == Increments - 1, "a compare-and-swap loop keeps the largest value, 99,999");

// Level 5: a volatile read is never cached, so the loop sees the other thread's write.
var stop = new StopFlag();
var spinner = new Thread(() => { while (!stop.IsSet) { } });
spinner.Start();
Thread.Sleep(20);
stop.Set();
Check(spinner.Join(TimeSpan.FromSeconds(5)), "the spinning thread noticed the flag and ended");

// Level 6: coordination. A semaphore admits at most k at a time.
Check(await PeakConcurrencyAsync(workers: 6, limit: 2) <= 2, "never more than 2 inside");

var counts = CountWords(["tea", "jam", "tea"], Threads);
Check(counts["tea"] == 8 && counts["jam"] == 4, "4 threads x (2 tea + 1 jam), no update lost");

Check(await ProduceAndConsumeAsync() == 55, "a bounded channel delivers 1 + 2 + ... + 10 = 55");

// Deadlock: each thread holds one lock and waits for the other's.
var (oneGotSecond, twoGotFirst) = TryLocksInOppositeOrder();
Check(!oneGotSecond && !twoGotFirst, "neither thread can take its second lock");

// Lock ordering removes the cycle: opposite transfers always lock the lower id first.
var a = new Account(1, 100);
var b = new Account(2, 100);
var there = new Thread(() => { for (int i = 0; i < 10_000; i++) Transfer(a, b, 1); });
var back = new Thread(() => { for (int i = 0; i < 10_000; i++) Transfer(b, a, 1); });
there.Start();
back.Start();
Check(there.Join(TimeSpan.FromSeconds(10)) && back.Join(TimeSpan.FromSeconds(10)), "no deadlock");
Check(a.Balance == 100 && b.Balance == 100 && a.Balance + b.Balance == 200, "money is conserved");

Console.WriteLine("OK");

static void RunOnThreads(int count, Action work)
{
    var threads = Enumerable.Range(0, count).Select(_ => new Thread(() => work())).ToList();
    threads.ForEach(t => t.Start());
    threads.ForEach(t => t.Join());
}

static int RacyCount(int threads, int increments)
{
    int count = 0;
    RunOnThreads(threads, () =>
    {
        for (int i = 0; i < increments; i++)
            count++; // three steps that another thread can interleave
    });
    return count;
}

static int LockedCount(int threads, int increments)
{
    int count = 0;
    Lock gate = new(); // System.Threading.Lock, .NET 9 and later
    RunOnThreads(threads, () =>
    {
        for (int i = 0; i < increments; i++)
        {
            lock (gate) { count++; } // one thread at a time in here
        }
    });
    return count;
}

static int AtomicCount(int threads, int increments)
{
    int count = 0;
    RunOnThreads(threads, () =>
    {
        for (int i = 0; i < increments; i++)
            Interlocked.Increment(ref count); // one indivisible read-modify-write
    });
    return count;
}

static void UpdateMax(ref int max, int candidate)
{
    int seen = Volatile.Read(ref max);
    while (candidate > seen)
    {
        int previous = Interlocked.CompareExchange(ref max, candidate, seen);
        if (previous == seen) return; // nobody changed max in between: candidate is in
        seen = previous;              // somebody did: compare again with their value
    }
}

static async Task<int> PeakConcurrencyAsync(int workers, int limit)
{
    using var slots = new SemaphoreSlim(limit);
    int inside = 0, peak = 0;
    async Task WorkAsync()
    {
        await slots.WaitAsync(); // waits without blocking a thread when all slots are taken
        try
        {
            UpdateMax(ref peak, Interlocked.Increment(ref inside));
            await Task.Delay(10);
            Interlocked.Decrement(ref inside);
        }
        finally
        {
            slots.Release();
        }
    }
    await Task.WhenAll(Enumerable.Range(0, workers).Select(_ => WorkAsync()));
    return peak;
}

static ConcurrentDictionary<string, int> CountWords(string[] words, int threads)
{
    var counts = new ConcurrentDictionary<string, int>();
    RunOnThreads(threads, () =>
    {
        foreach (string word in words)
            counts.AddOrUpdate(word, 1, (_, old) => old + 1); // retried until it applies atomically
    });
    return counts;
}

static async Task<int> ProduceAndConsumeAsync()
{
    var channel = Channel.CreateBounded<int>(capacity: 2); // when full, the writer waits
    var producer = Task.Run(async () =>
    {
        for (int i = 1; i <= 10; i++)
            await channel.Writer.WriteAsync(i);
        channel.Writer.Complete(); // no more items
    });
    int total = 0;
    await foreach (int item in channel.Reader.ReadAllAsync())
        total += item;
    await producer;
    return total;
}

static (bool OneGotSecond, bool TwoGotFirst) TryLocksInOppositeOrder()
{
    object first = new(), second = new();
    using var bothHoldOne = new Barrier(2);
    using var bothTried = new Barrier(2);
    bool oneGotSecond = false, twoGotFirst = false;
    var one = new Thread(() =>
    {
        lock (first)
        {
            bothHoldOne.SignalAndWait();                  // from here on, thread two holds "second"
            oneGotSecond = Monitor.TryEnter(second, 50);  // lock (second) would wait forever
            if (oneGotSecond) Monitor.Exit(second);
            bothTried.SignalAndWait();                    // keep holding "first" until both have tried
        }
    });
    var two = new Thread(() =>
    {
        lock (second)
        {
            bothHoldOne.SignalAndWait();
            twoGotFirst = Monitor.TryEnter(first, 50);
            if (twoGotFirst) Monitor.Exit(first);
            bothTried.SignalAndWait();
        }
    });
    one.Start();
    two.Start();
    one.Join();
    two.Join();
    return (oneGotSecond, twoGotFirst);
}

static void Transfer(Account from, Account to, int amount)
{
    var (low, high) = from.Id < to.Id ? (from, to) : (to, from); // a fixed global order
    lock (low.Gate)
    {
        lock (high.Gate)
        {
            from.Balance -= amount;
            to.Balance += amount;
        }
    }
}

static void Check(bool ok, string what)
{
    if (!ok) throw new Exception($"check failed: {what}");
}

sealed class StopFlag
{
    private volatile bool isSet; // every read goes to memory; every write is published

    public bool IsSet => isSet;

    public void Set() => isSet = true;
}

sealed class Account(int id, int balance)
{
    public int Id { get; } = id;
    public int Balance { get; set; } = balance;
    public Lock Gate { get; } = new();
}
