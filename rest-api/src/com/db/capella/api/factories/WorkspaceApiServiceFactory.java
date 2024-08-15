package com.db.capella.api.factories;

import com.db.capella.api.WorkspaceApiService;
import com.db.capella.api.impl.WorkspaceApiServiceImpl;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", comments = "Generator version: 7.7.0")
public class WorkspaceApiServiceFactory {
    private static final WorkspaceApiService service = new WorkspaceApiServiceImpl();

    public static WorkspaceApiService getWorkspaceApi() {
        return service;
    }
}
