// Run from programming-languages/go/labs with: go run -race ./interfaces
package main

import (
	"bytes"
	"errors"
	"fmt"
	"io"
	"strings"
	"testing"
	"unsafe"
)

// Level 1: a type satisfies an interface just by having its methods.
type Shape interface {
	Area() int
}

type Rect struct{ W, H int }

type Square struct{ Side int }

func (r Rect) Area() int { return r.W * r.H }

func (s Square) Area() int { return s.Side * s.Side }

// Compile-time checks: the build fails if a type stops satisfying Shape.
var _ Shape = Rect{}
var _ Shape = Square{}

func totalArea(shapes []Shape) int {
	total := 0
	for _, s := range shapes {
		total += s.Area() // each value runs its own Area
	}
	return total
}

// Level 2: a pointer receiver puts the method in *T's method set only.
type Resetter interface {
	Reset()
}

type Score struct{ points int }

func (s *Score) Reset() { s.points = 0 }

var _ Resetter = (*Score)(nil) // *Score has Reset; Score does not

// Level 3: an interface value is a (type, value) pair.
func dynamicTypes() []string {
	var s Shape // (nil, nil)
	types := []string{fmt.Sprintf("%T", s)}
	s = Rect{3, 4} // (Rect, {3 4})
	types = append(types, fmt.Sprintf("%T", s))
	s = &Square{2} // (*Square, a pointer): *Square has Area too
	types = append(types, fmt.Sprintf("%T", s))
	return types
}

// Level 4: a nil pointer inside an interface is not a nil interface.
type NotFoundError struct{ Name string }

func (e *NotFoundError) Error() string { return e.Name + " not found" }

func findBad(name string) error {
	var err *NotFoundError // a nil pointer
	if name == "" {
		err = &NotFoundError{Name: "(empty)"}
	}
	return err // (*NotFoundError, nil): NOT a nil interface
}

func findGood(name string) error {
	if name == "" {
		return &NotFoundError{Name: "(empty)"}
	}
	return nil // (nil, nil): a true nil interface
}

// Level 5: type assertions and type switches.
func describe(v any) string {
	switch x := v.(type) {
	case nil:
		return "nil"
	case int:
		return fmt.Sprintf("int %d", x)
	case string:
		return fmt.Sprintf("string of %d bytes", len(x))
	case fmt.Stringer:
		return "Stringer: " + x.String()
	default:
		return fmt.Sprintf("other %T", x)
	}
}

// Level 6: small interfaces from the standard library.
func (r Rect) String() string { return fmt.Sprintf("%dx%d rectangle", r.W, r.H) }

// report accepts the smallest interface it needs: anything with a Write method.
func report(w io.Writer, shapes []Shape) error {
	_, err := fmt.Fprintf(w, "total area %d\n", totalArea(shapes))
	return err
}

func countBytes(r io.Reader) (int64, error) {
	return io.Copy(io.Discard, r) // calls r.Read until it returns io.EOF
}

var _ io.ReadWriter = (*bytes.Buffer)(nil) // ReadWriter = Reader + Writer

// Underneath: storing a non-pointer value in an interface may allocate.
var sink any

func allocsToBox[T any](v T) float64 {
	return testing.AllocsPerRun(100, func() { sink = v })
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
	shapes := []Shape{Rect{3, 4}, Square{2}}
	check(totalArea(shapes) == 16, "3*4 + 2*2 = 16")

	score := &Score{points: 7}
	var r Resetter = score // *Score satisfies Resetter
	r.Reset()
	check(score.points == 0, "Reset through the interface changes the original")

	check(fmt.Sprint(dynamicTypes()) == "[<nil> main.Rect *main.Square]", "dynamic types")
	check(Shape(Rect{3, 4}) == Shape(Rect{3, 4}), "same type, equal values")
	check(Shape(Rect{2, 6}) != Shape(Rect{3, 4}), "equal areas, different values")
	check(any(1) != any(int64(1)), "different dynamic types are never equal")
	check(panicMessage(func() { _ = any([]int{1}) == any([]int{1}) }) ==
		"runtime error: comparing uncomparable type []int", "comparing slices inside any panics")

	bad := findBad("ann")
	check(bad != nil, "the trap: a nil *NotFoundError makes a non-nil error")
	check(fmt.Sprintf("%T", bad) == "*main.NotFoundError", "the type half is set")
	check(findGood("ann") == nil, "return nil explicitly")
	var notFound *NotFoundError
	check(errors.As(findGood(""), &notFound) && notFound.Name == "(empty)", "errors.As")

	check(describe(nil) == "nil" && describe(42) == "int 42", "type switch: nil, int")
	check(describe("héllo") == "string of 6 bytes", "type switch: string")
	check(describe(Rect{3, 4}) == "Stringer: 3x4 rectangle", "type switch: interface case")
	check(describe(3.5) == "other float64", "type switch: default")

	var v any = "hello"
	n, ok := v.(int) // comma-ok: no panic on a wrong guess
	check(n == 0 && !ok && v.(string) == "hello", "assertions")
	check(panicMessage(func() { _ = v.(int) }) ==
		"interface conversion: interface {} is string, not int", "a failed assertion panics")
	_, isStringer := v.(fmt.Stringer) // asserting to an interface checks the method set
	check(!isStringer, "string has no String method")

	var buf bytes.Buffer
	var sb strings.Builder
	check(report(&buf, shapes) == nil && buf.String() == "total area 16\n", "*bytes.Buffer is a Writer")
	check(report(&sb, shapes) == nil && sb.String() == "total area 16\n", "*strings.Builder too")
	size, err := countBytes(strings.NewReader("hello"))
	check(size == 5 && err == nil, "io.Copy reads 5 bytes")
	check(fmt.Sprint(Rect{3, 4}) == "3x4 rectangle", "fmt uses String")

	check(unsafe.Sizeof(sink) == 16 && unsafe.Sizeof(r) == 16, "two words: type and data")
	rect := Rect{3, 4}
	check(allocsToBox(rect) == 1 && allocsToBox(&rect) == 0, "a value is boxed; a pointer is not")
	fmt.Println("OK")
}
