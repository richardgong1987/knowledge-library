// Package semver compares module versions of the form vMAJOR.MINOR.PATCH.
// It lives under internal/, so only code inside packages-modules-and-testing/ may import it.
package semver

import (
	"cmp"
	"fmt"
)

// Compare returns -1, 0 or +1 when version a is older than, equal to, or newer than b.
// Both versions must be valid; Compare panics otherwise, like regexp.MustCompile.
func Compare(a, b string) int {
	x, y := parse(a), parse(b)
	for i := range x {
		if c := cmp.Compare(x[i], y[i]); c != 0 {
			return c
		}
	}
	return 0
}

// parse starts with a lower-case letter, so it is not exported: other packages cannot call it.
func parse(version string) [3]int {
	var n [3]int
	if _, err := fmt.Sscanf(version, "v%d.%d.%d", &n[0], &n[1], &n[2]); err != nil {
		panic(fmt.Sprintf("semver: invalid version %q", version))
	}
	return n
}
