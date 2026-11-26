// Run from programming-languages/csharp/labs with: dotnet run inheritance-and-interfaces.cs
using System.Reflection;

static void Check(bool ok, string what)
{
    if (!ok)
    {
        throw new Exception(what);
    }
}

// The problem: one loop adds up the areas of different shapes.
Shape[] shapes = [new Circle(1), new Square(2)];
double total = 0;
foreach (Shape shape in shapes)
{
    total += shape.Area(); // runs Circle.Area or Square.Area
}
Check(Math.Abs(total - (Math.PI + 4)) < 1e-12, "pi + 4 is about 7.14");

// Level 1: a derived class inherits members and can call the base version.
var tile = new LabelledSquare(2, "tile");
Check(tile.Describe() == "square: area 4.00 [tile]", "base.Describe() runs first");
Check(tile.Side == 2, "Side is inherited from Square");

// Level 2: override follows the object; new follows the variable.
Dog rex = new Dog();
Animal sameDog = rex; // same object, static type Animal
Check(sameDog.Sound() == "Woof", "virtual + override: the runtime type decides");
Check(sameDog.Kind() == "animal", "new: the static type decides");
Check(rex.Kind() == "dog", "the hiding method is visible through Dog");

// Level 3: abstract cannot be created; sealed cannot be extended.
Check(typeof(Shape).IsAbstract, "Shape is abstract");
Check(typeof(Circle).IsSealed, "Circle is sealed");

// Level 4: an interface is a contract; explicit implementation hides a member.
var ruler = new Ruler();
Check(((ICentimetres)ruler).Length == 30, "the centimetre view");
Check(Math.Round(((IInches)ruler).Length, 2) == 11.81, "the inch view");

// Level 5: a default interface method is used when the class has none.
IGreeter robot = new Robot();
IGreeter butler = new Butler();
Check(robot.Greet() == "Hello, I am R2", "the interface's default body");
Check(butler.Greet() == "Good evening, I am Jeeves", "the class's own body wins");

// Level 6: a static abstract member is called on a type parameter.
static T[] MakeThree<T>() where T : IFromSize<T> => [T.FromSize(1), T.FromSize(2), T.FromSize(3)];

Check(MakeThree<Square>().Sum(s => s.Area()) == 14, "1 + 4 + 9 = 14");

// Level 7: Liskov. A subclass must keep the base class's promises.
static double Stretch(Rectangle r)
{
    r.Width = 2;
    r.Height = 3;
    return r.Area; // every caller of a Rectangle expects 2 * 3 = 6
}

Check(Stretch(new Rectangle()) == 6, "a rectangle keeps both sides");
Check(Stretch(new SquareRectangle()) == 9, "the square broke the promise");

// Underneath: virtual introduces a slot, override reuses it.
MethodInfo introduced = typeof(Animal).GetMethod("Sound")!;
MethodInfo overriding = typeof(Dog).GetMethod("Sound")!;
Check(introduced.Attributes.HasFlag(MethodAttributes.NewSlot), "virtual gets a new slot");
Check(!overriding.Attributes.HasFlag(MethodAttributes.NewSlot), "override reuses the slot");
Check(overriding.GetBaseDefinition().DeclaringType == typeof(Animal), "the slot belongs to Animal");
Check(!typeof(Dog).GetMethod("Kind")!.IsVirtual, "new Kind() is a separate, non-virtual method");

// Interview: a struct converted to an interface is boxed, so the call mutates a copy.
var counter = new Counter();
ICounter boxed = counter; // boxing copies the struct onto the heap
boxed.Increment();
Check(counter.Count == 0 && boxed.Count == 1, "the box changed, the original did not");

Console.WriteLine("OK");

abstract class Shape
{
    public abstract double Area(); // no body: every concrete shape must supply one
    public virtual string Name => "shape";
    public virtual string Describe() => $"{Name}: area {Area():F2}";
}

sealed class Circle(double radius) : Shape
{
    public override double Area() => Math.PI * radius * radius;
    public override string Name => "circle";
}

interface IFromSize<TSelf> where TSelf : IFromSize<TSelf>
{
    static abstract TSelf FromSize(double size); // each type supplies its own factory
}

class Square(double side) : Shape, IFromSize<Square>
{
    public double Side => side;
    public override double Area() => side * side;
    public override string Name => "square";
    public static Square FromSize(double size) => new(size);
}

class LabelledSquare(double side, string label) : Square(side)
{
    public override string Describe() => $"{base.Describe()} [{label}]";
}

class Animal
{
    public virtual string Sound() => "...";
    public string Kind() => "animal";
}

class Dog : Animal
{
    public override string Sound() => "Woof"; // replaces Animal's slot
    public new string Kind() => "dog";        // a second method that hides the first
}

interface ICentimetres
{
    double Length { get; }
}

interface IInches
{
    double Length { get; }
}

class Ruler : ICentimetres, IInches
{
    double ICentimetres.Length => 30;
    double IInches.Length => 30 / 2.54;
}

interface IGreeter
{
    string Name { get; }
    string Greet() => $"Hello, I am {Name}"; // default body: implementers may skip it
}

class Robot : IGreeter
{
    public string Name => "R2";
}

class Butler : IGreeter
{
    public string Name => "Jeeves";
    public string Greet() => $"Good evening, I am {Name}";
}

class Rectangle
{
    public virtual double Width { get; set; }
    public virtual double Height { get; set; }
    public double Area => Width * Height;
}

class SquareRectangle : Rectangle
{
    public override double Width
    {
        get => base.Width;
        set { base.Width = value; base.Height = value; }
    }

    public override double Height
    {
        get => base.Height;
        set { base.Width = value; base.Height = value; }
    }
}

interface ICounter
{
    int Count { get; }
    void Increment();
}

struct Counter : ICounter
{
    public int Count { get; private set; }
    public void Increment() => Count++;
}
