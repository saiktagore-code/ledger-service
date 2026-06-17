#!/usr/bin/env bash
set -euo pipefail

echo "Running gateway-service build and tests (with coverage check)..."
cd gateway-service
mvn -B clean verify

echo "Running account-service build and tests (with coverage check)..."
cd ../account-service
mvn -B clean verify

echo "Local checks completed successfully."