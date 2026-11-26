// Run from programming-languages/go/labs with: go run -race ./functions-closures-and-iterators
package main

import (
	"fmt"
	"iter"
	"maps"
	"runtime"
	"slices"
)

// Level 1: a function is a value, and its signature is its type.
type Op func(int, int) int

func add(a, b int) int { return a + b }

func apply(op Op, a, b int) int {
	return op(a, b)
}

func sum(nums ...int) int { // variadic: nums is a []int
	total := 0
	for _, n := range nums {
		total += n
	}
	return total
}

// Level 2: a closure keeps the variables it uses alive, and shares them.
func counter() func() int {
	count := 0
	return func() int {
		count++ // the same variable on every call, not a copy
		return count
	}
}

// Level 3: Go 1.22 gives each loop iteration its own variable.
func callAll(funcs []func() int) []int {
	var got []int
	for _, f := range funcs {
		got = append(got, f())
	}
	return got
}

func perIteration() []int {
	var funcs []func() int
	for i := 0; i < 3; i++ {
		funcs = append(funcs, func() int { return i }) // a new i each iteration
	}
	return callAll(funcs)
}

func sharedVariable() []int {
	var funcs []func() int
	i := 0 // one variable for the whole loop: what every loop did before Go 1.22
	for i = 0; i < 3; i++ {
		funcs = append(funcs, func() int { return i })
	}
	return callAll(funcs)
}

// Level 4: method values bind a receiver; method expressions take it as an argument.
type Counter struct{ n int }

func (c *Counter) Inc() { c.n++ }

type Point struct{ X int }

func (p Point) GetX() int { return p.X }

// Level 5: a range-over-func iterator pushes each value to yield.
func Countdown(n int) iter.Seq[int] {
	return func(yield func(int) bool) {
		for i := n; i >= 1; i-- {
			if !yield(i) {
				return // the loop body said stop
			}
		}
	}
}

func Squares() iter.Seq[int] { // infinite: safe because the loop can stop it
	return func(yield func(int) bool) {
		for n := 1; ; n++ {
			if !yield(n * n) {
				return
			}
		}
	}
}

func firstSquaresAbove(limit, count int) []int {
	var out []int
	for sq := range Squares() {
		if sq <= limit {
			continue
		}
		out = append(out, sq)
		if len(out) == count {
			break // yield returns false, and Squares returns
		}
	}
	return out
}

// Common mistake: an iterator that ignores yield's false result.
func badCountdown(n int) iter.Seq[int] {
	return func(yield func(int) bool) {
		for i := n; i >= 1; i-- {
			yield(i) // wrong: keeps going after the loop said stop
		}
	}
}

func breakEarly(seq iter.Seq[int]) (message string) {
	defer func() { message = fmt.Sprint(recover()) }()
	for range seq {
		break
	}
	return "no panic"
}

// Level 6: iter.Pull turns a push iterator into next and stop functions.
func Equal(a, b iter.Seq[int]) bool {
	nextA, stopA := iter.Pull(a)
	defer stopA()
	nextB, stopB := iter.Pull(b)
	defer stopB()
	for {
		va, okA := nextA()
		vb, okB := nextB()
		if okA != okB || va != vb {
			return false
		}
		if !okA {
			return true // both ended at the same time
		}
	}
}

func check(ok bool, what string) {
	if !ok {
		panic(what)
	}
}

func main() {
	check(apply(add, 3, 4) == 7, "apply(add, 3, 4) = 7")
	check(apply(func(a, b int) int { return a * b }, 3, 4) == 12, "a function literal is a value too")
	check(sum() == 0 && sum(1, 2, 3) == 6, "variadic sum")
	xs := []int{4, 5}
	check(sum(xs...) == 9, "xs... passes a slice as the variadic argument")

	next := counter()
	check(next() == 1 && next() == 2 && next() == 3, "a closure keeps its count")
	other := counter()
	check(other() == 1, "each call to counter makes a new variable")

	check(slices.Equal(perIteration(), []int{0, 1, 2}), "Go 1.22+: one variable per iteration")
	check(slices.Equal(sharedVariable(), []int{3, 3, 3}), "one shared variable: every closure sees 3")

	var c Counter
	inc := c.Inc // method value: the receiver &c is bound now
	inc()
	inc()
	check(c.n == 2, "the method value changed c")
	incFn := (*Counter).Inc // method expression: func(*Counter)
	incFn(&c)
	check(c.n == 3, "the method expression takes the receiver as its first argument")

	p := Point{X: 1}
	getX := p.GetX // value receiver: p is copied now
	p.X = 2
	check(getX() == 1, "the method value kept the copy with X = 1")

	var down []int
	for v := range Countdown(3) {
		down = append(down, v)
	}
	check(slices.Equal(down, []int{3, 2, 1}), "range over Countdown(3)")
	check(slices.Equal(slices.Collect(Countdown(3)), []int{3, 2, 1}), "slices.Collect")

	var taken []int
	for v := range Countdown(5) {
		taken = append(taken, v)
		if len(taken) == 2 {
			break
		}
	}
	check(slices.Equal(taken, []int{5, 4}), "break after k = 2 of n = 5 values")
	check(slices.Equal(firstSquaresAbove(10, 3), []int{16, 25, 36}), "first 3 squares above 10")

	ages := map[string]int{"bob": 31, "ann": 29, "cid": 40}
	check(slices.Equal(slices.Sorted(maps.Keys(ages)), []string{"ann", "bob", "cid"}), "maps.Keys")
	var backward []string
	for i, s := range slices.Backward([]string{"a", "b", "c"}) {
		backward = append(backward, fmt.Sprint(i, s))
	}
	check(slices.Equal(backward, []string{"2c", "1b", "0a"}), "slices.Backward is an iter.Seq2")

	check(breakEarly(badCountdown(3)) ==
		"runtime error: range function continued iteration after function for loop body returned false",
		"the runtime catches an iterator that ignores false")

	check(Equal(Countdown(3), slices.Values([]int{3, 2, 1})), "equal sequences")
	check(!Equal(Countdown(3), Countdown(2)), "different lengths")

	before := runtime.NumGoroutine()
	pullNext, stop := iter.Pull(Countdown(2))
	first, ok := pullNext()
	check(first == 2 && ok, "next returns the first value")
	check(runtime.NumGoroutine() == before+1, "Pull runs the iterator on its own goroutine")
	stop()
	check(runtime.NumGoroutine() == before, "stop ends that goroutine")
	_, ok = pullNext()
	check(!ok, "after stop, next reports no more values")
	fmt.Println("OK")
}
