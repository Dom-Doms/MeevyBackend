#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

# Il deployment deve operare esclusivamente dal branch di deploy.
BRANCH="$(git branch --show-current)"
if [[ "$BRANCH" != "deploy/docker" ]]; then
    echo "ERRORE: branch corrente '$BRANCH'; richiesto 'deploy/docker'." >&2
    exit 1
fi

echo "1) Aggiorno deploy/docker (solo fast-forward)..."
git pull --ff-only origin deploy/docker

echo "2) Verifico la configurazione Compose..."
docker compose config --quiet

echo "3) Ricostruisco e avvio Meevy..."
docker compose up -d --build

echo "4) Stato container:"
docker compose ps

echo "Commit distribuito: $(git rev-parse --short HEAD)"
