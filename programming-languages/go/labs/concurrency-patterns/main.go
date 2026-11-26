// Run from programming-languages/go/labs with: go run -race ./concurrency-patterns
package main

import (
	"context"
	"errors"
	"fmt"
	"math"
	"slices"
	"sync"
	"sync/atomic"
	"time"
)

// Level 1: a generator is a function that returns a channel.
func gen(ctx context.Context, nums ...int) <-chan int {
	out := make(chan int)
	go func() {
		defer close(out)
		for _, n := range nums {
			select {
			case out <- n:
			case <-ctx.Done():
				return
			}
		}
	}()
	return out
}

// Level 2: a pipeline stage receives, transforms and sends.
func square(ctx context.Context, in <-chan int) <-chan int {
	out := make(chan int)
	go func() {
		defer close(out)
		for n := range in {
			select {
			case out <- n * n:
			case <-ctx.Done():
				return
			}
		}
	}()
	return out
}

func sum(in <-chan int) int {
	total := 0
	for n := range in {
		total += n
	}
	return total
}

func collect(in <-chan int) []int {
	var got []int
	for n := range in {
		got = append(got, n)
	}
	return got
}

// Level 3: fan-in merges several channels into one.
func merge(ctx context.Context, inputs ...<-chan int) <-chan int {
	out := make(chan int)
	var wg sync.WaitGroup
	for _, in := range inputs {
		wg.Go(func() {
			for n := range in {
				select {
				case out <- n:
				case <-ctx.Done():
					return
				}
			}
		})
	}
	go func() {
		wg.Wait()
		close(out)
	}()
	return out
}

func firstSquareThenStop() int {
	ctx, cancel := context.WithCancel(context.Background())
	squares := square(ctx, gen(ctx, 1, 2, 3, 4))
	first := <-squares
	cancel() // the consumer leaves early: both stages exit through ctx.Done
	for range squares {
		// drain until square closes its channel: proof that it exited
	}
	return first
}

func fanOutFanIn(ctx context.Context, workers int, nums ...int) int {
	in := gen(ctx, nums...)
	squares := make([]<-chan int, workers)
	for i := range squares {
		squares[i] = square(ctx, in) // fan-out: all read the same channel
	}
	return sum(merge(ctx, squares...)) // fan-in
}

// Level 4: bound how many tasks run at once.
type gauge struct{ running, peak atomic.Int64 }

func (g *gauge) enter() {
	n := g.running.Add(1)
	for {
		p := g.peak.Load()
		if n <= p || g.peak.CompareAndSwap(p, n) {
			return
		}
	}
}

func (g *gauge) leave() { g.running.Add(-1) }

func slowDouble(g *gauge) func(int) int {
	return func(n int) int {
		g.enter()
		defer g.leave()
		time.Sleep(2 * time.Millisecond) // pretend to call a server
		return 2 * n
	}
}

func workerPool(tasks []int, workers int, work func(int) int) []int {
	results := make([]int, len(tasks))
	jobs := make(chan int) // task indexes
	var wg sync.WaitGroup
	for range workers {
		wg.Go(func() {
			for i := range jobs {
				results[i] = work(tasks[i]) // each index is written by one worker only
			}
		})
	}
	for i := range tasks {
		jobs <- i
	}
	close(jobs) // ends every worker's range loop
	wg.Wait()
	return results
}

func withSemaphore(tasks []int, limit int, work func(int) int) []int {
	results := make([]int, len(tasks))
	sem := make(chan struct{}, limit)
	var wg sync.WaitGroup
	for i, task := range tasks {
		sem <- struct{}{} // acquire: blocks while limit tasks are running
		wg.Go(func() {
			defer func() { <-sem }() // release
			results[i] = work(task)
		})
	}
	wg.Wait()
	return results
}

// Level 5: the first error cancels the other workers (a tiny errgroup).
type Group struct {
	wg     sync.WaitGroup
	once   sync.Once
	err    error
	cancel context.CancelCauseFunc
}

func WithContext(ctx context.Context) (*Group, context.Context) {
	ctx, cancel := context.WithCancelCause(ctx)
	return &Group{cancel: cancel}, ctx
}

func (g *Group) Go(f func() error) {
	g.wg.Go(func() {
		if err := f(); err != nil {
			g.once.Do(func() {
				g.err = err
				g.cancel(err) // the first error cancels everyone else
			})
		}
	})
}

func (g *Group) Wait() error {
	g.wg.Wait()
	g.cancel(g.err) // free the context even when nothing failed
	return g.err
}

var errBadInput = errors.New("bad input in file 2")

func processFiles() (othersSaw []error, err error) {
	g, ctx := WithContext(context.Background())
	seen := make(chan error, 2)
	for file := range 3 {
		g.Go(func() error {
			if file == 2 {
				return errBadInput
			}
			<-ctx.Done() // a long job that checks for cancellation
			seen <- context.Cause(ctx)
			return ctx.Err()
		})
	}
	err = g.Wait()
	close(seen)
	for cause := range seen {
		othersSaw = append(othersSaw, cause)
	}
	return othersSaw, err
}

// Level 6: a ticker lets one request through per tick.
func rateLimited(requests []string, every time.Duration) []string {
	ticker := time.NewTicker(every)
	defer ticker.Stop()
	var served []string
	for _, r := range requests {
		<-ticker.C // wait for the next slot
		served = append(served, r)
	}
	return served
}

// Sizing: Little's law, busy workers L = arrival rate λ × time per task W.
func workersNeeded(perSecond float64, each time.Duration) int {
	return int(math.Ceil(perSecond * each.Seconds()))
}

func check(ok bool, what string) {
	if !ok {
		panic(what)
	}
}

func main() {
	ctx := context.Background()
	check(slices.Equal(collect(gen(ctx, 1, 2, 3)), []int{1, 2, 3}), "a generator yields in order")
	check(sum(square(ctx, gen(ctx, 1, 2, 3, 4))) == 30, "1 + 4 + 9 + 16 = 30")
	check(fanOutFanIn(ctx, 3, 1, 2, 3, 4) == 30, "three squarers, one merged sum")

	check(firstSquareThenStop() == 1, "take the first square, then cancel the pipeline")

	tasks := []int{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}
	want := []int{2, 4, 6, 8, 10, 12, 14, 16, 18, 20}
	var pool, sem gauge
	check(slices.Equal(workerPool(tasks, 3, slowDouble(&pool)), want), "the pool doubles every task")
	check(pool.peak.Load() <= 3, "never more than 3 workers busy")
	check(slices.Equal(withSemaphore(tasks, 2, slowDouble(&sem)), want), "the semaphore doubles every task")
	check(sem.peak.Load() <= 2, "never more than 2 tasks at once")

	othersSaw, err := processFiles()
	check(err == errBadInput, "Wait returns the first error")
	check(slices.Equal(othersSaw, []error{errBadInput, errBadInput}), "the others were cancelled with that cause")

	start := time.Now()
	served := rateLimited([]string{"a", "b", "c", "d", "e"}, 10*time.Millisecond)
	check(len(served) == 5 && time.Since(start) >= 50*time.Millisecond, "5 requests at 100/s take at least 50 ms")

	check(workersNeeded(200, 50*time.Millisecond) == 10, "200/s x 0.05 s = 10 workers")
	check(workersNeeded(200, 120*time.Millisecond) == 24, "200/s x 0.12 s = 24 workers")
	fmt.Println("OK")
}
