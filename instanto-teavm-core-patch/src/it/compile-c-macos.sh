#!/usr/bin/env sh
# TeaVM's C test runner invokes this inside the generated test directory.
set -eu
export LC_ALL=C.UTF-8
exec clang -std=c11 -g -iquote . -I custom-include -o run_test -D_DARWIN_C_SOURCE all.c
