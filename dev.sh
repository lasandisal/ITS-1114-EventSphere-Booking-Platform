#!/usr/bin/env bash

# =============================================================================
# EventSphere Backend Development Server Startup Script (dev.sh)
# Clean Unix process lifecycle management with SIGINT/SIGTERM traps.
# =============================================================================

set -e

# 1. Read PORT from .env configuration (or default to 7080)
PORT=7080
if [ -f ".env" ]; then
  ENV_PORT=$(grep -E '^PORT=' .env | cut -d '=' -f2 | tr -d '\r" '\''')
  if [ -n "$ENV_PORT" ]; then
    PORT="$ENV_PORT"
  fi
fi

# 2. Configure Java 21 Home
JAVA_21_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
if [ -d "$JAVA_21_HOME" ]; then
  export JAVA_HOME="$JAVA_21_HOME"
fi

# 3. Print clean startup banner
echo "EventSphere backend starting..."
echo "Java: 21"
echo "Port: $PORT"

# 4. Track child PID for targeted process tree cleanup
MAVEN_PID=""

cleanup() {
  # Prevent re-entrant trap calls
  trap - INT TERM EXIT
  echo ""
  echo "Stopping EventSphere backend..."

  if [ -n "$MAVEN_PID" ] && kill -0 "$MAVEN_PID" 2>/dev/null; then
    # Collect direct child PIDs (such as the spawned Spring Boot Java process)
    CHILD_PIDS=$(pgrep -P "$MAVEN_PID" 2>/dev/null || true)

    # Gracefully terminate Maven and its child processes
    kill -TERM "$MAVEN_PID" 2>/dev/null || true
    if [ -n "$CHILD_PIDS" ]; then
      kill -TERM $CHILD_PIDS 2>/dev/null || true
    fi

    # Wait up to 5 seconds for clean shutdown
    count=0
    while kill -0 "$MAVEN_PID" 2>/dev/null && [ $count -lt 25 ]; do
      sleep 0.2
      count=$((count + 1))
    done

    # Force kill if process tree is still alive
    if kill -0 "$MAVEN_PID" 2>/dev/null; then
      kill -9 "$MAVEN_PID" 2>/dev/null || true
      if [ -n "$CHILD_PIDS" ]; then
        kill -9 $CHILD_PIDS 2>/dev/null || true
      fi
    fi

    wait "$MAVEN_PID" 2>/dev/null || true
  fi

  echo "Backend stopped."
  echo "Port $PORT released."
  exit 0
}

# Register traps for SIGINT (Ctrl+C), SIGTERM, and EXIT
trap cleanup INT TERM EXIT

# 5. Launch Spring Boot through Maven wrapper
sh mvnw spring-boot:run &
MAVEN_PID=$!

# Wait for Maven process to keep dev.sh alive
wait "$MAVEN_PID"
