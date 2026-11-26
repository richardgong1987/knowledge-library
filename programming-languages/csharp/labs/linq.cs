// Run from programming-languages/csharp/labs with: dotnet run linq.cs
using System.Linq.Expressions;

Order[] orders =
[
    new(1, "Ann", 30),
    new(2, "Bob", 12),
    new(3, "Ann", 8),
    new(4, "Cid", 50),
    new(5, "Bob", 20),
];
Customer[] customers = [new("Ann", "Leeds"), new("Bob", "York"), new("Dee", "Bath")];

// Level 1: an operator is an ordinary static method; the dot is syntax sugar.
var bigByDot = orders.Where(o => o.Amount > 10);
var bigByCall = Enumerable.Where(orders, o => o.Amount > 10);
Check(bigByDot.SequenceEqual(bigByCall), "the two calls are the same call");
Check(bigByDot.Select(o => o.Id).SequenceEqual([1, 2, 4, 5]), "orders over 10 are 1, 2, 4, 5");

Check(orders.CountOver(25) == 2, "classic extension method: 30 and 50 are over 25");
Check(orders.Total == 120, "extension property: 30 + 12 + 8 + 50 + 20 = 120");
Check(orders.Over(25).Select(o => o.Id).SequenceEqual([1, 4]), "extension method from an extension block");

// Level 2: query syntax is translated into the method calls below.
var queryIds = from o in orders
               where o.Amount > 10
               orderby o.Amount descending
               select o.Id;
var methodIds = orders.Where(o => o.Amount > 10)
                      .OrderByDescending(o => o.Amount)
                      .Select(o => o.Id);
Check(queryIds.SequenceEqual([4, 1, 5, 2]), "query syntax: 50, 30, 20, 12");
Check(queryIds.SequenceEqual(methodIds), "both spellings compile to the same calls");

// Level 3: deferred execution. Building the query runs nothing.
int calls = 0;
var big = orders.Where(o => { calls++; return o.Amount > 10; });
Check(calls == 0, "nothing has run yet");
int firstCount = big.Count();  // runs the filter over 5 orders
int secondCount = big.Count(); // runs the whole filter again
Check(firstCount == 4 && secondCount == 4 && calls == 10, "two enumerations cost 2 x 5 = 10 calls");

calls = 0;
List<Order> snapshot = big.ToList(); // immediate: runs once, stores the results
Check(snapshot.Count == 4 && snapshot.Count == 4 && calls == 5, "ToList runs the filter 5 times, once");

var numbers = new List<int> { 1, 2, 3 };
var doubled = numbers.Select(n => n * 2);
numbers.Add(4); // the query has not run yet, so it will see this
Check(doubled.SequenceEqual([2, 4, 6, 8]), "a deferred query reads the source when enumerated");

// Level 4: grouping, folding and joining.
var totals = orders
    .GroupBy(o => o.Customer)
    .Select(g => (Customer: g.Key, Total: g.Sum(o => o.Amount)))
    .ToList();
Check(totals.SequenceEqual([("Ann", 38), ("Bob", 32), ("Cid", 50)]), "groups keep first-seen key order");

int total = orders.Aggregate(0, (sum, o) => sum + o.Amount);
Check(total == 120, "Aggregate folds 0 -> 30 -> 42 -> 50 -> 100 -> 120");

var withCity = orders.Join(customers,
    o => o.Customer, c => c.Name,
    (o, c) => $"{o.Id}:{c.City}");
Check(withCity.SequenceEqual(["1:Leeds", "2:York", "3:Leeds", "5:York"]), "inner join drops order 4 (Cid)");

// Level 5: newer operators. CountBy, AggregateBy and Index arrived in .NET 9.
var counts = orders.CountBy(o => o.Customer).ToDictionary();
var sums = orders.AggregateBy(o => o.Customer, seed: 0, (sum, o) => sum + o.Amount).ToDictionary();
var numbered = orders.Index().Select(pair => $"{pair.Index}:{pair.Item.Id}");
Check(counts["Ann"] == 2 && counts["Bob"] == 2 && counts["Cid"] == 1, "CountBy");
Check(sums["Ann"] == 38 && sums["Bob"] == 32 && sums["Cid"] == 50, "AggregateBy");
Check(numbered.First() == "0:1" && numbered.Last() == "4:5", "Index pairs a position with each item");

// LeftJoin and RightJoin arrived in .NET 10.
var everyOrder = orders.LeftJoin(customers,
    o => o.Customer, c => c.Name,
    (o, c) => $"{o.Id}:{c?.City ?? "?"}");
var everyCustomer = orders.RightJoin(customers,
    o => o.Customer, c => c.Name,
    (o, c) => $"{c.Name}:{o?.Id.ToString() ?? "-"}");
Check(everyOrder.SequenceEqual(["1:Leeds", "2:York", "3:Leeds", "4:?", "5:York"]), "LeftJoin keeps order 4");
Check(everyCustomer.SequenceEqual(["Ann:1", "Ann:3", "Bob:2", "Bob:5", "Dee:-"]), "RightJoin keeps Dee");

// Underneath: Where is an iterator, which is why it is deferred.
Check(MyWhere(orders, o => o.Amount > 10).SequenceEqual(bigByDot), "a hand-written Where gives the same result");

// IEnumerable takes a delegate (compiled code); IQueryable takes an expression tree (data).
Func<Order, bool> asCode = o => o.Amount > 10;
Expression<Func<Order, bool>> asData = o => o.Amount > 10;
Check(asCode(orders[0]), "a delegate can only be called");
Check(asData.ToString() == "o => (o.Amount > 10)", "an expression tree can be printed");
Check(asData.Body.NodeType == ExpressionType.GreaterThan, "and inspected node by node");
Check(asData.Compile()(orders[0]), "or compiled into a delegate");

IQueryable<Order> query = orders.AsQueryable().Where(o => o.Amount > 10);
Check(query.Expression.ToString() == "Order[].Where(o => (o.Amount > 10))", "the provider receives the tree");
IEnumerable<Order> inMemory = query.AsEnumerable().Where(o => o.Id < 3);
Check(inMemory is not IQueryable<Order>, "after AsEnumerable, Where is Enumerable.Where");
Check(inMemory.Select(o => o.Id).SequenceEqual([1, 2]), "orders 1 and 2");

// Common mistakes: the exact exception messages.
Check(Message(() => Array.Empty<int>().First()) == "Sequence contains no elements", "First on empty");
Check(Message(() => orders.Single(o => o.Customer == "Ann")) == "Sequence contains more than one matching element",
    "Single with two matches");
Check(Message(ModifyWhileEnumerating) == "Collection was modified; enumeration operation may not execute.",
    "changing a list inside foreach");

Console.WriteLine("OK");

static IEnumerable<T> MyWhere<T>(IEnumerable<T> source, Func<T, bool> predicate)
{
    foreach (T item in source)
    {
        if (predicate(item))
            yield return item; // runs only when the caller asks for the next item
    }
}

static void ModifyWhileEnumerating()
{
    var list = new List<int> { 1, 2, 3 };
    foreach (int n in list)
        list.Add(n);
}

static string Message(Action action)
{
    try { action(); }
    catch (InvalidOperationException e) { return e.Message; }
    return "no exception";
}

static void Check(bool ok, string what)
{
    if (!ok) throw new Exception($"check failed: {what}");
}

record Order(int Id, string Customer, int Amount);
record Customer(string Name, string City);

static class OrderExtensions
{
    // The classic form (C# 3): a static method whose first parameter is marked "this".
    public static int CountOver(this IEnumerable<Order> source, int amount) =>
        source.Count(o => o.Amount > amount);

    // The C# 14 form: an extension block can also hold properties.
    extension(IEnumerable<Order> source)
    {
        public int Total => source.Sum(o => o.Amount);
        public IEnumerable<Order> Over(int amount) => source.Where(o => o.Amount > amount);
    }
}
