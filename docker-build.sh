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

DEFAULT_TAG="che-api-sidecar:local"
FINAL_TAG=${1:-$DEFAULT_TAG}

./docker-compile.sh

echo "Building che-rest-apis image using tag $FINAL_TAG"
docker build -t $FINAL_TAG -f ./src/main/docker/Dockerfile .
