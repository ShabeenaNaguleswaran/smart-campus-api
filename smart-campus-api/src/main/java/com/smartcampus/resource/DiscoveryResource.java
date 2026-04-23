package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {

        Map<String, Object> response = new HashMap<>();

        // API version info
        response.put("version", "1.0");
        response.put("name", "Smart Campus API");
        response.put("description", "RESTful API for managing campus rooms and sensors");
        response.put("contact", "admin@smartcampus.ac.uk");

        // Links to available resources (this is the HATEOAS part)
        Map<String, String> links = new HashMap<>();
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        response.put("resources", links);

        return Response.ok(response).build();
    }
}