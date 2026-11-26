// Run from programming-languages/go/labs with: go run -race ./strings-bytes-and-runes
package main

import (
	"fmt"
	"slices"
	"strings"
	"testing"
	"unicode/utf8"
	"unsafe"
)

const word = "hé世🙂" // 1 + 2 + 3 + 4 bytes

// Level 1: a string is a read-only sequence of bytes.
func byteBasics() (int, byte, string) {
	s := "hello"
	return len(s), s[0], s[1:3]
}

func capitalise(s string) string {
	b := []byte(s) // a writable copy of the bytes
	b[0] = 'H'
	return string(b) // another copy, back into an immutable string
}

// Level 2: UTF-8 uses 1 to 4 bytes per code point.
func encode2(cp rune) [2]byte {
	return [2]byte{
		0xC0 | byte(cp>>6),   // 110xxxxx: the top 5 bits
		0x80 | byte(cp&0x3F), // 10xxxxxx: the low 6 bits
	}
}

// Level 3: len counts bytes; range decodes runes.
func runeStarts(s string) []int {
	var starts []int
	for i := range s { // i is the first byte of each rune
		starts = append(starts, i)
	}
	return starts
}

// Level 4: conversions decode, encode and copy.
func reverseBytes(s string) string {
	b := []byte(s)
	slices.Reverse(b)
	return string(b)
}

func reverseRunes(s string) string {
	r := []rune(s) // decode: one 4-byte rune per code point
	slices.Reverse(r)
	return string(r) // encode back to UTF-8
}

// Level 5: build strings with strings.Builder.
func joinPlus(parts []string) string {
	s := ""
	for _, p := range parts {
		s += p // each += copies everything so far into a new string
	}
	return s
}

func joinBuilder(parts []string) string {
	var b strings.Builder // the zero value is ready to use
	for _, p := range parts {
		b.WriteString(p) // appends to one growing buffer
	}
	return b.String() // no copy: String reuses the buffer
}

func copiedBuilder() {
	var a strings.Builder
	a.WriteString("x")
	b := a // copies the Builder, including its pointer to itself
	b.WriteString("y")
}

// bytesCopied counts what joinPlus copies for n parts of size bytes each.
func bytesCopied(n, size int) int {
	total := 0
	for k := 1; k <= n; k++ {
		total += k * size // the k-th += builds a string of k*size bytes
	}
	return total
}

// Level 6: invalid UTF-8 decodes as U+FFFD, one byte at a time.
func decodeInvalid() (rune, int, bool) {
	bad := "a\xffb" // the byte 0xFF never appears in UTF-8
	r, size := utf8.DecodeRuneInString(bad[1:])
	return r, size, utf8.ValidString(bad)
}

func runesOf(s string) []rune {
	var runes []rune
	for _, r := range s {
		runes = append(runes, r)
	}
	return runes
}

// Underneath: a substring points into the same bytes.
func substringShares(s string) bool {
	start := unsafe.StringData(s)
	sub := unsafe.StringData(s[1:])
	return sub == (*byte)(unsafe.Add(unsafe.Pointer(start), 1))
}

var sinkString string

func conversionAllocs(b []byte, m map[string]int) (float64, float64) {
	convert := testing.AllocsPerRun(100, func() { sinkString = string(b) })
	lookup := testing.AllocsPerRun(100, func() { _ = m[string(b)] }) // no copy for a lookup
	return convert, lookup
}

var sinkBool bool

func comparisonAllocs(b []byte) float64 {
	return testing.AllocsPerRun(100, func() { sinkBool = string(b) == "x" }) // no copy to compare
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
	n, first, middle := byteBasics()
	check(n == 5 && first == 104 && middle == "el", "bytes of hello")
	check('h' == 104 && utf8.RuneError == 0xFFFD, "a byte is a number")
	check(capitalise("hello") == "Hello", "change a []byte copy")

	check(len("h") == 1 && len("é") == 2 && len("世") == 3 && len("🙂") == 4, "1, 2, 3, 4 bytes")
	check(utf8.RuneLen('é') == 2 && utf8.RuneLen('🙂') == 4, "RuneLen agrees")
	check('é' == 0xE9 && '世' == 0x4E16 && '🙂' == 0x1F642, "code points")
	check(encode2('é') == [2]byte{0xC3, 0xA9} && string([]byte{0xC3, 0xA9}) == "é", "é is C3 A9")

	check(len(word) == 10 && utf8.RuneCountInString(word) == 4, "10 bytes, 4 runes")
	check(word[1] == 0xC3, "indexing gives a byte, not a character")
	check(slices.Equal(runeStarts(word), []int{0, 1, 3, 6}), "range jumps 1, 2, 3 bytes")

	check(!utf8.ValidString(reverseBytes("hé")) && reverseBytes("hé") == "\xa9\xc3h", "bytes reversed")
	check(reverseRunes(word) == "🙂世éh", "runes reversed")
	check(len([]rune(word)) == 4 && unsafe.Sizeof(rune(0)) == 4, "4 runes of 4 bytes")
	check(reverseRunes("éx") == "x́e", "the accent moved onto x")

	parts := []string{"abc", "def", "ghi", "jkl"}
	check(joinPlus(parts) == "abcdefghijkl" && joinBuilder(parts) == "abcdefghijkl", "same text")
	check(bytesCopied(4, 3) == 30 && 3*4*5/2 == 30, "L n(n+1)/2 = 30")
	check(bytesCopied(1000, 10) == 5005000, "1000 parts of 10 bytes")
	check(panicMessage(copiedBuilder) == "strings: illegal use of non-zero Builder copied by value",
		"a copied Builder panics")

	r, size, valid := decodeInvalid()
	check(r == utf8.RuneError && size == 1 && !valid, "U+FFFD, width 1")
	check(slices.Equal(runesOf("a\xffb"), []rune{'a', 0xFFFD, 'b'}), "range yields U+FFFD")
	check(strings.ToValidUTF8("a\xffb", "?") == "a?b", "replace the bad byte")
	realR, realSize := utf8.DecodeRuneInString("�")
	check(realR == utf8.RuneError && realSize == 3, "a real U+FFFD is 3 bytes")

	check(unsafe.Sizeof("") == 16 && substringShares(word), "header 16 bytes; s[1:] shares")
	long := []byte("a key longer than the 32-byte buffer")
	convert, lookup := conversionAllocs(long, map[string]int{string(long): 1})
	check(convert == 1 && lookup == 0, "string(b) copies; m[string(b)] does not")
	check(len(long) == 36 && comparisonAllocs(long) == 0, "string(b) == \"x\" does not copy")
	fmt.Println("OK")
}
