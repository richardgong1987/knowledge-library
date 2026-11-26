package main

import (
	"fmt"
	"maps"
	"slices"
	"testing"

	"knowledge-library/go-labs/packages-modules-and-testing/internal/semver"
)

func TestCompare(t *testing.T) {
	tests := []struct {
		name string
		a, b string
		want int
	}{
		{"equal", "v1.2.0", "v1.2.0", 0},
		{"newer patch", "v1.2.1", "v1.2.0", 1},
		{"minor beats patch", "v1.2.0", "v1.1.9", 1},
		{"numbers, not text", "v1.9.0", "v1.10.0", -1},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel() // subtests run at the same time; tt is per-iteration since Go 1.22
			if got := semver.Compare(tt.a, tt.b); got != tt.want {
				t.Errorf("Compare(%q, %q) = %d, want %d", tt.a, tt.b, got, tt.want)
			}
		})
	}
}

func TestBuildList(t *testing.T) {
	got := BuildList(coxGraph, a1)
	want := map[string]string{"A": "v1.0.0", "B": "v1.2.0", "C": "v1.2.0", "D": "v1.4.0", "E": "v1.2.0"}
	if !maps.Equal(got, want) {
		t.Errorf("BuildList(coxGraph, A) = %v, want %v", got, want)
	}
}

func BenchmarkBuildList(b *testing.B) {
	for b.Loop() { // Go 1.24+: runs the body as often as needed and times only the loop
		BuildList(coxGraph, a1)
	}
}

func FuzzCompareIsAntisymmetric(f *testing.F) {
	f.Add(1, 9, 0, 1, 10, 0) // seed corpus: also run by plain go test
	f.Fuzz(func(t *testing.T, aMajor, aMinor, aPatch, bMajor, bMinor, bPatch int) {
		a := fmt.Sprintf("v%d.%d.%d", aMajor, aMinor, aPatch)
		b := fmt.Sprintf("v%d.%d.%d", bMajor, bMinor, bPatch)
		if semver.Compare(a, b) != -semver.Compare(b, a) {
			t.Errorf("Compare(%s, %s) is not minus Compare(%s, %s)", a, b, b, a)
		}
	})
}

func ExampleBuildList() {
	list := BuildList(coxGraph, a1)
	for _, path := range slices.Sorted(maps.Keys(list)) {
		fmt.Println(path, list[path])
	}
	// Output:
	// A v1.0.0
	// B v1.2.0
	// C v1.2.0
	// D v1.4.0
	// E v1.2.0
}
