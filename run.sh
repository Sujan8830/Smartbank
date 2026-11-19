#!/usr/bin/env bash
# Linux helper: ./run.sh console   |   ./run.sh api   |   ./run.sh logs
set -e
cd "$(dirname "$0")/backend"
case "$1" in
  console) mvn -q compile exec:java ;;
  api)     nohup mvn -q compile exec:java -Dexec.args="api" > ../smartbank.log 2>&1 &
           echo "API started, PID $!  (tail -f smartbank.log)" ;;
  logs)    tail -f ../smartbank.log ;;
  *)       echo "Usage: $0 {console|api|logs}" ;;
esac
