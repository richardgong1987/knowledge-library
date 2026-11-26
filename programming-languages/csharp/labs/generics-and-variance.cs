// Run from programming-languages/csharp/labs with: dotnet run generics-and-variance.cs
using System.Collections;
using System.Numerics;
using System.Runtime.CompilerServices;

static void Check(bool ok, string what)
{
    if (!ok)
    {
        throw new Exception(what);
    }
}

// The problem: a non-generic list accepts anything, so the mistake surfaces at run time.
var untyped = new ArrayList { 1, 2, "three" }; // compiles: everything is an object
int sum = 0;
string problem = "";
try
{
    foreach (object item in untyped)
    {
        sum += (int)item; // the cast fails on "three"
    }
}
catch (InvalidCastException e)
{
    problem = e.Message;
}
Check(sum == 3, "1 + 2 were added before the crash");
Check(problem == "Unable to cast object of type 'System.String' to type 'System.Int32'.", "the exact message");

// Level 1: one generic method works for many types.
static T Largest<T>(T a, T b) where T : IComparable<T> => a.CompareTo(b) >= 0 ? a : b;

Check(Largest(3, 7) == 7, "T = int");
Check(Largest("pear", "apple") == "pear", "T = string");

// Level 2: generics are reified. T is a real type at run time, and ints stay unboxed.
static string NameOf<T>() => typeof(T).Name; // T is still known when the code runs

Check(NameOf<int>() == "Int32" && NameOf<double>() == "Double", "value-type arguments");
Check(NameOf<string>() == "String" && NameOf<object>() == "Object", "reference-type arguments");
Check(new List<int>().GetType() != new List<string>().GetType(), "two different runtime types");
Check(new List<int>().GetType().ToString() == "System.Collections.Generic.List`1[System.Int32]", "the full name");

static long BytesAllocatedBy(Action action)
{
    long before = GC.GetAllocatedBytesForCurrentThread();
    action();
    return GC.GetAllocatedBytesForCurrentThread() - before;
}

static List<int> FillList(int n)
{
    var list = new List<int>(n); // one int[n] array holds the values themselves
    for (int i = 0; i < n; i++) list.Add(i);
    return list;
}

static ArrayList FillArrayList(int n)
{
    var list = new ArrayList(n); // one object[n] array holds references to boxes
    for (int i = 0; i < n; i++) list.Add(i); // each Add boxes an int
    return list;
}

Check(BytesAllocatedBy(() => FillList(1000)) == 4056, "4 bytes per int + 56 fixed");
Check(BytesAllocatedBy(() => FillArrayList(1000)) == 32056, "8 + 24 bytes per int + 56 fixed");

// Level 3: constraints say what T must be, and so what the body may do with it.
static T Make<T>() where T : new() => new T();                            // has a parameterless constructor
static int SizeOf<T>() where T : unmanaged => Unsafe.SizeOf<T>();         // plain bytes, no references
static bool HasValue<T>(T? maybe) where T : struct => maybe.HasValue;     // T? is Nullable<T> here
static bool IsMissing<T>(T? maybe) where T : class => maybe is null;      // T is a reference type
static string MessageOf<T>(T error) where T : Exception => error.Message; // base class: Message exists
static Dictionary<T, int> Tally<T>(IEnumerable<T> keys) where T : notnull // keys must not be null
{
    var counts = new Dictionary<T, int>();
    foreach (T key in keys) counts[key] = counts.GetValueOrDefault(key) + 1;
    return counts;
}

Check(Make<List<int>>().Count == 0, "new() builds an empty list");
Check(SizeOf<int>() == 4 && SizeOf<decimal>() == 16, "unmanaged sizes");
Check(!HasValue<int>(null) && HasValue<int>(5), "struct: T? is Nullable<T>");
Check(IsMissing<string>(null), "class: T? is a nullable reference");
Check(MessageOf(new InvalidOperationException("boom")) == "boom", "base-class member");
Check(Tally(["a", "b", "a"])["a"] == 2, "notnull keys");

// Level 4: covariance. IEnumerable<out T> only hands T out, so strings can be read as objects.
IEnumerable<string> names = ["Ada", "Grace"];
IEnumerable<object> things = names; // allowed: IEnumerable<out T>
Check(ReferenceEquals(things, names), "no copy: the same list, seen as objects");
Check(things.First() is "Ada", "reading gives back the strings");

// Level 5: contravariance. Action<in T> only takes T in, so an object handler accepts strings.
var printed = new List<string>();
Action<object> printAnything = x => printed.Add($"{x}");
Action<string> printText = printAnything; // allowed: Action<in T>
printText("hi");
Check(printed.Single() == "hi", "the object handler received a string");

// Level 6: variance needs references; arrays are covariant without compile-time safety.
object ints = new List<int>();
Check(ints is not IEnumerable<object>, "an int must be boxed to become an object, so no variance");
object[] objects = new string[2]; // array covariance: allowed since C# 1.0
objects[0] = "fine";
string storeError = "";
try
{
    objects[1] = 42; // compiles, but the array really holds strings
}
catch (ArrayTypeMismatchException e)
{
    storeError = e.Message;
}
Check(storeError == "Attempted to access an element as a type incompatible with the array.", "checked at run time");

// Level 7: generic math. Static abstract members let T supply Zero and +.
static T Sum<T>(IEnumerable<T> values) where T : INumber<T>
{
    T total = T.Zero;
    foreach (T value in values)
    {
        total += value; // T's own + operator
    }
    return total;
}

Check(Sum([1, 2, 3]) == 6, "int");
Check(Sum([0.1m, 0.2m]) == 0.3m, "decimal: exact");
Check(Sum([0.1, 0.2]) == 0.30000000000000004, "double: binary rounding");

// Underneath: every closed generic type is its own runtime type with its own static fields.
PerType<int>.Count = 1;
PerType<string>.Count = 2;
Check(PerType<int>.Count == 1 && PerType<string>.Count == 2, "separate statics per T");

Console.WriteLine("OK");

static class PerType<T>
{
    public static int Count;
}
