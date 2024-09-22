package com.project.Messenger.backend.broker.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class SerializationParser {
    private ObjectMapper mapper = new ObjectMapper();

    public SerializationParser() {
        mapper.registerModule(new JavaTimeModule());
    }

    public String processJsonMessage(SerializedMessage serializedMessage) throws JsonProcessingException {
        return mapper.writeValueAsString(serializedMessage);
    }


    public SerializedMessage processStringMessage(String message) throws JsonProcessingException {
        return mapper.readValue(message, SerializedMessage.class);
    }
}
