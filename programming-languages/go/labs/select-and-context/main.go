// Run from programming-languages/go/labs with: go run -race ./select-and-context
package main

import (
	"context"
	"errors"
	"fmt"
	"slices"
	"time"
)

// Level 1: select waits for whichever case is ready.
func firstOfTwo() string {
	letters := make(chan string, 1)
	numbers := make(chan string) // nobody ever sends here
	letters <- "a"
	select {
	case v := <-letters:
		return v
	case v := <-numbers:
		return v
	}
}

// Both cases are always ready, so select picks one at random each time.
func countChoices(trials int) (left, right int) {
	a, b := make(chan int), make(chan int)
	close(a) // a closed channel is always ready to receive
	close(b)
	for range trials {
		select {
		case <-a:
			left++
		case <-b:
			right++
		}
	}
	return left, right
}

// Level 2: default turns a blocking select into a "try".
func tryReceive(ch <-chan int) (int, bool) {
	select {
	case v := <-ch:
		return v, true
	default:
		return 0, false // nothing ready: do not wait
	}
}

// Level 3: timeouts, timers and tickers.
var errTimeout = errors.New("timed out")

func fetchWithTimeout(results <-chan string, limit time.Duration) (string, error) {
	select {
	case r := <-results:
		return r, nil
	case <-time.After(limit):
		return "", errTimeout
	}
}

func stopBeforeFiring() (bool, bool) {
	t := time.NewTimer(time.Hour)
	stopped := t.Stop() // true: the timer had not fired yet
	select {
	case <-t.C:
		return stopped, true
	default:
		return stopped, false // Go 1.23+: no stale value after Stop
	}
}

func tick(n int, every time.Duration) time.Duration {
	start := time.Now()
	ticker := time.NewTicker(every)
	defer ticker.Stop()
	for range n {
		<-ticker.C
	}
	return time.Since(start)
}

// Level 4: a for-select loop; a nil channel switches its case off.
func count(n int) <-chan int {
	out := make(chan int)
	go func() {
		defer close(out)
		for i := 1; i <= n; i++ {
			out <- i
		}
	}()
	return out
}

func mergeSum(a, b <-chan int) int {
	total := 0
	for a != nil || b != nil {
		select {
		case v, ok := <-a:
			if !ok {
				a = nil // closed: switch this case off
				continue
			}
			total += v
		case v, ok := <-b:
			if !ok {
				b = nil
				continue
			}
			total += v
		}
	}
	return total
}

// Level 5: context.Context carries cancellation to every goroutine.
func numbers(ctx context.Context) <-chan int {
	out := make(chan int)
	go func() {
		defer close(out)
		for i := 1; ; i++ {
			select {
			case out <- i:
			case <-ctx.Done():
				return // the receiver left: exit instead of leaking
			}
		}
	}()
	return out
}

func takeThree() []int {
	ctx, cancel := context.WithCancel(context.Background())
	in := numbers(ctx)
	got := []int{<-in, <-in, <-in}
	cancel() // tell the producer to stop
	for range in {
		// drain until the producer closes out: proof that it exited
	}
	return got
}

func cancelTree() (afterLeft, afterParent []error) {
	parent, cancelParent := context.WithCancel(context.Background())
	left, cancelLeft := context.WithCancel(parent)
	right, cancelRight := context.WithCancel(parent)
	defer cancelRight()

	cancelLeft() // cancels left only: a child cannot cancel its parent
	afterLeft = []error{parent.Err(), left.Err(), right.Err()}
	cancelParent() // cancels parent and everything below it
	afterParent = []error{parent.Err(), left.Err(), right.Err()}
	return afterLeft, afterParent
}

func waitForTimeout() error {
	ctx, cancel := context.WithTimeout(context.Background(), 20*time.Millisecond)
	defer cancel() // always call cancel: it frees the timer early
	<-ctx.Done()
	return ctx.Err()
}

func childCannotOutliveParent() bool {
	parent, cancel := context.WithTimeout(context.Background(), time.Hour)
	defer cancel()
	child, cancelChild := context.WithTimeout(parent, 2*time.Hour) // asks for more
	defer cancelChild()
	p, _ := parent.Deadline()
	c, _ := child.Deadline()
	return c.Equal(p)
}

// Level 6: why it was cancelled, what runs afterwards, and request values.
var errUserLeft = errors.New("user closed the page")

func cancelWithCause() (error, error) {
	ctx, cancel := context.WithCancelCause(context.Background())
	cancel(errUserLeft)
	return ctx.Err(), context.Cause(ctx)
}

func afterFuncDemo() bool {
	ctx, cancel := context.WithCancel(context.Background())
	ran := make(chan struct{})
	context.AfterFunc(ctx, func() { close(ran) })
	stop := context.AfterFunc(ctx, func() { panic("never runs") })
	stopped := stop() // true: the second function will never run
	cancel()
	<-ran // the first function runs in its own goroutine after cancel
	return stopped
}

type requestIDKey struct{} // an unexported key type: no other package can collide

func withRequestID(ctx context.Context, id string) context.Context {
	return context.WithValue(ctx, requestIDKey{}, id)
}

func requestID(ctx context.Context) string {
	id, _ := ctx.Value(requestIDKey{}).(string)
	return id
}

// Interview: ask every replica, keep the first answer, cancel the rest.
func first(ctx context.Context, replicas ...func(context.Context) string) string {
	ctx, cancel := context.WithCancel(ctx)
	defer cancel() // the losers see ctx.Done and stop

	results := make(chan string, len(replicas)) // buffered: losers never block
	for _, replica := range replicas {
		go func() { results <- replica(ctx) }()
	}
	return <-results
}

func fastReplica(context.Context) string { return "fast" }

func slowReplica(ctx context.Context) string {
	<-ctx.Done() // only answers after the caller has given up
	return "slow"
}

func check(ok bool, what string) {
	if !ok {
		panic(what)
	}
}

func main() {
	check(firstOfTwo() == "a", "select takes the ready case")
	left, right := countChoices(1000)
	check(left+right == 1000 && left > 300 && right > 300, "both ready cases get chosen")

	empty := make(chan int, 1)
	_, ok := tryReceive(empty)
	check(!ok, "default runs when nothing is ready")
	empty <- 5
	v, ok := tryReceive(empty)
	check(v == 5 && ok, "a ready value is taken")

	start := time.Now()
	_, err := fetchWithTimeout(make(chan string), 20*time.Millisecond)
	check(err == errTimeout && time.Since(start) >= 20*time.Millisecond, "time.After fires after 20 ms")
	ready := make(chan string, 1)
	ready <- "result"
	r, err := fetchWithTimeout(ready, time.Hour)
	check(r == "result" && err == nil, "a ready result beats the timeout")
	stopped, stale := stopBeforeFiring()
	check(stopped && !stale, "Stop returns true and leaves no stale value")
	check(tick(3, 10*time.Millisecond) >= 30*time.Millisecond, "3 ticks of 10 ms take at least 30 ms")

	check(mergeSum(count(3), count(4)) == 16, "6 + 10 = 16 through a nil-channel merge")

	check(slices.Equal(takeThree(), []int{1, 2, 3}), "take three, then cancel the producer")
	afterLeft, afterParent := cancelTree()
	check(slices.Equal(afterLeft, []error{nil, context.Canceled, nil}), "cancelling a child leaves its parent alone")
	check(slices.Equal(afterParent, []error{context.Canceled, context.Canceled, context.Canceled}), "cancelling the parent cancels all")
	check(waitForTimeout() == context.DeadlineExceeded, "WithTimeout ends in DeadlineExceeded")
	check(context.DeadlineExceeded.Error() == "context deadline exceeded", "DeadlineExceeded message")
	check(context.Canceled.Error() == "context canceled", "Canceled message")
	check(childCannotOutliveParent(), "deadline is the minimum of parent and child")

	errValue, cause := cancelWithCause()
	check(errValue == context.Canceled && cause == errUserLeft, "Err says canceled, Cause says why")
	check(afterFuncDemo(), "AfterFunc runs after cancel; stop prevents it")

	ctx := withRequestID(context.Background(), "req-42")
	child, cancel := context.WithCancel(ctx)
	check(requestID(child) == "req-42", "values are found by walking up the tree")
	check(requestID(context.Background()) == "", "a missing value is the zero value")
	cancel()

	check(first(context.Background(), slowReplica, fastReplica, slowReplica) == "fast", "first answer wins")
	fmt.Println("OK")
}
