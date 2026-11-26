// Run from programming-languages/csharp/labs with: dotnet run pattern-matching.cs
using System.Runtime.CompilerServices;

// Level 1: "is" tests a type and names the converted value in one step.
Check(Describe(42) == "positive int 42", "type pattern with a declaration");
Check(Describe("hi") == "string of length 2", "string pattern");
Check(Describe(null) == "null", "constant pattern null");
Check(Describe(3.5) == "something else", "no pattern matched");

// Level 2: a switch expression with relational patterns is a piecewise function.
Check(TicketPrice(4) == 0 && TicketPrice(10) == 6, "under 5 free, under 18 child price");
Check(TicketPrice(30) == 12 && TicketPrice(70) == 7, "adult and senior");
Check(Catch<ArgumentOutOfRangeException>(() => TicketPrice(-1)).ParamName == "age", "a negative age is rejected");

// Level 3: type, positional and property patterns take objects apart.
Check(Math.Abs(Area(new Circle(1)) - Math.PI) < 1e-12, "circle of radius 1");
Check(Area(new Rectangle(3, 4)) == 12, "rectangle 3 x 4");
Check(Area(new Triangle(6, 2)) == 6, "triangle: 0.5 x 6 x 2");
Check(Name(new Rectangle(2, 2)) == "square" && Name(new Rectangle(2, 3)) == "rectangle", "when guard");
Check(Name(new Circle(0)) == "point" && Name(new Triangle(1, 1)) == "other", "constant inside a property pattern");

var (width, height) = new Rectangle(3, 4); // deconstruction calls the generated Deconstruct
Check(width == 3 && height == 4, "a positional record deconstructs");

Check(Winner("rock", "scissors") == "first" && Winner("rock", "paper") == "second", "tuple patterns");
Check(Winner("paper", "paper") == "draw", "draw");

// Level 4: and, or, not combine patterns.
Check(IsLetter('q') && IsLetter('Q') && !IsLetter('5'), "letter ranges");
Check(IsVowelOrDigit('e') && IsVowelOrDigit('7') && !IsVowelOrDigit('x'), "or between patterns");

// Level 5: list patterns (C# 11).
Check(DescribeList([]) == "empty" && DescribeList([7]) == "one: 7", "length 0 and 1");
Check(DescribeList([1, 2, 9]) == "starts 1, 2", "a prefix");
Check(DescribeList([5, 6, 8]) == "from 5 to 8", "first and last around a slice");
Check(Sum([1, 2, 3, 4]) == 10, "recursion on head and tail: 1 + 2 + 3 + 4");

// Exhaustiveness: if the warning is ignored, the gap shows up at run time.
var unmatched = Catch<SwitchExpressionException>(() => ChildPrice(30));
Check(unmatched.UnmatchedValue is 30, "the exception carries the unmatched value");
Check(unmatched.Message.StartsWith("Non-exhaustive switch expression failed to match its input."), "message");

// Underneath: the arms become a tree of comparisons, each tested at most once.
for (int age = -5; age <= 120; age++)
    Check(age < 0 || TicketPrice(age) == TicketPriceByHand(age), $"hand-lowered price for {age}");

// "is null" cannot be fooled by an overloaded ==.
var weird = new AlwaysEqual();
Check(weird == null, "the overloaded operator says true");
Check(weird is not null, "the null pattern checks the reference itself");

Console.WriteLine("OK");

static string Describe(object? value)
{
    if (value is int n && n > 0) return $"positive int {n}";
    if (value is string s) return $"string of length {s.Length}";
    if (value is null) return "null";
    return "something else";
}

static int TicketPrice(int age) => age switch
{
    < 0 => throw new ArgumentOutOfRangeException(nameof(age)),
    < 5 => 0,
    < 18 => 6,
    >= 65 => 7,
    _ => 12,
};

static int TicketPriceByHand(int age)
{
    if (age < 5)
    {
        if (age < 0) throw new ArgumentOutOfRangeException(nameof(age));
        return 0;
    }
    if (age < 18) return 6;
    if (age < 65) return 12;
    return 7;
}

static double Area(Shape shape) => shape switch
{
    Circle(var r) => Math.PI * r * r,
    Rectangle { Width: var w, Height: var h } => w * h,
    Triangle t => 0.5 * t.Base * t.Height,
    _ => throw new ArgumentException("unknown shape", nameof(shape)),
};

static string Name(Shape shape) => shape switch
{
    Circle { Radius: 0 } => "point",
    Rectangle(var w, var h) when w == h => "square",
    Rectangle => "rectangle",
    _ => "other",
};

static string Winner(string first, string second) => (first, second) switch
{
    _ when first == second => "draw",
    ("rock", "scissors") or ("scissors", "paper") or ("paper", "rock") => "first",
    _ => "second",
};

static bool IsLetter(char c) => c is (>= 'a' and <= 'z') or (>= 'A' and <= 'Z');

static bool IsVowelOrDigit(char c) => c is 'a' or 'e' or 'i' or 'o' or 'u' or (>= '0' and <= '9');

static string DescribeList(int[] numbers) => numbers switch
{
    [] => "empty",
    [var only] => $"one: {only}",
    [1, 2, ..] => "starts 1, 2",
    [var first, .., var last] => $"from {first} to {last}",
};

static int Sum(ReadOnlySpan<int> numbers) => numbers switch
{
    [] => 0,
    [var head, .. var tail] => head + Sum(tail),
};

#pragma warning disable CS8509 // deliberately not exhaustive, to observe the run-time failure
static int ChildPrice(int age) => age switch
{
    < 5 => 0,
    < 18 => 6,
};
#pragma warning restore CS8509

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

abstract record Shape;
record Circle(double Radius) : Shape;
record Rectangle(double Width, double Height) : Shape;
record Triangle(double Base, double Height) : Shape;

class AlwaysEqual
{
    public static bool operator ==(AlwaysEqual? a, AlwaysEqual? b) => true;
    public static bool operator !=(AlwaysEqual? a, AlwaysEqual? b) => false;
    public override bool Equals(object? obj) => true;
    public override int GetHashCode() => 0;
}
