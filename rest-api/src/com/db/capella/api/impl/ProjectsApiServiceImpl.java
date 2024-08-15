package com.db.capella.api.impl;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import com.db.capella.api.ApiException;
import com.db.capella.api.ApiResponseMessage;
import com.db.capella.api.NotFoundException;
import com.db.capella.api.ProjectsApiService;
import com.db.capella.integration.WorkspaceProjectInt;
import com.db.capella.model.ImportProjectRequest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", comments = "Generator version: 7.7.0")
public class ProjectsApiServiceImpl extends ProjectsApiService {
    private IProject getWorkspaceProjectByName(String name) throws ApiException {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject workspaceProject = workspace.getRoot().getProject(name);
        if (!workspaceProject.exists()) {
            throw new ApiException(Response.Status.NOT_FOUND.getStatusCode(), "Project named '" + name + "' not found");
        }
        return workspaceProject;
    }

    @Override
    public Response closeProjectByName(String projectName,
            SecurityContext securityContext)
            throws NotFoundException {
        // do some magic!
        return Response.ok()
                .entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!"))
                .build();
    }

    @Override
    public Response deleteProjectByName(String projectName,
            Boolean deleteContents,
            SecurityContext securityContext)
            throws NotFoundException {
        // do some magic!
        return Response.ok()
                .entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!"))
                .build();
    }

    @Override
    public Response getDiagramsByProjectName(String projectName,
            SecurityContext securityContext)
            throws NotFoundException {
        // do some magic!
        return Response.ok()
                .entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!"))
                .build();
    }

    @Override
    public Response getProjectByName(String projectName,
            SecurityContext securityContext)
            throws NotFoundException {
        // do some magic!
        return Response.ok()
                .entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!"))
                .build();
    }

    @Override
    public Response importProject(ImportProjectRequest importProjectRequest,
            SecurityContext securityContext)
            throws NotFoundException {
        // do some magic!
        return Response.ok()
                .entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!"))
                .build();
    }

    @Override
    public Response listProjects(SecurityContext securityContext)
            throws NotFoundException {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject[] projects = workspace.getRoot().getProjects();
        java.util.List<com.db.capella.model.WorkspaceProject> projectIntList = new java.util.ArrayList<com.db.capella.model.WorkspaceProject>();
        for (IProject project : projects) {
            WorkspaceProjectInt projectInt = new WorkspaceProjectInt(
                    (org.eclipse.core.internal.resources.Project) project);
            projectIntList.add(projectInt);
        }
        return Response.ok().entity(projectIntList).build();
    }

    @Override
    public Response openProjectByName(String projectName,
            SecurityContext securityContext)
            throws NotFoundException {
        // do some magic!
        return Response.ok()
                .entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!"))
                .build();
    }

    @Override
    public Response saveProjectByName(String projectName,
            SecurityContext securityContext)
            throws NotFoundException {
        IProject workspaceProject;
        try {
            workspaceProject = getWorkspaceProjectByName(projectName);
        } catch (ApiException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, e.getMessage())).build();
        }
        WorkspaceProjectInt projectInt = new WorkspaceProjectInt(
                (org.eclipse.core.internal.resources.Project) workspaceProject);
        projectInt.save();
        return Response.ok()
                .entity(new ApiResponseMessage(ApiResponseMessage.OK, "Project '" + projectName + "' is saved."))
                .build();
    }
}
