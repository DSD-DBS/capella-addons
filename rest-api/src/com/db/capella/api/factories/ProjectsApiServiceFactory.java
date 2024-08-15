package com.db.capella.api.factories;

import com.db.capella.api.ProjectsApiService;
import com.db.capella.api.impl.ProjectsApiServiceImpl;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", comments = "Generator version: 7.7.0")
public class ProjectsApiServiceFactory {
    private static final ProjectsApiService service = new ProjectsApiServiceImpl();

    public static ProjectsApiService getProjectsApi() {
        return service;
    }
}
