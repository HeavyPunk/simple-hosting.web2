#!/bin/bash

set -eu

bash publish-prod.sh

DOCKER_IMAGE=kirieshki/simple-hosting.web2
DOCKER_TAG="2023.11.27.2"
docker build -t $DOCKER_IMAGE:$DOCKER_TAG .
docker push $DOCKER_IMAGE:$DOCKER_TAG
