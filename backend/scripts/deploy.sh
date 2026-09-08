#!/usr/bin/env bash
set -euo pipefail

REPO="joe-bor/family-hub-api"
SERVER="root@joe-bor.me"
COMPOSE_DIR="/opt/familyhub"
COMPOSE_FILE="docker-compose.prod.yml"
SERVICE="api"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

echo "Fetching latest release version..."
# The `|| BE_VERSION=""` is required: a failed command substitution does not
# reliably trigger `set -e`, so we catch the failure here and let the `-z`
# guard below fail closed.
BE_VERSION="$(bash "$SCRIPT_DIR/resolve-release-version.sh")" || BE_VERSION=""

if [ -z "$BE_VERSION" ]; then
  echo "No published backend release could be resolved; refusing to deploy." >&2
  exit 1
fi

echo "Deploying version: ${BE_VERSION}"

ssh "$SERVER" bash -s "$BE_VERSION" "$COMPOSE_DIR" "$COMPOSE_FILE" "$SERVICE" <<'REMOTE'
set -euo pipefail
BE_VERSION="$1"
COMPOSE_DIR="$2"
COMPOSE_FILE="$3"
SERVICE="$4"

cd "$COMPOSE_DIR"
export BE_IMAGE_TAG="$BE_VERSION"

echo "Pulling images..."
docker compose -f "$COMPOSE_FILE" pull --quiet "$SERVICE"

echo "Starting services..."
docker compose -f "$COMPOSE_FILE" up -d "$SERVICE"

echo "Pruning old images..."
docker image prune -f

echo "Waiting for container to be healthy..."
for i in $(seq 1 15); do
  STATUS=$(docker compose -f "$COMPOSE_FILE" ps "$SERVICE" --format '{{.Health}}' 2>/dev/null || echo "unknown")
  if [ "$STATUS" = "healthy" ]; then
    echo "Service is healthy"
    exit 0
  fi
  sleep 2
done

echo "Warning: service did not become healthy within 30s"
docker compose -f "$COMPOSE_FILE" logs --tail 20 "$SERVICE"
exit 1
REMOTE
