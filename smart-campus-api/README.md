# Smart Campus API

A RESTful API for managing university campus Rooms and Sensors, built with JAX-RS (Jersey) as a WAR-deployable Java Web Application for Tomcat 10 / GlassFish.

## Student Info

| Field | Details |
|-------|---------|
| **Name** | Shabeena Naguleswaran |
| **Module** | 5COSC022W - Client-Server Architectures |
| **University** | University of Westminster |
| **Date** | April 2026 |

---

## API Overview

This API provides endpoints to manage:
- **Rooms** — Campus rooms with capacity tracking
- **Sensors** — CO2, Temperature, Occupancy sensors deployed in rooms
- **Sensor Readings** — Historical reading logs per sensor

Base URL: `http://localhost:8080/smart-campus-api/api/v1/`

### Resource Hierarchy

```
/api/v1/
├── GET  /                        → Discovery endpoint (API metadata)
├── /rooms
│   ├── GET    /rooms             → List all rooms
│   ├── POST   /rooms             → Create a new room
│   ├── GET    /rooms/{id}        → Get a specific room
│   └── DELETE /rooms/{id}        → Delete a room (blocked if sensors exist)
└── /sensors
    ├── GET    /sensors            → List all sensors (supports ?type= filter)
    ├── POST   /sensors            → Register a new sensor
    ├── GET    /sensors/{id}       → Get a specific sensor
    └── /sensors/{id}/readings
        ├── GET  /readings         → Get reading history for a sensor
        └── POST /readings         → Add a new reading (updates sensor currentValue)
```

---

## Technology Stack

- **Java 17**
- **Jakarta REST (JAX-RS)** — Jersey 3.1.6 implementation
- **WAR deployment** — deploy to Tomcat 10 / GlassFish
- **Jackson** — JSON serialization/deserialization
- **Maven** — build and dependency management
- **In-memory storage** — HashMap/ArrayList, no database used

---

## How to Build and Run

### Prerequisites

- Java JDK 17 or higher
- Maven 3.8 or higher
- Tomcat 10.x or GlassFish 7+

### Build the WAR

```bash
mvn clean package
```

This creates:

```
target/smart-campus-api.war
```

### Deploy

Deploy the generated WAR to your application server.

After deployment, the API root is:

```
http://localhost:8080/smart-campus-api/api/v1/
```

### NetBeans

1. Open NetBeans.
2. Choose **File > Open Project** and select the `smart-campus-api` folder.
3. Let NetBeans resolve Maven dependencies.
4. Right-click the project and choose **Properties**.
5. Under **Run**, set the server to **Tomcat 10** or **GlassFish**.
6. Right-click the project and choose **Run** or **Deploy**.

---

## Sample curl Commands

### 1. Discovery Endpoint
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/
```

### 2. Get All Rooms
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms
```

### 3. Create a New Room
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-001","name":"Main Hall","capacity":200}'
```

### 4. Get a Specific Room
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms/LIB-301
```

### 5. Delete a Room (with sensors — expect 409 Conflict)
```bash
curl -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/LIB-301
```

### 6. Get All Sensors Filtered by Type
```bash
curl -X GET "http://localhost:8080/smart-campus-api/api/v1/sensors?type=CO2"
```

### 7. Register a New Sensor
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","status":"ACTIVE","currentValue":21.0,"roomId":"LIB-301"}'
```

### 8. Register Sensor with Invalid Room (expect 422)
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-999"}'
```

### 9. Add a Sensor Reading
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.5}'
```

### 10. Get Sensor Reading History
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-001/readings
```

---

## Error Handling Summary

| Scenario | Exception Class | HTTP Status |
|----------|----------------|-------------|
| Delete room that still has sensors | `RoomNotEmptyException` | 409 Conflict |
| Register sensor with non-existent roomId | `LinkedResourceNotFoundException` | 422 Unprocessable Entity |
| Post reading to a MAINTENANCE sensor | `SensorUnavailableException` | 403 Forbidden |
| Any unexpected runtime error | `GlobalExceptionMapper` (Throwable) | 500 Internal Server Error |

---

## Project Structure

```
smart-campus-api/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── SmartCampusApplication.java
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── store/
    │   └── DataStore.java
    ├── resource/
    │   ├── DiscoveryResource.java
    │   ├── RoomResource.java
    │   ├── SensorResource.java
    │   └── SensorReadingResource.java
    └── exception/
        ├── RoomNotEmptyException.java
        ├── RoomNotEmptyExceptionMapper.java
        ├── LinkedResourceNotFoundException.java
        ├── LinkedResourceNotFoundExceptionMapper.java
        ├── SensorUnavailableException.java
        ├── SensorUnavailableExceptionMapper.java
        └── GlobalExceptionMapper.java
```

---

## Report: Question Answers

### Part 1.1 — JAX-RS Resource Lifecycle & In-Memory Data Management

By default, JAX-RS creates a new instance of each resource class for every incoming HTTP request. This behaviour is known as request-scoped, and it has an important impact on how data is handled.

If data such as a rooms HashMap is stored as an instance variable inside RoomResource, it will not persist between requests. After each request is completed, that instance is discarded by the runtime along with its data. As a result, a subsequent request like GET /rooms would return an empty list, since the previously stored data no longer exists.

To handle this, a DataStore class was implemented using the Singleton design pattern. This class maintains a single static instance of itself, which can be accessed by all resource classes through the DataStore.getInstance() method. The getInstance() method is marked as synchronized, ensuring that if multiple requests try to initialise the DataStore at the same time, only one thread is allowed to proceed. This prevents race conditions and ensures safe initialisation.

The three HashMaps — for rooms, sensors, and readings — are stored inside this singleton instance. Since this instance is shared across all requests and threads, any updates (such as adding a room or modifying a sensor’s currentValue) are immediately visible to all future requests. This approach provides a simple and thread-safe in-memory data storage solution without the need for a database.

---

### Part 1.2 — HATEOAS (Hypermedia as the Engine of Application State)

HATEOAS means that API responses don’t just return data — they also include links that help the client navigate the API. These links show what actions can be taken next and where to go, similar to how you move through a website by clicking links instead of remembering every URL.

In the discovery endpoint (GET /api/v1/), the response includes a resources map that provides links to endpoints like /api/v1/rooms and /api/v1/sensors. This allows the client to read and follow these links dynamically instead of hardcoding them.

The main advantage of this approach is flexibility. If the API’s URL structure changes in the future, clients using HATEOAS won’t break because they rely on the links provided in the response rather than fixed URLs. Unlike static documentation, which can quickly become outdated, HATEOAS makes the API more self-descriptive. It also reduces the dependency between the client and server, which is an important goal in RESTful API design.
---

### Part 2.1 — Returning IDs Only vs Full Room Objects in List Responses

This is a practical design decision with clear trade-offs, and it mainly depends on how the API is going to be used.

If only IDs are returned in a list response, the payload stays small and fast to transfer. However, the client then has to send a separate GET request for each ID to get the actual details. In a system with many rooms, this could lead to a large number of extra requests — this is known as the N+1 problem, and it can seriously affect performance.

On the other hand, returning full room objects in the list gives the client all the required information in a single request. For example, in a campus system where a dashboard needs to show room names, capacities, and sensor counts, this approach is much more practical. The downside is that the response size becomes larger, which can be an issue on slower or mobile networks.

In this implementation, full room objects are returned in the list. Since clients in a campus management system usually need more than just the ID (like name and capacity), it makes sense to get everything in one request instead of making multiple calls.

A common middle-ground used in real-world APIs is to return a summary object (for example, just id and name) in the list, and provide full details only when calling GET /rooms/{id}. This balances performance and usability by reducing payload size while still limiting the number of requests.

---

### Part 2.2 — Is DELETE Idempotent in This Implementation?

Yes — this is something important to think about. Idempotency means that making the same request multiple times should result in the same state on the server as making it once. According to the HTTP specification, DELETE is considered an idempotent operation.

In this implementation, when DELETE /api/v1/rooms/HALL-001 is called for the first time, the server locates the room, removes it from the HashMap, and returns 200 OK. If the same request is sent again, the room no longer exists, so the server returns 404 Not Found because there is nothing left to delete.

At first, it might seem like getting different response codes (200 vs 404) means the operation is not idempotent. However, idempotency is concerned with the state of the server, not the response itself. After the first request — and after every repeated request — the server remains in the same state: the room does not exist.

So even though the responses differ, the final result on the server is identical each time. This satisfies the definition of idempotency and is the expected behaviour for RESTful DELETE operations.

---

### Part 3.1 — Technical Consequences of @Consumes Media Type Mismatches

The @Consumes(MediaType.APPLICATION_JSON) annotation in JAX-RS is used to specify that an endpoint only accepts requests with a Content-Type of application/json. It acts as a clear contract between the client and the server.

If a client sends a POST request to /api/v1/sensors with a different content type, such as text/plain or application/xml, the request will not reach the createSensor() method. Instead, JAX-RS checks the Content-Type header against what is defined in @Consumes. If they do not match, the framework automatically returns a 415 Unsupported Media Type response.

The advantage of this approach is that no manual validation code is needed. JAX-RS handles everything at the framework level, keeping the code cleaner. This also creates a clear separation of concerns — the framework ensures the request format is correct, while the business logic only runs when valid input is provided. Additionally, clients receive a standard and meaningful error response that they can handle properly, rather than a generic server error.

---

### Part 3.2 — @QueryParam vs Path Parameters for Filtering Collections

Using a path parameter like /api/v1/sensors/type/CO2 suggests that type/CO2 is a separate resource in the API structure. However, CO2 is not actually a resource — it’s just a filter condition. Designing URLs this way can be misleading and also makes the API less flexible. For example, if you need to filter by both type and status, the path would become something like /sensors/type/CO2/status/ACTIVE, which quickly becomes messy and hard to maintain.

Query parameters, such as /api/v1/sensors?type=CO2, are a better fit for filtering. They are optional, so the base endpoint /api/v1/sensors can still return all sensors when no filters are applied. Adding ?type=CO2 simply refines the results without changing the actual resource. It also scales well — multiple filters can be easily combined, for example ?type=CO2&status=ACTIVE.

In JAX-RS, the @QueryParam annotation makes this easy to handle. If a parameter is not provided, it is received as null, and the filtering logic can simply skip it. Overall, query parameters are cleaner, more flexible, and are the standard approach in REST APIs for filtering and searching collections.

---

### Part 4.1 — Architectural Benefits of the Sub-Resource Locator Pattern

The sub-resource locator pattern is used to delegate handling of nested paths to a separate class, instead of putting everything into one large controller.

In this implementation, the /readings path is not handled directly inside SensorResource. Instead, a locator method passes control to a separate SensorReadingResource class. The SensorResource does not deal with how readings work — it simply delegates the request and passes the sensorId to the new resource through its constructor.

This approach has clear benefits, especially in larger APIs. Each resource class has a single responsibility. For example, SensorReadingResource only focuses on readings — such as their history, validation, and any related logic. If changes are needed, there is a specific place to make them. It also makes unit testing easier, since SensorReadingResource can be tested independently without involving sensor-related logic.

In contrast, having one large controller that handles everything can quickly become difficult to manage. As more endpoints are added, the class becomes harder to read and maintain. The sub-resource locator pattern helps avoid this by keeping the structure modular and organised. It follows the Single Responsibility Principle and works well as the API grows in complexity.

---

### Part 5.2 — Why HTTP 422 is More Semantically Accurate Than HTTP 404

HTTP 404 Not Found means that the requested URL does not exist on the server. For example, if a client calls GET /api/v1/rooms/FAKE-999 and that room does not exist, returning 404 is correct because the resource itself cannot be found.

However, when a client sends a POST request to /api/v1/sensors with a valid JSON body that includes a roomId which does not exist, the situation is different. In this case, the endpoint /api/v1/sensors is valid, and the request format is correct. The issue lies in the data itself — the roomId is referencing something that does not exist.

Returning 404 here would be misleading, as it suggests that the endpoint is wrong. This could confuse the client and lead them to debug the wrong problem. Instead, HTTP 422 Unprocessable Entity is more appropriate. It clearly indicates that the request was understood and processed, but the data provided is semantically invalid.

This makes the error more accurate and helpful, allowing developers to quickly identify and fix the issue.

---

### Part 5.4 — Cybersecurity Risks of Exposing Java Stack Traces

When an unhandled exception occurs in Java and the stack trace is returned in an API response, it often exposes full class names and the internal package structure of the application. This allows an attacker to understand how the system is organised, including class hierarchy and sometimes even the exact line numbers where the error happened. This reveals details about both the code structure and the internal logic flow.

More importantly, stack traces can expose the libraries and their versions being used (for example, Jersey 2.41, Jackson 2.15, Java 11). An attacker can take these versions and compare them against public vulnerability databases such as CVE lists to identify known security issues. This enables targeted attacks, which are far more dangerous than random or blind attempts.

In addition, stack traces may reveal internal file paths on the server, database connection details if an exception propagates too far, and even hints about business logic. All of this information can help an attacker craft more precise inputs to exploit weaknesses in the system.

The global ExceptionMapper<Throwable> solves this problem by catching all unhandled exceptions and returning a clean, generic HTTP 500 Internal Server Error response. This response contains no internal details — only a simple message indicating that something went wrong on the server. This ensures that sensitive implementation details are not exposed and cannot be used maliciously.

---

### Part 5.5 — Why JAX-RS Filters Are Superior to Manual Logging in Resource Methods

The main advantage of using JAX-RS filters for cross-cutting concerns like logging is that they keep business logic separate from infrastructure-related tasks. A cross-cutting concern is something that applies to all requests and responses across the API, regardless of which resource method is being executed.

If logging was implemented manually by adding Logger.info() statements inside every resource method, it would quickly become difficult to manage. Every new endpoint would require additional logging code. Also, if the log format needed to change — for example, adding a timestamp or request ID — every single resource method across the application would need to be updated individually. This approach is repetitive, error-prone, and goes against the DRY principle (Don’t Repeat Yourself).

A single filter class implementing both ContainerRequestFilter and ContainerResponseFilter solves all of this elegantly. The filter sits outside the resource methods and intercepts every request before it reaches the method and every response before it leaves the server. This means logging happens automatically for every endpoint — existing ones and any new ones added in the future — without touching the resource classes at all.

This approach also makes the resource methods cleaner and easier to read, since they only contain the business logic they are supposed to handle. The filter handles observability as a separate, dedicated responsibility. In production systems this pattern is used for logging, authentication checks, CORS headers, rate limiting and compression — all without polluting the resource layer with infrastructure code.
