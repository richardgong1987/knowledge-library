// Run from programming-languages/go/labs with: go run -race ./generics
package main

import (
	"cmp"
	"fmt"
	"strconv"
)

// The problem: the same loop written twice, once per type.
func SumInts(xs []int) int {
	total := 0
	for _, x := range xs {
		total += x
	}
	return total
}

func SumFloats(xs []float64) float64 {
	total := 0.0
	for _, x := range xs {
		total += x
	}
	return total
}

// Level 1: one function with a type parameter T.
func Sum[T int | float64](xs []T) T {
	var total T // the zero value of T: 0 for int, 0.0 for float64
	for _, x := range xs {
		total += x
	}
	return total
}

// Level 2: a constraint is a set of types; ~float64 admits every type built on float64.
type Number interface {
	~int | ~int64 | ~float64
}

type Celsius float64

func SumOf[T Number](xs []T) T {
	var total T
	for _, x := range xs {
		total += x
	}
	return total
}

// Level 3: comparable permits ==, cmp.Ordered permits < and >.
func Index[T comparable](xs []T, want T) int {
	for i, x := range xs {
		if x == want {
			return i
		}
	}
	return -1
}

func Largest[T cmp.Ordered](xs []T) T {
	best := xs[0]
	for _, x := range xs[1:] {
		if x > best {
			best = x
		}
	}
	return best
}

// Level 4: a generic type. Stack[int] and Stack[string] are two different types.
type Stack[T any] struct {
	items []T
}

func (s *Stack[T]) Push(v T) {
	s.items = append(s.items, v)
}

func (s *Stack[T]) Pop() (T, bool) {
	var zero T
	if len(s.items) == 0 {
		return zero, false
	}
	top := s.items[len(s.items)-1]
	s.items = s.items[:len(s.items)-1]
	return top, true
}

// Level 5: Go 1.27 lets a method declare its own type parameter U.
func (s *Stack[T]) Map[U any](f func(T) U) *Stack[U] {
	out := &Stack[U]{}
	for _, v := range s.items {
		out.Push(f(v))
	}
	return out
}

// Level 5: Go 1.24 lets an alias have type parameters. Set[string] IS map[string]struct{}.
type Set[T comparable] = map[T]struct{}

// A defined type, for contrast: a new, distinct type.
type DefinedSet[T comparable] map[T]struct{}

// Common mistake: comparable admits interface types, and == on them can panic.
func compareSlicesInAny() (message string) {
	defer func() { message = fmt.Sprint(recover()) }()
	Index([]any{[]int{1}}, any([]int{1}))
	return "no panic"
}

func check(ok bool, what string) {
	if !ok {
		panic(what)
	}
}

func main() {
	check(SumInts([]int{1, 2, 3}) == 6, "SumInts 1+2+3 = 6")
	check(SumFloats([]float64{1.5, 2.5}) == 4, "SumFloats 1.5+2.5 = 4")

	check(Sum([]int{1, 2, 3}) == 6, "Sum infers T = int")
	check(Sum([]float64{1.5, 2.5}) == 4, "Sum infers T = float64")
	check(Sum[float64](nil) == 0, "explicit instantiation; empty input gives the zero value")

	check(SumOf([]Celsius{20, 21.5}) == 41.5, "Celsius is in the type set of ~float64")
	check(SumOf([]float64{0.5, 0.25}) == 0.75, "float64 itself is in the type set")
	check(SumOf([]int64{1 << 40, 1}) == 1<<40+1, "int64 is in the type set too")

	check(Index([]string{"go", "rust", "zig"}, "zig") == 2, "Index finds zig at 2")
	check(Index([]int{4, 5}, 9) == -1, "Index reports -1 when absent")
	check(Largest([]string{"pear", "apple", "plum"}) == "plum", "strings order by bytes")
	check(Largest([]float64{2.5, -1, 9}) == 9, "Largest of floats")

	var s Stack[int]
	s.Push(1)
	s.Push(2)
	s.Push(3)
	top, ok := s.Pop()
	check(top == 3 && ok, "a stack pops the last push first")
	labels := s.Map(strconv.Itoa)
	label, _ := labels.Pop()
	check(label == "2", "Map turns Stack[int] into Stack[string]")
	var none Stack[string]
	empty, ok := none.Pop()
	check(empty == "" && !ok, "popping an empty stack gives (zero value, false)")

	check(fmt.Sprintf("%T", Set[string]{}) == "map[string]struct {}", "an alias is the same type")
	check(fmt.Sprintf("%T", DefinedSet[string]{}) == "main.DefinedSet[string]", "a defined type is new")

	check(compareSlicesInAny() == "runtime error: comparing uncomparable type []int", "== on any can panic")
	fmt.Println("OK")
}
