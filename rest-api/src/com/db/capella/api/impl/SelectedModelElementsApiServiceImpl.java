package com.db.capella.api.impl;

import com.db.capella.api.*;
import com.db.capella.model.SelectedModelElement;

import java.util.List;
import com.db.capella.api.NotFoundException;

import java.io.InputStream;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", comments = "Generator version: 7.7.0")
public class SelectedModelElementsApiServiceImpl extends SelectedModelElementsApiService {
    @Override
    public Response getSelectedModelElements(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
}
