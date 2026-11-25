#!/usr/bin/env bash
# Shell lab: creates small files, runs every command shown in the lesson, compares the output, prints OK.
# Run from this folder with: bash shell_lab.sh
# Works with the BSD tools on macOS and the GNU tools on Linux (sed -E and POSIX awk only).
set -u
export LC_ALL=C

work=$(mktemp -d "${TMPDIR:-/tmp}/shell_lab.XXXXXX") || exit 1
trap 'rm -rf "$work"' EXIT
cd "$work" || exit 1

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

expect() { # expect <what> <actual> <expected>
  [ "$2" = "$3" ] || fail "$1: got [$2], expected [$3]"
}

printf '%s\n' 'INFO start' 'ERROR disk full' 'INFO retry' 'ERROR disk full' 'WARN slow' 'INFO done' > log.txt

# Level 1: stdout, stderr and exit codes.
actual=$(
grep -c ERROR log.txt
)
expect "count ERROR lines" "$actual" "2"

actual=$(
grep FATAL log.txt; echo "exit status: $?"
)
expect "no match is exit status 1" "$actual" "exit status: 1"

actual=$(
grep ERROR missing.txt 2> errors.txt; echo "exit status: $?"
)
expect "a missing file is exit status 2" "$actual" "exit status: 2"
[ -s errors.txt ] || fail "the error message went to stderr, so into errors.txt"

# Level 2: a pipeline is function composition.
actual=$(
cut -d ' ' -f 1 log.txt | sort | uniq -c | sort -rn | awk '{ print $2, $1 }'
)
expect "count lines per level" "$actual" "$(printf '%s\n' 'INFO 3' 'ERROR 2' 'WARN 1')"
expect "6 lines in" "$(cut -d ' ' -f 1 log.txt | awk 'END { print NR }')" "6"
expect "3 lines after uniq" "$(cut -d ' ' -f 1 log.txt | sort | uniq -c | awk 'END { print NR }')" "3"

# Level 3: the toolbox.
actual=$(
sed -E 's/^([A-Z]+) (.*)$/\2 [\1]/' log.txt | head -n 2
)
expect "sed -E with capture groups" "$actual" "$(printf '%s\n' 'start [INFO]' 'disk full [ERROR]')"

actual=$(
tail -n 1 log.txt | tr 'a-z' 'A-Z'
)
expect "tail and tr" "$actual" "INFO DONE"

# Level 4: awk keeps a table in memory, so it counts without sorting first.
actual=$(
awk '{ count[$1]++ } END { for (level in count) print level, count[level] }' log.txt | sort
)
expect "awk counts" "$actual" "$(printf '%s\n' 'ERROR 2' 'INFO 3' 'WARN 1')"

# Level 5: the exit status of a pipeline.
actual=$(
grep FATAL log.txt | sort; echo "exit status: $?"
)
expect "a pipeline reports its last command" "$actual" "exit status: 0"

actual=$(
set -o pipefail; grep FATAL log.txt | sort; echo "exit status: $?"
)
expect "pipefail reports the failure" "$actual" "exit status: 1"

actual=$(
grep -q ERROR log.txt && echo "found errors" || echo "all clear"
)
expect "&& and ||" "$actual" "found errors"

# Interview 1: LeetCode 195, Tenth Line.
seq 1 12 | sed 's/^/line /' > file.txt
actual=$(
sed -n '10p' file.txt
)
expect "LC 195" "$actual" "line 10"
seq 1 7 | sed 's/^/line /' > file.txt
actual=$(
sed -n '10p' file.txt
)
expect "LC 195 with only 7 lines prints nothing" "$actual" ""

# Interview 2: LeetCode 193, Valid Phone Numbers.
printf '%s\n' '987-123-4567' '123 456 7890' '(123) 456-7890' '(12) 345-6789' '555-0100' '(555)123-4567' > file.txt
actual=$(
grep -E '^(\([0-9]{3}\) |[0-9]{3}-)[0-9]{3}-[0-9]{4}$' file.txt
)
expect "LC 193" "$actual" "$(printf '%s\n' '987-123-4567' '(123) 456-7890')"

# Interview 3: LeetCode 192, Word Frequency.
printf '%s\n' 'red blue red green' 'red  blue red' 'blue red' > words.txt
actual=$(
tr -s ' ' '\n' < words.txt | sort | uniq -c | sort -rn | awk '{ print $2, $1 }'
)
expect "LC 192" "$actual" "$(printf '%s\n' 'red 5' 'blue 3' 'green 1')"

# Interview 4: LeetCode 194, Transpose File.
printf '%s\n' 'name city age' 'ana leeds 9' 'ben cork 10' > file.txt
actual=$(
awk '{ for (i = 1; i <= NF; i++) row[i] = (NR == 1 ? $i : row[i] " " $i); cols = NF }
     END { for (i = 1; i <= cols; i++) print row[i] }' file.txt
)
expect "LC 194" "$actual" "$(printf '%s\n' 'name ana ben' 'city leeds cork' 'age 9 10')"

echo OK
