#!/bin/sh

#bazel run //org/brotli/dec:round_trip_fuzz_test crash-6360f0607badad289eeb482dc185b9d7b45c48d9

bazel run --jvmopt=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005 --jvmopt=--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED //org/brotli/dec:round_trip_fuzz_test crash-6360f0607badad289eeb482dc185b9d7b45c48d9
