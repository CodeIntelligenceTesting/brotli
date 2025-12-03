#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR"

COVERAGE_DIR="$SCRIPT_DIR/coverage"
mkdir -p "$COVERAGE_DIR"
find "$COVERAGE_DIR" -maxdepth 1 -name '*.lcov.dat' -type f -delete
rm -f "$COVERAGE_DIR/lcov.info"

TARGETS=$(bazel query 'kind("java_test", filter("fuzz", //org/brotli/dec:all))')
if [ -z "$TARGETS" ]; then
  echo "No fuzz tests found under //org/brotli/dec" >&2
  exit 1
fi

# Run fuzz tests
for target in $TARGETS; do
  name=${target##*:}
  corpus_dir="corpus/$name"
  mkdir -p "$corpus_dir"
  echo ""
  echo "Running $target with corpus $corpus_dir"
  bazel run "$target" -- "$(realpath "$corpus_dir")" -max_total_time=6

  echo ""
  echo "Collecting coverage for $target"
  bazel coverage --combined_report=lcov --test_output=errors \
    --test_arg="$(realpath "$corpus_dir")" \
    --test_arg=-runs=1 \
    "$target"

  report_path="bazel-out/_coverage/_coverage_report.dat"
  if [ ! -f "$report_path" ]; then
    report_path=$(find bazel-out -name '_coverage_report.dat' -print | head -n 1 || true)
  fi

  dest="$COVERAGE_DIR/$name.lcov.dat"
  cp "$report_path" "$dest"
  echo "Stored LCOV trace at $dest"
done

dat_files=$(find "$COVERAGE_DIR" -maxdepth 1 -name '*.lcov.dat' -type f | sort)
combined="lcov.info"

echo ""
echo "Merging LCOV traces into $combined"
rm -f "$combined"
(
  set -- lcov
  for file in $dat_files; do
    set -- "$@" --add-tracefile "$file"
  done
  set -- "$@" --output-file "$combined"
  "$@"
)

