package project.matchalatte.support.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;

public record LogData(
        LocalDateTime timestamp,
        String traceId,
        Long userId,
        String api,
        String message
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static String of(String traceId, Long userId, String api, String message) {
        try {
            return OBJECT_MAPPER.writeValueAsString(new LogData(LocalDateTime.now(), traceId, userId, api, message));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}