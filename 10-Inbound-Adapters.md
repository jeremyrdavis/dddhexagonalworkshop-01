# Step 10: Wrapping Up With an Inbound Adapter (REST Endpoint)

## tl;dr

Inbound adapters translate between external protocols and domain operations. Our REST endpoint handles HTTP concerns while delegating business logic to domain services:

```java
package dddhexagonalworkshop.conference.attendees.infrastructure;

import dddhexagonalworkshop.conference.attendees.domain.services.AttendeeService;
import dddhexagonalworkshop.conference.attendees.domain.services.RegisterAttendeeCommand;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;


/**
 * "The application is blissfully ignorant of the nature of the input device. When the application has something to send out, it sends it out through a port to an adapter, which creates the appropriate signals needed by the receiving technology (human or automated). The application has a semantically sound interaction with the adapters on all sides of it, without actually knowing the nature of the things on the other side of the adapters."
 * Alistair Cockburn, Hexagonal Architecture, 2005.
 *
 */
@Path("/attendees")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AttendeeEndpoint {

    @Inject
    AttendeeService attendeeService;

    @POST
    public Response registerAttendee(RegisterAttendeeCommand registerAttendeeCommand) {
        Log.debugf("Creating attendee %s", registerAttendeeCommand);

        AttendeeDTO attendeeDTO = attendeeService.registerAttendee(registerAttendeeCommand);

        Log.debugf("Created attendee %s", attendeeDTO);

        return Response.created(URI.create("/" + attendeeDTO.email())).entity(attendeeDTO).build();
    }

}
```

## Learning Objectives
- **Understand** Inbound Adapters as the entry point for external requests into the domain
- **Implement** AttendeeEndpoint as a REST adapter using JAX-RS
- **Apply** Hexagonal Architecture principles to decouple HTTP concerns from business logic
- **Complete** the end-to-end DDD workflow from HTTP request to domain operation

## What You'll Build
An `AttendeeEndpoint` REST controller that serves as the inbound adapter, handling HTTP requests, delegating to domain services, and returning JSON responses while maintaining clean architectural boundaries.

## Why Inbound Adapters Are Critical

Inbound Adapters solve the fundamental problem of **how external systems interact with your domain** without polluting business logic with technology-specific concerns:

**The Technology Intrusion Problem**: Without adapters, HTTP concerns leak into domain logic:

❌ HTTP concerns mixed with business logic

```java
public class AttendeeService {
    public Response registerAttendee(HttpServletRequest request) {
        // HTTP parsing in domain service!
        String email = request.getParameter("email");
        if (email == null) {
            return Response.status(400).entity("Email required").build();
        }
        
        // Domain logic mixed with HTTP response handling
        try {
            Attendee attendee = Attendee.registerAttendee(email);
            String json = objectMapper.writeValueAsString(attendee);
            return Response.ok(json).build();
        } catch (Exception e) {
            return Response.status(500).entity("Server error").build();
        }
    }
}
```

✅ Clean separation through inbound adapter

**The Inbound Adapter Solution**: Clean separation between HTTP and domain:
```java
@Path("/attendees")
public class AttendeeEndpoint {
    @Inject AttendeeService attendeeService;  // Domain interface
    
    @POST
    public Response register(RegisterAttendeeCommand command) {
        // Adapter handles HTTP specifics
        AttendeeDTO result = attendeeService.registerAttendee(command);
        return Response.created(URI.create("/" + result.email())).entity(result).build();
    }
}

// Domain service stays pure
public class AttendeeService {
    public AttendeeDTO registerAttendee(RegisterAttendeeCommand command) {
        // Pure business logic, no HTTP concerns
    }
}
```

## Hexagonal Architecture: Inbound vs Outbound Deep Dive

Understanding the flow of data through hexagonal architecture is crucial for proper adapter implementation:

### Adapter Flow Patterns

| Flow Type | Direction | Purpose | Examples | Initiator |
|-----------|-----------|---------|----------|-----------|
| **Inbound (Primary)** | External → Domain | Receive requests | REST, GraphQL, CLI, Events | External systems |
| **Outbound (Secondary)** | Domain → External | Send commands/queries | Database, Messaging, Email | Domain logic |

### Inbound Adapter Responsibilities

| Responsibility | Description | Example |
|----------------|-------------|---------|
| **Protocol Translation** | Convert external protocols to domain calls | HTTP → Domain Commands |
| **Input Validation** | Validate external input format | JSON schema, field validation |
| **Authentication/Authorization** | Security boundary enforcement | JWT validation, role checks |
| **Error Translation** | Convert domain errors to external format | Domain exceptions → HTTP status codes |
| **Content Negotiation** | Handle different response formats | JSON, XML, CSV responses |
| **Rate Limiting** | Protect domain from overload | Request throttling, circuit breakers |

**Protocol Translation Example**:
```java
@Path("/attendees")
public class AttendeeEndpoint {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(
        @Valid RegisterAttendeeRequest request,  // HTTP JSON → Request DTO
        @Context HttpHeaders headers,
        @Context UriInfo uriInfo
    ) {
        // Translate HTTP request to domain command
        RegisterAttendeeCommand command = request.toCommand();
        
        // Delegate to domain
        AttendeeDTO result = attendeeService.registerAttendee(command);
        
        // Translate domain result to HTTP response
        URI location = uriInfo.getAbsolutePathBuilder()
            .path(result.email())
            .build();
            
        return Response.created(location)
            .entity(result)
            .build();
    }
}
```

#### REST Endpoint Patterns Comparison

| Pattern | Coupling | Testability | Complexity | Flexibility | Use Case |
|---------|----------|-------------|------------|-------------|----------|
| **Direct Service Call** | High | Difficult | Low | Low | Simple CRUD |
| **Command/Query Pattern** | Medium | Good | Medium | High | CQRS applications |
| **Use Case Pattern** | Low | Excellent | High | Very High | Complex domains |
| **Event-Driven** | Very Low | Excellent | High | Very High | Reactive systems |

**Direct Service Call** (Simple but coupled):
```java
@Path("/attendees")
public class AttendeeEndpoint {
    @Inject AttendeeService service;
    
    @POST
    public AttendeeDTO register(RegisterAttendeeRequest request) {
        return service.registerAttendee(request.toCommand());
    }
}
```

**Use Case Pattern** (Clean but more complex):
```java
@Path("/attendees")
public class AttendeeEndpoint {
    @Inject RegisterAttendeeUseCase registerUseCase;
    @Inject FindAttendeeUseCase findUseCase;
    
    @POST
    public AttendeeDTO register(RegisterAttendeeRequest request) {
        RegisterAttendeeCommand command = request.toCommand();
        return registerUseCase.execute(command);
    }
    
    @GET
    @Path("/{email}")
    public AttendeeDTO find(@PathParam("email") String email) {
        FindAttendeeQuery query = new FindAttendeeQuery(email);
        return findUseCase.execute(query);
    }
}
```

**Event-Driven Pattern** (Async but complex):
```java
@Path("/attendees")
public class AttendeeEndpoint {
    @Inject CommandBus commandBus;
    
    @POST
    public Response register(RegisterAttendeeRequest request) {
        RegisterAttendeeCommand command = request.toCommand();
        String correlationId = commandBus.send(command);
        
        return Response.accepted()
            .header("X-Correlation-ID", correlationId)
            .entity(Map.of("status", "PROCESSING", "correlationId", correlationId))
            .build();
    }
}
```

## Implementation

Inbound adapters translate between external protocols and domain operations. Our REST endpoint handles HTTP concerns while delegating business logic to domain services.

```java
package dddhexagonalworkshop.conference.attendees.infrastructure;

import dddhexagonalworkshop.conference.attendees.domain.services.AttendeeService;
import dddhexagonalworkshop.conference.attendees.domain.services.RegisterAttendeeCommand;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * REST Inbound Adapter for attendee operations.
 * 
 * This adapter serves as the primary port in hexagonal architecture,
 * translating HTTP requests into domain operations while maintaining
 * clean separation between web concerns and business logic.
 * 
 * Responsibilities:
 * - HTTP protocol handling (request/response mapping)
 * - Input validation and sanitization
 * - Error translation (domain exceptions → HTTP status codes)
 * - Content negotiation and response formatting
 * - Security boundary enforcement
 * - Request logging and monitoring
 */
@Path("/attendees")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AttendeeEndpoint {

    @Inject
    AttendeeService attendeeService;

    /**
     * Registers a new attendee for the conference.
     * 
     * This endpoint demonstrates the complete inbound adapter pattern:
     * 1. Receives HTTP POST request with JSON payload
     * 2. Validates input using Bean Validation
     * 3. Converts request to domain command
     * 4. Delegates to domain service
     * 5. Translates domain result to HTTP response
     * 6. Returns appropriate HTTP status code and location header
     * 
     * @param command The registration command (auto-deserialized from JSON)
     * @param uriInfo JAX-RS context for building response URIs
     * @return HTTP 201 Created with attendee DTO and location header
     */
    @POST
    public Response registerAttendee(
        @Valid RegisterAttendeeCommand command,
        @Context UriInfo uriInfo
    ) {
        Log.infof("Received attendee registration request for email: %s", 
                 maskEmail(command.email()));

        try {
            // Delegate to domain service (pure business logic)
            AttendeeDTO attendeeDTO = attendeeService.registerAttendee(command);

            // Build location URI for created resource
            URI location = uriInfo.getAbsolutePathBuilder()
                .path(attendeeDTO.email())
                .build();

            Log.infof("Successfully registered attendee: %s", 
                     maskEmail(attendeeDTO.email()));

            // Return HTTP 201 Created with location header
            return Response.created(location)
                .entity(attendeeDTO)
                .build();

        } catch (DuplicateRegistrationException e) {
            Log.warnf("Duplicate registration attempt for email: %s", 
                     maskEmail(command.email()));
            return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse("DUPLICATE_REGISTRATION", e.getMessage()))
                .build();

        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid registration data: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("INVALID_INPUT", e.getMessage()))
                .build();

        } catch (AttendeeRegistrationException e) {
            Log.errorf(e, "Registration failed for email: %s", 
                      maskEmail(command.email()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("REGISTRATION_FAILED", 
                       "Registration could not be completed. Please try again."))
                .build();

        } catch (Exception e) {
            Log.errorf(e, "Unexpected error during registration for email: %s", 
                      maskEmail(command.email()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", 
                       "An unexpected error occurred. Please contact support."))
                .build();
        }
    }

    /**
     * Retrieves an attendee by email address.
     * 
     * Demonstrates query operations and proper HTTP semantics:
     * - HTTP 200 OK when attendee is found
     * - HTTP 404 Not Found when attendee doesn't exist
     * - HTTP 400 Bad Request for invalid email format
     * 
     * @param email The attendee's email address
     * @return HTTP response with attendee DTO or error
     */
    @GET
    @Path("/{email}")
    public Response getAttendee(
        @PathParam("email") 
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email must be valid") 
        String email
    ) {
        Log.debugf("Retrieving attendee for email: %s", maskEmail(email));

        try {
            Optional<AttendeeDTO> attendee = attendeeService.findAttendeeByEmail(email);

            if (attendee.isPresent()) {
                Log.debugf("Found attendee: %s", maskEmail(email));
                return Response.ok(attendee.get()).build();
            } else {
                Log.debugf("Attendee not found: %s", maskEmail(email));
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("ATTENDEE_NOT_FOUND", 
                           "Attendee with email " + email + " not found"))
                    .build();
            }

        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid email format: %s", email);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("INVALID_EMAIL", e.getMessage()))
                .build();

        } catch (Exception e) {
            Log.errorf(e, "Error retrieving attendee: %s", maskEmail(email));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("RETRIEVAL_ERROR", 
                       "Could not retrieve attendee information"))
                .build();
        }
    }

    /**
     * Lists all registered attendees.
     * 
     * Demonstrates collection endpoints with:
     * - Pagination support (query parameters)
     * - Content negotiation
     * - Performance considerations
     * 
     * @param page Page number (0-based, default 0)
     * @param size Page size (default 20, max 100)
     * @param status Optional status filter
     * @return HTTP response with attendee list
     */
    @GET
    public Response listAttendees(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("20") int size,
        @QueryParam("status") String status
    ) {
        Log.debugf("Listing attendees - page: %d, size: %d, status: %s", 
                  page, size, status);

        try {
            // Validate pagination parameters
            if (page < 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_PAGE", "Page must be >= 0"))
                    .build();
            }

            if (size <= 0 || size > 100) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_SIZE", "Size must be 1-100"))
                    .build();
            }

            // Delegate to domain service
            PagedResult<AttendeeDTO> result = attendeeService.findAttendees(page, size, status);

            // Add pagination headers
            Response.ResponseBuilder responseBuilder = Response.ok(result.getContent())
                .header("X-Total-Count", result.getTotalElements())
                .header("X-Page-Number", result.getPageNumber())
                .header("X-Page-Size", result.getPageSize())
                .header("X-Total-Pages", result.getTotalPages());

            // Add Link header for pagination (RFC 5988)
            if (result.hasNext()) {
                responseBuilder.header("Link", 
                    String.format("</attendees?page=%d&size=%d>; rel=\"next\"", 
                                page + 1, size));
            }

            Log.debugf("Returning %d attendees (page %d of %d)", 
                      result.getContent().size(), result.getPageNumber(), result.getTotalPages());

            return responseBuilder.build();

        } catch (Exception e) {
            Log.errorf(e, "Error listing attendees");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("LIST_ERROR", "Could not retrieve attendee list"))
                .build();
        }
    }

    /**
     * Cancels an attendee's registration.
     * 
     * Demonstrates:
     * - HTTP DELETE semantics
     * - Idempotent operations
     * - Business rule validation
     * 
     * @param email The attendee's email address
     * @return HTTP 204 No Content on success
     */
    @DELETE
    @Path("/{email}")
    public Response cancelRegistration(
        @PathParam("email") 
        @NotBlank @Email String email
    ) {
        Log.infof("Cancelling registration for email: %s", maskEmail(email));

        try {
            attendeeService.cancelRegistration(email);

            Log.infof("Successfully cancelled registration for: %s", maskEmail(email));
            return Response.noContent().build();

        } catch (AttendeeNotFoundException e) {
            Log.warnf("Attempted to cancel non-existent attendee: %s", maskEmail(email));
            // Return 204 for idempotent behavior (already doesn't exist)
            return Response.noContent().build();

        } catch (CancellationNotAllowedException e) {
            Log.warnf("Cancellation not allowed for: %s - %s", maskEmail(email), e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse("CANCELLATION_NOT_ALLOWED", e.getMessage()))
                .build();

        } catch (Exception e) {
            Log.errorf(e, "Error cancelling registration for: %s", maskEmail(email));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("CANCELLATION_ERROR", 
                       "Could not cancel registration"))
                .build();
        }
    }

    /**
     * Health check endpoint for monitoring and load balancers.
     * 
     * @return HTTP 200 OK with simple status
     */
    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public Response health() {
        return Response.ok("OK").build();
    }

    /**
     * Masks email addresses for privacy in logs.
     * Shows first 2 characters and domain for identification.
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        String maskedLocal = localPart.length() <= 2 
            ? "**" 
            : localPart.substring(0, 2) + "***";

        return maskedLocal + domain;
    }
}

/**
 * Standard error response DTO for consistent error handling.
 */
record ErrorResponse(
    String errorCode,
    String message,
    String timestamp
) {
    public ErrorResponse(String errorCode, String message) {
        this(errorCode, message, java.time.Instant.now().toString());
    }
}

/**
 * Paged result wrapper for collection endpoints.
 */
record PagedResult<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {
    public static <T> PagedResult<T> of(
        List<T> content, 
        int page, 
        int size, 
        long total
    ) {
        int totalPages = (int) Math.ceil((double) total / size);
        return new PagedResult<>(
            content,
            page,
            size,
            total,
            totalPages,
            page < totalPages - 1,
            page > 0
        );
    }
}
```

### Key Design Decisions

**JAX-RS Annotations**: Standard Java REST annotations provide declarative configuration for HTTP mapping.

**Bean Validation**: `@Valid` annotations enable automatic input validation with meaningful error messages.

**Error Translation**: Domain exceptions are caught and translated to appropriate HTTP status codes.

**Privacy Protection**: Email masking in logs prevents sensitive data exposure.

**HTTP Semantics**: Proper use of status codes (201 Created, 404 Not Found, 409 Conflict) follows REST conventions.

**Content Type Handling**: Explicit content type declarations ensure proper serialization/deserialization.

## Testing Your Implementation

**Unit Testing the Endpoint**:
```java
@ExtendWith(MockitoExtension.class)
class AttendeeEndpointTest {
    
    @Mock AttendeeService attendeeService;
    @Mock UriInfo uriInfo;
    @Mock UriBuilder uriBuilder;
    
    @InjectMocks AttendeeEndpoint endpoint;
    
    @BeforeEach
    void setUp() {
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(URI.create("/attendees/test@example.com"));
    }
    
    @Test
    void shouldRegisterAttendeeSuccessfully() {
        // Given
        RegisterAttendeeCommand command = new RegisterAttendeeCommand("test@example.com");
        AttendeeDTO expectedDTO = new AttendeeDTO("test@example.com");
        when(attendeeService.registerAttendee(command)).thenReturn(expectedDTO);
        
        // When
        Response response = endpoint.registerAttendee(command, uriInfo);
        
        // Then
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getEntity()).isEqualTo(expectedDTO);
        assertThat(response.getLocation()).isNotNull();
        verify(attendeeService).registerAttendee(command);
    }
    
    @Test
    void shouldReturnConflictForDuplicateRegistration() {
        // Given
        RegisterAttendeeCommand command = new RegisterAttendeeCommand("duplicate@example.com");
        when(attendeeService.registerAttendee(command))
            .thenThrow(new DuplicateRegistrationException("Already registered"));
        
        // When
        Response response = endpoint.registerAttendee(command, uriInfo);
        
        // Then
        assertThat(response.getStatus()).isEqualTo(409);
        ErrorResponse error = (ErrorResponse) response.getEntity();
        assertThat(error.errorCode()).isEqualTo("DUPLICATE_REGISTRATION");
    }
    
    @Test
    void shouldReturnBadRequestForInvalidInput() {
        // Given
        RegisterAttendeeCommand command = new RegisterAttendeeCommand("invalid-email");
        when(attendeeService.registerAttendee(command))
            .thenThrow(new IllegalArgumentException("Invalid email"));
        
        // When
        Response response = endpoint.registerAttendee(command, uriInfo);
        
        // Then
        assertThat(response.getStatus()).isEqualTo(400);
        ErrorResponse error = (ErrorResponse) response.getEntity();
        assertThat(error.errorCode()).isEqualTo("INVALID_INPUT");
    }
    
    @Test
    void shouldFindExistingAttendee() {
        // Given
        String email = "existing@example.com";
        AttendeeDTO expectedDTO = new AttendeeDTO(email);
        when(attendeeService.findAttendeeByEmail(email))
            .thenReturn(Optional.of(expectedDTO));
        
        // When
        Response response = endpoint.getAttendee(email);
        
        // Then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isEqualTo(expectedDTO);
    }
    
    @Test
    void shouldReturnNotFoundForMissingAttendee() {
        // Given
        String email = "missing@example.com";
        when(attendeeService.findAttendeeByEmail(email))
            .thenReturn(Optional.empty());
        
        // When
        Response response = endpoint.getAttendee(email);
        
        // Then
        assertThat(response.getStatus()).isEqualTo(404);
        ErrorResponse error = (ErrorResponse) response.getEntity();
        assertThat(error.errorCode()).isEqualTo("ATTENDEE_NOT_FOUND");
    }
}
```

**Integration Testing with REST Assured**:
```java
@QuarkusTest
class AttendeeEndpointIntegrationTest {
    
    @Test
    void shouldCompleteRegistrationWorkflow() {
        // Register new attendee
        RegisterAttendeeCommand command = new RegisterAttendeeCommand("integration@example.com");
        
        ValidatableResponse response = given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(command)
        .when()
            .post("/attendees")
        .then()
            .statusCode(201)
            .header("Location", notNullValue())
            .body("email", equalTo("integration@example.com"))
            .body("registrationStatus", equalTo("REGISTERED"));
        
        // Verify attendee can be retrieved
        given()
        .when()
            .get("/attendees/integration@example.com")
        .then()
            .statusCode(200)
            .body("email", equalTo("integration@example.com"));
        
        // Verify duplicate registration is rejected
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(command)
        .when()
            .post("/attendees")
        .then()
            .statusCode(409)
            .body("errorCode", equalTo("DUPLICATE_REGISTRATION"));
    }
    
    @Test
    void shouldValidateInputCorrectly() {
        // Test invalid email
        RegisterAttendeeCommand invalidCommand = new RegisterAttendeeCommand("invalid-email");
        
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(invalidCommand)
        .when()
            .post("/attendees")
        .then()
            .statusCode(400);
        
        // Test empty email
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"email\": \"\"}")
        .when()
            .post("/attendees")
        .then()
            .statusCode(400);
    }
    
    @Test
    void shouldHandlePaginationCorrectly() {
        // Register multiple attendees
        for (int i = 1; i <= 25; i++) {
            RegisterAttendeeCommand command = new RegisterAttendeeCommand("test" + i + "@example.com");
            given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(command)
            .when()
                .post("/attendees");
        }
        
        // Test pagination
        given()
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/attendees")
        .then()
            .statusCode(200)
            .header("X-Total-Count", notNullValue())
            .header("X-Page-Number", equalTo("0"))
            .header("X-Page-Size", equalTo("10"))
            .header("Link", containsString("rel=\"next\""))
            .body("size()", equalTo(10));
    }
}
```

**Contract Testing for API Stability**:
```java
@Test
void shouldMaintainAPIContract() {
    // Test that API contract remains stable
    RegisterAttendeeCommand command = new RegisterAttendeeCommand("contract@example.com");
    
    String responseJson = given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(command)
    .when()
        .post("/attendees")
    .then()
        .statusCode(201)
        .extract()
        .asString();
    
    // Verify JSON structure
    JsonPath jsonPath = JsonPath.from(responseJson);
    assertThat(jsonPath.getString("email")).isEqualTo("contract@example.com");
    assertThat(jsonPath.getString("registration_status")).isNotNull();
    assertThat(jsonPath.getString("registered_at")).isNotNull();
    
    // Verify required fields are present
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(responseJson);
    assertThat(jsonNode.has("email")).isTrue();
    assertThat(jsonNode.has("registration_status")).isTrue();
    assertThat(jsonNode.has("registered_at")).isTrue();
}
```

## Connection to Other Components

This endpoint completes the hexagonal architecture by:
1. **Receiving** HTTP requests from external clients
2. **Converting** JSON to domain commands
3. **Delegating** to `AttendeeService` for business logic
4. **Transforming** domain results to DTOs
5. **Returning** JSON responses with proper HTTP semantics

## Advanced Inbound Adapter Patterns

**Content Negotiation** for multiple response formats:
```java
@Path("/attendees")
public class AttendeeEndpoint {
    
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "text/csv"})
    public Response listAttendees(@Context HttpHeaders headers) {
        List<AttendeeDTO> attendees = attendeeService.findAllAttendees();
        
        MediaType acceptedType = headers.getAcceptableMediaTypes().get(0);
        
        return switch (acceptedType.toString()) {
            case MediaType.APPLICATION_XML -> 
                Response.ok(new AttendeeListXML(attendees)).build();
            case "text/csv" -> 
                Response.ok(convertToCSV(attendees))
                    .header("Content-Disposition", "attachment; filename=attendees.csv")
                    .build();
            default -> 
                Response.ok(attendees).build();
        };
    }
}
```

**Versioning Support** for API evolution:
```java
@Path("/attendees")
public class AttendeeEndpoint {
    
    @POST
    public Response register(
        RegisterAttendeeCommand command,
        @HeaderParam("Accept-Version") String version,
        @Context UriInfo uriInfo
    ) {
        AttendeeDTO result = attendeeService.registerAttendee(command);
        
        // Transform response based on requested version
        Object responseBody = switch (version) {
            case "v1" -> AttendeeV1DTO.fromV2(result);
            case "v2" -> result;
            case null, default -> result;  // Latest version as default
        };
        
        return Response.created(buildLocation(result, uriInfo))
            .entity(responseBody)
            .header("Content-Version", version != null ? version : "v2")
            .build();
    }
}
```

**Security Integration** with authentication and authorization:
```java
@Path("/attendees")
@RolesAllowed({"USER", "ADMIN"})
public class AttendeeEndpoint {

    @POST
    @RolesAllowed("ADMIN")  // Only admins can register others
    public Response register(
            RegisterAttendeeCommand command,
            @Context SecurityContext securityContext
    ) {
        // Get current user info
        Principal principal = securityContext.getUserPrincipal();
        String currentUser = principal.getName();

        // Add audit info to command
        AuditedRegisterAttendeeCommand auditedCommand =
                new AuditedRegisterAttendeeCommand(command, currentUser);

        AttendeeDTO result = attendeeService.registerAttendee(auditedCommand);
        return Response.created(buildLocation(result)).entity(result).build();
    }

    @GET
    @Path("/{email}")
    public Response getAttendee(
            @PathParam("email") String email,
            @Context SecurityContext securityContext
    ) {
        // Users can only access their own data
        if (!securityContext.isUserInRole("ADMIN") &&
                !securityContext.getUserPrincipal().getName().equals(email)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        Optional<AttendeeDTO> attendee = attendeeService.findAttendeeByEmail(email);
        return attendee.map(a -> Response.ok(a).build())









