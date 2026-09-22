#!/usr/bin/env bash
# ========================================================
# Goal Decomposition Engine - 1-Click Startup Script (macOS/Linux)
# ========================================================

echo "========================================================"
echo "  Goal Decomposition Engine (Spring Boot + SQLite)"
echo "  100% Local, Offline, Rule-Based Project"
echo "========================================================"

if ! command -v java &> /dev/null; then
    echo "[ERROR] Java 17+ is required but not found in PATH."
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "[ERROR] Maven is required but not found in PATH."
    exit 1
fi

echo "[INFO] Starting application on http://localhost:8080..."

# Open browser if possible
if command -v xdg-open &> /dev/null; then
    (sleep 4 && xdg-open http://localhost:8080) &
elif command -v open &> /dev/null; then
    (sleep 4 && open http://localhost:8080) &
fi

mvn spring-boot:run
