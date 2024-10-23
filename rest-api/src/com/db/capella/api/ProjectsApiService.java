// Copyright DB InfraGO AG and contributors
// SPDX-License-Identifier: Apache-2.0

package com.db.capella.api;

import com.db.capella.api.*;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import com.db.capella.model.Diagram;
import com.db.capella.model.DiagramEditor;
import com.db.capella.model.ImportProjectRequest;
import com.db.capella.model.WorkspaceProject;

import java.util.List;
import com.db.capella.api.NotFoundException;

import java.io.InputStream;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", comments = "Generator version: 7.9.0")
public abstract class ProjectsApiService {
    public abstract Response closeProjectByName(String projectName,SecurityContext securityContext) throws NotFoundException;
    public abstract Response deleteProjectByName(String projectName,Boolean deleteContents,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getDiagramEditorsByProjectName(String projectName,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getDiagramsByProjectName(String projectName,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getProjectByName(String projectName,SecurityContext securityContext) throws NotFoundException;
    public abstract Response importProject(ImportProjectRequest importProjectRequest,SecurityContext securityContext) throws NotFoundException;
    public abstract Response listProjects(SecurityContext securityContext) throws NotFoundException;
    public abstract Response openProjectByName(String projectName,SecurityContext securityContext) throws NotFoundException;
    public abstract Response saveProjectByName(String projectName,SecurityContext securityContext) throws NotFoundException;
}
