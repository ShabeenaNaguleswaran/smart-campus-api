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

By default, JAX-RS creates a brand new instance of each resource class for every single HTTP request that comes in. This is what is called "request-scoped" behaviour — and it has an important consequence for how data is stored.

If data like a rooms HashMap were stored as instance variables inside RoomResource, those would get wiped out after every request. The next call to GET /rooms would return an empty list because the previous instance — along with all its data — was already thrown away by the runtime.

To solve this, a DataStore class was implemented using the Singleton design pattern. The class keeps one static instance of itself, and every resource class accesses that same shared instance through DataStore.getInstance(). The getInstance() method is marked as synchronized, which means if two requests arrive at exactly the same time and both try to initialise the DataStore simultaneously, Java ensures only one of them proceeds — preventing what is known as a race condition.

The three HashMaps — one for rooms, one for sensors, and one for readings — all live inside this singleton. Because they are shared across all requests and threads, any write operation such as adding a room or updating a sensor's currentValue is immediately visible to all subsequent reads. This provides a clean, thread-safe, in-memory data layer without needing a database.

---

### Part 1.2 — HATEOAS (Hypermedia as the Engine of Application State)

HATEOAS means that API responses should include navigational links — not just data. Those links tell the client what actions are available next and where to find related resources, much like how a website lets you follow links rather than memorising every URL.

In the discovery endpoint at GET /api/v1/, the response includes a "resources" map that points clients directly to /api/v1/rooms and /api/v1/sensors. A client application can read those links dynamically rather than having them hardcoded.

The real benefit for developers is flexibility and resilience. If the URL structure ever changes, clients that rely on HATEOAS links do not break because they discover the URL from the response rather than assuming it. Compared to static documentation which goes stale the moment the API changes, hypermedia makes the API genuinely self-describing. It reduces the coupling between client and server, which is exactly what good RESTful design aims for.

---

### Part 2.1 — Returning IDs Only vs Full Room Objects in List Responses

This is a practical design decision with real trade-offs that depends heavily on how the API will be used.

Returning only IDs in a list response keeps the payload small and fast to transfer. But the client then has to make a separate GET request for each ID to retrieve any useful information. In a system with hundreds of rooms, that could mean hundreds of follow-up requests — known as the N+1 problem, which is a serious performance issue.

Returning full room objects in the list gives the client everything it needs in a single call. For a campus system where a dashboard wants to display room names, capacities and sensor counts all at once, this is far more practical. The downside is a larger payload, which matters on slow or mobile connections.

In this implementation, full room objects are returned in the list. For a campus management API where clients typically need name and capacity alongside the ID, one round trip is better than many. A good middle-ground used in production APIs is to return a summary object with just id and name in the list, and reserve the full detail for GET /rooms/{id} — giving the best of both approaches.

---

### Part 2.2 — Is DELETE Idempotent in This Implementation?

Yes — and this is something worth thinking about carefully. Idempotency means that making the same request multiple times produces the same server state as making it once. DELETE is defined as idempotent by the HTTP specification.

Here is exactly what happens in this implementation. The first time DELETE /api/v1/rooms/HALL-001 is called, the server finds the room, removes it from the HashMap and returns 200 OK. If the exact same DELETE is called again, the room is already gone — the server returns 404 Not Found because there is nothing there to delete.

Some might argue that receiving a different status code (200 vs 404) means it is not truly idempotent. But idempotency is about the state of the resource on the server, not the response code. After the first DELETE and every subsequent one, the server is in exactly the same state — the room does not exist. The resource is gone either way. That satisfies the definition of idempotency, and it is the standard behaviour expected of RESTful DELETE endpoints.

---

### Part 3.1 — Technical Consequences of @Consumes Media Type Mismatches

The @Consumes(MediaType.APPLICATION_JSON) annotation is JAX-RS's way of declaring that an endpoint only accepts requests with a Content-Type header of application/json. It is a contract between the server and the client.

If a client sends a POST request to /api/v1/sensors but sets the Content-Type to text/plain or application/xml, JAX-RS intercepts the request before it even reaches the createSensor() method. The framework compares the incoming Content-Type against what @Consumes declares, finds a mismatch, and automatically sends back an HTTP 415 Unsupported Media Type response.

What makes this elegant is that no manual content-type validation code needs to be written. JAX-RS handles it entirely at the framework level. This is a clean separation of concerns — the framework enforces the contract, and the business logic only runs when the input format is correct. It also gives clients a clear, standardised error code they can handle programmatically rather than some vague server error.

---

### Part 3.2 — @QueryParam vs Path Parameters for Filtering Collections

The path parameter approach (/api/v1/sensors/type/CO2) implies that "type/CO2" is a distinct, addressable resource in the hierarchy. But CO2 is not a resource — it is a filter criterion. Designing URLs this way is semantically misleading and makes the API rigid. When filtering by both type AND status is needed, the path approach produces something like /sensors/type/CO2/status/ACTIVE, which is awkward and does not scale.

Query parameters (/api/v1/sensors?type=CO2) are optional by nature. The base URL /api/v1/sensors still works perfectly fine without them, returning all sensors. Adding ?type=CO2 refines the results without changing what the resource fundamentally is. Combining multiple filters is also natural: ?type=CO2&status=ACTIVE.

The @QueryParam annotation in JAX-RS makes this trivial to implement — if the parameter is not provided it comes in as null and the filtering step is simply skipped. Query parameters are clean, intuitive and completely standard REST practice for filtering and searching collections.

---

### Part 4.1 — Architectural Benefits of the Sub-Resource Locator Pattern

The sub-resource locator pattern delegates handling of nested paths to a separate dedicated class rather than defining every nested endpoint in one massive controller.

In this implementation, instead of handling /readings directly inside SensorResource, a locator method hands off control to a dedicated SensorReadingResource class when the /readings path is hit. SensorResource does not need to know anything about how readings work — it simply delegates and passes the sensorId as context through the constructor.

The benefits in a large real-world API are significant. Each resource class has a single, clear responsibility. SensorReadingResource only thinks about readings — their history, their validation, their side effects. If the readings logic needs to change, there is one clear file to open. Unit testing is also cleaner because SensorReadingResource can be tested in isolation without worrying about sensor registration logic.

Compare that to a single monolithic controller handling all paths in one place. That class grows endlessly, becomes difficult to navigate and is a maintenance nightmare. The sub-resource locator pattern applies the Single Responsibility Principle to the API resource structure, and it scales well as the API grows in complexity.

---

### Part 5.2 — Why HTTP 422 is More Semantically Accurate Than HTTP 404

HTTP 404 Not Found means the URL that was requested does not exist on the server. If a client calls GET /api/v1/rooms/FAKE-999 and that room does not exist, 404 is the correct answer — the resource at that URL was not found.

But when a client POSTs to /api/v1/sensors with a perfectly valid JSON body that contains a roomId referencing a room that does not exist, the situation is completely different. The URL /api/v1/sensors is real and found. The JSON is syntactically valid. The problem is a broken reference inside the payload — the value of roomId points to something that does not exist in the system.

Using 404 here would be actively misleading. The client might think they have the wrong endpoint URL and start debugging in the wrong place entirely. HTTP 422 Unprocessable Entity is designed exactly for this scenario — it tells the client that the request was understood, the endpoint was found, the JSON parsed correctly, but the semantic content of the data is invalid. That is a much more accurate and useful error for a developer to receive and act on.

---

### Part 5.4 — Cybersecurity Risks of Exposing Java Stack Traces

When an unhandled exception occurs in Java and the stack trace leaks out in an API response, it typically contains the full class names and package structure of the application. An attacker immediately learns the package names, class hierarchy and exact line numbers where the error occurred — revealing both code structure and potentially the logic flow.

Perhaps most dangerously, stack traces expose the names and versions of the libraries being used — for example Jersey 2.41, Jackson 2.15, Java 11. An attacker can cross-reference these specific versions against public vulnerability databases like the CVE database and find known exploits that work against those exact versions. This is called a targeted attack and it is far more effective than blind probing.

Beyond version information, stack traces can reveal internal file system paths on the server, database connection details if a database exception propagates up, and business logic information that helps an attacker craft malicious inputs designed to exploit specific weaknesses.

The global ExceptionMapper<Throwable> prevents all of this by catching every unhandled exception and returning a clean, generic HTTP 500 response with no internal details — just enough to tell the client something went wrong on the server, without revealing anything that could be weaponised by an attacker.

---

### Part 5.5 — Why JAX-RS Filters Are Superior to Manual Logging in Resource Methods

The core advantage of using JAX-RS filters for cross-cutting concerns like logging is that they enforce a clean separation between business logic and infrastructure concerns. A cross-cutting concern is something that applies universally across the entire API — every single request and response — regardless of which resource method handles it.

If logging were added manually by inserting Logger.info() statements inside every resource method, the problems would multiply quickly. Every new endpoint added in the future would need its own logging code. If the log format ever needed to change — for example adding a timestamp or a request ID — every single method across every resource class would need to be updated individually. It is repetitive, error-prone and violates the DRY principle (Don't Repeat Yourself).

A single filter class implementing both ContainerRequestFilter and ContainerResponseFilter solves all of this elegantly. The filter sits outside the resource methods and intercepts every request before it reaches the method and every response before it leaves the server. This means logging happens automatically for every endpoint — existing ones and any new ones added in the future — without touching the resource classes at all.

This approach also makes the resource methods cleaner and easier to read, since they only contain the business logic they are supposed to handle. The filter handles observability as a separate, dedicated responsibility. In production systems this pattern is used for logging, authentication checks, CORS headers, rate limiting and compression — all without polluting the resource layer with infrastructure code.