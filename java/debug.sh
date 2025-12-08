#!/bin/sh

bazel run --jvmopt=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005 --jvmopt=--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED //org/brotli/dec:decode_fuzz_test