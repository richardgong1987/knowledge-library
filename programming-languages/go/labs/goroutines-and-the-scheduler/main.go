// Run from programming-languages/go/labs with: go run -race ./goroutines-and-the-scheduler
package main

import (
	"fmt"
	"io"
	"math"
	"os"
	"runtime"
	"runtime/pprof"
	"slices"
	"sync"
	"sync/atomic"
	"time"
)

// Level 1: go starts a goroutine; a WaitGroup waits for all of them.
func squares(n int) []int {
	results := make([]int, n)
	var wg sync.WaitGroup
	for i := range n {
		wg.Add(1)
		go func() {
			defer wg.Done()
			results[i] = i * i // each goroutine writes its own slot: no race
		}()
	}
	wg.Wait()
	return results
}

// Level 1: Go 1.25's wg.Go does Add, go and Done in one call.
func squaresGo(n int) []int {
	results := make([]int, n)
	var wg sync.WaitGroup
	for i := range n {
		wg.Go(func() { results[i] = i * i })
	}
	wg.Wait()
	return results
}

// Level 2: goroutines are cheap. Park n of them and count.
func parked(n int) (alive int, stackBytes uint64) {
	var before, during runtime.MemStats
	runtime.ReadMemStats(&before)
	release := make(chan struct{})
	var wg sync.WaitGroup
	for range n {
		wg.Go(func() { <-release })
	}
	alive = runtime.NumGoroutine()
	runtime.ReadMemStats(&during)
	close(release)
	wg.Wait()
	return alive, (during.StackInuse - before.StackInuse) / uint64(n)
}

// Level 2: a goroutine's stack grows on demand, so deep recursion just works.
func depth(n int) int {
	if n == 0 {
		return 0
	}
	return 1 + depth(n-1)
}

// Level 3: the default GOMAXPROCS on Linux since Go 1.25 (limit 0 means no cgroup limit).
func defaultProcs(cpus int, limit float64) int {
	if limit <= 0 {
		return cpus
	}
	return min(cpus, max(2, int(math.Ceil(limit))))
}

// Level 3: concurrency without parallelism. One P, yet the sleeps overlap.
func sleepers(n int, d time.Duration) time.Duration {
	start := time.Now()
	var wg sync.WaitGroup
	for range n {
		wg.Go(func() { time.Sleep(d) })
	}
	wg.Wait()
	return time.Since(start)
}

// Level 4: since Go 1.14 the runtime can stop a loop that never calls a function.
func preemptSpinner() time.Duration {
	start := time.Now()
	var stop atomic.Bool
	done := make(chan struct{})
	go func() {
		for !stop.Load() { // a tight loop: no function calls, no yield points
		}
		close(done)
	}()
	time.Sleep(20 * time.Millisecond) // the spinner now holds the only P
	stop.Store(true)                  // this line runs only because the spinner was preempted
	<-done
	return time.Since(start)
}

// Level 5: a goroutine leak. Two senders block forever on an unbuffered channel.
func firstResult(buffer int) int {
	results := make(chan int, buffer)
	for i := 1; i <= 3; i++ {
		go func() { results <- i * 10 }()
	}
	return <-results // with buffer 0, the two slower senders wait forever
}

func waitForGoroutines(want int) int {
	deadline := time.Now().Add(2 * time.Second)
	for runtime.NumGoroutine() != want && time.Now().Before(deadline) {
		time.Sleep(time.Millisecond)
	}
	return runtime.NumGoroutine()
}

// Go 1.27: the goroutineleak profile finds goroutines that can never wake up.
func leakedGoroutines() int {
	profile := pprof.Lookup("goroutineleak")
	if err := profile.WriteTo(io.Discard, 0); err != nil { // runs a leak-detecting GC
		panic(err)
	}
	return profile.Count()
}

// Interview: Amdahl's law, the speedup of N processors when a fraction p is parallel.
func amdahl(p float64, n int) float64 {
	return 1 / ((1 - p) + p/float64(n))
}

func check(ok bool, what string) {
	if !ok {
		panic(what)
	}
}

func near(a, b float64) bool { return math.Abs(a-b) < 0.005 }

func main() {
	check(slices.Equal(squares(4), []int{0, 1, 4, 9}), "four goroutines, four squares")
	check(slices.Equal(squaresGo(4), []int{0, 1, 4, 9}), "wg.Go gives the same result")

	base := runtime.NumGoroutine()
	alive, stackBytes := parked(1000)
	check(alive == base+1000, "NumGoroutine counts the 1000 parked goroutines")
	check(stackBytes > 0 && stackBytes <= 8192, fmt.Sprintf("at most 8 KiB of stack each, got %d", stackBytes))
	check(waitForGoroutines(base) == base, "all 1000 exit after release")

	var got int
	var wg sync.WaitGroup
	wg.Go(func() { got = depth(100_000) })
	wg.Wait()
	check(got == 100_000, "100,000 nested calls fit: the stack grew")

	check(defaultProcs(8, 0) == 8, "no limit: all 8 CPUs")
	check(defaultProcs(8, 1.5) == 2, "limit 1.5: ceil gives 2")
	check(defaultProcs(8, 0.5) == 2, "limit 0.5: raised to the floor of 2")
	check(defaultProcs(8, 4) == 4, "limit 4: 4")
	check(defaultProcs(1, 0.5) == 1, "never more than the CPUs")
	if os.Getenv("GOMAXPROCS") == "" {
		procs := runtime.GOMAXPROCS(0) // 0 reads the value without changing it
		check(procs >= 1 && procs <= runtime.NumCPU(), "the default never exceeds NumCPU")
	}

	previous := runtime.GOMAXPROCS(1)
	elapsed := sleepers(4, 25*time.Millisecond)
	check(elapsed >= 25*time.Millisecond && elapsed < 100*time.Millisecond, "4 sleeps of 25 ms overlap")
	check(preemptSpinner() >= 20*time.Millisecond, "the spinner was preempted")
	runtime.GOMAXPROCS(previous)

	check(amdahl(0.9, 8) > 4.70 && amdahl(0.9, 8) < 4.71, "p = 0.9, N = 8: about 4.7")
	check(near(amdahl(0.9, 1_000_000), 10), "p = 0.9: never more than 10")
	check(near(amdahl(0.5, 2), 4.0/3.0), "p = 0.5, N = 2: 1.33")

	base = runtime.NumGoroutine()
	check(slices.Contains([]int{10, 20, 30}, firstResult(3)), "some sender wins")
	check(waitForGoroutines(base) == base, "buffer 3: every sender finishes")
	check(slices.Contains([]int{10, 20, 30}, firstResult(0)), "some sender wins")
	check(waitForGoroutines(base+2) == base+2, "buffer 0: two senders leak")
	check(leakedGoroutines() == 2, "the goroutineleak profile finds both")
	fmt.Println("OK")
}
