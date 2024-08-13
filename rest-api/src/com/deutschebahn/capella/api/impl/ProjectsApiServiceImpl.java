// Copyright DB InfraGO AG and contributors
// SPDX-License-Identifier: Apache-2.0

package com.deutschebahn.capella.api.impl;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.sirius.business.api.dialect.DialectManager;
import org.eclipse.sirius.business.api.session.Session;
import org.eclipse.sirius.business.api.session.SessionManager;
import org.eclipse.sirius.ui.business.api.session.SessionEditorInput;
import org.eclipse.sirius.viewpoint.DRepresentationDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.deutschebahn.capella.api.ApiException;
import com.deutschebahn.capella.api.ApiResponseMessage;
import com.deutschebahn.capella.api.ProjectsApiService;
import com.deutschebahn.capella.integration.WorkspaceProjectInt;
import com.deutschebahn.capella.model.Diagram;
import com.deutschebahn.capella.model.DiagramEditor;
import com.deutschebahn.capella.model.ImportProjectRequest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", comments = "Generator version: 7.7.0")
public class ProjectsApiServiceImpl extends ProjectsApiService {
    private static URI getEMFURIForAirdFile(IPath directoryPath) {
        File directory = directoryPath.toFile();
        if (directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".aird"));
            if (files != null && files.length > 0) {
                // Assuming there's only one .aird file in the directory
                File airdFile = files[0];
                return URI.createFileURI(airdFile.getAbsolutePath());
            }
        }
        return null;
    }

    private Session getSession(IProject project) throws ApiException {
        URI airdFileName = getEMFURIForAirdFile(project.getLocation());
        if (airdFileName == null) {
            throw new ApiException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "An .aird file is required to get a session for the Capella project.");
        }
        final Session session = SessionManager.INSTANCE.getSession(airdFileName, new NullProgressMonitor());
        if (session == null) {
            throw new ApiException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Cannot get session for project");
        }
        return session;
    }

    private IProject getWorkspaceProjectByName(String name) throws ApiException {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject workspaceProject = workspace.getRoot().getProject(name);
        if (!workspaceProject.exists()) {
            throw new ApiException(Response.Status.NOT_FOUND.getStatusCode(), "Project named '" + name + "' not found");
        }
        return workspaceProject;
    }

    @Override
    public Response closeProjectByName(String projectName, SecurityContext securityContext) {
        IProject workspaceProject;
        try {
            workspaceProject = getWorkspaceProjectByName(projectName);
        } catch (ApiException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, e.getMessage())).build();
        }
        try {
            workspaceProject.close(null);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, e.getMessage())).build();
        }
        return Response.noContent().build();
    }

    @Override
    public Response deleteProjectByName(String projectName,
            Boolean deleteContents,
            SecurityContext securityContext) {
        IProject workspaceProject = null;
        try {
            workspaceProject = getWorkspaceProjectByName(projectName);
        } catch (ApiException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, e.getMessage())).build();
        }
        try {
            workspaceProject.delete(deleteContents, true, null);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, e.getMessage())).build();
        }
        return Response.noContent().build();
    }

    @Override
    public Response getDiagramEditorsByProjectName(String projectName, SecurityContext securityContext) {
        java.util.List<DiagramEditor> diagramEditorList = new java.util.ArrayList<DiagramEditor>();
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
        for (IWorkbenchWindow window : windows) {
            IWorkbenchPage[] pages = window.getPages();
            for (IWorkbenchPage page : pages) {
                IEditorReference[] editorReferences = page.getEditorReferences();
                for (IEditorReference editorReference : editorReferences) {
                    try {
                        IEditorInput input = editorReference.getEditorInput();
                        if (input instanceof SessionEditorInput) {
                            SessionEditorInput sessionEditorInput = (SessionEditorInput) input;
                            URI uri = sessionEditorInput.getURI();
                            IFile file = ResourcesPlugin.getWorkspace().getRoot()
                                    .getFile(new Path(uri.toPlatformString(true)));
                            IProject project = file.getProject();
                            if (project.getName().equals(projectName)) {
                                DiagramEditor diagramEditor = new DiagramEditor();
                                diagramEditor.setName(sessionEditorInput.getName());
                                diagramEditor.setUri(sessionEditorInput.getURI().toString());
                                String repDesUri = sessionEditorInput.getRepDescUri().toString();
                                diagramEditor.setId(repDesUri.split("#")[1]);
                                diagramEditorList.add(diagramEditor);
                                if (editorReference.getEditor(false) == null) {
                                    System.out.println(sessionEditorInput.getName() + " is null");
                                }
                            }
                        }
                    } catch (PartInitException e) {
                    }
                }
            }
        }

        return Response.ok().entity(diagramEditorList).build();
    }

    @Override
    public Response getDiagramsByProjectName(String projectName, SecurityContext securityContext) {
        IProject workspaceProject = null;
        try {
            workspaceProject = getWorkspaceProjectByName(projectName);
        } catch (ApiException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, e.getMessage())).build();
        }
        Session session = null;
        try {
            session = getSession(workspaceProject);
        } catch (ApiException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, e.getMessage())).build();
        }
        Collection<DRepresentationDescriptor> representationDescriptors = DialectManager.INSTANCE
                .getAllRepresentationDescriptors(session);
        java.util.List<Diagram> diagramList = new java.util.ArrayList<Diagram>();
        for (DRepresentationDescriptor representationDescriptor : representationDescriptors) {
            Diagram diagram = new Diagram();
            diagram.setId(representationDescriptor.getUid());
            diagram.setName(representationDescriptor.getName());
            diagramList.add(diagram);
        }
        return Response.ok().entity(diagramList).build();
    }

    @Override
    public Response importProject(ImportProjectRequest body, SecurityContext securityContext) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        String projectFolderPath = body.getLocation();
        File projectFolder = new File(projectFolderPath);
        if (!projectFolder.exists()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Project folder not found")).build();
        }
        File dotProjectFile = new File(projectFolder, ".project");
        if (!dotProjectFile.exists()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR,
                            "Project folder does not contain .project file"))
                    .build();
        }
        IPath projectLocation = new Path(dotProjectFile.getAbsolutePath());
        try {
            IProjectDescription projectDescription = workspace
                    .loadProjectDescription(projectLocation);
            IProject project = workspace.getRoot().getProject(projectDescription.getName());
            if (project.exists()) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Project already exists")).build();
            }
            project.create(projectDescription, null);
            project.open(null);
            WorkspaceProjectInt projectInt = new WorkspaceProjectInt(
                    (org.eclipse.core.internal.resources.Project) project);
            // todo: URL in response header is `http://localhost:5007/projects/(...)` which
            // is wrong
            return Response.created(new java.net.URI("/projects/" + projectInt.getName()))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Failed to import project")).build();
    }

    @Override
    public Response listProjects(SecurityContext securityContext) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject[] projects = workspace.getRoot().getProjects();
        java.util.List<com.deutschebahn.capella.model.WorkspaceProject> projectIntList = new java.util.ArrayList<com.deutschebahn.capella.model.WorkspaceProject>();
        for (IProject project : projects) {
            WorkspaceProjectInt projectInt = new WorkspaceProjectInt(
                    (org.eclipse.core.internal.resources.Project) project);
            projectIntList.add(projectInt);
        }
        return Response.ok().entity(projectIntList).build();
    }

    @Override
    public Response openProjectByName(String projectName, SecurityContext securityContext) {
        IProject workspaceProject;
        try {
            workspaceProject = getWorkspaceProjectByName(projectName);
        } catch (ApiException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, e.getMessage())).build();
        }
        try {
            workspaceProject.open(null);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, e.getMessage())).build();
        }
        return Response.noContent().build();
    }

    @Override
    public Response saveProjectByName(String projectName,
            SecurityContext securityContext) {
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
        return Response.noContent().build();
    }
}
