#!/usr/bin/env bash
# Compiles the program and runs every tests/<name>.in, comparing the output with tests/<name>.expected.
# Exits non-zero if compilation fails or any test differs.
set -euo pipefail
cd "$(dirname "$0")"

rm -rf out
javac --release 21 -Xlint:all,-serial -Werror -d out src/*.java

tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT

pass=0
fail=0
for input in tests/*.in; do
    name="${input%.in}"
    if diff -u "$name.expected" <(java -cp out Main < "$input") > "$tmp"; then
        echo "PASS  $(basename "$name")"
        pass=$((pass + 1))
    else
        echo "FAIL  $(basename "$name")"
        cat "$tmp"
        fail=$((fail + 1))
    fi
done
echo "$pass passed, $fail failed"
[ "$fail" -eq 0 ]
