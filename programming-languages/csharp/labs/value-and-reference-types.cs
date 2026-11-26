// Run from programming-languages/csharp/labs with: dotnet run value-and-reference-types.cs

// Level 1: assignment copies a struct but shares a class instance.
var a = new PointStruct(1, 2);
var b = a;      // copies both fields
b.X = 10;       // a.X is still 1

var c = new PointClass(1, 2);
var d = c;      // copies the reference only
d.X = 10;       // c.X is now 10, because c and d name one object

Check(a.X == 1 && b.X == 10, "a struct assignment copies the value");
Check(c.X == 10 && ReferenceEquals(c, d), "a class assignment shares one object");

// Level 2: boxing copies a value into a new object; unboxing copies it back out.
int n = 42;
object box = n;      // boxing: copy n into a new object on the GC heap
n = 7;               // changes n only, not the copy inside the box
int back = (int)box; // unboxing: copy the value out again

Check(back == 42 && n == 7, "the box holds its own copy");

object first = 42;
object second = 42;
bool sameObject = first == second;     // false: == on object compares references
bool sameValue = first.Equals(second); // true: Int32.Equals compares the numbers

Check(!sameObject && sameValue, "two boxes of 42 are two objects with equal values");
Check(UnboxAsLong(box) == "Unable to cast object of type 'System.Int32' to type 'System.Int64'.",
    "unboxing must name the exact type");

// Level 3: by value, ref, out and in.
var shared = new PointClass(10, 2);
Rename(shared);      // the callee changes the object both variables refer to
Replace(shared);     // the callee reassigns its own copy of the reference
Check(shared.X == 99, "a method can mutate the object, but not replace the caller's reference");

ReplaceRef(ref shared); // ref: the parameter is the caller's variable
Check(shared.X == 0, "with ref, reassignment reaches the caller");

var spot = new PointStruct(1, 2);
MoveRight(ref spot);
Check(spot.X == 2, "ref lets a method change a caller's struct");

bool parsed = int.TryParse("123", out int number); // out: the method must assign it
Check(parsed && number == 123, "out returns a second result");

Check(Length(new PointStruct(3, 4)) == 5, "in passes a read-only reference");

// Level 4: ref locals and ref returns.
int[] scores = [10, 20, 30];
ref int middle = ref scores[1]; // an alias for the array element, not a copy
middle = 25;                    // scores[1] is now 25
LargestElement(scores) = 0;     // assign through the returned reference

Check(scores is [10, 25, 0], "ref locals and ref returns write into the array");

// Level 5: a readonly struct cannot change after construction.
var price = new Money(5.00m, "GBP");
var total = price.Add(2.50m);   // returns a new value; price is unchanged

Check(price.Amount == 5.00m && total.Amount == 7.50m, "readonly struct methods return new values");

// Level 6: default equality is identity for classes and field-by-field for structs.
var p1 = new PointClass(1, 2);
var p2 = new PointClass(1, 2);
bool classesEqual = p1.Equals(p2);  // false: Object.Equals compares identity

var s1 = new PointStruct(1, 2);
var s2 = new PointStruct(1, 2);
bool structsEqual = s1.Equals(s2);  // true: ValueType.Equals compares every field

Check(!classesEqual && structsEqual, "classes compare identity, structs compare fields");

// Level 7: strings are immutable references with value equality.
string greeting = "hello";
string alias = greeting;
alias += " world";      // builds a new string; greeting still refers to "hello"

Check(greeting == "hello" && alias == "hello world", "strings never change in place");

string literal = "hello";
string built = "HELLO".ToLowerInvariant();             // a new string, made at run time
bool equalText = literal == built;                     // true: string overloads ==
bool sameInstance = ReferenceEquals(literal, built);   // false: two objects
bool interned = ReferenceEquals(literal, string.Intern(built)); // true: the pooled copy

Check(equalText && !sameInstance && interned, "== compares text; Intern returns the pooled instance");
Check(ReferenceEquals(literal, greeting), "equal literals in one assembly are one instance");

// Underneath: measured heap sizes on 64-bit .NET.
Check(BytesEach(i => i) == 24, "a boxed int takes 24 bytes");
Check(BytesEach(i => (decimal)i) == 32, "a boxed decimal takes 32 bytes");
Check(BytesEach(_ => new object()) == 24, "even an object with no fields takes 24 bytes");

long start = GC.GetAllocatedBytesForCurrentThread();
var structs = new PointStruct[1000];   // one object: the points sit inside the array
long structBytes = GC.GetAllocatedBytesForCurrentThread() - start;

start = GC.GetAllocatedBytesForCurrentThread();
var classes = new PointClass[1000];    // one array of references...
for (int i = 0; i < classes.Length; i++) classes[i] = new PointClass(i, i); // ...plus 1000 objects
long classBytes = GC.GetAllocatedBytesForCurrentThread() - start;

Check(structs.Length == classes.Length, "both arrays hold 1000 points");
Check(structBytes == 8024 && classBytes == 32024, "inline structs versus separate objects");

// Interview: casting a struct to an interface boxes a copy.
var counter = new Counter();
IIncrement boxedCounter = counter;  // boxing copies counter into a new object
boxedCounter.Increment();           // changes the box, not the local

Check(counter.Value == 0 && ((Counter)boxedCounter).Value == 1, "the interface call mutates the box");

Console.WriteLine("OK");

static void Rename(PointClass p) => p.X = 99;

static void Replace(PointClass p) => p = new PointClass(0, 0);

static void ReplaceRef(ref PointClass p) => p = new PointClass(0, 0);

static void MoveRight(ref PointStruct p) => p.X += 1;

static double Length(in PointStruct p) => Math.Sqrt(p.X * p.X + p.Y * p.Y);

static ref int LargestElement(int[] values)
{
    int best = 0;
    for (int i = 1; i < values.Length; i++)
        if (values[i] > values[best]) best = i;
    return ref values[best]; // a reference to the element, not its value
}

static string UnboxAsLong(object boxed)
{
    try
    {
        return ((long)boxed).ToString();
    }
    catch (InvalidCastException e)
    {
        return e.Message;
    }
}

static long BytesEach(Func<int, object> make)
{
    var kept = new object[1000]; // allocated before measuring, so only the new objects count
    long before = GC.GetAllocatedBytesForCurrentThread();
    for (int i = 0; i < kept.Length; i++) kept[i] = make(i);
    return (GC.GetAllocatedBytesForCurrentThread() - before) / kept.Length;
}

static void Check(bool ok, string what)
{
    if (!ok) throw new Exception(what);
}

struct PointStruct(int x, int y)
{
    public int X = x;
    public int Y = y;
}

class PointClass(int x, int y)
{
    public int X = x;
    public int Y = y;
}

readonly struct Money(decimal amount, string currency)
{
    public decimal Amount { get; } = amount;
    public string Currency { get; } = currency;

    public Money Add(decimal extra) => new(Amount + extra, Currency);
}

interface IIncrement
{
    void Increment();
}

struct Counter : IIncrement
{
    public int Value;

    public void Increment() => Value++;
}
