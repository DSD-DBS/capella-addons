// Copyright DB InfraGO AG and contributors
// SPDX-License-Identifier: Apache-2.0

package com.deutschebahn.capella;

import java.net.URI;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IStartup;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class Main implements IStartup {
    public static final String BASE_URI = "http://0.0.0.0:5007/api/v1";

    public static void log(int severity, String message, Throwable exception) {
        Bundle bundle = FrameworkUtil.getBundle(Main.class);
        ILog log = Platform.getLog(bundle);
        IStatus status = new Status(severity, bundle.getSymbolicName(),
                message, exception);
        log.log(status);
    }

    @Override
    public void earlyStartup() {
        final ResourceConfig resourceConfig = new Application();
        try {
            final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                    URI.create(BASE_URI),
                    resourceConfig, true);
            log(IStatus.INFO, "Capella REST API server listens on " + BASE_URI + " ...", null);
        } catch (Exception e) {
            String msg = "There was an error while starting Capella REST API server.";
            log(IStatus.ERROR, msg, null);
            System.err.println(msg);
            e.printStackTrace();
        }
    }
}
