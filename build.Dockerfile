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

FROM quay.io/quarkus/centos-quarkus-maven:19.1.1
USER root

# Cache dependencies
RUN mkdir /usr/src/app
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml dependency:resolve

# Build the project
COPY src /usr/src/app/src
RUN mvn -f /usr/src/app/pom.xml -Pnative clean package
