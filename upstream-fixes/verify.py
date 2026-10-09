#!/usr/bin/env python3
"""Test draft TeaVM 0.16.0 fixes without changing a checkout or Maven artifacts.

Requires Java 21, Maven and Chrome. Pass a TeaVM Git checkout containing tag
0.16.0. Each invocation retains its isolated fixture and Maven log for review.
Example: python3 upstream-fixes/verify.py --teavm-source ../teavm --level FULL
Add --stock to reproduce failures without compiler replacements.
"""

import argparse
from pathlib import Path
import shutil
import subprocess
import tempfile


CASES = {
    "class-init": ["ClassInitializationTest", "UmbrellaCatchTest", "JoinAfterCatchTest"],
    "field": ["RepeatedFieldReadTest", "RepeatedFieldReadEliminationTest"],
    "timer": ["TimerTest", "GlobalInvocationTest"],
    "minifier": ["MinifierCaptureTest", "TopLevelCollisionTest"],
    "wasm": ["PromiseTest", "PromiseVariantsTest"],
}
BASELINE = "0.16.0"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--teavm-source", type=Path, required=True)
    parser.add_argument("--case", choices=["all", *CASES], default="all")
    parser.add_argument("--level", choices=["SIMPLE", "ADVANCED", "FULL"], default="FULL")
    parser.add_argument("--stock", action="store_true")
    args = parser.parse_args()
    source = args.teavm_source.resolve()
    here = Path(__file__).resolve().parent
    work = Path(tempfile.mkdtemp(prefix="teavm-fix-review-"))
    fixture = work / "fixture"
    shutil.copytree(here / "src", fixture / "src")
    shutil.copyfile(here / "pom.xml", fixture / "pom.xml")
    cases = list(CASES) if args.case == "all" else [args.case]

    def upstream(name):
        return subprocess.check_output(
            ["git", "show", f"{BASELINE}:{name}"], cwd=source, text=True
        )

    def test_source(name, content):
        path = fixture / "src/test/java" / name
        path.parent.mkdir(parents=True, exist_ok=True)
        # Maven distributions relocate these libraries; upstream source does not.
        content = content.replace("com.carrotsearch.hppc", "org.teavm.hppc")
        content = content.replace("org.mozilla.javascript", "org.teavm.rhino.javascript")
        path.write_text(content)

    if not args.stock:
        tree = work / "patched-source"
        tree.mkdir()
        for case in cases:
            patch = here / "patches" / f"{case}.patch"
            names = [line[6:] for line in patch.read_text().splitlines() if line.startswith("--- a/")]
            for name in names:
                path = tree / name
                path.parent.mkdir(parents=True, exist_ok=True)
                # Several patches may touch one file; start from upstream only once.
                if not path.exists():
                    path.write_text(upstream(name))
            subprocess.run(["git", "apply", "--check", str(patch)], cwd=tree, check=True)
            subprocess.run(["git", "apply", str(patch)], cwd=tree, check=True)
            for name in names:
                test_source(name.split("/src/main/java/")[1], (tree / name).read_text())

    if "field" in cases:
        prefix = "core/src/test/"
        names = subprocess.check_output(
            ["git", "ls-tree", "-r", "--name-only", BASELINE, prefix], cwd=source, text=True
        ).splitlines()
        for name in names:
            if ("repeated-field-read-elimination/" in name
                    or name.endswith("/RepeatedFieldReadEliminationTest.java")
                    or name.endswith("/ListingParseUtils.java")):
                content = upstream(name)
                if name.endswith("/RepeatedFieldReadEliminationTest.java"):
                    content = content.replace("    private void doTest() {", """    @Test
    public void readInsideException() {
        doTest();
    }

    private void doTest() {""")
                path = fixture / "src/test" / name.removeprefix(prefix)
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(content)

    # The stock runner offers SIMPLE and FULL, but not ADVANCED. Select the
    # requested level in its configuration only; all compiler passes stay active.
    name = "tools/junit/src/main/java/org/teavm/junit/TeaVMTestConfiguration.java"
    configuration = upstream(name).replace("TeaVMOptimizationLevel.SIMPLE", "TeaVMOptimizationLevel." + args.level)
    test_source("org/teavm/junit/TeaVMTestConfiguration.java", configuration)
    tests = [test for case in cases for test in CASES[case]]
    command = ["mvn", "-Dspotless.skip=true", "-Dtest=" + ",".join(tests),
               "-Dteavm.junit.optimized=false", "-Dteavm.junit.minified=true", "test"]
    log = work / "maven.log"
    print(f"{args.case}: {'stock' if args.stock else 'patched'} {BASELINE}, {args.level}", flush=True)
    print(f"Fixture: {fixture}\nLog: {log}", flush=True)
    with log.open("w") as output:
        result = subprocess.run(command, cwd=fixture, stdout=output, stderr=subprocess.STDOUT)
    for line in log.read_text().splitlines():
        if "Tests run:" in line or "BUILD SUCCESS" in line or "BUILD FAILURE" in line:
            print(line)
    raise SystemExit(result.returncode)


if __name__ == "__main__":
    main()
