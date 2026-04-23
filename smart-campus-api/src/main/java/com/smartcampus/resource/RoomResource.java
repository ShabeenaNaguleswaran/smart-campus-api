package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private DataStore store = DataStore.getInstance();

    // GET /api/v1/rooms — list all rooms
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(store.getRooms().values());
        return Response.ok(roomList).build();
    }

    // POST /api/v1/rooms — create a new room
    @POST
    public Response createRoom(Room room) {
        if (store.getRoom(room.getId()) != null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Room with ID " + room.getId() + " already exists");
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }

        store.addRoom(room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    // GET /api/v1/rooms/{roomId} — get one room by ID
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);

        if (room == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Room not found: " + roomId);
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        return Response.ok(room).build();
    }

    // DELETE /api/v1/rooms/{roomId} — delete a room
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);

        if (room == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Room not found: " + roomId);
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Cannot delete room '" + roomId + "'. " +
                            "It still has " + room.getSensorIds().size() + " sensor(s) assigned to it. " +
                            "Please remove all sensors first."
            );
        }

        store.deleteRoom(roomId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Room " + roomId + " successfully deleted");
        return Response.ok(response).build();
    }
}