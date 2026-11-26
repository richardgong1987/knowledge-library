// Run from programming-languages/go/labs with: go run -race ./structs-methods-and-embedding
package main

import (
	"encoding/json"
	"fmt"
	"reflect"
	"unsafe"
)

// Level 1: a struct groups named fields.
type Point struct {
	X, Y int
}

func literals() (Point, Point, *Point) {
	a := Point{X: 1, Y: 2} // keyed: clear, and safe if fields are reordered
	b := Point{Y: 5}       // a missing field gets its zero value
	p := &Point{3, 4}      // positional, and its address
	p.X = 30               // shorthand for (*p).X
	return a, b, p
}

// Level 2: a value receiver gets a copy; a pointer receiver gets the original.
type Counter struct {
	n int
}

func (c Counter) IncCopy() { c.n++ } // value receiver: changes a copy

func (c *Counter) Inc() { c.n++ } // pointer receiver: changes the original

func (c Counter) Value() int { return c.n }

func receivers() (int, int) {
	var c Counter
	c.IncCopy()
	c.IncCopy()
	afterCopies := c.Value() // still 0
	c.Inc()                  // c is addressable, so Go calls (&c).Inc()
	c.Inc()
	return afterCopies, c.Value()
}

// Level 3: method values and method expressions.
func methodValues() int {
	var c Counter
	inc := c.Inc // a method value: binds &c now
	inc()
	inc()
	(*Counter).Inc(&c) // a method expression: the receiver is the first argument
	return c.Value()
}

// Level 4: embedding promotes fields and methods.
type Logger struct {
	prefix string
}

func (l Logger) Log(msg string) string { return l.prefix + msg }

type Server struct {
	Logger // embedded: the field's name is Logger
	name   string
}

// Embedding is not inheritance: Greet's receiver is the inner Animal.
type Animal struct{}

func (Animal) Name() string { return "animal" }

func (a Animal) Greet() string { return "I am an " + a.Name() }

type Dog struct {
	Animal
}

func (Dog) Name() string { return "dog" } // hides Animal.Name, but does not override it

// Level 5: two embedded fields with the same name at the same depth.
type Home struct{ ID int }
type Work struct{ ID int }

type Contact struct {
	Home
	Work
}

// Level 6: tags, anonymous structs and comparison.
type User struct {
	Name  string `json:"name"`
	Email string `json:"email,omitempty"`
}

func jsonTag(field int) string {
	return reflect.TypeFor[User]().Field(field).Tag.Get("json")
}

func anonymous() (string, Point) {
	point := struct{ X, Y int }{1, 2} // a struct type with no name
	var p Point = point               // allowed: identical fields, and one side is unnamed
	return fmt.Sprintf("%+v", point), p
}

// Underneath: field order changes the size.
type Loose struct {
	A bool  // offset 0, then 7 bytes of padding
	B int64 // offset 8
	C bool  // offset 16, then 7 bytes of padding
}

type Tight struct {
	B int64 // offset 0
	A bool  // offset 8
	C bool  // offset 9, then 6 bytes of padding
}

// Interview: a struct with an interface field compiles with ==, but can panic.
type Box struct {
	Content any
}

func check(ok bool, what string) {
	if !ok {
		panic(what)
	}
}

func panicMessage(operation func()) (message string) {
	defer func() { message = fmt.Sprint(recover()) }()
	operation()
	return "no panic"
}

func main() {
	a, b, p := literals()
	check(a == Point{1, 2} && b == Point{0, 5} && *p == Point{30, 4}, "literals")

	afterCopies, afterInc := receivers()
	check(afterCopies == 0 && afterInc == 2, "copies change nothing; pointers change c")
	literal := &Counter{} // & on a composite literal is allowed
	literal.Inc()
	check(literal.Value() == 1, "a pointer to a literal has Inc")
	check(methodValues() == 3, "two calls through a method value, one through an expression")
	check(reflect.TypeFor[Counter]().NumMethod() == 2, "method set of Counter: IncCopy, Value")
	check(reflect.TypeFor[*Counter]().NumMethod() == 3, "method set of *Counter: all three")

	s := Server{Logger: Logger{prefix: "[web] "}, name: "web"}
	check(s.Log("up") == "[web] up" && s.Logger.Log("up") == "[web] up", "promoted method")
	check(s.prefix == "[web] " && s.name == "web", "promoted field")
	api := Server{prefix: "[api] "} // Go 1.27+: a promoted field as a literal key
	check(api.Logger.prefix == "[api] ", "the key sets the embedded field")
	check(reflect.TypeFor[Server]().NumMethod() == 1, "Server's method set includes Log")

	check(Dog{}.Name() == "dog", "the shallower Name wins")
	check(Dog{}.Greet() == "I am an animal", "no virtual dispatch through embedding")

	c := Contact{Home{1}, Work{2}}
	check(c.Home.ID == 1 && c.Work.ID == 2, "qualify an ambiguous name")

	encoded, err := json.Marshal(User{Name: "Ann"})
	check(err == nil && string(encoded) == `{"name":"Ann"}`, "tags rename; omitempty drops")
	check(jsonTag(0) == "name" && jsonTag(1) == "email,omitempty", "reflect reads tags")
	text, converted := anonymous()
	check(text == "{X:1 Y:2}" && converted == Point{1, 2}, "anonymous struct")

	check(Point{1, 2} == Point{1, 2} && Point{1, 2} != Point{2, 1}, "structs compare field by field")
	visits := map[Point]int{{1, 2}: 3}
	check(visits[Point{1, 2}] == 3, "a comparable struct is a map key")
	check(Box{1} == Box{1}, "comparable dynamic values compare")
	check(panicMessage(func() { _ = Box{[]int{1}} == Box{[]int{1}} }) ==
		"runtime error: comparing uncomparable type []int", "a slice inside panics")
	seen := map[string]struct{}{"go": {}} // a set: the value takes no space
	_, ok := seen["go"]
	check(ok && unsafe.Sizeof(struct{}{}) == 0, "struct{} has size 0")

	check(unsafe.Sizeof(Loose{}) == 24 && unsafe.Sizeof(Tight{}) == 16, "24 vs 16 bytes")
	check(unsafe.Offsetof(Loose{}.B) == 8 && unsafe.Offsetof(Loose{}.C) == 16, "Loose offsets")
	check(unsafe.Offsetof(Tight{}.A) == 8 && unsafe.Offsetof(Tight{}.C) == 9, "Tight offsets")
	check(unsafe.Alignof(int64(0)) == 8 && unsafe.Alignof(true) == 1, "alignments 8 and 1")
	fmt.Println("OK")
}
