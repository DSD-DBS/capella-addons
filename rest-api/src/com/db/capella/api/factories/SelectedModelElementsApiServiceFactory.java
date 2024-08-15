// Copyright DB InfraGO AG and contributors
// SPDX-License-Identifier: Apache-2.0

package com.db.capella.api.factories;

import com.db.capella.api.SelectedModelElementsApiService;
import com.db.capella.api.impl.SelectedModelElementsApiServiceImpl;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", comments = "Generator version: 7.9.0")
public class SelectedModelElementsApiServiceFactory {
    private static final SelectedModelElementsApiService service = new SelectedModelElementsApiServiceImpl();

    public static SelectedModelElementsApiService getSelectedModelElementsApi() {
        return service;
    }
}
