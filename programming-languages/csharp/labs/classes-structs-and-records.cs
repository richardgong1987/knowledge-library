// Run from programming-languages/csharp/labs with: dotnet run classes-structs-and-records.cs
using System.Reflection;
using System.Runtime.CompilerServices;

// Level 1: constructors establish the rules, and one can chain to another.
var opened = new Account("Ada", 50m);
opened.Deposit(25m);
var empty = new Account("Bob");   // chains to the main constructor with 0

Check(opened.Balance == 75m && empty.Balance == 0m, "constructors set the starting state");
Check(Rejects(() => new Account("Eve", -1m)), "the constructor refuses a negative opening balance");

// Level 2: primary constructors (C# 12) put parameters in scope for the whole class.
var greeter = new Greeter("Ada");
var tickets = new TicketCounter(100);

Check(greeter.Greet() == "Hello, Ada", "a primary constructor parameter is visible in members");
Check(tickets.Next() == 101 && tickets.Next() == 102, "a captured parameter is mutable state");

// Level 3: init and required properties.
var settings = new Settings { Host = "example.com" };

Check(settings.Host == "example.com" && settings.Port == 443, "required is set once; Port keeps its default");

// Level 4: the field keyword (C# 14) adds a rule without a hand-written backing field.
var room = new Thermostat { Celsius = 21.5 };

Check(Rejects(() => room.Celsius = -300) && room.Celsius == 21.5, "the setter rejects -300 and keeps 21.5");

// Level 5: records compare by value, print themselves, copy with `with` and deconstruct.
var price = new Money(5m, "GBP");
var same = new Money(5m, "GBP");
var raised = price with { Amount = 7.5m };   // a copy with one property changed
var (amount, currency) = raised;             // positional records deconstruct

Check(price == same && !ReferenceEquals(price, same), "records compare values, not identity");
Check(price.ToString() == "Money { Amount = 5, Currency = GBP }", "ToString lists the properties");
Check(price.Amount == 5m && amount == 7.5m && currency == "GBP", "with copies; the original is unchanged");

var fee = new Fee(5m, "GBP");
Check(price != fee && !price.Equals(fee), "a derived record is never equal to its base");

// Level 6: record structs (C# 10) are values with generated equality.
var cell = new GridPoint(1, 2);
cell.X = 5;                                  // record struct properties are read-write
var london = new Coordinate(51.5, -0.1);

Check(cell == new GridPoint(5, 2), "record structs get == for free");
Check(london == new Coordinate(51.5, -0.1), "readonly record structs too");

// Level 7: the mutable-struct copy trap.
var holder = new Holder();
holder.Counter.Increment();  // increments a temporary copy returned by the getter
holder.Frozen.Increment();   // increments a defensive copy of the readonly field
holder.Plain.Increment();    // a plain field is a variable: this one really changes

Check(holder.Counter.Count == 0 && holder.Frozen.Count == 0 && holder.Plain.Count == 1,
    "only a call on a variable changes the stored struct");

// Underneath: what the compiler generated.
var hidden = BindingFlags.NonPublic | BindingFlags.Instance;
Check(typeof(TicketCounter).GetField("<last>P", hidden) is not null, "the captured parameter became a field");
Check(typeof(Thermostat).GetField("<Celsius>k__BackingField", hidden) is not null, "field is the auto-property's field");
Check(typeof(Money).GetMethod("<Clone>$") is not null, "with calls the generated <Clone>$ method");
var contract = typeof(Money).GetProperty("EqualityContract", hidden)!.GetValue(price);
Check(contract is Type type && type == typeof(Money), "EqualityContract is the record's own type");

var host = typeof(Settings).GetProperty(nameof(Settings.Host))!;
Check(host.SetMethod!.ReturnParameter.GetRequiredCustomModifiers().Contains(typeof(IsExternalInit)),
    "init is a set accessor marked with IsExternalInit");
Check(host.IsDefined(typeof(RequiredMemberAttribute)), "required is recorded as RequiredMemberAttribute");

// Interview: with makes a shallow copy.
var basket = new Basket(["apple"]);
var copy = basket with { };      // a new Basket that shares the same list
copy.Items.Add("pear");

Check(basket.Items.Count == 2 && basket == copy, "with copies the reference, not the list");

Console.WriteLine("OK");

static bool Rejects(Action change)
{
    try
    {
        change();
        return false;
    }
    catch (ArgumentOutOfRangeException)
    {
        return true;
    }
}

static void Check(bool ok, string what)
{
    if (!ok) throw new Exception(what);
}

class Account
{
    public string Owner { get; }
    public decimal Balance { get; private set; }

    public Account(string owner, decimal opening)
    {
        ArgumentOutOfRangeException.ThrowIfNegative(opening);
        Owner = owner;
        Balance = opening;
    }

    public Account(string owner) : this(owner, 0m) { }

    public void Deposit(decimal amount) => Balance += amount;
}

class Greeter(string name)
{
    public string Greet() => $"Hello, {name}"; // name is in scope in every member
}

class TicketCounter(int last)
{
    public int Next() => ++last;               // captured, so the parameter lives on and changes
}

class Settings
{
    public required string Host { get; init; } // must be set when the object is created, then frozen
    public int Port { get; init; } = 443;      // optional, with a default
}

class Thermostat
{
    public double Celsius
    {
        get;
        set => field = value >= -273.15 ? value : throw new ArgumentOutOfRangeException(nameof(value));
    }
}

record Money(decimal Amount, string Currency);

record Fee(decimal Amount, string Currency) : Money(Amount, Currency);

record struct GridPoint(int X, int Y);                     // properties are { get; set; }

readonly record struct Coordinate(double Lat, double Lon); // properties are { get; init; }

struct MutableCounter
{
    public int Count;

    public void Increment() => Count++;
}

class Holder
{
    public MutableCounter Counter { get; } = new(); // a property: every read returns a copy
    public readonly MutableCounter Frozen = new();  // a readonly field: a value outside the constructor
    public MutableCounter Plain = new();            // a plain field: a variable
}

record Basket(List<string> Items);
