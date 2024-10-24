// Copyright DB InfraGO AG and contributors
// SPDX-License-Identifier: Apache-2.0

package com.db.capella.api;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.*;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;

public class Bootstrap extends HttpServlet {

  private static final long serialVersionUID = 20230810;

  @Override
  public void init(ServletConfig config) throws ServletException {

    Info info = new Info()
      .title("OpenAPI Server")
      .description("API to access live data and modify Capella projects")
      .termsOfService("")
      .contact(new Contact()
        .email(""))
      .license(new License()
        .name("")
        .url("http://unlicense.org"));

    OpenAPI oas = new OpenAPI();
    oas.info(info);

    SwaggerConfiguration openApiConfig = new SwaggerConfiguration()
        .openAPI(oas)
        .prettyPrint(true)
        .resourcePackages(Stream.of("io.swagger.sample.resource").collect(Collectors.toSet()));

    try {
        new JaxrsOpenApiContextBuilder()
            .servletConfig(config)
            .openApiConfiguration(openApiConfig)
            .buildContext(true);

    } catch (OpenApiConfigurationException e) {
        throw new RuntimeException(e.getMessage(), e);
    }
  }
}
