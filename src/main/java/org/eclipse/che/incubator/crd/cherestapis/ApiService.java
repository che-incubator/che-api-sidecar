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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableMap;

import org.eclipse.che.account.spi.AccountImpl;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.core.model.workspace.Runtime;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.workspace.server.devfile.exception.DevfileException;
import org.eclipse.che.api.workspace.server.devfile.exception.DevfileFormatException;
import org.eclipse.che.api.workspace.server.devfile.validator.DevfileIntegrityValidator;
import org.eclipse.che.api.workspace.server.DtoConverter;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.shared.dto.RuntimeDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDto;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.che.api.workspace.server.model.impl.devfile.DevfileImpl;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.util.Config;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ApiService {
    private static final Logger LOGGER = LoggerFactory.getLogger("ApiService");

    @Inject
    @ConfigProperty(name = "che.workspace.name")
    String workspaceName;

    @Inject
    @ConfigProperty(name = "che.workspace.id")
    String workspaceId;

    @Inject
    @ConfigProperty(name = "che.workspace.namespace")
    String workspaceNamespace;

    @Inject
    @ConfigProperty(name = "che.workspace.crd.version", defaultValue = "v1alpha1")
    String workspaceCrdVersion;

    @Inject
    @ConfigProperty(name = "che.workspace.runtime.json.path")
    String workspaceRuntimePath;

    @Inject
    @ConfigProperty(name = "che.workspace.devfile.path")
    String workspaceDevfilePath;

    private ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
    private DevfileIntegrityValidator devfileIntegrityValidator = null;

    public void onStart(@Observes StartupEvent ev) {
        LOGGER.info("Loading SunEC library");
        try {
            System.loadLibrary("sunec");
        } catch (Throwable t) {
            if (!t.getMessage().contains("already loaded")) {
                LOGGER.error("Error while loading the Java `sunec` dynamic library", t);
                throw t;
            }
        }

        try {
            if (workspaceId == null) {
                throw new RuntimeException("The CHE_WORKSPACE_ID environment variable should be set");
            }
            if (workspaceNamespace == null) {
                throw new RuntimeException("The CHE_WORKSPACE_NAMESPACE environment variable should be set");
            }
            if (workspaceName == null) {
                throw new RuntimeException("The CHE_WORKSPACE_NAME environment variable should be set");
            }
            if (workspaceRuntimePath == null) {
                throw new RuntimeException("The CHE_WORKSPACE_RUNTIME_YAML_PATH environment variable should be set");
            }
            if (workspaceDevfilePath == null) {
                throw new RuntimeException("The CHE_WORKSPACE_DEVFILE_PATH environment variable should be set");
            }

            LOGGER.info("Workspace Id: {}", workspaceId);
            LOGGER.info("Workspace Name: {}", workspaceName);

            init();
        } catch (RuntimeException e) {
            LOGGER.error("Che Api Service cannot start", e);
            throw e;
        }
    }

    public WorkspaceDto getWorkspace(String workspaceId) {
        LOGGER.info("Getting workspace {} {}", workspaceId, this.workspaceId);
        if (!this.workspaceId.equals(workspaceId)) {
            String message = "The workspace " + workspaceId + " is not found (current workspace is " + this.workspaceId
                    + ")";
            LOGGER.error(message);
            throw new NotFoundException(message);
        }

        DevfileImpl devfileObj;
        RuntimeDto runtimeObj;
        try {
            devfileObj = yamlObjectMapper.treeToValue(
                    yamlObjectMapper.readTree(Files.readAllBytes(Paths.get(workspaceDevfilePath))), DevfileImpl.class);
            LOGGER.debug(devfileObj.toString());
        } catch (JsonMappingException e) {
            throw new RuntimeException("Failed to parse devfile yaml to devfile", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read devfile yaml from file", e);
        }
        try {
            String runtimeJson = new String(Files.readAllBytes(Paths.get(workspaceRuntimePath)),
                    StandardCharsets.UTF_8);
            runtimeObj = parseRuntime(runtimeJson);
            LOGGER.debug(runtimeObj.toString());
        } catch (JsonMappingException e) {
            throw new RuntimeException("Failed to parse runtime json to devfile", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read runtime json from file", e);
        }
        LOGGER.info("Convert to workspace");
        try {
            return convertToWorkspace(devfileObj, runtimeObj);
        } catch (ServerException | DevfileException | ValidationException e) {
            throw new RuntimeException("The devfile could not be converted correcly to a workspace: " + devfileObj, e);
        } catch (ApiException e) {
            throw new RuntimeException("Problem while retrieving the Workspace runtime information from K8s objects",
                    e);
        }
    }

    RuntimeDto parseRuntime(String runtimeJson) throws JsonProcessingException, IOException {
        LOGGER.info("Runtime content for workspace {}: {}", workspaceName, runtimeJson);
        RuntimeDto runtimeObj = DtoFactory.getInstance().createDtoFromJson(runtimeJson, RuntimeDto.class);
        return runtimeObj;
    }

    WorkspaceDto convertToWorkspace(DevfileImpl devfileObj, Runtime runtimeObj)
            throws DevfileException, ServerException, ValidationException, ApiException {
        LOGGER.info("validateDevfile");
        try {
            devfileIntegrityValidator.validateDevfile(devfileObj);
        } catch (DevfileFormatException e) {
            LOGGER.warn("Validation of the devfile failed", e);
        }

        LOGGER.info(" WorkspaceImpl.builder().build()");
        WorkspaceImpl workspace = WorkspaceImpl.builder().setId(workspaceId).setConfig(null).setDevfile(devfileObj)
                .setAccount(new AccountImpl("anonymous", "anonymous", "anonymous"))
                .setAttributes(Collections.emptyMap()).setTemporary(false).setRuntime(runtimeObj)
                .setStatus(WorkspaceStatus.RUNNING).build();

        return DtoConverter.asDto(workspace);
    }

    void init() {
        devfileIntegrityValidator = new DevfileIntegrityValidator(ImmutableMap.of());
        try {
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
        } catch (IOException e) {
            throw new RuntimeException("Kubernetes client cannot be created", e);
        }
    }

}
