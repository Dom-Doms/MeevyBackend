#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

BRANCH="$(git branch --show-current)"
if [[ "$BRANCH" != "deploy/docker" ]]; then
    echo "ERRORE: branch corrente '$BRANCH'; richiesto 'deploy/docker'." >&2
    exit 1
fi

# Aggiorna prima i file Compose, senza fare merge automatici.
git pull --ff-only origin deploy/docker

# Tag immutabile: se la CI non ha ancora pubblicato questo commit,
# il pull fallisce e i container esistenti rimangono in funzione.
export MEEVY_IMAGE_TAG="sha-$(git rev-parse HEAD)"

echo "Deploy image: ghcr.io/dom-doms/meevybackend:${MEEVY_IMAGE_TAG}"
docker compose config --quiet

echo "Scarico l'immagine già compilata da GHCR..."
docker compose pull backend

echo "Avvio Meevy senza compilare sul server..."
docker compose up -d --no-build --pull missing

echo "Stato servizi:"
docker compose ps

echo "Commit distribuito: $(git rev-parse --short HEAD)"
