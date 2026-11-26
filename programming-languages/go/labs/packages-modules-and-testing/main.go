// Run from programming-languages/go/labs with: go run -race ./packages-modules-and-testing
// and test with: go test -race ./packages-modules-and-testing
package main

import (
	"fmt"
	"maps"
	"slices"

	"knowledge-library/go-labs/packages-modules-and-testing/internal/semver"
)

// Level 2: package initialisation. Variables first, in dependency order, then init functions.
var initLog []string

var total = price + tax // declared first, but initialised last: it needs price and tax
var price = record("price", 100)
var tax = record("tax", 20)

func record(name string, value int) int {
	initLog = append(initLog, name)
	return value
}

func init() { initLog = append(initLog, "init 1") }

func init() { initLog = append(initLog, "init 2") }

// Level 4: Minimal Version Selection.
type Version struct{ Path, Version string }

// Graph maps each module version to the requirements in its go.mod file.
type Graph map[Version][]Version

// BuildList visits everything reachable from root and keeps the highest version of each path.
func BuildList(g Graph, root Version) map[string]string {
	selected := map[string]string{}
	seen := map[Version]bool{}
	var visit func(v Version)
	visit = func(v Version) {
		if seen[v] {
			return // the graph may have cycles: F v1.1.0 and G v1.1.0 require each other
		}
		seen[v] = true
		if old, ok := selected[v.Path]; !ok || semver.Compare(v.Version, old) > 0 {
			selected[v.Path] = v.Version
		}
		for _, required := range g[v] {
			visit(required)
		}
	}
	visit(root)
	return selected
}

// The example graph from Russ Cox, "Minimal Version Selection" (2018).
var (
	a1  = Version{"A", "v1.0.0"}
	b11 = Version{"B", "v1.1.0"}
	b12 = Version{"B", "v1.2.0"}
	c12 = Version{"C", "v1.2.0"}
	c13 = Version{"C", "v1.3.0"}
	d11 = Version{"D", "v1.1.0"}
	d12 = Version{"D", "v1.2.0"}
	d13 = Version{"D", "v1.3.0"}
	d14 = Version{"D", "v1.4.0"}
	e11 = Version{"E", "v1.1.0"}
	e12 = Version{"E", "v1.2.0"}
	f11 = Version{"F", "v1.1.0"}
	g11 = Version{"G", "v1.1.0"}
)

var coxGraph = Graph{
	a1:  {b12, c12},
	b11: {d11},
	b12: {d13},
	c12: {d14},
	c13: {f11},
	d11: {e11},
	d12: {e11},
	d13: {e12},
	d14: {e12},
	f11: {g11},
	g11: {f11},
}

// Upgrading one module adds one requirement; it never removes the old ones.
func upgrade(g Graph, from, to Version) Graph {
	upgraded := maps.Clone(g)
	upgraded[from] = append(slices.Clone(g[from]), to)
	return upgraded
}

func check(ok bool, what string) {
	if !ok {
		panic(what)
	}
}

func main() {
	check(slices.Equal(initLog, []string{"price", "tax", "init 1", "init 2"}), "variables, then init functions")
	check(total == 120, "total = price + tax = 120")

	check(semver.Compare("v1.10.0", "v1.9.0") == 1, "10 > 9 as numbers")
	check("v1.10.0" < "v1.9.0", "but not as strings")

	built := BuildList(coxGraph, a1)
	check(maps.Equal(built, map[string]string{
		"A": "v1.0.0", "B": "v1.2.0", "C": "v1.2.0", "D": "v1.4.0", "E": "v1.2.0",
	}), "MVS picks the highest required version of each module")

	afterUpgrade := BuildList(upgrade(coxGraph, a1, c13), a1)
	check(maps.Equal(afterUpgrade, map[string]string{
		"A": "v1.0.0", "B": "v1.2.0", "C": "v1.3.0", "D": "v1.4.0", "E": "v1.2.0", "F": "v1.1.0", "G": "v1.1.0",
	}), "upgrading C keeps D at v1.4.0 and adds F and G")
	fmt.Println("OK")
}
