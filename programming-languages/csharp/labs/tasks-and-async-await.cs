// Run from programming-languages/csharp/labs with: dotnet run tasks-and-async-await.cs
using System.Collections.Concurrent;
using System.Diagnostics;
using System.Reflection;
using System.Runtime.CompilerServices;

Check(SynchronizationContext.Current is null, "a console app starts with no synchronization context");

// Level 1: a Task<T> is a promise of a T, and await unwraps it.
Task<int> ready = Task.FromResult(42);
Check(ready.IsCompletedSuccessfully && await ready == 42, "an already completed task");

var log = new List<string>();
var gate = new TaskCompletionSource<int>();
Task<int> sum = AddWhenReadyAsync(gate.Task, 3, log);
log.Add("caller continues");
Check(!sum.IsCompleted, "the method is suspended at its await");
gate.SetResult(2); // completing the promise resumes the method
Check(sum.IsCompleted, "SetResult ran the rest of the method inline, on this thread");
Check(await sum == 5, "2 + 3");
Check(log.SequenceEqual(["start", "caller continues", "resumed"]), "runs synchronously up to the first await");

// Level 2: await does not block a thread, so 1000 waits overlap.
var clock = Stopwatch.StartNew();
await Task.WhenAll(Enumerable.Range(0, 1000).Select(_ => Task.Delay(50)));
Check(clock.ElapsedMilliseconds < 2000, "1000 waits of 50 ms take far less than 1000 x 50 ms");

// Level 3: awaiting one by one adds the delays; WhenAll takes the longest.
clock.Restart();
string user = await LoadAsync("user", 30);
string orders = await LoadAsync("orders", 50);
string ads = await LoadAsync("ads", 20);
long sequential = clock.ElapsedMilliseconds; // about 30 + 50 + 20 = 100

clock.Restart();
string[] all = await Task.WhenAll(LoadAsync("user", 30), LoadAsync("orders", 50), LoadAsync("ads", 20));
long concurrent = clock.ElapsedMilliseconds; // about max(30, 50, 20) = 50

Check(sequential >= 90 && concurrent >= 45 && concurrent < sequential, "sum versus max");
Check(all.SequenceEqual([user, orders, ads]), "WhenAll returns results in argument order");

// WhenAny returns the first task to finish; cancel the loser.
using var cts = new CancellationTokenSource();
Task<string> cache = LoadAsync("cache", 20);
Task<string> database = LoadAsync("database", 5000, cts.Token);
Task<string> winner = await Task.WhenAny(cache, database);
cts.Cancel(); // the 5-second load stops now instead of running on
Check(await winner == "cache", "the 20 ms task wins");
var canceled = await CatchAsync<TaskCanceledException>(database);
Check(database.IsCanceled && canceled.Message == "A task was canceled.", "the loser ends as Canceled");

// Level 4: cancellation is cooperative, and Task.Run moves CPU work off the caller.
Check(await Task.Run(() => SumTo(10_000_000, CancellationToken.None)) == 50_000_005_000_000, "n(n+1)/2");
using var stopSoon = new CancellationTokenSource(TimeSpan.FromMilliseconds(30));
var stopped = await CatchAsync<OperationCanceledException>(Task.Run(() => SumTo(long.MaxValue, stopSoon.Token)));
Check(stopped.Message == "The operation was canceled.", "the loop noticed the token and stopped");

// Level 5: ValueTask avoids allocating when the answer is already known.
var prices = new Dictionary<string, int>();
ValueTask<int> miss = GetPriceAsync(prices, "tea");
Check(!miss.IsCompleted && await miss == 7, "the first call waits for the slow path");
long before = GC.GetAllocatedBytesForCurrentThread();
ValueTask<int> hit = GetPriceAsync(prices, "tea");
long allocated = GC.GetAllocatedBytesForCurrentThread() - before;
Check(hit.IsCompletedSuccessfully && hit.Result == 7 && allocated == 0, "a cache hit allocates nothing");

// Level 6: await captures SynchronizationContext.Current; ConfigureAwait(false) does not.
var counting = new CountingContext();
SynchronizationContext.SetSynchronizationContext(counting);
Task captured = DelayAsync(captureContext: true);
Task free = DelayAsync(captureContext: false);
SynchronizationContext.SetSynchronizationContext(null);
await Task.WhenAll(captured, free);
Check(counting.Posts == 1, "only the default await resumed through the context");

// Sync over async: blocking the only thread that may run the continuation.
var ui = new SingleThreadContext();
SynchronizationContext.SetSynchronizationContext(ui);
Task blocked = DelayAsync(captureContext: true);
bool finished = blocked.Wait(200); // like .Result: this thread waits for the task...
Check(!finished, "...but the continuation is queued for this same thread: deadlock");
Check(SpinWait.SpinUntil(() => { ui.RunPending(); return blocked.IsCompleted; }, 2000), "pumping the queue frees it");
Task notBlocked = DelayAsync(captureContext: false);
Check(notBlocked.Wait(2000), "with ConfigureAwait(false) the continuation runs on the pool instead");
SynchronizationContext.SetSynchronizationContext(null);

// Underneath: the compiler turned AddWhenReadyAsync into a state machine type.
Func<Task<int>, int, List<string>, Task<int>> method = AddWhenReadyAsync;
Type machine = method.Method.GetCustomAttribute<AsyncStateMachineAttribute>()!.StateMachineType;
Check(typeof(IAsyncStateMachine).IsAssignableFrom(machine), "it implements IAsyncStateMachine");
#if DEBUG
Check(!machine.IsValueType, "debug builds generate a class");
#else
Check(machine.IsValueType, "release builds generate a struct");
#endif

Console.WriteLine("OK");

static async Task<int> AddWhenReadyAsync(Task<int> input, int extra, List<string> log)
{
    log.Add("start");        // runs synchronously, on the caller's thread
    int value = await input; // input is not complete: return to the caller here
    log.Add("resumed");      // runs later, when input completes
    return value + extra;
}

static async Task<string> LoadAsync(string name, int milliseconds, CancellationToken token = default)
{
    await Task.Delay(milliseconds, token);
    return name;
}

static long SumTo(long n, CancellationToken token)
{
    long total = 0;
    for (long i = 1; i <= n; i++)
    {
        if (i % 1_000_000 == 0) token.ThrowIfCancellationRequested();
        total += i;
    }
    return total;
}

static ValueTask<int> GetPriceAsync(Dictionary<string, int> cache, string item)
{
    if (cache.TryGetValue(item, out int price))
        return ValueTask.FromResult(price); // completed synchronously: no Task object
    return new ValueTask<int>(LoadPriceAsync(cache, item));
}

static async Task<int> LoadPriceAsync(Dictionary<string, int> cache, string item)
{
    await Task.Delay(10); // stands for a database query
    cache[item] = 7;
    return 7;
}

static async Task DelayAsync(bool captureContext) =>
    await Task.Delay(10).ConfigureAwait(captureContext);

static async Task<TException> CatchAsync<TException>(Task task) where TException : Exception
{
    try { await task; }
    catch (TException e) { return e; }
    throw new Exception($"expected {typeof(TException).Name}");
}

static void Check(bool ok, string what)
{
    if (!ok) throw new Exception($"check failed: {what}");
}

// Counts how many continuations come back through this context.
sealed class CountingContext : SynchronizationContext
{
    public int Posts;

    public override void Post(SendOrPostCallback callback, object? state)
    {
        Interlocked.Increment(ref Posts);
        base.Post(callback, state); // the base class queues the callback to the thread pool
    }
}

// Like a UI thread: posted callbacks wait in a queue until the owning thread runs them.
sealed class SingleThreadContext : SynchronizationContext
{
    private readonly ConcurrentQueue<(SendOrPostCallback Callback, object? State)> queue = new();

    public override void Post(SendOrPostCallback callback, object? state) => queue.Enqueue((callback, state));

    public void RunPending()
    {
        while (queue.TryDequeue(out var item))
            item.Callback(item.State);
    }
}
