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

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.che.api.workspace.shared.dto.WorkspaceDto;

@Path("/api")
public class ApiResource {

    @Inject
    ApiService apiService;

    private Response addCorsHeader(Object o) {
        return Response.ok(o).header("Access-Control-Allow-Origin", "*").build();
    }

    @GET
    @Path("workspace/{key:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByKey(@PathParam("key") String key,
            @QueryParam("includeInternalServers") String includeInternalServers)
            throws NotFoundException, ServerErrorException, ForbiddenException, BadRequestException {
        return addCorsHeader(apiService.getWorkspace(key));
    }
    
    @PUT
    @Path("workspace/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(
        @PathParam("id") String id,
        WorkspaceDto update) {
      return addCorsHeader(apiService.getWorkspace(id));
    }  
}