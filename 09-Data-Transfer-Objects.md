# Step 9: Data Transfer Objects (DTOs)

***Note:*** This step is not specific to Domain Driven Design.  This is simply a useful coding practice.

## tl;dr

DTOs are used to transfer data between layers, especially when the data structure differs from the domain model. Our AttendeeDTO provides a clean external representation for JSON serialization.

```java
package dddhexagonalworkshop.conference.attendees.infrastructure;

/**
 * Data Transfer Object for Attendee information.
 *
 * DTOs are not specifically a DDD concept, but they are useful in DDD.  This DTO serves as the external contract for attendee data in REST API responses.
 * It provides a stable, clean representation that can evolve independently of the
 * internal domain model structure.
 *
 * Key characteristics:
 * - Immutable (record) to prevent accidental modification
 * - JSON serialization optimized with proper annotations
 * - Input validation annotations for request scenarios
 * - Clear field documentation for API consumers
 * - Decoupled from internal domain model changes
 */
public record AttendeeDTO(String email) {
}
```

## Learning Objectives
- **Understand** DTOs as the boundary between domain and presentation layers
- **Implement** AttendeeDTO for JSON serialization in REST responses
- **Apply** proper separation between domain models and external representations
- **Connect** domain services to REST endpoints through well-designed data contracts

## What You'll Build
An `AttendeeDTO` record that represents attendee data for JSON serialization, providing a stable external API contract independent of internal domain model changes.

## Why DTOs Are Essential

DTOs solve the critical problem of **how to expose domain data to external systems** without coupling your internal model to external contracts:

**The Domain Exposure Problem**: Without DTOs, domain objects get exposed directly:

❌ Domain aggregate exposed directly as JSON

```java
@Path("/attendees")
public class AttendeeEndpoint {
    @POST
    public Response register(RegisterAttendeeCommand cmd) {
        AttendeeRegistrationResult result = attendeeService.registerAttendee(cmd);

        // Exposing internal domain structure!
        return Response.ok(result.attendee()).build();  // Bad: internal structure exposed
    }
}

// Client receives internal domain structure
{
    "email": "john@example.com",
    "domainEvents": [...],           // Internal implementation detail!
    "aggregateVersion": 1,           // Internal versioning exposed!
    "internalState": {...}           // Internal data leaked!
}
```

**The DTO Solution**: Clean separation between internal and external representations:

✅ Clean DTO for external representation

```java
@Path("/attendees")
public class AttendeeEndpoint {
    @POST
    public Response register(RegisterAttendeeCommand cmd) {
        AttendeeRegistrationResult result = attendeeService.registerAttendee(cmd);

        // Clean external representation
        AttendeeDTO dto = new AttendeeDTO(result.attendee().getEmail());
        return Response.ok(dto).build();
    }
}

// Client receives clean, stable contract
{
    "email": "john@example.com"     // Only relevant external data
}
```

## DTOs vs Other Data Representation Patterns: Deep Dive

Understanding different approaches to data representation helps choose the right pattern for each scenario:

### Data Representation Pattern Comparison

| Pattern | Purpose | Layer | Mutability | Serialization | Use Case |
|---------|---------|-------|------------|---------------|----------|
| **Domain Aggregate** | Business logic & state | Domain | Controlled by business rules | Not intended for external use | Core business operations |
| **Persistence Entity** | Database mapping | Infrastructure | ORM-managed | Database-specific formats | Data storage & retrieval |
| **Data Transfer Object** | External data contracts | Presentation | Immutable | JSON/XML optimized | API responses & requests |
| **View Model** | UI-specific data | Presentation | UI-framework specific | UI binding formats | User interface rendering |
| **Event Payload** | Inter-service communication | Infrastructure | Immutable | Message-specific formats | Async messaging |

### DTO Types and Responsibilities

| DTO Type | Responsibility | Direction | Examples |
|----------|----------------|-----------|----------|
| **Request DTO** | Input validation & parsing | External → Domain | `RegisterAttendeeRequest`, `UpdateAttendeeRequest` |
| **Response DTO** | Output formatting & serialization | Domain → External | `AttendeeDTO`, `AttendeeListDTO` |
| **Command DTO** | Action encapsulation | External → Domain | `RegisterAttendeeCommand` (can serve dual purpose) |
| **Event DTO** | Event serialization | Domain → External | `AttendeeRegisteredEventDTO` |

**Request DTO Example**:
```java
// Input validation and parsing
public record RegisterAttendeeRequest(
    @NotBlank @Email String email,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @Valid AddressRequest address
) {
    // Converts to domain command
    public RegisterAttendeeCommand toCommand() {
        return new RegisterAttendeeCommand(email, firstName, lastName,
                                         address.toDomainObject());
    }
}
```

**Response DTO Example**:
```java
// Output formatting and serialization
public record AttendeeDTO(
    String email,
    String fullName,
    String registrationDate,
    String status
) {
    // Factory method from domain aggregate
    public static AttendeeDTO fromAggregate(Attendee attendee) {
        return new AttendeeDTO(
            attendee.getEmail(),
            attendee.getFullName(),
            attendee.getRegistrationDate().toString(),
            attendee.getStatus().name()
        );
    }
}
```

### DTO Design Patterns

| Pattern | Description | Pros | Cons | When to Use |
|---------|-------------|------|------|-------------|
| **Simple Mapping** | 1:1 field mapping | Easy to understand | Can expose too much | Simple CRUD operations |
| **Aggregated DTO** | Combines multiple domain objects | Reduces API calls | More complex | List views, dashboards |
| **Layered DTOs** | Different DTOs per layer | Clean separation | More classes | Complex applications |
| **Generic DTO** | Dynamic field mapping | Flexible | Type safety lost | Configuration-driven APIs |

**Simple Mapping Pattern**:
```java
public record AttendeeDTO(String email) {
    public static AttendeeDTO fromDomain(Attendee attendee) {
        return new AttendeeDTO(attendee.getEmail());
    }
}
```

**Aggregated DTO Pattern**:
```java
public record ConferenceAttendeeDTO(
    String email,
    String fullName,
    String conferenceName,
    String conferenceDate,
    List<SessionDTO> registeredSessions,
    BadgeDTO badge
) {
    public static ConferenceAttendeeDTO fromDomainObjects(
        Attendee attendee,
        Conference conference,
        List<Session> sessions,
        Badge badge
    ) {
        return new ConferenceAttendeeDTO(
            attendee.getEmail(),
            attendee.getFullName(),
            conference.getName(),
            conference.getDate().toString(),
            sessions.stream().map(SessionDTO::fromDomain).toList(),
            BadgeDTO.fromDomain(badge)
        );
    }
}
```

**Layered DTOs Pattern**:
```java
// API Layer DTO (external contract)
public record AttendeeApiDTO(String email, String name, String status) {}

// Service Layer DTO (internal contract)
public record AttendeeServiceDTO(String email, String firstName, String lastName,
                                AttendeeStatus status, LocalDateTime registeredAt) {}

// Conversion between layers
public class AttendeeDTOMapper {
    public static AttendeeApiDTO toApi(AttendeeServiceDTO service) {
        return new AttendeeApiDTO(
            service.email(),
            service.firstName() + " " + service.lastName(),
            service.status().getDisplayName()
        );
    }
}
```

## Implementation

DTOs are used to transfer data between layers, especially when the data structure differs from the domain model. Our AttendeeDTO provides a clean external representation for JSON serialization.

```java
package dddhexagonalworkshop.conference.attendees.infrastructure;

/**
 * Data Transfer Object for Attendee information.
 *
 * DTOs are not specifically a DDD concept, but they are useful in DDD.  This DTO serves as the external contract for attendee data in REST API responses.
 * It provides a stable, clean representation that can evolve independently of the
 * internal domain model structure.
 *
 * Key characteristics:
 * - Immutable (record) to prevent accidental modification
 * - JSON serialization optimized with proper annotations
 * - Input validation annotations for request scenarios
 * - Clear field documentation for API consumers
 * - Decoupled from internal domain model changes
 */
public record AttendeeDTO(String email) {
}
```

### Key Design Decisions

**Record Type**: Using records provides immutability, automatic equals/hashCode, and clean syntax perfect for DTOs.

**JSON Annotations**: `@JsonProperty` controls JSON field names, allowing clean external contracts independent of Java naming.

**Validation Annotations**: Bean Validation annotations enable automatic input validation in REST endpoints.

**Factory Methods**: Static factory methods provide clean APIs for creating DTOs from various sources.

**Status Mapping**: Explicit mapping between domain and DTO status values maintains stable external contracts.

**Privacy Considerations**: Email masking in toString() prevents accidental exposure in logs.

### JSON Serialization Configuration

Configure Jackson for optimal JSON handling in `application.properties`:

```properties
# JSON serialization configuration
quarkus.jackson.write-dates-as-timestamps=false
quarkus.jackson.write-durations-as-timestamps=false
quarkus.jackson.serialization-inclusion=NON_NULL
quarkus.jackson.deserialization.fail-on-unknown-properties=false
quarkus.jackson.serialization.indent-output=true

# Date format for consistent API responses
quarkus.jackson.date-format=yyyy-MM-dd'T'HH:mm:ss.SSSZ
quarkus.jackson.time-zone=UTC
```

### Testing Your Implementation

**Unit Testing DTO Behavior**:
```java
class AttendeeDTOTest {

    @Test
    void shouldCreateDTOWithEmail() {
        // Test simple constructor
        AttendeeDTO dto = new AttendeeDTO("test@example.com");

        assertThat(dto.email()).isEqualTo("test@example.com");
        assertThat(dto.registrationStatus()).isEqualTo("REGISTERED");
        assertThat(dto.registeredAt()).isNotNull();
    }

    @Test
    void shouldCreateFromDomainAggregate() {
        // Test domain conversion
        Attendee attendee = Attendee.registerAttendee("domain@example.com").attendee();

        AttendeeDTO dto = AttendeeDTO.fromDomain(attendee);

        assertThat(dto.email()).isEqualTo("domain@example.com");
        assertThat(dto.registrationStatus()).isEqualTo("REGISTERED");
    }

    @Test
    void shouldValidateCorrectly() {
        // Test validation logic
        AttendeeDTO validDTO = new AttendeeDTO("valid@example.com");
        AttendeeDTO invalidDTO = new AttendeeDTO("");

        assertThat(validDTO.isValid()).isTrue();
        assertThat(invalidDTO.isValid()).isFalse();
    }

    @Test
    void shouldMaskEmailInToString() {
        // Test privacy protection
        AttendeeDTO dto = new AttendeeDTO("sensitive@example.com");

        String stringRepresentation = dto.toString();

        assertThat(stringRepresentation).contains("se***@example.com");
        assertThat(stringRepresentation).doesNotContain("sensitive");
    }

    @Test
    void shouldSupportStatusTransitions() {
        // Test immutable updates
        AttendeeDTO original = new AttendeeDTO("test@example.com", "PENDING", "2023-01-01T00:00:00Z");

        AttendeeDTO updated = original.withStatus("CONFIRMED");

        assertThat(original.registrationStatus()).isEqualTo("PENDING");
        assertThat(updated.registrationStatus()).isEqualTo("CONFIRMED");
        assertThat(updated.email()).isEqualTo(original.email());
    }
}
```

**JSON Serialization Testing**:
```java
@QuarkusTest
class AttendeeDTOSerializationTest {

    @Inject ObjectMapper objectMapper;

    @Test
    void shouldSerializeToJSON() throws JsonProcessingException {
        // Test JSON output format
        AttendeeDTO dto = new AttendeeDTO("json@example.com", "REGISTERED", "2023-01-01T00:00:00Z");

        String json = objectMapper.writeValueAsString(dto);

        JsonNode jsonNode = objectMapper.readTree(json);
        assertThat(jsonNode.get("email").asText()).isEqualTo("json@example.com");
        assertThat(jsonNode.get("registration_status").asText()).isEqualTo("REGISTERED");
        assertThat(jsonNode.get("registered_at").asText()).isEqualTo("2023-01-01T00:00:00Z");
    }

    @Test
    void shouldDeserializeFromJSON() throws JsonProcessingException {
        // Test JSON input parsing
        String json = """
            {
                "email": "deserialize@example.com",
                "registration_status": "PENDING",
                "registered_at": "2023-01-01T00:00:00Z"
            }
            """;

        AttendeeDTO dto = objectMapper.readValue(json, AttendeeDTO.class);

        assertThat(dto.email()).isEqualTo("deserialize@example.com");
        assertThat(dto.registrationStatus()).isEqualTo("PENDING");
        assertThat(dto.registeredAt()).isEqualTo("2023-01-01T00:00:00Z");
    }

    @Test
    void shouldHandleNullFields() throws JsonProcessingException {
        // Test graceful handling of missing fields
        String json = """
            {
                "email": "partial@example.com"
            }
            """;

        AttendeeDTO dto = objectMapper.readValue(json, AttendeeDTO.class);

        assertThat(dto.email()).isEqualTo("partial@example.com");
        // Other fields should have sensible defaults or null handling
    }
}
```

**Bean Validation Testing**:
```java
@Test
void shouldValidateWithBeanValidation() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    // Test valid DTO
    AttendeeDTO validDTO = new AttendeeDTO("valid@example.com");
    Set<ConstraintViolation<AttendeeDTO>> violations = validator.validate(validDTO);
    assertThat(violations).isEmpty();

    // Test invalid email
    AttendeeDTO invalidDTO = new AttendeeDTO("invalid-email");
    violations = validator.validate(invalidDTO);
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage()).contains("Email must be valid");
}
```

## Connection to Other Components

This DTO will be:
1. **Created** by the `AttendeeService` when returning results
2. **Serialized** to JSON by Jackson in REST responses
3. **Used** by the `AttendeeEndpoint` as response body
4. **Consumed** by external clients as the API contract
5. **Validated** using Bean Validation annotations in request scenarios

## Advanced DTO Patterns

**Nested DTOs** for complex data structures:
```java
public record ConferenceAttendeeDTO(
    String email,
    String fullName,
    AddressDTO address,
    List<SessionDTO> sessions,
    BadgeInfoDTO badge
) {
    public static ConferenceAttendeeDTO fromDomainAggregate(
        Attendee attendee,
        List<Session> sessions,
        Badge badge
    ) {
        return new ConferenceAttendeeDTO(
            attendee.getEmail(),
            attendee.getFullName(),
            AddressDTO.fromDomain(attendee.getAddress()),
            sessions.stream().map(SessionDTO::fromDomain).toList(),
            BadgeInfoDTO.fromDomain(badge)
        );
    }
}

public record AddressDTO(String street, String city, String zipCode) {
    public static AddressDTO fromDomain(Address address) {
        return new AddressDTO(address.getStreet(), address.getCity(), address.getZipCode());
    }
}
```

**Versioned DTOs** for API evolution:
```java
// Version 1
public record AttendeeV1DTO(String email) {}

// Version 2 - backward compatible
public record AttendeeV2DTO(
    String email,
    @JsonProperty(defaultValue = "UNKNOWN") String status,
    @JsonProperty(defaultValue = "") String registeredAt
) {
    // Conversion from V1
    public static AttendeeV2DTO fromV1(AttendeeV1DTO v1) {
        return new AttendeeV2DTO(v1.email(), "REGISTERED", Instant.now().toString());
    }
}
```

**Generic DTO Builder** for dynamic scenarios:
```java
public class DynamicAttendeeDTO {
    private final Map<String, Object> data = new HashMap<>();

    public DynamicAttendeeDTO email(String email) {
        data.put("email", email);
        return this;
    }

    public DynamicAttendeeDTO status(String status) {
        data.put("registration_status", status);
        return this;
    }

    public DynamicAttendeeDTO customField(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public Map<String, Object> build() {
        return Collections.unmodifiableMap(data);
    }
}
```

**DTO Projection** for performance optimization:
```java
// Lightweight DTO for list views
public record AttendeeListItemDTO(String email, String status) {
    public static AttendeeListItemDTO fromDomain(Attendee attendee) {
        return new AttendeeListItemDTO(
            attendee.getEmail(),
            attendee.getStatus().name()
        );
    }
}

// Full DTO for detail views
public record AttendeeDetailDTO(
    String email,
    String fullName,
    String status,
    String registeredAt,
    AddressDTO address,
    List<String> dietaryRestrictions
) {
    public static AttendeeDetailDTO fromDomain(Attendee attendee) {
        return new AttendeeDetailDTO(
            attendee.getEmail(),
            attendee.getFullName(),
            attendee.getStatus().name(),
            attendee.getRegistrationDate().toString(),
            AddressDTO.fromDomain(attendee.getAddress()),
            attendee.getDietaryRestrictions().stream()
                .map(DietaryRestriction::getName)
                .toList()
        );
    }
}
```

### Real-World Considerations

**API Versioning Strategy**:
```java
// URL versioning
@Path("/v1/attendees")
public class AttendeeV1Endpoint {
    @GET
    public List<AttendeeV1DTO> list() { ... }
}

@Path("/v2/attendees")
public class AttendeeV2Endpoint {
    @GET
    public List<AttendeeV2DTO> list() { ... }
}

// Header versioning
@Path("/attendees")
public class AttendeeEndpoint {
    @GET
    public Response list(@HeaderParam("Accept-Version") String version) {
        return switch (version) {
            case "v1" -> Response.ok(convertToV1DTOs()).build();
            case "v2" -> Response.ok(convertToV2DTOs()).build();
            default -> Response.ok(convertToLatestDTOs()).build();
        };
    }
}
```

**Performance Optimization**:
```java
// Lazy loading for expensive fields
public record AttendeeDTO(
    String email,
    String status,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Supplier<List<SessionDTO>> sessions  // Only load when accessed
) {
    @JsonIgnore
    public List<SessionDTO> getSessions() {
        return sessions != null ? sessions.get() : Collections.emptyList();
    }
}

// Field selection for mobile APIs
public record AttendeeDTO(
    String email,
    String status,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String fullName,  // Optional for list views
    @JsonInclude(JsonInclude.Include.NON_NULL)
    AddressDTO address  // Optional for mobile
) {}
```

**Security Considerations**:
```java
public record SecureAttendeeDTO(
    String email,
    String status,
    @JsonIgnore  // Never serialize sensitive data
    String internalNotes,
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)  // Read-only field
    String createdBy
) {
    // Custom serializer for role-based field filtering
    @JsonIgnore
    public AttendeeDTO forRole(UserRole role) {
        return switch (role) {
            case ADMIN -> this;  // Full data
            case USER -> new AttendeeDTO(email, status, null, null);  // Limited data
            case GUEST -> new AttendeeDTO(email, null, null, null);  // Minimal data
        };
    }
}
```

## Common Questions

**Q: Should DTOs contain business logic?**
A: No, DTOs should be pure data containers. Business logic belongs in domain aggregates and services.

**Q: How do I handle DTO evolution and backward compatibility?**
A: Use optional fields, default values, and versioning strategies. Consider separate DTO versions for major changes.

**Q: Should I have separate DTOs for requests and responses?**
A: It depends on complexity. Simple cases can share DTOs, but complex scenarios benefit from separate request/response DTOs.

**Q: How do I handle nested object relationships in DTOs?**
A: Use nested DTOs for composition, reference IDs for associations, or provide multiple representation options.

**Q: Should DTOs be mutable or immutable?**
A: Prefer immutable DTOs (records) for thread safety and clarity. Use mutable DTOs only when framework requirements demand it.

## Next Steps

In the final step, we'll create the `AttendeeEndpoint` REST controller that serves as the inbound adapter for our hexagonal architecture. The endpoint will receive HTTP requests, convert them to commands, delegate to our domain service, transform results to DTOs, and return JSON responses, completing our end-to-end DDD implementation.

