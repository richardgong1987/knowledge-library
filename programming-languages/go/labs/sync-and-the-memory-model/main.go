// Run from programming-languages/go/labs with: go run -race ./sync-and-the-memory-model
package main

import (
	"fmt"
	"sync"
	"sync/atomic"
)

// Level 1: count++ is read, add, write. Interleaving two of them loses an update.
func lostUpdate() int {
	count := 0
	r1 := count    // goroutine 1 reads 0
	r2 := count    // goroutine 2 reads 0
	count = r1 + 1 // goroutine 1 writes 1
	count = r2 + 1 // goroutine 2 writes 1: goroutine 1's increment is lost
	return count
}

// Level 2: a mutex lets one goroutine at a time run the critical section.
type Counter struct {
	mu sync.Mutex
	n  int
}

func (c *Counter) Inc() {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.n++
}

func countWithMutex(goroutines, each int) int {
	var c Counter
	var wg sync.WaitGroup
	for range goroutines {
		wg.Go(func() {
			for range each {
				c.Inc()
			}
		})
	}
	wg.Wait() // every Inc happens before Wait returns
	return c.n
}

func readersShare() (secondReader, writer bool) {
	var mu sync.RWMutex
	mu.RLock()
	secondReader = mu.TryRLock() // true: readers share the lock
	writer = mu.TryLock()        // false: a writer needs every reader gone
	mu.RUnlock()
	if secondReader {
		mu.RUnlock()
	}
	return secondReader, writer
}

// Level 3: run something exactly once, however many goroutines ask.
func loadConfigOnce(callers int) (loads int32, region string) {
	var calls atomic.Int32
	config := sync.OnceValue(func() map[string]string {
		calls.Add(1)
		return map[string]string{"region": "eu-west"}
	})
	var wg sync.WaitGroup
	for range callers {
		wg.Go(func() { _ = config()["region"] })
	}
	wg.Wait()
	return calls.Load(), config()["region"]
}

func panicMessage(operation func()) (message string) {
	defer func() { message = fmt.Sprint(recover()) }()
	operation()
	return "no panic"
}

// Level 4: atomics for single words.
func countWithAtomic(goroutines, each int) int64 {
	var n atomic.Int64
	var wg sync.WaitGroup
	for range goroutines {
		wg.Go(func() {
			for range each {
				n.Add(1)
			}
		})
	}
	wg.Wait()
	return n.Load()
}

func storeMax(m *atomic.Int64, v int64) {
	for {
		old := m.Load()
		if v <= old || m.CompareAndSwap(old, v) {
			return // already large enough, or we won the race to replace old
		}
	}
}

type Config struct {
	Region  string
	Workers int
}

var current atomic.Pointer[Config]

func publishConfig(region string, workers int) {
	c := &Config{Region: region, Workers: workers} // fill in every field first
	current.Store(c)                               // then publish the pointer
}

// Level 5: sync.Map and sync.Cond, two specialised tools.
func firstWriterWins() (any, bool) {
	var leaders sync.Map
	leaders.LoadOrStore("shard-1", "alice")
	return leaders.LoadOrStore("shard-1", "bob") // "alice", true: already there
}

type Mailbox struct {
	mu      sync.Mutex
	arrived *sync.Cond
	letters []string
}

func (m *Mailbox) Put(letter string) {
	m.mu.Lock()
	m.letters = append(m.letters, letter)
	m.mu.Unlock()
	m.arrived.Signal() // wake one waiter
}

func (m *Mailbox) Take() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	for len(m.letters) == 0 { // a loop, not an if: re-check after waking
		m.arrived.Wait() // unlocks mu while asleep, relocks before returning
	}
	letter := m.letters[0]
	m.letters = m.letters[1:]
	return letter
}

// Underneath: Unlock n happens before Lock n+1 returns.
func publishWithMutex() string {
	var mu sync.Mutex
	var message string
	mu.Lock()
	go func() {
		message = "hello" // (1) write
		mu.Unlock()       // (2) unlock number 1
	}()
	mu.Lock() // (3) lock number 2 returns only after (2)
	defer mu.Unlock()
	return message // (4) guaranteed to see "hello"
}

func check(ok bool, what string) {
	if !ok {
		panic(what)
	}
}

func main() {
	check(lostUpdate() == 1, "two interleaved increments give 1, not 2")

	check(countWithMutex(8, 1000) == 8000, "8 x 1000 locked increments = 8000")
	secondReader, writer := readersShare()
	check(secondReader && !writer, "readers share; a writer waits")

	loads, region := loadConfigOnce(10)
	check(loads == 1 && region == "eu-west", "10 callers, 1 load")
	boom := sync.OnceFunc(func() { panic("config missing") })
	check(panicMessage(boom) == "config missing", "OnceFunc panics on the first call")
	check(panicMessage(boom) == "config missing", "and with the same value on every later call")

	check(countWithAtomic(8, 1000) == 8000, "8 x 1000 atomic adds = 8000")
	var highest atomic.Int64
	var wg sync.WaitGroup
	for v := range int64(100) {
		wg.Go(func() { storeMax(&highest, v+1) })
	}
	wg.Wait()
	check(highest.Load() == 100, "the CAS loop keeps the maximum, 100")

	publishConfig("eu-west", 4)
	done := make(chan Config)
	go func() { done <- *current.Load() }()
	check(<-done == Config{Region: "eu-west", Workers: 4}, "a published pointer shows every field")

	owner, loaded := firstWriterWins()
	check(owner == "alice" && loaded, "LoadOrStore keeps the first value")

	box := &Mailbox{}
	box.arrived = sync.NewCond(&box.mu)
	got := make(chan string)
	go func() { got <- box.Take() }()
	box.Put("postcard")
	check(<-got == "postcard", "Take waits until Put signals")

	check(publishWithMutex() == "hello", "unlock -> lock makes the write visible")
	fmt.Println("OK")
}
