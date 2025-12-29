#!/usr/bin/env bash

set -eu

BAZEL_SAN_FLAGS=(
  --repo_env=CC=clang
  --repo_env=CXX=clang++
  --copt=-fsanitize=address
  --copt=-fsanitize=fuzzer-no-link
  --copt=-fno-omit-frame-pointer
  --copt=-fno-stack-protector
  --copt=-U_FORTIFY_SOURCE
  --copt=-D_FORTIFY_SOURCE=0
  --copt=-g
  --linkopt=-fsanitize=address
  --linkopt=-fsanitize=fuzzer-no-link
  --linkopt=-shared-libasan
  --strip=never
)

run_bazel() {
  local subcommand=$1
  shift
  bazel "$subcommand" "${BAZEL_SAN_FLAGS[@]}" "$@"
}

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR"

# Build instrumented brotli lib
run_bazel build //:brotli_jni.dll

COVERAGE_DIR="$SCRIPT_DIR/coverage"
mkdir -p "$COVERAGE_DIR"
find "$COVERAGE_DIR" -maxdepth 1 -name '*.lcov.dat' -type f -delete
rm -f "$COVERAGE_DIR/lcov.info"

TARGETS=(
  //org/brotli/dec:decode_fuzz_test
  //org/brotli/dec:round_trip_fuzz_test
  //org/brotli/dec:diff_fuzz_test
)

# Run fuzz tests
for target in "${TARGETS[@]}"; do
  name=${target##*:}
  corpus_dir="corpus/$name"
  mkdir -p "$corpus_dir"
  echo ""
  echo "Running $target with corpus $corpus_dir"
  run_bazel run "$target" -- --asan "$(realpath "$corpus_dir")" -max_total_time=99999

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

# Get coverage
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
