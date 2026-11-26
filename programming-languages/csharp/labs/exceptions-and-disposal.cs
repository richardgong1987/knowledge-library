// Run from programming-languages/csharp/labs with: dotnet run exceptions-and-disposal.cs
using System.Runtime.CompilerServices;

var log = new List<string>();

// Level 1: try, catch and finally run in a fixed order.
Check(ParseOrZero("42", log) == 42 && ParseOrZero("forty-two", log) == 0, "parse results");
Check(log.SequenceEqual(["try 42", "finally", "try forty-two", "catch FormatException", "finally"]),
    "finally runs after a return and after a catch");

// The hierarchy: a catch clause for a base class also catches every derived class.
Check(typeof(ArgumentNullException).BaseType == typeof(ArgumentException), "ArgumentNullException is an ArgumentException");
Check(typeof(ArgumentException).BaseType == typeof(SystemException), "which is a SystemException");
Check(typeof(SystemException).BaseType == typeof(Exception), "which is an Exception");
Check(typeof(ArgumentOutOfRangeException).BaseType == typeof(ArgumentException), "ArgumentOutOfRangeException");
Check(typeof(InvalidOperationException).BaseType == typeof(SystemException), "InvalidOperationException");
Check(typeof(ObjectDisposedException).BaseType == typeof(InvalidOperationException), "ObjectDisposedException");
Check(typeof(FormatException).BaseType == typeof(SystemException), "FormatException");
Check(typeof(OperationCanceledException).BaseType == typeof(SystemException), "OperationCanceledException");
Check(typeof(TaskCanceledException).BaseType == typeof(OperationCanceledException), "TaskCanceledException");
Check(Classify(new ArgumentNullException("name")) == "null argument", "most specific clause first");
Check(Classify(new ArgumentOutOfRangeException("age")) == "bad argument", "caught by its base class");
Check(Classify(new FormatException()) == "anything else", "caught by Exception");

// Level 2: exception filters choose a handler without catching first.
Check(Handle(503) == "retry" && Handle(404) == "not found", "the filter picks the clause");
var unhandled = Catch<HttpStatusException>(() => Handle(400));
Check(unhandled.StatusCode == 400 && unhandled.Message == "HTTP 400", "no filter matched, so it propagates");

// Filters run before the stack unwinds: the inner finally has not run yet.
log.Clear();
try
{
    ThrowWithFinally(log);
}
catch (InvalidOperationException) when (Note(log, "filter"))
{
    log.Add("catch");
}
Check(log.SequenceEqual(["filter", "inner finally", "catch"]), "pass 1 runs filters, pass 2 unwinds");

// Level 3: "throw;" keeps the original stack trace, "throw e;" restarts it.
string kept = Catch<InvalidOperationException>(RethrowKeepingTrace).StackTrace!;
string lost = Catch<InvalidOperationException>(RethrowLosingTrace).StackTrace!;
Check(kept.Contains("DiskFull") && kept.Contains("RethrowKeepingTrace"), "throw; shows where it started");
Check(!lost.Contains("DiskFull") && lost.Contains("RethrowLosingTrace"), "throw e; hides the origin");

// Level 4: using declarations dispose at the end of the scope, in reverse order.
log.Clear();
UseThree(log);
Check(log.SequenceEqual(["open a", "open b", "open c", "work", "close c", "close b", "close a"]), "LIFO");

log.Clear();
Catch<InvalidOperationException>(() => FailWhileOpen(log));
Check(log.SequenceEqual(["open file", "close file"]), "Dispose runs even when the body throws");

log.Clear();
UseOneByHand(log);
Check(log.SequenceEqual(["open a", "work", "close a"]), "using is try/finally");

// Level 5: the dispose pattern makes Dispose idempotent and guards every method.
var connection = new Connection();
connection.Send();
connection.Dispose();
connection.Dispose(); // a second call must be harmless
var disposedError = Catch<ObjectDisposedException>(connection.Send);
Check(disposedError.Message == $"Cannot access a disposed object.{Environment.NewLine}Object name: 'Connection'.",
    "the message names the type");

// A finalizer runs only after a garbage collection, and the memory waits for a second one.
WeakReference leaky = Abandon();
GC.Collect();                  // unreachable, but it has a finalizer: queue it instead of freeing it
GC.WaitForPendingFinalizers(); // the finalizer thread runs ~Leaky
Check(Leaky.Finalized == 1 && leaky.IsAlive, "finalized, but its memory is not freed yet");
GC.Collect();                  // now nothing can reach it, not even the finalization queue
Check(!leaky.IsAlive, "the second collection frees it");

// Level 6: await using calls DisposeAsync.
log.Clear();
await WriteAsync(log);
Check(log.SequenceEqual(["write", "flushed and closed"]), "await using");

Console.WriteLine("OK");

static int ParseOrZero(string text, List<string> log)
{
    try
    {
        log.Add($"try {text}");
        return int.Parse(text);
    }
    catch (FormatException)
    {
        log.Add("catch FormatException");
        return 0;
    }
    finally
    {
        log.Add("finally"); // runs on the return and after the catch
    }
}

static string Classify(Exception exception)
{
    try
    {
        throw exception;
    }
    catch (ArgumentNullException) { return "null argument"; }
    catch (ArgumentException) { return "bad argument"; }
    catch (Exception) { return "anything else"; }
}

static string Handle(int statusCode)
{
    try
    {
        throw new HttpStatusException(statusCode);
    }
    catch (HttpStatusException e) when (e.StatusCode >= 500)
    {
        return "retry";
    }
    catch (HttpStatusException e) when (e.StatusCode == 404)
    {
        return "not found";
    }
}

static void ThrowWithFinally(List<string> log)
{
    try { throw new InvalidOperationException(); }
    finally { log.Add("inner finally"); }
}

static bool Note(List<string> log, string message)
{
    log.Add(message);
    return true;
}

[MethodImpl(MethodImplOptions.NoInlining)]
static void DiskFull() => throw new InvalidOperationException("disk full");

static void RethrowKeepingTrace()
{
    try { DiskFull(); }
    catch (InvalidOperationException) { throw; }
}

#pragma warning disable CA2200 // deliberately wrong, to measure what it loses
static void RethrowLosingTrace()
{
    try { DiskFull(); }
    catch (InvalidOperationException e) { throw e; }
}
#pragma warning restore CA2200

static void UseThree(List<string> log)
{
    using var a = new Resource("a", log);
    using var b = new Resource("b", log);
    using var c = new Resource("c", log);
    log.Add("work");
} // disposed here, in reverse order: c, b, a

static void FailWhileOpen(List<string> log)
{
    using var file = new Resource("file", log);
    throw new InvalidOperationException("parse error");
}

static void UseOneByHand(List<string> log)
{
    var a = new Resource("a", log);
    try
    {
        log.Add("work");
    }
    finally
    {
        a?.Dispose();
    }
}

[MethodImpl(MethodImplOptions.NoInlining)]
static WeakReference Abandon() => new(new Leaky(), trackResurrection: true); // only a weak reference remains

static async Task WriteAsync(List<string> log)
{
    await using var writer = new AsyncWriter(log);
    log.Add("write");
}

static TException Catch<TException>(Action action) where TException : Exception
{
    try { action(); }
    catch (TException e) { return e; }
    throw new Exception($"expected {typeof(TException).Name}");
}

static void Check(bool ok, string what)
{
    if (!ok) throw new Exception($"check failed: {what}");
}

class HttpStatusException(int statusCode) : Exception($"HTTP {statusCode}")
{
    public int StatusCode { get; } = statusCode;
}

sealed class Resource : IDisposable
{
    private readonly string name;
    private readonly List<string> log;

    public Resource(string name, List<string> log)
    {
        this.name = name;
        this.log = log;
        log.Add($"open {name}");
    }

    public void Dispose() => log.Add($"close {name}");
}

class Connection : IDisposable
{
    private bool disposed;

    public void Send() => ObjectDisposedException.ThrowIf(disposed, this);

    public void Dispose()
    {
        Dispose(disposing: true);
        GC.SuppressFinalize(this);
    }

    protected virtual void Dispose(bool disposing)
    {
        if (disposed) return;
        if (disposing)
        {
            // release managed resources here: streams, sockets, other IDisposable fields
        }
        // release unmanaged resources here, if this class owned any directly
        disposed = true;
    }
}

class Leaky
{
    public static int Finalized;

    ~Leaky() => Interlocked.Increment(ref Finalized);
}

sealed class AsyncWriter(List<string> log) : IAsyncDisposable
{
    public async ValueTask DisposeAsync()
    {
        await Task.Yield(); // stands for flushing a buffer over the network
        log.Add("flushed and closed");
    }
}
