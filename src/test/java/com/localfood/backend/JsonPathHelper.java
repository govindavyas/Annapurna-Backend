package com.localfood.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class JsonPathHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonPathHelper() {
    }

    static String read(String json, String path) throws Exception {
        JsonNode current = OBJECT_MAPPER.readTree(json);
        String[] tokens = path.replace("$.", "").split("\\.");
        for (String token : tokens) {
            current = current.get(token);
        }
        return current.asText();
    }
}
