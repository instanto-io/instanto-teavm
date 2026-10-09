#!/usr/bin/env bash
set -euo pipefail

task_root=$(cd "$(dirname "$0")/../../.." && pwd)
fixture="$task_root/instanto-teavm-core-patch/src/it/consumer"
outputs="$task_root/instanto-teavm-core-patch/target"

# A successfully compiled stock application must fail at runtime. This ensures
# the consumer actually exercises the compiler defect, not just plugin loading.
mvn -B -q -f "$fixture/pom.xml" "$@" -Dspotless.skip=true \
  -P'!teavm-class-init-fix' -DskipTests \
  "-Dfixture.outputDirectory=$outputs/consumer-stock" package
if node "$fixture/check.mjs" "$outputs/consumer-stock/app.mjs" > "$outputs/consumer-stock.log" 2>&1; then
  echo 'Expected the stock TeaVM consumer to fail.' >&2
  exit 1
fi
if ! grep -Eq 'NullPointerException|Cannot read properties of null' "$outputs/consumer-stock.log"; then
  cat "$outputs/consumer-stock.log" >&2
  exit 1
fi

for optimization in SIMPLE ADVANCED FULL; do
  for minifying in false true; do
    args=(-DskipTests)
    if [[ "$optimization" == SIMPLE && "$minifying" == false ]]; then
      args=(-DskipTests=false -Pteavm-class-init-fix,teavm-browser-tests)
    fi
    destination="$outputs/consumer-$optimization-$minifying"
    mvn -B -q -f "$fixture/pom.xml" "$@" -Dspotless.skip=true \
      -Pteavm-class-init-fix "${args[@]}" \
      "-Dfixture.optimization=$optimization" "-Dfixture.minifying=$minifying" \
      "-Dfixture.outputDirectory=$destination" package
    node "$fixture/check.mjs" "$destination/app.mjs"
  done
done

if mvn -B -q -f "$fixture/pom.xml" "$@" -Dspotless.skip=true \
    -Pteavm-class-init-fix -Dteavm.version=0.15.0 validate > "$outputs/version-guard.log" 2>&1; then
  echo 'Expected the compiler patch to reject a different TeaVM version.' >&2
  exit 1
fi
grep -q 'class initialization patch requires TeaVM 0.16.0' "$outputs/version-guard.log"
echo 'Compiler patch: stock failure, patched compiler/test consumers, and version guard verified.'
