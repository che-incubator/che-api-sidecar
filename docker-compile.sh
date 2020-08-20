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
# A script to autmate compiling of the che-rest-apis

echo "Compiling che-rest-apis native binary in container"
docker build --ulimit nofile=122880:122880 -m 5G -t che-api-sidecar-builder -f build.Dockerfile .

echo "Copying binary to ./target"
docker create --name builder che-api-sidecar-builder
docker cp builder:/usr/src/app/target ./target/
docker rm builder
