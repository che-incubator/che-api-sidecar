#!/bin/bash
#
# Copyright (c) 2019-2020 Red Hat, Inc.
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#   Red Hat, Inc. - initial API and implementation
#
# A script to autmate building the che-rest-apis image
# Parameters:
#   $1 - docker tag used for final image

DEFAULT_TAG="che-rest-apis"
FINAL_TAG=${1:-$DEFAULT_TAG}
echo "Building image using tag $FINAL_TAG"

echo "Building che-rest-apis native binary in container"
docker build --ulimit nofile=122880:122880 -m 5G -t che-rest-apis-builder -f build.Dockerfile .

echo "Copying binary to ./target"
docker create --name builder che-rest-apis-builder
docker cp builder:/usr/src/app/target ./target/
docker rm builder

echo "Building che-rest-apis image"
docker build -t $FINAL_TAG -f ./src/main/docker/Dockerfile .
