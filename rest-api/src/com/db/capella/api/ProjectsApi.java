// Copyright DB InfraGO AG and contributors
// SPDX-License-Identifier: Apache-2.0

package com.db.capella.api;

import com.db.capella.api.ProjectsApiService;
import com.db.capella.api.factories.ProjectsApiServiceFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.db.capella.model.Diagram;
import com.db.capella.model.DiagramEditor;
import com.db.capella.model.ImportProjectRequest;
import com.db.capella.model.WorkspaceProject;

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

@Path("/projects")


@Tag(description = "the projects API", name = "")
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", comments = "Generator version: 7.9.0")
public class ProjectsApi  {

   private final ProjectsApiService delegate;

   public ProjectsApi(@Context ServletConfig servletContext) {

      ProjectsApiService delegate = null;
      if (servletContext != null) {
         String implClass = servletContext.getInitParameter("ProjectsApi.implementation");
         if (implClass != null && !"".equals(implClass.trim())) {
            try {
               delegate = (ProjectsApiService) Class.forName(implClass).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         }
      }

      if (delegate == null) {
         delegate = ProjectsApiServiceFactory.getProjectsApi();
      }
      this.delegate = delegate;
   }


    @jakarta.ws.rs.POST
    @Path("/{project_name}/close")
    @Operation(summary = "Close a project by name", description = "", responses = {
            @ApiResponse(responseCode = "200", description = "Project closed successfully", content =
                @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content =
                @Content(schema = @Schema(implementation = Void.class))),
            }, tags={ "Projects", })
    public Response closeProjectByName(@Schema(description= "Unique name of the project to be closed", required = true) @PathParam("project_name") @NotNull  String projectName,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.closeProjectByName(projectName, securityContext);
    }

    @jakarta.ws.rs.DELETE
    @Path("/{project_name}")
    @Operation(summary = "Delete a project by name", description = "", responses = {
            @ApiResponse(responseCode = "200", description = "Project deleted successfully", content =
                @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content =
                @Content(schema = @Schema(implementation = Void.class))),
            }, tags={ "Projects", })
    public Response deleteProjectByName(@Schema(description= "Unique name of the project to be deleted", required = true) @PathParam("project_name") @NotNull  String projectName,@Schema(description = "Whether to delete the project contents on disk (cannot be undone) or not ", defaultValue = "false") @DefaultValue("false") @QueryParam("deleteContents")  Boolean deleteContents,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.deleteProjectByName(projectName, deleteContents, securityContext);
    }

    @jakarta.ws.rs.GET
    @Path("/{project_name}/diagram-editors")
    @Produces({ "application/json" })
    @Operation(summary = "Get a list of all open diagram editors for a project by name", description = "", responses = {
            @ApiResponse(responseCode = "200", description = "A list of open diagram editors", content =
                @Content(schema = @Schema(implementation = DiagramEditor.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content =
                @Content(schema = @Schema(implementation = Void.class))),
            }, tags={ "Diagrams", })
    public Response getDiagramEditorsByProjectName(@Schema(description= "Unique name of the project", required = true) @PathParam("project_name") @NotNull  String projectName,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.getDiagramEditorsByProjectName(projectName, securityContext);
    }

    @jakarta.ws.rs.GET
    @Path("/{project_name}/diagrams")
    @Produces({ "application/json" })
    @Operation(summary = "Get a list of all diagrams for a project by name", description = "", responses = {
            @ApiResponse(responseCode = "200", description = "A list of diagrams", content =
                @Content(schema = @Schema(implementation = Diagram.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content =
                @Content(schema = @Schema(implementation = Void.class))),
            }, tags={ "Diagrams", })
    public Response getDiagramsByProjectName(@Schema(description= "Unique name of the project", required = true) @PathParam("project_name") @NotNull  String projectName,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.getDiagramsByProjectName(projectName, securityContext);
    }

    @jakarta.ws.rs.GET
    @Path("/{project_name}")
    @Produces({ "application/json" })
    @Operation(summary = "Get a project by name", description = "", responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content =
                @Content(schema = @Schema(implementation = WorkspaceProject.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content =
                @Content(schema = @Schema(implementation = Void.class))),
            }, tags={ "Projects", })
    public Response getProjectByName(@Schema(description= "Unique name of the project", required = true) @PathParam("project_name") @NotNull  String projectName,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.getProjectByName(projectName, securityContext);
    }

    @jakarta.ws.rs.POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Operation(summary = "Import a project into the workspace", description = "", responses = {
            @ApiResponse(responseCode = "201", description = "Project imported successfully.", content =
                @Content(schema = @Schema(implementation = WorkspaceProject.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content =
                @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content =
                @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "409", description = "Project already exists in the workspace", content =
                @Content(schema = @Schema(implementation = Void.class))),
            }, tags={ "Projects", })
    public Response importProject(@Schema(description = "", required = true) @NotNull @Valid  ImportProjectRequest importProjectRequest,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.importProject(importProjectRequest, securityContext);
    }

    @jakarta.ws.rs.GET
    @Produces({ "application/json" })
    @Operation(summary = "List all projects in the workspace", description = "", responses = {
            @ApiResponse(responseCode = "200", description = "A list of projects", content =
                @Content(schema = @Schema(implementation = WorkspaceProject.class))),
            }, tags={ "Projects", })
    public Response listProjects(@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.listProjects(securityContext);
    }

    @jakarta.ws.rs.POST
    @Path("/{project_name}/open")
    @Operation(summary = "Open a project by name", description = "", responses = {
            @ApiResponse(responseCode = "200", description = "Project opened successfully", content =
                @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content =
                @Content(schema = @Schema(implementation = Void.class))),
            }, tags={ "Projects", })
    public Response openProjectByName(@Schema(description= "Unique name of the project to be opened", required = true) @PathParam("project_name") @NotNull  String projectName,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.openProjectByName(projectName, securityContext);
    }

    @jakarta.ws.rs.POST
    @Path("/{project_name}/save")
    @Operation(summary = "Save a project by name", description = "", responses = {
            @ApiResponse(responseCode = "200", description = "Project saved successfully", content =
                @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content =
                @Content(schema = @Schema(implementation = Void.class))),
            }, tags={ "Projects", })
    public Response saveProjectByName(@Schema(description= "Unique name of the project to be saved", required = true) @PathParam("project_name") @NotNull  String projectName,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.saveProjectByName(projectName, securityContext);
    }
}
