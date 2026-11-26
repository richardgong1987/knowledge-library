// Run from programming-languages/go/labs with: go run -race ./memory-and-garbage-collection
// See escape analysis with: go build -gcflags=-m ./memory-and-garbage-collection
package main

import (
	"bytes"
	"fmt"
	"maps"
	"runtime"
	"runtime/debug"
	"slices"
	"sync"
	"testing"
	"time"
	"weak"
)

// Level 1: stack or heap? Escape analysis decides.
type point struct{ x, y int }

//go:noinline
func sumPoint(x, y int) int {
	p := &point{x, y} // a pointer, but it never leaves the function: stack
	return p.x + p.y
}

//go:noinline
func newPoint(x, y int) *point {
	return &point{x, y} // the pointer outlives the call: moved to the heap
}

var sink *point

func allocsPerCall() (stack, heap float64) {
	stack = testing.AllocsPerRun(100, func() { _ = sumPoint(1, 2) })
	heap = testing.AllocsPerRun(100, func() { sink = newPoint(1, 2) })
	return stack, heap
}

// Level 2: the collector frees what the roots cannot reach.
func mark(edges map[string][]string, roots ...string) []string {
	black := map[string]bool{}  // scanned: reachable
	grey := slices.Clone(roots) // found but not yet scanned
	for len(grey) > 0 {
		object := grey[len(grey)-1]
		grey = grey[:len(grey)-1]
		if black[object] {
			continue
		}
		black[object] = true
		grey = append(grey, edges[object]...)
	}
	return slices.Sorted(maps.Keys(black)) // everything else is white: garbage
}

// Level 3: GOGC sets the heap target.
func heapTarget(liveHeap, roots, gogc float64) float64 {
	return liveHeap + (liveHeap+roots)*gogc/100
}

func setAndRestore() (gogc int, limit int64) {
	old := debug.SetGCPercent(50) // like GOGC=50
	gogc = debug.SetGCPercent(old)
	oldLimit := debug.SetMemoryLimit(64 << 20) // like GOMEMLIMIT=64MiB
	limit = debug.SetMemoryLimit(oldLimit)
	return gogc, limit
}

func forcedCycles() uint32 {
	var before, after runtime.MemStats
	runtime.ReadMemStats(&before)
	runtime.GC() // blocks until a full cycle has finished
	runtime.ReadMemStats(&after)
	return after.NumGC - before.NumGC
}

// Level 4: sync.Pool reuses objects between garbage collections.
var bufferPool = sync.Pool{
	New: func() any { return new(bytes.Buffer) },
}

func greet(name string) string {
	buf := bufferPool.Get().(*bytes.Buffer)
	defer bufferPool.Put(buf)
	buf.Reset() // a pooled buffer may still hold an earlier caller's bytes
	buf.WriteString("hello, ")
	buf.WriteString(name)
	return buf.String()
}

// Level 5: weak pointers and cleanups.
type blob struct{ data [64]byte }

func newWeakBlob() (weak.Pointer[blob], bool) {
	strong := &blob{}
	w := weak.Make(strong)
	return w, w.Value() == strong // alive: strong still points to it
}

func weakPointer() (whileHeld, afterGC bool) {
	w, whileHeld := newWeakBlob()
	runtime.GC() // no strong pointer to the blob is left anywhere
	return whileHeld, w.Value() == nil
}

type file struct{ name string }

func openAndForget(closed chan<- string) {
	f := &file{name: "data.csv"}
	runtime.AddCleanup(f, func(name string) { closed <- name }, f.name)
} // f is unreachable once this returns

func cleanupRuns() string {
	closed := make(chan string, 1)
	openAndForget(closed)
	runtime.GC() // finds the unreachable file and queues its cleanup
	select {
	case name := <-closed:
		return name
	case <-time.After(5 * time.Second):
		return "cleanup did not run"
	}
}

func check(ok bool, what string) {
	if !ok {
		panic(what)
	}
}

func main() {
	stack, heap := allocsPerCall()
	check(stack == 0 && heap == 1, "0 allocations on the stack, 1 on the heap")
	check(sumPoint(1, 2) == 3 && *newPoint(1, 2) == point{1, 2}, "both compute the same point")

	edges := map[string][]string{
		"root": {"A"},
		"A":    {"B"},
		"C":    {"A"}, // C points to A, but nothing points to C
		"D":    {"E"}, // D and E point to each other: a cycle
		"E":    {"D"}, // that no root reaches
	}
	check(slices.Equal(mark(edges, "root"), []string{"A", "B", "root"}), "only root, A and B survive")

	check(heapTarget(8, 2, 100) == 18, "8 + (8 + 2) x 100/100 = 18 MiB")
	check(heapTarget(8, 2, 50) == 13, "8 + (8 + 2) x 50/100 = 13 MiB")
	check(heapTarget(8, 2, 200) == 28, "8 + (8 + 2) x 200/100 = 28 MiB")
	gogc, limit := setAndRestore()
	check(gogc == 50 && limit == 64<<20, "the setters return the previous value")
	check(forcedCycles() >= 1, "runtime.GC runs at least one cycle")

	check(greet("gopher") == "hello, gopher", "a pooled buffer is reset before use")
	check(greet("Ada") == "hello, Ada", "and reused safely")

	whileHeld, afterGC := weakPointer()
	check(whileHeld && afterGC, "a weak pointer works until the object is collected")
	check(cleanupRuns() == "data.csv", "the cleanup ran after the file became unreachable")
	fmt.Println("OK")
}
