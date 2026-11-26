// Run from programming-languages/csharp/labs with: dotnet run memory-gc-and-span.cs
using System.Buffers;
using System.Runtime;
using System.Runtime.CompilerServices;

// Level 1: every heap object pays a header; GetAllocatedBytesForCurrentThread counts exactly.
Check(AllocatedBy(() => Sink.Kept = new object()) == 24, "the smallest object: 24 bytes");
Check(AllocatedBy(() => Sink.Kept = new byte[1]) == 32, "24 + 1 = 25, rounded up to 32");
Check(AllocatedBy(() => Sink.Kept = new int[10]) == 64, "24 + 10 x 4 = 64");
Check(AllocatedBy(() => Sink.Kept = new byte[1000]) == 1024, "24 + 1000 = 1024");

// Level 2: generations. A survivor is promoted 0 -> 1 -> 2.
var survivor = new object();
Check(GC.GetGeneration(survivor) == 0, "new objects start in generation 0");
GC.Collect();
Check(GC.GetGeneration(survivor) == 1, "surviving one collection promotes it to generation 1");
GC.Collect();
Check(GC.GetGeneration(survivor) == 2, "surviving another promotes it to generation 2");
GC.Collect();
Check(GC.GetGeneration(survivor) == 2 && GC.MaxGeneration == 2, "generation 2 is the oldest");

// 85,000 bytes or more goes to the large object heap, which counts as generation 2.
Check(GC.GetGeneration(new byte[84_975]) == 0, "24 + 84,975 = 84,999 bytes: small object heap");
Check(GC.GetGeneration(new byte[84_976]) == 2, "24 + 84,976 = 85,000 bytes: large object heap");
Check(GC.GetGeneration(GC.AllocateArray<byte>(100, pinned: true)) == 2, "the pinned object heap is also old");

Check(!GCSettings.IsServerGC, "a console app uses workstation GC by default");
Check(GCSettings.LatencyMode == GCLatencyMode.Interactive, "with background (concurrent) collection on");

// Level 3: a Span is a window onto existing memory. Slicing copies nothing.
int[] scores = [10, 20, 30, 40, 50];
Span<int> middle = scores.AsSpan(1, 3); // 20, 30, 40: a view, not a copy
middle[0] = 99;                          // writes through to the array
Check(scores[1] == 99 && middle.Length == 3, "the span and the array share memory");

string csv = "12,7,30,5";
Check(SumCsv(csv) == 54 && SumCsvWithSplit(csv) == 54, "12 + 7 + 30 + 5 = 54");
Check(AllocatedBy(() => Sink.Number = SumCsvWithSplit(csv)) > 0, "Split allocates an array and four strings");
Check(AllocatedBy(() => Sink.Number = SumCsv(csv)) == 0, "slicing a ReadOnlySpan<char> allocates nothing");

// Level 4: stackalloc puts a buffer on the stack, freed when the method returns.
Check(SumOfSquares(4) == 30, "1 + 4 + 9 + 16 = 30");
Check(AllocatedBy(() => Sink.Number = SumOfSquares(4)) == 0, "no heap allocation");

// Level 5: Memory<T> can live on the heap and cross an await; ArrayPool reuses arrays.
int[] prices = [3, 4, 5];
Check(await SumLaterAsync(prices.AsMemory(1)) == 9, "4 + 5, read after an await");

var pool = ArrayPool<byte>.Shared;
byte[] buffer = pool.Rent(1000); // at least 1000 bytes, possibly more
pool.Return(buffer);
byte[] again = pool.Rent(1000);
pool.Return(again);
Check(buffer.Length >= 1000 && ReferenceEquals(buffer, again), "the pool handed back the same array");
Check(AllocatedBy(() => pool.Return(pool.Rent(1000))) == 0, "renting from a warm pool allocates nothing");

// Level 6: a weak reference does not keep its target alive.
var held = new byte[100];
var weakToHeld = new WeakReference<byte[]>(held);
WeakReference<byte[]> weakToGarbage = WeakToNewArray();
GC.Collect();
Check(weakToHeld.TryGetTarget(out var target) && target == held, "a strongly referenced target survives");
Check(!weakToGarbage.TryGetTarget(out _), "an unreferenced target is collected");
GC.KeepAlive(held);

Console.WriteLine("OK");

static long AllocatedBy(Action action)
{
    action(); // warm up, so that JIT compilation is not counted
    long before = GC.GetAllocatedBytesForCurrentThread();
    action();
    return GC.GetAllocatedBytesForCurrentThread() - before;
}

static int SumCsvWithSplit(string text) => text.Split(',').Sum(int.Parse);

static int SumCsv(ReadOnlySpan<char> text)
{
    int total = 0;
    while (true)
    {
        int comma = text.IndexOf(',');
        if (comma < 0) return total + int.Parse(text);
        total += int.Parse(text[..comma]); // a slice of the original characters, not a new string
        text = text[(comma + 1)..];
    }
}

static int SumOfSquares(int n)
{
    Span<int> squares = stackalloc int[n]; // on this method's stack frame
    for (int i = 0; i < n; i++)
        squares[i] = (i + 1) * (i + 1);
    int total = 0;
    foreach (int square in squares)
        total += square;
    return total;
}

static async Task<int> SumLaterAsync(Memory<int> numbers)
{
    await Task.Yield(); // a Span<int> parameter could not be used after this line
    int total = 0;
    foreach (int n in numbers.Span)
        total += n;
    return total;
}

[MethodImpl(MethodImplOptions.NoInlining)]
static WeakReference<byte[]> WeakToNewArray() => new(new byte[100]); // nothing else refers to the array

static void Check(bool ok, string what)
{
    if (!ok) throw new Exception($"check failed: {what}");
}

// Storing results in static fields makes them escape, so the JIT cannot optimise the allocations away.
static class Sink
{
    public static object? Kept;
    public static int Number;
}
