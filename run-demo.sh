#!/usr/bin/env bash
# One-command demo (Linux/macOS/Git Bash): builds, starts the server, sends both sample files with
# the client, prints the output file and the database contents, then stops the server.
# Needs JDK 11+ and Maven.
#
# Server and client only take a port, everything else is a fixed relative path (input/, output/,
# messages.db). So both are started from target/demo, which gets its own copy of input/ and
# never touches the project's own files.
set -euo pipefail
cd "$(dirname "$0")"

PORT="${PORT:-5077}"
DEMO=target/demo
CP="../classes:../lib/*"          # relative to target/demo

if [ "${SKIP_BUILD:-0}" != "1" ]; then
  echo "== Building (first run downloads dependencies) =="
  mvn -q -DskipTests package dependency:copy-dependencies -DoutputDirectory=target/lib
fi

rm -rf "$DEMO" && mkdir -p "$DEMO" && cp -r input "$DEMO/input"
cd "$DEMO"

echo "== Starting server on port $PORT =="
java -cp "$CP" com.globalpayments.task.ServerMain "$PORT" > server.log 2>&1 &
SERVER_PID=$!
trap 'kill $SERVER_PID 2>/dev/null || true' EXIT

for _ in $(seq 1 100); do
  grep -q "listening" server.log 2>/dev/null && break
  kill -0 $SERVER_PID 2>/dev/null || { echo "Server failed to start:"; cat server.log; exit 1; }
  sleep 0.1
done

# The client always reads input/sample-input-hr.bin (sample 2, Croatian characters).
echo "== Client sends sample 2: input/sample-input-hr.bin =="
java -cp "$CP" com.globalpayments.task.ClientMain "$PORT"

# To send sample 1 as well, the demo copy of that file is replaced with the ASCII sample.
echo "== Client sends sample 1: input/sample-input-ascii.bin =="
cp input/sample-input-ascii.bin input/sample-input-hr.bin
java -cp "$CP" com.globalpayments.task.ClientMain "$PORT"

# wait until the server has finished both connections
for _ in $(seq 1 100); do
  [ "$(grep -c ' done: ' server.log)" -ge 2 ] && break
  sleep 0.1
done
kill $SERVER_PID 2>/dev/null || true

echo
echo "== Server log =="
cat server.log
echo
echo "== Output file (target/demo/output/messages.txt, UTF-8) =="
cat output/messages.txt
echo
java -cp "$CP" com.globalpayments.task.QueryMain
