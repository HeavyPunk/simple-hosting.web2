#!/bin/bash

set -eu

bash publish-prod.sh

DOCKER_IMAGE=kirieshki/simple-hosting.web2
DOCKER_TAG="2024.01.23.1"
docker buildx build -t $DOCKER_IMAGE:$DOCKER_TAG --progress plain --platform linux/amd64,linux/arm64,linux/arm64/v8 --push .
