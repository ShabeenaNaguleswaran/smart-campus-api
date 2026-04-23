package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private String sensorId;
    private DataStore store = DataStore.getInstance();

    // Constructor — receives the sensorId from SensorResource
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // GET /api/v1/sensors/{sensorId}/readings — get all readings for a sensor
    @GET
    public Response getReadings() {
        Sensor sensor = store.getSensor(sensorId);

        if (sensor == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Sensor not found: " + sensorId);
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        List<SensorReading> readings = store.getReadings(sensorId);
        return Response.ok(readings).build();
    }

    // POST /api/v1/sensors/{sensorId}/readings — add a new reading
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensor(sensorId);

        // Sensor doesn't exist
        if (sensor == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Sensor not found: " + sensorId);
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        // Sensor is in MAINTENANCE — block new readings (Part 5.3)
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is currently under MAINTENANCE " +
                            "and cannot accept new readings."
            );
        }

        // Create the reading with auto-generated ID and timestamp
        SensorReading newReading = new SensorReading(reading.getValue());

        // Save the reading
        store.addReading(sensorId, newReading);

        // ⭐ Side effect — update the sensor's currentValue (required by Part 4.2)
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(newReading).build();
    }
}