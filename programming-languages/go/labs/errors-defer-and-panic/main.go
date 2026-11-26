// Run from programming-languages/go/labs with: go run -race ./errors-defer-and-panic
package main

import (
	"errors"
	"fmt"
	"runtime"
	"slices"
	"strconv"
)

// Level 1: an error is an ordinary value that a function returns.
var ErrMissing = errors.New("missing setting")

func lookup(settings map[string]string, key string) (string, error) {
	value, ok := settings[key]
	if !ok {
		return "", ErrMissing
	}
	return value, nil
}

// Level 2: %w wraps an error, adding context while keeping the original inside.
func parsePort(text string) (int, error) {
	port, err := strconv.Atoi(text)
	if err != nil {
		return 0, fmt.Errorf("parse port %q: %w", text, err)
	}
	return port, nil
}

// Level 3: a typed error carries data the caller can inspect.
type RangeError struct {
	Port int
}

func (e *RangeError) Error() string {
	return fmt.Sprintf("port %d is outside 1-65535", e.Port)
}

func checkPort(port int) error {
	if port < 1 || port > 65535 {
		return &RangeError{Port: port}
	}
	return nil
}

// Interview: errors.Join reports several failures at once; nil entries are dropped.
func checkAll(ports []int) error {
	var errs []error
	for _, port := range ports {
		errs = append(errs, checkPort(port))
	}
	return errors.Join(errs...)
}

// Common mistake: a nil pointer stored in an error interface is not a nil error.
func checkPortWrong(port int) error {
	var rangeErr *RangeError
	if port < 1 || port > 65535 {
		rangeErr = &RangeError{Port: port}
	}
	return rangeErr // never nil: the interface holds the type *RangeError
}

// Level 4: deferred calls run when the function returns, last in, first out.
func appendTo(log *[]int, v int) {
	*log = append(*log, v)
}

func deferOrder(log *[]int) {
	for i := 1; i <= 3; i++ {
		defer appendTo(log, i)
	}
}

func deferArgs(log *[]int) {
	x := 1
	defer appendTo(log, x) // x is evaluated now, so 1 is saved
	x = 2
	appendTo(log, x)
}

func doubled() (n int) {
	defer func() { n *= 2 }()
	return 21 // sets n = 21; then the deferred call makes it 42
}

// Level 5: recover in a deferred function turns a panic back into an error.
func safeDivide(a, b int) (q int, err error) {
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf("recovered: %v", r)
		}
	}()
	return a / b, nil
}

func helper() any { return recover() }

func recoverDepth() (direct, indirect any) {
	defer func() { direct = recover() }()  // runs second: stops the panic
	defer func() { indirect = helper() }() // runs first: one call too deep, gets nil
	panic("boom")
}

func panicNil() (recovered any) {
	defer func() { recovered = recover() }()
	panic(nil)
}

func check(ok bool, what string) {
	if !ok {
		panic(what)
	}
}

func main() {
	settings := map[string]string{"port": "8080"}
	value, err := lookup(settings, "port")
	check(value == "8080" && err == nil, "lookup finds port")
	_, err = lookup(settings, "host")
	check(err == ErrMissing && err.Error() == "missing setting", "lookup reports the sentinel")

	wrapped := fmt.Errorf("load config: %w", err)
	check(wrapped.Error() == "load config: missing setting", "the wrapper adds context")
	check(wrapped != ErrMissing && errors.Is(wrapped, ErrMissing), "== fails, errors.Is unwraps")
	flattened := fmt.Errorf("load config: %v", err)
	check(flattened.Error() == wrapped.Error() && !errors.Is(flattened, ErrMissing), "%v keeps text only")

	port, err := parsePort("8080")
	check(port == 8080 && err == nil, "8080 parses")
	_, err = parsePort("80a0")
	check(err.Error() == `parse port "80a0": strconv.Atoi: parsing "80a0": invalid syntax`, "wrapped message")
	check(errors.Is(err, strconv.ErrSyntax), "errors.Is walks a chain of three")
	numErr, ok := errors.AsType[*strconv.NumError](err)
	check(ok && numErr.Func == "Atoi", "errors.AsType finds the middle link")

	err = fmt.Errorf("load config: %w", checkPort(70000))
	rangeErr, ok := errors.AsType[*RangeError](err)
	check(ok && rangeErr.Port == 70000, "errors.AsType extracts the typed error")
	var target *RangeError
	check(errors.As(err, &target) && target.Port == 70000, "errors.As is the older form")
	check(checkPort(443) == nil, "443 is in range")

	joined := checkAll([]int{80, 0, 70000})
	check(joined.Error() == "port 0 is outside 1-65535\nport 70000 is outside 1-65535", "Join lists both")
	first, _ := errors.AsType[*RangeError](joined)
	check(first.Port == 0, "AsType returns the first match, depth first")
	check(checkAll([]int{80, 443}) == nil, "Join of only nils is nil")

	check(checkPortWrong(80) != nil, "a typed nil pointer makes a non-nil error")

	var order []int
	deferOrder(&order)
	check(slices.Equal(order, []int{3, 2, 1}), "defers run last in, first out")
	var args []int
	deferArgs(&args)
	check(slices.Equal(args, []int{2, 1}), "defer saved x = 1 before x became 2")
	check(doubled() == 42, "a deferred call can change a named result")

	q, err := safeDivide(10, 2)
	check(q == 5 && err == nil, "10 / 2 = 5")
	q, err = safeDivide(1, 0)
	check(q == 0 && err.Error() == "recovered: runtime error: integer divide by zero", "panic became an error")

	direct, indirect := recoverDepth()
	check(direct == "boom" && indirect == nil, "recover works only when called directly")

	recovered := panicNil()
	_, isPanicNil := recovered.(*runtime.PanicNilError)
	check(isPanicNil && fmt.Sprint(recovered) == "runtime error: panic called with nil argument", "panic(nil)")
	fmt.Println("OK")
}
