package com.smartcampus.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {

        Map<String, String> error = new HashMap<>();
        error.put("status", "500 Internal Server Error");
        error.put("error", "Unexpected Server Error");
        error.put("message", "Something went wrong on the server. Please try again later.");

        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)  // 500
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}