package com.smartcampus.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException exception) {

        Map<String, String> error = new HashMap<>();
        error.put("status", "403 Forbidden");
        error.put("error", "Sensor Unavailable");
        error.put("message", exception.getMessage());

        return Response
                .status(Response.Status.FORBIDDEN)  // 403
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}