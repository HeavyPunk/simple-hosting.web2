#!/bin/bash

bash publish-prod.sh

DOCKER_IMAGE=kirieshki/simple-hosting.web2
DOCKER_TAG=latest
docker build -t $DOCKER_IMAGE:$DOCKER_TAG .
docker push $DOCKER_IMAGE:$DOCKER_TAG
