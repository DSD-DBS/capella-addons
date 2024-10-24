// Copyright DB InfraGO AG and contributors
// SPDX-License-Identifier: Apache-2.0

package com.db.capella.api;

import com.db.capella.api.WorkspaceApiService;
import com.db.capella.api.factories.WorkspaceApiServiceFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.db.capella.model.Workspace;

import java.util.Map;
import java.util.List;
import com.db.capella.api.NotFoundException;

import java.io.InputStream;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.*;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;

@Path("/workspace")


@Tag(description = "the workspace API", name = "")
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", comments = "Generator version: 7.9.0")
public class WorkspaceApi  {

   private final WorkspaceApiService delegate;

   public WorkspaceApi(@Context ServletConfig servletContext) {

      WorkspaceApiService delegate = null;
      if (servletContext != null) {
         String implClass = servletContext.getInitParameter("WorkspaceApi.implementation");
         if (implClass != null && !"".equals(implClass.trim())) {
            try {
               delegate = (WorkspaceApiService) Class.forName(implClass).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         }
      }

      if (delegate == null) {
         delegate = WorkspaceApiServiceFactory.getWorkspaceApi();
      }
      this.delegate = delegate;
   }


    @jakarta.ws.rs.GET
    @Produces({ "application/json" })
    @Operation(summary = "Get the current workspace", description = "", responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content =
                @Content(schema = @Schema(implementation = Workspace.class))),
            }, tags={ "Workspace", })
    public Response getWorkspace(@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.getWorkspace(securityContext);
    }
}
