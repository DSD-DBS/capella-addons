// Copyright DB InfraGO AG and contributors
// SPDX-License-Identifier: Apache-2.0

package com.db.capella;

import org.glassfish.jersey.server.ResourceConfig;

import com.db.capella.api.JacksonJsonProvider;
import com.db.capella.api.ProjectsApi;

public class Application extends ResourceConfig {
    public Application() {
        register(ProjectsApi.class);
        register(JacksonJsonProvider.class);
    }
}
