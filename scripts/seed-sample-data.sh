#!/usr/bin/env bash

# Manually populates MongoDB with LOTR-themed sample data.
#
# Loads dev.env, then runs the Spring Boot app with --seed.run=true so
# SampleDataSeeder fires once and the app exits. Idempotent per collection:
# each user only gets a theme/deck/org slot if they don't already have one.
# Nothing is ever deleted.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Sourcing dev.env mirrors scripts/brainflex.sh. dev.env may contain stray
# non-assignment lines (legacy comments) that bash will report as errors; we
# tolerate them so the env vars we do need still land in the environment.
if [ -f "${PROJECT_ROOT}/dev.env" ]; then
  set -a
  # shellcheck disable=SC1091
  source "${PROJECT_ROOT}/dev.env" 2>/dev/null || true
  set +a
fi

cd "${PROJECT_ROOT}/backend"
# server.port=0 binds to a random free port so this can run alongside a
# normally-running backend on 8080 without a bind collision.
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--seed.run=true --server.port=0"
