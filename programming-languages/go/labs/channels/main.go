// Run from programming-languages/go/labs with: go run -race ./channels
package main

import (
	"fmt"
	"slices"
	"sync"
)

// Level 1: an unbuffered channel is a handshake.
func handshake() int {
	ch := make(chan int) // unbuffered: capacity 0
	go func() { ch <- 42 }()
	return <-ch // waits until the goroutine sends
}

// Level 2: a buffered channel is a queue with a fixed number of slots.
func bufferedLenCap() (int, int) {
	ch := make(chan string, 2)
	ch <- "a" // no receiver needed: a slot is free
	ch <- "b" // the queue is now full
	return len(ch), cap(ch)
}

func sendWithoutReceiver(capacity int) bool {
	ch := make(chan int, capacity)
	select {
	case ch <- 1:
		return true // the send completed although nobody was receiving
	default:
		return false // the send would have blocked
	}
}

// Level 3: close says "no more values"; range stops after it.
func produce(n int) <-chan int {
	out := make(chan int)
	go func() {
		defer close(out) // the sender closes, once, when it is done
		for i := 1; i <= n; i++ {
			out <- i
		}
	}()
	return out
}

func sum(in <-chan int) int {
	total := 0
	for v := range in { // ends when out is closed and empty
		total += v
	}
	return total
}

func receivedInOrder(in <-chan int) []int {
	var got []int
	for v := range in {
		got = append(got, v)
	}
	return got
}

func receiveAfterClose() (int, bool, int, bool) {
	ch := make(chan int, 1)
	ch <- 7
	close(ch)
	first, firstOK := <-ch // the buffered 7 is still delivered
	later, laterOK := <-ch // now empty and closed: zero value, ok == false
	return first, firstOK, later, laterOK
}

// Level 5: the three operations that panic.
func panicMessage(operation func()) (message string) {
	defer func() { message = fmt.Sprint(recover()) }()
	operation()
	return "no panic"
}

func sendOnClosed() {
	ch := make(chan int, 1)
	close(ch)
	ch <- 1
}

func closeTwice() {
	ch := make(chan int)
	close(ch)
	close(ch)
}

func closeNil() {
	var ch chan int // nil: declared but never made
	close(ch)
}

// Underneath: the memory model. The receive makes the earlier write visible.
func publish() string {
	var message string
	done := make(chan struct{})
	go func() {
		message = "hello"  // (1) write
		done <- struct{}{} // (2) send
	}()
	<-done         // (3) receive completes
	return message // (4) read: guaranteed to see "hello"
}

// Interview: many senders, one closer.
func merge(inputs ...<-chan int) <-chan int {
	out := make(chan int)
	var wg sync.WaitGroup
	for _, in := range inputs {
		wg.Go(func() {
			for v := range in {
				out <- v
			}
		})
	}
	go func() {
		wg.Wait()
		close(out) // the only closer: runs after every sender has finished
	}()
	return out
}

func check(ok bool, what string) {
	if !ok {
		panic(what)
	}
}

func main() {
	check(handshake() == 42, "unbuffered handshake delivers 42")

	length, capacity := bufferedLenCap()
	check(length == 2 && capacity == 2, "two sends fill a channel of capacity 2")
	check(!sendWithoutReceiver(0), "an unbuffered send needs a receiver")
	check(sendWithoutReceiver(1), "a send into a free slot completes alone")

	check(sum(produce(4)) == 10, "1 + 2 + 3 + 4 = 10")
	check(slices.Equal(receivedInOrder(produce(4)), []int{1, 2, 3, 4}), "one channel is FIFO")
	first, firstOK, later, laterOK := receiveAfterClose()
	check(first == 7 && firstOK, "buffered values survive close")
	check(later == 0 && !laterOK, "a drained closed channel gives (0, false)")

	check(panicMessage(sendOnClosed) == "send on closed channel", "send on closed panics")
	check(panicMessage(closeTwice) == "close of closed channel", "double close panics")
	check(panicMessage(closeNil) == "close of nil channel", "closing nil panics")

	check(publish() == "hello", "write -> send -> receive -> read")

	merged := receivedInOrder(merge(produce(3), produce(4)))
	slices.Sort(merged)
	check(slices.Equal(merged, []int{1, 1, 2, 2, 3, 3, 4}), "merge delivers all 7 values once")
	fmt.Println("OK")
}
