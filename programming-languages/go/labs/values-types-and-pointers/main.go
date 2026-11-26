// Run from programming-languages/go/labs with: go run -race ./values-types-and-pointers
package main

import (
	"bytes"
	"fmt"
	"math"
	"unsafe"
)

// The problem: a function gets a copy of its argument.
func birthdayCopy(age int) {
	age++ // changes the copy only
}

// Level 1: every type has a zero value.
func zeroValues() (int, float64, bool, string, *int) {
	var i int
	var f float64
	var b bool
	var s string
	var p *int
	return i, f, b, s, p
}

type Point struct {
	X, Y int
}

func int8Wraps() int8 {
	var x int8 = 127
	x++ // wraps around: no error, no panic
	return x
}

// Level 2: var, :=, constants and iota.
func declare() (int, int, string) {
	var a int    // no value given: the zero value 0
	var b = 2    // type inferred from the value: int
	c := "three" // short form, only inside functions
	return a, b, c
}

func shadow(update bool) int {
	x := 1
	if update {
		x := 2 // := declares a NEW x that hides the outer one
		_ = x
	}
	return x
}

const (
	_  = iota             // iota is 0 here; the blank name throws it away
	KB = 1 << (10 * iota) // iota is 1: 1 << 10
	MB                    // the expression repeats with iota = 2: 1 << 20
	GB                    // iota = 3: 1 << 30
)

const huge = 1 << 100 // fine: constants are exact, not limited to 64 bits
const four = huge >> 98

// Level 3: assignment copies the value.
func arrayCopy() ([3]int, [3]int) {
	a := [3]int{1, 2, 3}
	b := a // copies all three elements
	b[0] = 9
	return a, b
}

func resetFirst(scores [3]int) {
	scores[0] = 0 // changes the function's own copy only
}

// Level 4: pointers.
func birthday(age *int) {
	*age++ // follow the address and change the variable there
}

func repoint(p *int) {
	other := 100
	p = &other // changes only this function's copy of the pointer
}

// Level 5: comparability.
func compare() (bool, bool, bool) {
	x, y := 1, 1
	samePoint := Point{1, 2} == Point{1, 2} // field by field
	sameArray := [2]int{1, 2} == [2]int{1, 2}
	sameAddress := &x == &y // two different variables
	return samePoint, sameArray, sameAddress
}

// Underneath: a pointer to a local variable stays valid.
func newCounter() *int {
	n := 0
	return &n // safe: the compiler moves n to the heap
}

// Interview: useful zero values.
func zeroBuffer() string {
	var buf bytes.Buffer // no constructor needed
	buf.WriteString("ready")
	return buf.String()
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
	age := 10
	birthdayCopy(age)
	check(age == 10, "a copy cannot change the caller's age")

	i, f, b, s, p := zeroValues()
	check(i == 0 && f == 0 && !b && s == "" && p == nil, "zero values 0, 0, false, \"\", nil")
	var origin Point
	check(origin == Point{X: 0, Y: 0}, "a struct's zero value has zero fields")
	check(int8Wraps() == -128, "int8 127 + 1 wraps to -128")
	check(math.MinInt8 == -128 && math.MaxInt8 == 127 && math.MaxUint8 == 255, "8-bit ranges")
	check(math.MaxInt64 == 9223372036854775807, "2^63 - 1")

	da, db, dc := declare()
	check(da == 0 && db == 2 && dc == "three", "var and := declarations")
	check(shadow(true) == 1, "the inner x shadows the outer x")
	check(KB == 1024 && MB == 1048576 && GB == 1073741824, "iota powers of 1024")
	check(four == 4, "2^100 >> 98 = 4, computed exactly")

	a, bb := arrayCopy()
	check(a == [3]int{1, 2, 3} && bb == [3]int{9, 2, 3}, "arrays copy on assignment")
	scores := [3]int{90, 80, 70}
	resetFirst(scores)
	check(scores == [3]int{90, 80, 70}, "an array argument is a copy")

	birthday(&age)
	check(age == 11, "a pointer lets the function change age")
	counter := new(int) // a fresh int variable, set to 0
	*counter += 5
	check(*counter == 5, "new(int) points at a usable zero")
	answer := new(42) // Go 1.26+: new with an initial value
	check(*answer == 42, "new(42) points at 42")
	ptr := &age
	repoint(ptr)
	check(ptr == &age && *ptr == 11, "the caller's pointer is unchanged")

	samePoint, sameArray, sameAddress := compare()
	check(samePoint && sameArray && !sameAddress, "comparison rules")
	check(map[Point]string{{1, 2}: "a"}[Point{1, 2}] == "a", "a comparable struct is a map key")

	check(newCounter() != newCounter(), "each call makes a new variable")
	check(unsafe.Sizeof(int(0)) == 8 && unsafe.Sizeof([3]int{}) == 24, "int 8, [3]int 24 bytes")
	check(unsafe.Sizeof(Point{}) == 16 && unsafe.Sizeof(ptr) == 8, "Point 16, *int 8 bytes")
	check(unsafe.Sizeof("") == 16 && unsafe.Sizeof([]int{}) == 24, "string 16, slice 24 bytes")
	check(unsafe.Sizeof([1000]int64{}) == 8000, "a big array is 8000 bytes")

	check(zeroBuffer() == "ready", "a zero bytes.Buffer is ready to use")
	var missing *Point
	check(panicMessage(func() { _ = missing.X }) ==
		"runtime error: invalid memory address or nil pointer dereference", "nil dereference panics")
	fmt.Println("OK")
}
