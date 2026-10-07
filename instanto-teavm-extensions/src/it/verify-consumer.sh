#!/usr/bin/env bash
set -euo pipefail

task_root=$(cd "$(dirname "$0")/../../.." && pwd)
fixture="$task_root/instanto-teavm-extensions/src/it/consumer"
outputs="$task_root/instanto-teavm-extensions/target"

for mode in baseline default checked; do
  args=(-Dspotless.skip=true -Dinstanto.teavm.threadLocalChecks=false)
  if [[ "$mode" != baseline ]]; then
    args+=(-Pextensions)
  fi
  if [[ "$mode" == checked ]]; then
    args+=(-Dinstanto.teavm.threadLocalChecks=true)
  fi
  mvn -B -q -f "$fixture/pom.xml" \
    "-Dfixture.outputDirectory=$outputs/consumer-$mode" "$@" "${args[@]}" package
  expected=0
  if [[ "$mode" == checked ]]; then expected=7; fi
  node "$fixture/check.mjs" "$outputs/consumer-$mode/app.mjs" "$expected"
done

cmp "$outputs/consumer-baseline/app.mjs" "$outputs/consumer-default/app.mjs"
echo 'Extension disabled: generated JavaScript is byte-for-byte identical to baseline.'
