// Run from programming-languages/go/labs with: go run -race ./slices-and-maps
package main

import (
	"encoding/json"
	"fmt"
	"maps"
	"slices"
)

// Level 1: an array has a fixed length; a slice is a view of an array.
func arrayVsSlice() (int, int, int) {
	var week [7]string          // array: the length 7 is part of the type
	scores := []int{90, 80, 70} // slice: a view of a hidden array
	return len(week), len(scores), cap(scores)
}

// Level 2: slicing shares the backing array.
func sliceShares() ([]int, []int) {
	scores := []int{90, 80, 70, 60}
	middle := scores[1:3] // a window onto elements 1 and 2
	middle[0] = 85        // writes into the shared array
	return scores, middle
}

// Level 3: append writes in place while there is room, and copies when there is not.
func appendInPlace() ([]int, []int) {
	base := make([]int, 3, 4) // len 3, room for 4
	grown := append(base, 7)  // fits: same backing array
	grown[0] = 1              // so base sees this too
	return base, grown
}

func appendCopies() ([]int, []int) {
	base := []int{1, 2, 3}   // len 3, cap 3: full
	grown := append(base, 4) // no room: a new, bigger array
	grown[0] = 99            // base is not affected
	return base, grown
}

// nextCap copies the rule in runtime.nextslicecap (Go 1.18+), before rounding.
func nextCap(newLen, oldCap int) int {
	if newLen > 2*oldCap {
		return newLen
	}
	if oldCap < 256 {
		return 2 * oldCap
	}
	newCap := oldCap
	for newCap < newLen {
		newCap += (newCap + 3*256) / 4
	}
	return newCap
}

func capAfterAppend(n int) int {
	full := make([]int, n) // len n, cap n: no room left
	return cap(append(full, 0))
}

// Level 4: two appends to one base can write the same slot.
func twoAppends() ([]int, []int) {
	base := make([]int, 3, 10)
	a := append(base, 4) // writes slot 3 of the shared array
	b := append(base, 5) // writes the SAME slot 3
	return a, b
}

func appendSafely() ([]int, []int) {
	base := make([]int, 3, 10)
	limited := base[0:3:3]  // cap = 3 - 0: no spare room
	a := append(limited, 4) // must copy, so it cannot touch base's array
	b := append(limited, 5)
	return a, b
}

func copyIndependent() ([]int, []int, int) {
	src := []int{1, 2, 3}
	dst := make([]int, 2)
	n := copy(dst, src) // copies min(len(dst), len(src)) = 2 elements
	dst[0] = 100        // src is not affected
	return src, dst, n
}

// The slices package (Go 1.21+) holds the everyday loops.
func sortedCopy(nums []int) []int {
	sorted := slices.Clone(nums) // an independent copy
	slices.Sort(sorted)
	return sorted
}

// Level 5: maps.
func wordCount(words []string) map[string]int {
	counts := make(map[string]int)
	for _, w := range words {
		counts[w]++ // a missing key reads as 0
	}
	return counts
}

func sortedKeys(m map[string]int) []string {
	return slices.Sorted(maps.Keys(m)) // map order is random; sort for stable output
}

func nilMapRead() (int, int) {
	var m map[string]int // nil: reading works
	return m["x"], len(m)
}

func nilMapWrite() {
	var m map[string]int
	m["x"] = 1
}

// Level 6: map elements are not addressable.
type Player struct {
	Score int
}

func bumpScore(players map[string]Player, name string) {
	p := players[name] // a copy of the element
	p.Score++
	players[name] = p // write the copy back
}

// Underneath: the Swiss-table split of a hash, from internal/runtime/maps.
func h1(hash uint64) uint64 { return hash >> 7 }   // picks the first group to probe
func h2(hash uint64) uint64 { return hash & 0x7f } // stored in the slot's control byte

func probeGroups(hash, groups uint64) []uint64 {
	var order []uint64
	offset := h1(hash) & (groups - 1)
	for i := uint64(0); i < groups; i++ {
		order = append(order, offset)
		offset = (offset + i + 1) & (groups - 1) // triangular steps: +1, +2, +3
	}
	return order
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
	weekLen, scoresLen, scoresCap := arrayVsSlice()
	check(weekLen == 7 && scoresLen == 3 && scoresCap == 3, "array 7; slice len 3 cap 3")

	scores, middle := sliceShares()
	check(slices.Equal(scores, []int{90, 85, 70, 60}), "the write shows through scores")
	check(slices.Equal(middle, []int{85, 70}) && cap(middle) == 3, "len 3-1 = 2, cap 4-1 = 3")

	base, grown := appendInPlace()
	check(slices.Equal(base, []int{1, 0, 0}) && slices.Equal(grown, []int{1, 0, 0, 7}), "shared")
	base, grown = appendCopies()
	check(slices.Equal(base, []int{1, 2, 3}) && slices.Equal(grown, []int{99, 2, 3, 4}), "copied")

	check(nextCap(6, 5) == 10 && nextCap(101, 100) == 200, "below 256: double")
	check(nextCap(257, 256) == 512 && nextCap(513, 512) == 832, "256 + 256, 512 + 320")
	check(nextCap(7, 2) == 7, "a big append takes the needed length")
	check(capAfterAppend(5) == 10 && capAfterAppend(100) == 224, "200 rounds up to 224")
	check(capAfterAppend(256) == 512 && capAfterAppend(512) == 848, "832 rounds up to 848")
	check(cap(append(make([]int, 2), 1, 2, 3, 4, 5)) == 8, "7 rounds up to 8")

	a, b := twoAppends()
	check(slices.Equal(a, []int{0, 0, 0, 5}) && slices.Equal(b, []int{0, 0, 0, 5}), "aliasing")
	a, b = appendSafely()
	check(slices.Equal(a, []int{0, 0, 0, 4}) && slices.Equal(b, []int{0, 0, 0, 5}), "no aliasing")
	src, dst, n := copyIndependent()
	check(slices.Equal(src, []int{1, 2, 3}) && slices.Equal(dst, []int{100, 2}) && n == 2, "copy")

	nums := []int{3, 1, 2}
	check(slices.Equal(sortedCopy(nums), []int{1, 2, 3}) && nums[0] == 3, "Clone, then Sort")
	check(slices.Contains(nums, 2) && slices.Index(nums, 2) == 2, "Contains and Index")
	check(slices.Equal(slices.Delete(nums, 0, 1), []int{1, 2}), "Delete removes [0, 1)")

	var nilSlice []int
	emptySlice := []int{}
	check(nilSlice == nil && emptySlice != nil, "nil and empty differ only in nil-ness")
	check(len(nilSlice) == 0 && len(append(nilSlice, 1)) == 1, "a nil slice can be appended to")
	check(slices.Equal(nilSlice, emptySlice), "slices.Equal treats them as equal")
	nilJSON, _ := json.Marshal(nilSlice)
	emptyJSON, _ := json.Marshal(emptySlice)
	check(string(nilJSON) == "null" && string(emptyJSON) == "[]", "JSON null vs []")

	counts := wordCount([]string{"go", "is", "fun", "go"})
	check(counts["go"] == 2 && counts["is"] == 1 && len(counts) == 3, "word counts")
	missing, ok := counts["rust"] // comma-ok: tells "absent" from "stored 0"
	check(missing == 0 && !ok, "absent key: (0, false)")
	check(slices.Equal(sortedKeys(counts), []string{"fun", "go", "is"}), "sorted keys")
	delete(counts, "is")
	delete(counts, "nope") // deleting a missing key is a no-op
	check(len(counts) == 2, "delete removes one key")
	clear(counts) // Go 1.21+: remove every key
	check(len(counts) == 0, "clear empties the map")

	value, length := nilMapRead()
	check(value == 0 && length == 0, "a nil map reads as empty")
	check(panicMessage(nilMapWrite) == "assignment to entry in nil map", "nil map write panics")

	players := map[string]Player{"ann": {Score: 1}}
	bumpScore(players, "ann")
	check(players["ann"].Score == 2, "read, modify, write back")

	check(h1(1000) == 7 && h2(1000) == 104, "1000 = 7*128 + 104")
	check(slices.Equal(probeGroups(1000, 4), []uint64{3, 0, 2, 1}), "visits every group once")
	fmt.Println("OK")
}
