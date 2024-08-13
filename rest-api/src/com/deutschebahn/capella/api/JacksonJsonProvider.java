// Copyright DB InfraGO AG and contributors
// SPDX-License-Identifier: Apache-2.0

package com.deutschebahn.capella.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.fasterxml.jackson.datatype.jsr310.*;

import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

@Provider
@Produces({MediaType.APPLICATION_JSON})
public class JacksonJsonProvider extends JacksonXmlBindJsonProvider {

    public JacksonJsonProvider() {

        ObjectMapper objectMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .registerModule(new JavaTimeModule())
            .setDateFormat(new RFC3339DateFormat());

        setMapper(objectMapper);
    }
}
