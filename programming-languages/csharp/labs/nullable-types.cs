// Run from programming-languages/csharp/labs with: dotnet run nullable-types.cs
using System.Diagnostics.CodeAnalysis;
using System.Reflection;
using System.Runtime.CompilerServices;

// Level 1: Nullable<T> gives a value type one extra value, "no value".
int? age = null;                     // short for Nullable<int>
bool known = age.HasValue;           // false
int shown = age.GetValueOrDefault(); // 0, the default of int
age = 36;
int certain = age.Value;             // 36: Value is safe only when HasValue is true

Check(!known && shown == 0 && certain == 36, "HasValue, GetValueOrDefault and Value");
string emptyMessage = ValueOfEmpty();
Check(emptyMessage == "Nullable object must have a value.", $"Value on an empty int? throws, got: {emptyMessage}");

int? five = 5;
int? nothing = null;
int? sum = five + nothing;          // null: 5 plus "unknown" is unknown
bool greater = five > nothing;      // false
bool lessOrEqual = five <= nothing; // also false: every ordering with null is false
bool bothEmpty = nothing == null;   // true: == treats two nulls as equal

Check(sum is null && !greater && !lessOrEqual && bothEmpty, "lifted operators");

// Level 2: ?. ?? and ??= handle "no value" without an if.
Check(Letters(null) == 0 && Letters("Grace") == 5, "?. then ??");

List<string>? tags = null;
tags ??= [];          // assigns only because tags was null
tags ??= ["ignored"]; // tags is not null now, so nothing happens
tags.Add("vip");
Check(tags is ["vip"], "??= assigns once");

int lookups = 0;
Customer? nobody = null;
nobody?.Name = NextName(ref lookups); // C# 14: skipped entirely when nobody is null
Customer? ada = new();
ada?.Name = NextName(ref lookups);    // runs: ada is not null
Check(lookups == 1 && ada?.Name == "Ada", "the right side runs only when the target exists");

// Level 3: nullable reference types are a compile-time analysis.
Check(SafeLength(null) == 0 && SafeLength("Ada") == 3, "the check narrows string? to string");

// Level 4: at run time string? is just string, and nothing checks for null.
var nickname = typeof(Person).GetProperty(nameof(Person.Nickname))!;
var name = typeof(Person).GetProperty(nameof(Person.Name))!;
Check(nickname.PropertyType == typeof(string), "string? is System.String at run time");

var nullability = new NullabilityInfoContext();
Check(nullability.Create(nickname).ReadState == NullabilityState.Nullable, "string? is recorded in metadata");
Check(nullability.Create(name).ReadState == NullabilityState.NotNull, "string is recorded as not-null");

string notReally = null!; // ! silences the compiler; nothing checks at run time
Check(LengthOrError(notReally) == "Object reference not set to an instance of an object.",
    "a non-nullable string can still hold null");

var names = new string[2]; // no warning, yet both elements are null
Check(names[0] is null && names[1] is null, "arrays start full of nulls");

// Level 6: attributes tell the compiler what a method guarantees.
int emailLength = 0;
if (TryFindEmail("ada", out var email))
    emailLength = email.Length; // no warning: true means email is not null
Check(emailLength == 15 && !TryFindEmail("bob", out _), "[NotNullWhen(true)]");

string? input = "hi";
Require(input);
int inputLength = input.Length; // no warning: Require returns only if input was not null
Check(inputLength == 2, "[NotNull] on a guard parameter");

// Underneath: Nullable<T> is a struct of a bool and a T.
Check(Unsafe.SizeOf<int?>() == 8 && Unsafe.SizeOf<long?>() == 16, "a flag next to the value, padded");

int? empty = null;
int? filled = 5;
object? boxedEmpty = empty;   // boxing an empty int? gives a null reference
object? boxedFilled = filled; // boxing a filled int? gives a boxed int, not a boxed int?
Check(boxedEmpty is null && boxedFilled!.GetType() == typeof(int), "Nullable<T> boxes as T or null");
Check((int?)boxedFilled == 5, "a boxed int unboxes to int?");

// Interview: T? on an unconstrained T does not mean Nullable<T>.
Check(FirstOrNothing<int>([]) == 0, "for int, T? is plain int: the default is 0");
Check(FirstOrNothing<string>([]) is null, "for string, T? is string?: the default is null");

Console.WriteLine("OK");

static string ValueOfEmpty()
{
    int? missing = null;
    try
    {
        return missing!.Value.ToString();
    }
    catch (InvalidOperationException e)
    {
        return e.Message;
    }
}

static int Letters(string? name) => name?.Length ?? 0;

static string NextName(ref int lookups)
{
    lookups++;
    return "Ada";
}

static int SafeLength(string? text)
{
    if (text is null) return 0;
    return text.Length; // no warning: here the compiler knows text is not null
}

static string LengthOrError(string text)
{
    try
    {
        return text.Length.ToString();
    }
    catch (NullReferenceException e)
    {
        return e.Message;
    }
}

static bool TryFindEmail(string user, [NotNullWhen(true)] out string? email)
{
    email = user == "ada" ? "ada@example.com" : null;
    return email is not null;
}

static void Require([NotNull] string? value)
{
    ArgumentNullException.ThrowIfNull(value);
}

static T? FirstOrNothing<T>(T[] items) => items.Length > 0 ? items[0] : default;

static void Check(bool ok, string what)
{
    if (!ok) throw new Exception(what);
}

class Customer
{
    public string? Name { get; set; }
}

class Person(string name)
{
    public string Name { get; } = name;
    public string? Nickname { get; init; }
}
