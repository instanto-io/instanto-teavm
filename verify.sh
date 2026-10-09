#!/usr/bin/env bash
set -euo pipefail

task_root=$(cd "$(dirname "$0")" && pwd)
mvn -B -f "$task_root/pom.xml" -Pteavm-browser-tests,teavm-class-init-fix -Dspotless.skip=true \
  -Dinstanto.teavm.threadLocalChecks=true "$@" clean install
bash "$task_root/instanto-teavm-extensions/src/it/verify-consumer.sh" "$@"
bash "$task_root/instanto-teavm-core-patch/src/it/verify-consumer.sh" "$@"
