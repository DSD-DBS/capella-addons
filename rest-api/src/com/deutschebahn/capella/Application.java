// Copyright DB InfraGO AG and contributors
// SPDX-License-Identifier: Apache-2.0

package com.deutschebahn.capella;

import org.glassfish.jersey.server.ResourceConfig;

import com.deutschebahn.capella.api.JacksonJsonProvider;
import com.deutschebahn.capella.api.ProjectsApi;

public class Application extends ResourceConfig {
    public Application() {
        register(ProjectsApi.class);
        register(JacksonJsonProvider.class);
    }
}
