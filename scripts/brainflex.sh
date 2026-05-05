#!/usr/bin/env bash

# Script to start entire app in dev mode
trap 'echo -e "\nStopping all services..."; kill 0; docker compose stop; exit' SIGINT SIGTERM

echo "🐳 Starting Docker containers..."

docker compose up -d

echo "⚛️ Starting Frontend..."

(cd frontend && npm run dev) &

echo "🍃 Starting Backend..."
(
  set -a
  source .env
  set +a
  cd backend
  ./mvnw spring-boot:run
) &

echo "🚀 All services are booting up! Press Ctrl+C to stop everything."

wait