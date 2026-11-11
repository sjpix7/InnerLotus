#!/bin/bash
# Compiles the Android-free sources with a stock Kotlin compiler and runs PureTests.
# Usage: tools/jvmtest/run.sh [outDir] [secondsPerSound]
# Needs: kotlinc-jvm (or the Gradle-embedded compiler at /opt/gradle/lib), kotlinx-serialization-json and kotlinx-coroutines jars.
set -e
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${1:-$ROOT/build/jvmtest}"; SECS="${2:-12}"
L=${GRADLE_LIB:-/opt/gradle/lib}
CP_RT="$L/kotlin-stdlib-2.0.21.jar:$L/kotlinx-serialization-core-jvm-1.6.2.jar:$L/kotlinx-serialization-json-jvm-1.6.2.jar:$L/kotlinx-coroutines-core-jvm-1.6.4.jar"
CP_C="$L/kotlin-compiler-embeddable-2.0.21.jar:$L/kotlin-stdlib-2.0.21.jar:$L/kotlin-script-runtime-2.0.21.jar:$L/kotlin-reflect-2.0.21.jar:$L/kotlin-daemon-embeddable-2.0.21.jar:$L/trove4j-1.0.20200330.jar:$L/annotations-24.0.1.jar:$L/kotlinx-coroutines-core-jvm-1.6.4.jar"
SRC="$ROOT/app/src/main/java/com/sjpixels/innerlotus"
FILES="$SRC/core/Lang.kt $SRC/core/StringTable.kt $SRC/core/Catalog.kt $SRC/content/Composer.kt $SRC/content/Practices.kt $(ls $SRC/data/*.kt) $(ls $SRC/audio/dsp/*.kt) $ROOT/tools/jvmtest/PureTests.kt"
mkdir -p "$OUT/classes"
java -Xmx2g -cp "$CP_C" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -nowarn -classpath "$CP_RT" -d "$OUT/classes" $FILES 2>&1 | grep -v "JAVA_TOOL_OPTIONS" || true
java -cp "$OUT/classes:$CP_RT" PureTestsKt "$OUT" "$SECS" 2>&1 | grep -v "JAVA_TOOL_OPTIONS"
