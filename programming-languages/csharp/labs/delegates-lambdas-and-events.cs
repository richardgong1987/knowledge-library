// Run from programming-languages/csharp/labs with: dotnet run delegates-lambdas-and-events.cs
using System.Reflection;
using System.Runtime.CompilerServices;

// Level 1: a delegate type is a method signature you can store in a variable.
static int Double(int x) => x * 2;

Transform transform = Double;
Check(transform(21) == 42, "the delegate calls the method it holds");
transform = x => x + 10;
Check(transform(5) == 15, "a lambda converts to the same delegate type");

// Level 2: Func, Action and Predicate are ready-made generic delegate types.
Func<int, int, int> add = (a, b) => a + b;
Predicate<int> isEven = n => n % 2 == 0;
var printed = new List<string>();
Action<string> print = text => printed.Add(text);

print($"{add(2, 3)} {isEven(4)}");
Check(printed[0] == "5 True", "Func returns a value, Predicate returns bool, Action returns nothing");

// Level 3: one delegate can hold a list of methods.
var calls = new List<string>();
Func<int> first = () => { calls.Add("first"); return 1; };
Func<int> second = () => { calls.Add("second"); return 2; };

Func<int> both = first + second;
int result = both();
Check(calls.SequenceEqual(["first", "second"]), "handlers run in the order they were added");
Check(result == 2, "the caller only sees the last handler's return value");
Check(both.GetInvocationList().Length == 2, "the invocation list has two entries");
Check(typeof(Transform).BaseType == typeof(MulticastDelegate), "every delegate type is a MulticastDelegate");

Func<int>? none = both - first - second;
Check(none is null, "removing every handler leaves null, not an empty delegate");

// Level 4: a lambda captures the variable itself, not a copy of its value.
int counter = 0;
Action increment = () => counter++;
increment();
increment();
Check(counter == 2, "the lambda changed the captured variable");

static Func<int> MakeCounter()
{
    int count = 0;
    return () => ++count;
}

Func<int> counterA = MakeCounter();
Func<int> counterB = MakeCounter();
Check(counterA() == 1 && counterA() == 2, "counterA keeps its own count alive");
Check(counterB() == 1, "each call to MakeCounter creates a new variable");

// Level 5: for has one loop variable; foreach (since C# 5) has one per iteration.
var fromFor = new List<Func<int>>();
for (int i = 0; i < 3; i++)
{
    fromFor.Add(() => i);
}

var fromForeach = new List<Func<int>>();
foreach (int i in new[] { 0, 1, 2 })
{
    fromForeach.Add(() => i);
}

Check(fromFor.Select(f => f()).SequenceEqual([3, 3, 3]), "all three share the one i, which ended at 3");
Check(fromForeach.Select(f => f()).SequenceEqual([0, 1, 2]), "each foreach iteration has its own i");

var fixedFor = new List<Func<int>>();
for (int i = 0; i < 3; i++)
{
    int copy = i; // a new variable on every pass of the loop body
    fixedFor.Add(() => copy);
}
Check(fixedFor.Select(f => f()).SequenceEqual([0, 1, 2]), "copying into a body variable fixes the for loop");

// Level 6: a static lambda promises to capture nothing.
Func<int, int> square = static x => x * x;
Check(square(7) == 49, "a static lambda still uses its own parameters");

// Level 7: an event lets outsiders subscribe, but only the owner can raise it.
var sensor = new Sensor();
var screen = new List<string>();
var logFile = new List<string>();
sensor.TemperatureChanged += (_, e) => screen.Add($"{e.NewCelsius} C");
sensor.TemperatureChanged += (_, e) => logFile.Add($"{e.OldCelsius} -> {e.NewCelsius}");

sensor.Read(25);
sensor.Read(25); // no change, so no event
Check(screen.SequenceEqual(["25 C"]), "the display saw one change");
Check(logFile.SequenceEqual(["20 -> 25"]), "the logger saw the old and new values");

var button = new CountingButton();
EventHandler onClick = (_, _) => { };
button.Clicked += onClick;
button.Clicked += onClick;
button.Clicked -= onClick;
Check(button.SubscriberCount == 1, "custom add and remove accessors decide what += and -= do");

button.Clicked += (_, _) => { };
button.Clicked -= (_, _) => { }; // looks the same, but it is a different delegate
Check(button.SubscriberCount == 2, "removing a lookalike lambda removes nothing");

// Level 8: a subscription is a reference from the publisher to the subscriber.
[MethodImpl(MethodImplOptions.NoInlining)]
static WeakReference Subscribe(Sensor publisher, bool unsubscribe)
{
    var display = new Display();
    publisher.TemperatureChanged += display.OnChanged;
    if (unsubscribe)
    {
        publisher.TemperatureChanged -= display.OnChanged;
    }
    return new WeakReference(display);
}

var longLived = new Sensor();
WeakReference subscribed = Subscribe(longLived, unsubscribe: false);
WeakReference unsubscribed = Subscribe(longLived, unsubscribe: true);
GC.Collect();
GC.WaitForPendingFinalizers();
GC.Collect();
Check(subscribed.IsAlive, "the publisher's event keeps the forgotten display alive");
Check(!unsubscribed.IsAlive, "after -= nothing points at the display, so the GC frees it");
GC.KeepAlive(longLived);

// Underneath: a delegate is an object with a target and a method.
Display shown = new();
EventHandler<TemperatureChangedEventArgs> bound = shown.OnChanged;
Check(bound.Target == shown, "an instance-method delegate stores the object it calls");
Check(bound.Method.Name == "OnChanged", "and the method to call on it");
Transform fromStatic = Double;
Check(fromStatic.Target is null, "a delegate to a static method has no target");

object closure = increment.Target!;
Check(closure.GetType().Name.Contains("DisplayClass"), "captured variables live in a compiler-generated class");
Check(ReferenceEquals(closure, print.Target), "lambdas capturing variables of one scope share one closure object");

static Func<int, int> MakeSquare() => x => x * x;
Check(ReferenceEquals(MakeSquare(), MakeSquare()), "a lambda that captures nothing is cached and reused");
Check(MakeSquare().Target!.GetType().Name == "<>c", "it is an instance method of one shared singleton");
Check(!ReferenceEquals(MakeCounter(), MakeCounter()), "a capturing lambda needs a new closure each time");

FieldInfo? backing = typeof(Sensor).GetField("TemperatureChanged", BindingFlags.NonPublic | BindingFlags.Instance);
Check(backing?.FieldType == typeof(EventHandler<TemperatureChangedEventArgs>), "a field-like event is a private delegate field");
Check(typeof(Sensor).GetMethod("add_TemperatureChanged") is not null, "plus a public add accessor");

// Interview: a public delegate field can be overwritten by anyone; an event cannot.
var leaky = new FieldSensor();
var seen = new List<int>();
leaky.Changed += celsius => seen.Add(celsius);
leaky.Changed = null; // compiles: any caller can wipe every subscription
leaky.Changed?.Invoke(30); // and any caller can raise it
Check(seen.Count == 0, "the subscriber was silently removed by an outsider");

var order = new List<string>();
Action handlers = () => throw new InvalidOperationException("display failed");
handlers += () => order.Add("logger");
try
{
    handlers();
}
catch (InvalidOperationException e)
{
    order.Add(e.Message);
}
Check(order.SequenceEqual(["display failed"]), "an exception stops the rest of the invocation list");

Console.WriteLine("OK");

static void Check(bool ok, string what)
{
    if (!ok)
    {
        throw new Exception(what);
    }
}

delegate int Transform(int x);

record TemperatureChangedEventArgs(int OldCelsius, int NewCelsius);

class Sensor
{
    public event EventHandler<TemperatureChangedEventArgs>? TemperatureChanged;

    public int Celsius { get; private set; } = 20;

    public void Read(int celsius)
    {
        if (celsius == Celsius) return;
        var change = new TemperatureChangedEventArgs(Celsius, celsius);
        Celsius = celsius;
        TemperatureChanged?.Invoke(this, change); // only code inside Sensor may raise it
    }
}

class CountingButton
{
    readonly List<EventHandler> handlers = [];

    public int SubscriberCount => handlers.Count;

    public event EventHandler Clicked
    {
        add => handlers.Add(value);
        remove => handlers.Remove(value);
    }
}

class Display
{
    public string Shown { get; private set; } = "";

    public void OnChanged(object? sender, TemperatureChangedEventArgs e) => Shown = $"{e.NewCelsius} C";
}

class FieldSensor
{
    public Action<int>? Changed; // a plain public delegate field, not an event
}
