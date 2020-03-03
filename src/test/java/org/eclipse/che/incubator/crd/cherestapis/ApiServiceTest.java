/*
 * Copyright (c) 2019-2020 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */

package org.eclipse.che.incubator.crd.cherestapis;

import io.kubernetes.client.ApiException;
import io.quarkus.test.junit.QuarkusTest;

import org.apache.commons.io.IOUtils;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.workspace.server.devfile.exception.DevfileException;
import org.eclipse.che.api.workspace.server.model.impl.devfile.DevfileImpl;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

@QuarkusTest
public class ApiServiceTest {

    @Inject
    ApiService service;

    @BeforeEach
    public void init() {
        service.init();
    }

    // TODO: Commit 648156a in the original repo https://github.com/che-incubator/che-workspace-operator
    // sets workspace.getConfig() to null directly, causing an NPE in this test.
    @Disabled
    @Test
    public void parseDevfile()
            throws IOException, ServerException, DevfileException, ValidationException, ApiException {
        InputStream stream = this.getClass().getResourceAsStream("devfiles/petclinic-sample.yaml");
        String devfileYaml = IOUtils.toString(stream);
        DevfileImpl devfile = service.parseDevFile(devfileYaml);
        WorkspaceDto workspace = service.convertToWorkspace(devfile, null);
        Assertions.assertEquals(workspace.getConfig().getName(), "petclinic");
    }
}