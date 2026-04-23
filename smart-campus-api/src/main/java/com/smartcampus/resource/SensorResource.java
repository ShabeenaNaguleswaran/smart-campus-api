package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private DataStore store = DataStore.getInstance();

    // GET /api/v1/sensors — get all sensors (with optional ?type= filter)
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {

        List<Sensor> sensorList = new ArrayList<>(store.getSensors().values());

        // If type filter is provided, filter the list
        if (type != null && !type.isEmpty()) {
            List<Sensor> filtered = new ArrayList<>();
            for (Sensor s : sensorList) {
                if (s.getType().equalsIgnoreCase(type)) {
                    filtered.add(s);
                }
            }
            return Response.ok(filtered).build();
        }

        return Response.ok(sensorList).build();
    }

    // GET /api/v1/sensors/{sensorId} — get one sensor by ID
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);

        if (sensor == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Sensor not found: " + sensorId);
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        return Response.ok(sensor).build();
    }

    // POST /api/v1/sensors — register a new sensor
    @POST
    public Response createSensor(Sensor sensor) {

        // Check if sensor ID already exists
        if (store.getSensor(sensor.getId()) != null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Sensor with ID " + sensor.getId() + " already exists");
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }

        // Check if the roomId actually exists — if not throw 422
        if (store.getRoom(sensor.getRoomId()) == null) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor. Room with ID '" + sensor.getRoomId() + "' does not exist."
            );
        }

        // Add sensor to the store
        store.addSensor(sensor);

        // Link sensor ID to the room's sensorIds list
        store.getRoom(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    // Sub-resource locator — hands off to SensorReadingResource
    // GET/POST /api/v1/sensors/{sensorId}/readings
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}