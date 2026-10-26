#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
mkdir -p build/engine-test
"$TASK_JAVA_HOME/bin/javac" -J-Xmx192m -encoding UTF-8 -cp ".deps/ort-desktop.jar:.deps/json.jar" -d build/engine-test app/src/com/jitu/fluidspeech/AsrEngine.java tests/EngineSmokeTest.java
/usr/bin/time -l nice -n 19 "$TASK_JAVA_HOME/bin/java" -Xmx192m -cp "build/engine-test:.deps/ort-desktop.jar:.deps/json.jar" com.jitu.fluidspeech.EngineSmokeTest .validation/model .validation/jfk.wav
