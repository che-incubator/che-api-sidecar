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

LIBS="/usr/lib64/libz.so.1 /usr/lib64/libgcc_s.so.1"
for lib in $LIBS
do
    fullpath=$(readlink -f $lib)
    destination=$1/$(basename $lib)
    mkdir -p $(dirname $destination)
    cp -vf $fullpath $destination
done
