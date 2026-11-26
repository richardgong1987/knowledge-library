// Run from programming-languages/csharp/labs with: dotnet run iterators-and-yield.cs
using System.Runtime.CompilerServices;

// The problem: the first three even squares from 1 to 1,000,000, without a million-element list.
int examined = 0;
IEnumerable<int> UpTo(int limit)
{
    for (int n = 1; n <= limit; n++)
    {
        examined++;
        yield return n;
    }
}

var firstEvenSquares = UpTo(1_000_000).Where(n => n % 2 == 0).Select(n => n * n).Take(3).ToList();
Check(firstEvenSquares.SequenceEqual([4, 16, 36]), "2*2, 4*4 and 6*6");
Check(examined == 6, "only 1 to 6 were ever produced");

// Level 1: IEnumerable hands out an IEnumerator; MoveNext and Current walk it.
IEnumerable<int> numbers = new List<int> { 10, 20, 30 };
IEnumerator<int> cursor = numbers.GetEnumerator();
var walked = new List<int>();
while (cursor.MoveNext())
{
    walked.Add(cursor.Current);
}
cursor.Dispose();
Check(walked.SequenceEqual([10, 20, 30]), "MoveNext steps forward, Current reads the element");

var growing = new List<int> { 1, 2 };
string? modified = null;
try
{
    foreach (int n in growing)
    {
        growing.Add(n); // changes the list under its own enumerator
    }
}
catch (InvalidOperationException e)
{
    modified = e.Message;
}
Check(modified == "Collection was modified; enumeration operation may not execute.", "List<T> detects the change");

// Level 2: foreach only needs GetEnumerator, MoveNext and Current, not the interfaces.
var counted = new List<int>();
foreach (int n in new Countdown(3))
{
    counted.Add(n);
}
Check(counted.SequenceEqual([3, 2, 1]), "foreach binds to the pattern by name");
Check(typeof(List<int>.Enumerator).IsValueType, "List<T> hands foreach a struct enumerator");

// Level 3: yield return turns a method into a lazy sequence.
static IEnumerable<int> TracedSquares(int count, List<string> trace)
{
    trace.Add("start");
    for (int i = 1; i <= count; i++)
    {
        trace.Add($"yield {i * i}");
        yield return i * i;
    }
    trace.Add("end");
}

var trace = new List<string>();
IEnumerable<int> squares = TracedSquares(3, trace);
Check(trace.Count == 0, "calling an iterator method runs none of its body");

foreach (int square in squares)
{
    trace.Add($"got {square}");
}
Check(trace.SequenceEqual(["start", "yield 1", "got 1", "yield 4", "got 4", "yield 9", "got 9", "end"]),
    "producer and consumer take turns, one element at a time");

squares.ToList();
Check(trace.Count(line => line == "start") == 2, "every new enumeration runs the body again");

// Level 4: yield break ends early, and finally runs when the consumer stops.
static IEnumerable<T> FirstN<T>(IEnumerable<T> source, int n)
{
    if (n <= 0) yield break;
    foreach (T item in source)
    {
        yield return item;
        if (--n == 0) yield break;
    }
}

Check(FirstN(new[] { 7, 8, 9 }, 2).SequenceEqual([7, 8]), "yield break stops after two");
Check(!FirstN(new[] { 7, 8, 9 }, 0).Any(), "yield break before any yield gives an empty sequence");

static IEnumerable<int> Guarded(List<string> log)
{
    try
    {
        yield return 1;
        yield return 2;
    }
    finally
    {
        log.Add("finally");
    }
}

var guardLog = new List<string>();
foreach (int n in Guarded(guardLog))
{
    guardLog.Add($"got {n}");
    break; // foreach calls Dispose, and Dispose runs the finally block
}
Check(guardLog.SequenceEqual(["got 1", "finally"]), "breaking out still runs finally");

// Level 5: an infinite sequence is fine, as long as the consumer stops.
static IEnumerable<long> Fibonacci()
{
    long a = 0, b = 1;
    while (true)
    {
        yield return a;
        (a, b) = (b, a + b);
    }
}

Check(FirstN(Fibonacci(), 10).SequenceEqual([0L, 1, 1, 2, 3, 5, 8, 13, 21, 34]), "the first ten Fibonacci numbers");

// Level 6: argument checks inside an iterator are deferred too.
static IEnumerable<int> CountTo(int limit)
{
    ArgumentOutOfRangeException.ThrowIfNegative(limit);
    for (int i = 1; i <= limit; i++)
    {
        yield return i;
    }
}

IEnumerable<int> lazyError = CountTo(-1); // no exception yet
string? thrown = null;
try
{
    lazyError.GetEnumerator().MoveNext();
}
catch (ArgumentOutOfRangeException e)
{
    thrown = e.ParamName;
}
Check(thrown == "limit", "the check only ran on the first MoveNext");

static IEnumerable<int> CountToChecked(int limit)
{
    ArgumentOutOfRangeException.ThrowIfNegative(limit); // runs at the call
    return Iterate();

    IEnumerable<int> Iterate()
    {
        for (int i = 1; i <= limit; i++)
        {
            yield return i;
        }
    }
}

bool threwAtCall = false;
try
{
    CountToChecked(-1);
}
catch (ArgumentOutOfRangeException)
{
    threwAtCall = true;
}
Check(threwAtCall, "a plain wrapper method checks its arguments immediately");
Check(CountToChecked(3).SequenceEqual([1, 2, 3]), "and the local iterator still yields lazily");

bool selectThrew = false;
try
{
    Func<int, int> noSelector = null!;
    new[] { 1, 2 }.Select(noSelector);
}
catch (ArgumentNullException)
{
    selectThrew = true;
}
Check(selectThrew, "LINQ validates its arguments at the call, not at the first MoveNext");

// Level 7: async iterators await between elements.
static async IAsyncEnumerable<int> Readings([EnumeratorCancellation] CancellationToken token = default)
{
    for (int i = 1; ; i++)
    {
        await Task.Delay(10, token); // a slow sensor
        yield return 20 + i;
    }
}

var received = new List<int>();
await foreach (int celsius in Readings())
{
    received.Add(celsius);
    if (received.Count == 3) break;
}
Check(received.SequenceEqual([21, 22, 23]), "await foreach pulls three readings, then stops");

using var cts = new CancellationTokenSource();
var beforeCancel = new List<int>();
bool cancelled = false;
try
{
    await foreach (int celsius in Readings().WithCancellation(cts.Token))
    {
        beforeCancel.Add(celsius);
        if (beforeCancel.Count == 2) cts.Cancel();
    }
}
catch (OperationCanceledException)
{
    cancelled = true;
}
Check(cancelled && beforeCancel.SequenceEqual([21, 22]), "WithCancellation reaches the iterator's token");

// Underneath: the compiler generates one object that is both the sequence and its cursor.
IEnumerable<int> machine = TracedSquares(2, new List<string>());
Check(machine is IEnumerator<int>, "the generated class implements both interfaces");
Check(machine.GetType().Name.Contains("TracedSquares"), "it is a compiler-generated class named after the method");

IEnumerator<int> firstCursor = machine.GetEnumerator();
IEnumerator<int> secondCursor = machine.GetEnumerator();
Check(ReferenceEquals(firstCursor, machine), "the first GetEnumerator hands out the object itself");
Check(!ReferenceEquals(secondCursor, machine), "a second one gets a fresh copy with its own state");

Check(firstCursor.MoveNext() && firstCursor.Current == 1, "first cursor at 1");
Check(firstCursor.MoveNext() && firstCursor.Current == 4, "first cursor at 4");
Check(secondCursor.MoveNext() && secondCursor.Current == 1, "the second cursor starts from the beginning");
Check(!firstCursor.MoveNext(), "the first cursor is finished after two squares");
bool resetFailed = false;
try
{
    firstCursor.Reset();
}
catch (NotSupportedException)
{
    resetFailed = true;
}
Check(resetFailed, "generated enumerators do not support Reset");

// Interview: LINQ operators are iterators, so a query is a recipe until something pulls on it.
var source = new List<int> { 1, 2, 3 };
IEnumerable<int> doubled = source.Select(n => n * 2);
source.Add(4);
Check(doubled.Sum() == 20, "the query saw the 4 added after it was written: 2 + 4 + 6 + 8");

Console.WriteLine("OK");

static void Check(bool ok, string what)
{
    if (!ok)
    {
        throw new Exception(what);
    }
}

class Countdown(int start)
{
    public CountdownEnumerator GetEnumerator() => new(start);
}

class CountdownEnumerator(int start)
{
    int next = start + 1;

    public int Current => next;

    public bool MoveNext() => --next >= 1;
}
