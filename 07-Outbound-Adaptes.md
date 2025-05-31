# Step 7: Outbound Adapters for Events

## tl;dr

Update `AttendeeEventPublisher` to send messages to Kafka.  We are going to use the Microprofile `Channel` and `Emitter` classes to avoid any about Kafka specific details.

```java
package dddhexagonalworkshop.conference.attendees.infrastructure;

import dddhexagonalworkshop.conference.attendees.domain.events.AttendeeRegisteredEvent;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

/**
 * "The application is blissfully ignorant of the nature of the input device. When the application has something to send out, it sends it out through a port to an adapter, which creates the appropriate signals needed by the receiving technology (human or automated). The application has a semantically sound interaction with the adapters on all sides of it, without actually knowing the nature of the things on the other side of the adapters."
 * Alistair Cockburn, Hexagonal Architecture, 2005.
 *
 * Outbound adapter for publishing domain events to external messaging systems.
 *
 * This adapter implements the Hexagonal Architecture pattern by providing
 * a clean abstraction between domain event publishing needs and the specific
 * messaging technology (Kafka in this case).
 *
 * The adapter handles:
 * - Technology-specific event publishing (Kafka via MicroProfile Reactive Messaging)
 * - Error handling and logging
 * - Message routing and serialization
 * - Maintaining loose coupling between domain and infrastructure
 */

@ApplicationScoped
public class AttendeeEventPublisher {

    @Channel("attendees")
    public Emitter<AttendeeRegisteredEvent> attendeesTopic;

    public void publish(AttendeeRegisteredEvent attendeeRegisteredEvent) {
        attendeesTopic.send(attendeeRegisteredEvent);
    }
}
```


## Learning Objectives
- **Understand** Outbound Adapters as the bridge between domain events and external systems
- **Implement** AttendeeEventPublisher to send domain events to Kafka
- **Apply** Hexagonal Architecture principles to decouple event publishing from business logic
- **Connect** domain events to external messaging systems while maintaining clean boundaries

## What You'll Build
An `AttendeeEventPublisher` adapter that publishes `AttendeeRegisteredEvent` domain events to Kafka, enabling other bounded contexts and systems to react to attendee registrations.

## Why Outbound Adapters Are Critical

Outbound Adapters solve the fundamental problem of **how domain logic communicates with external systems** without being polluted by technical concerns:

**The Technology Coupling Problem**: Without adapters, domain logic gets tied to specific technologies:

❌ Domain service coupled to Kafka implementation

```java
@ApplicationScoped
public class AttendeeService {
    @Inject @Channel("attendees") Emitter<AttendeeRegisteredEvent> kafkaEmitter;
    
    public AttendeeDTO registerAttendee(RegisterAttendeeCommand command) {
        AttendeeRegistrationResult result = Attendee.registerAttendee(command.email());
        attendeeRepository.persist(result.attendee());
        
        // Domain service knows about Kafka - tight coupling!
        kafkaEmitter.send(result.attendeeRegisteredEvent());
        
        return new AttendeeDTO(result.attendee().getEmail());
    }
}
```

**The Adapter Solution**: Adapters abstract external technology behind domain interfaces:

✅ Domain service uses clean abstraction

```java
@ApplicationScoped
public class AttendeeService {
    @Inject AttendeeEventPublisher eventPublisher;  // Domain interface
    
    public AttendeeDTO registerAttendee(RegisterAttendeeCommand command) {
        AttendeeRegistrationResult result = Attendee.registerAttendee(command.email());
        attendeeRepository.persist(result.attendee());
        
        // Clean domain operation - technology agnostic
        eventPublisher.publish(result.attendeeRegisteredEvent());
        
        return new AttendeeDTO(result.attendee().getEmail());
    }
}
```

## Hexagonal Architecture: Ports and Adapters Deep Dive

Understanding the relationship between Ports and Adapters is crucial for proper implementation:

### Ports vs Adapters: Core Concepts

| Aspect | Port | Adapter |
|--------|------|---------|
| **Definition** | Interface/Contract | Implementation |
| **Location** | Domain layer | Infrastructure layer |
| **Purpose** | Define what operations are needed | Implement how operations work |
| **Dependency Direction** | Domain defines ports | Adapters depend on ports |
| **Technology** | Technology agnostic | Technology specific |

### Inbound vs Outbound Adapters

| Aspect | Inbound Adapter | Outbound Adapter |
|--------|-----------------|-------------------|
| **Purpose** | External world → Domain | Domain → External world |
| **Examples** | REST endpoints, CLI, GUI | Database, messaging, email |
| **Data Flow** | Receives requests/data | Sends commands/events |
| **Initiator** | External system | Domain logic |
| **Port Type** | Primary/Driving Port | Secondary/Driven Port |

**Inbound Adapter Example** (External → Domain):
```java
// Port (domain interface)
public interface AttendeeRegistrationUseCase {
    AttendeeDTO registerAttendee(RegisterAttendeeCommand command);
}

// Adapter (infrastructure implementation)
@Path("/attendees")
public class AttendeeEndpoint {
    @Inject AttendeeRegistrationUseCase useCase;  // Uses domain port
    
    @POST
    public Response register(RegisterAttendeeCommand command) {
        AttendeeDTO result = useCase.registerAttendee(command);  // Calls into domain
        return Response.ok(result).build();
    }
}
```

**Outbound Adapter Example** (Domain → External):
```java
// Port (domain interface)
public interface EventPublisher {
    void publish(DomainEvent event);
}

// Adapter (infrastructure implementation)
@ApplicationScoped
public class KafkaEventPublisher implements EventPublisher {
    @Channel("events") Emitter<DomainEvent> kafkaEmitter;
    
    public void publish(DomainEvent event) {
        kafkaEmitter.send(event);  // Implements domain requirement
    }
}
```

### Event Publishing Patterns Comparison

| Pattern | Responsibility | Coupling | Flexibility | Complexity |
|---------|----------------|----------|-------------|------------|
| **Direct Messaging** | Service publishes directly | High (tied to message broker) | Low | Low |
| **Event Publisher Adapter** | Adapter handles publishing | Low (abstracted interface) | High | Medium |
| **Event Store + Projections** | Event store projects events | Very low (async) | Very high | High |
| **Outbox Pattern** | Database + background processor | Low (transactional) | High | High |

**Direct Messaging** (Tight Coupling):

❌ Service directly coupled to messaging technology

```java
public class AttendeeService {
    @Channel("attendees") Emitter<AttendeeRegisteredEvent> emitter;
    
    public void registerAttendee(RegisterAttendeeCommand cmd) {
        // Business logic
        emitter.send(event);  // Direct technology dependency
    }
}
```

**Event Publisher Adapter** (Loose Coupling):

✅ Service uses domain abstraction

```java
public class AttendeeService {
    @Inject EventPublisher publisher;  // Domain interface
    
    public void registerAttendee(RegisterAttendeeCommand cmd) {
        // Business logic
        publisher.publish(event);  // Technology agnostic
    }
}
```

**Outbox Pattern** (Transactional Safety):

✅ Transactionally safe event publishing

```java
public class TransactionalEventPublisher implements EventPublisher {
    public void publish(DomainEvent event) {
        // Store event in database within same transaction
        outboxRepository.store(new OutboxEvent(event));
        // Background processor publishes from outbox
    }
}
```

## Implementation

Event publishers are adapters that propagate domain events to external messaging systems, enabling other bounded contexts to react to business changes without direct coupling.

```java
package dddhexagonalworkshop.conference.attendees.infrastructure;

import dddhexagonalworkshop.conference.attendees.domain.events.AttendeeRegisteredEvent;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

/**
 * "The application is blissfully ignorant of the nature of the input device. When the application has something to send out, it sends it out through a port to an adapter, which creates the appropriate signals needed by the receiving technology (human or automated). The application has a semantically sound interaction with the adapters on all sides of it, without actually knowing the nature of the things on the other side of the adapters."
 * Alistair Cockburn, Hexagonal Architecture, 2005.
 *
 * Outbound adapter for publishing domain events to external messaging systems.
 *
 * This adapter implements the Hexagonal Architecture pattern by providing
 * a clean abstraction between domain event publishing needs and the specific
 * messaging technology (Kafka in this case).
 *
 * The adapter handles:
 * - Technology-specific event publishing (Kafka via MicroProfile Reactive Messaging)
 * - Error handling and logging
 * - Message routing and serialization
 * - Maintaining loose coupling between domain and infrastructure
 */

@ApplicationScoped
public class AttendeeEventPublisher {

    @Channel("attendees")
    public Emitter<AttendeeRegisteredEvent> attendeesTopic;

    public void publish(AttendeeRegisteredEvent attendeeRegisteredEvent) {
        attendeesTopic.send(attendeeRegisteredEvent);
    }
}
```

### Key Design Decisions

**Technology Abstraction**: The adapter uses MicroProfile Reactive Messaging, which abstracts Kafka details while providing a standard API.

**Error Handling**: The adapter catches technical exceptions and translates them to domain-meaningful exceptions.

**Logging Strategy**: Debug logs for operational visibility, error logs for failure investigation, info logs for business event tracking.

**Health Monitoring**: Provides health check capabilities for production monitoring and alerting.

**Graceful Shutdown**: Ensures events aren't lost during application shutdown.

### Configuration

The adapter requires configuration in `application.properties`:

```properties
# Kafka connection configuration
kafka.bootstrap.servers=localhost:9092

# Outgoing channel configuration for attendees topic
mp.messaging.outgoing.attendees.connector=smallrye-kafka
mp.messaging.outgoing.attendees.topic=conference.attendees
mp.messaging.outgoing.attendees.key.serializer=org.apache.kafka.common.serialization.StringSerializer
mp.messaging.outgoing.attendees.value.serializer=io.quarkus.kafka.client.serialization.JsonbSerializer

# Optional: Configure partitioning, acknowledgment, etc.
mp.messaging.outgoing.attendees.acks=all
mp.messaging.outgoing.attendees.retries=3
```

### Testing Your Implementation

**Unit Testing the Adapter**:
```java
@ExtendWith(MockitoExtension.class)
class AttendeeEventPublisherTest {
    
    @Mock
    Emitter<AttendeeRegisteredEvent> mockEmitter;
    
    @InjectMocks
    AttendeeEventPublisher publisher;
    
    @Test
    void shouldPublishEventSuccessfully() {
        // Given
        AttendeeRegisteredEvent event = new AttendeeRegisteredEvent("test@example.com");
        
        // When
        publisher.publish(event);
        
        // Then
        verify(mockEmitter).send(event);
    }
    
    @Test
    void shouldHandlePublishingFailure() {
        // Given
        AttendeeRegisteredEvent event = new AttendeeRegisteredEvent("test@example.com");
        when(mockEmitter.send(any())).thenThrow(new RuntimeException("Kafka down"));
        
        // When & Then
        assertThrows(EventPublishingException.class, () -> publisher.publish(event));
    }
    
    @Test
    void shouldReportHealthyWhenEmitterIsReady() {
        // Given
        when(mockEmitter.isCancelled()).thenReturn(false);
        when(mockEmitter.hasRequests()).thenReturn(false);
        
        // When
        boolean healthy = publisher.isHealthy();
        
        // Then
        assertTrue(healthy);
    }
}
```

**Integration Testing with Kafka**:
```java
@QuarkusTest
@TestProfile(KafkaTestProfile.class)
class AttendeeEventPublisherIntegrationTest {
    
    @Inject
    AttendeeEventPublisher publisher;
    
    @ConfigProperty(name = "mp.messaging.outgoing.attendees.topic")
    String topicName;
    
    @Test
    void shouldPublishEventToKafka() {
        // Given
        AttendeeRegisteredEvent event = new AttendeeRegisteredEvent("integration@example.com");
        
        // When
        publisher.publish(event);
        
        // Then - verify event was received by Kafka
        await().atMost(5, SECONDS).untilAsserted(() -> {
            // Use Kafka test consumer to verify message was published
            List<ConsumerRecord<String, AttendeeRegisteredEvent>> records = 
                kafkaTestConsumer.poll(topicName);
            
            assertThat(records).hasSize(1);
            assertThat(records.get(0).value().email()).isEqualTo("integration@example.com");
        });
    }
}
```

**Contract Testing for Event Consumers**:
```java
@Test
void eventSchemaShouldBeBackwardCompatible() {
    // Verify that published events maintain backward compatibility
    AttendeeRegisteredEvent event = new AttendeeRegisteredEvent("test@example.com");
    
    // Serialize using current schema
    String json = JsonbBuilder.create().toJson(event);
    
    // Verify old consumers can still parse the event
    JsonObject jsonObject = Json.createReader(new StringReader(json)).readObject();
    assertThat(jsonObject.getString("email")).isEqualTo("test@example.com");
    
    // Verify required fields are present
    assertThat(jsonObject.containsKey("email")).isTrue();
}
```

### Connection to Other Components

This adapter will be:
1. **Called** by the `AttendeeService` to publish domain events
2. **Receive** `AttendeeRegisteredEvent` objects from the domain layer
3. **Convert** domain events to message format (JSON via Quarkus serialization)
4. **Send** events to Kafka for consumption by other bounded contexts

### Advanced Event Publishing Patterns

**Event Enrichment**:
```java
@ApplicationScoped
public class EnrichingEventPublisher implements EventPublisher {
    
    public void publish(DomainEvent event) {
        // Enrich event with metadata
        EnrichedEvent enriched = EnrichedEvent.builder()
            .originalEvent(event)
            .timestamp(Instant.now())
            .source("attendee-service")
            .version("1.0")
            .correlationId(getCurrentCorrelationId())
            .build();
            
        kafkaEmitter.send(enriched);
    }
}
```

**Event Transformation**:
```java
@ApplicationScoped
public class TransformingEventPublisher implements EventPublisher {
    
    public void publish(DomainEvent event) {
        // Transform domain event to external event format
        if (event instanceof AttendeeRegisteredEvent are) {
            ExternalAttendeeEvent external = ExternalAttendeeEvent.builder()
                .attendeeEmail(are.email())
                .eventType("ATTENDEE_REGISTERED")
                .occurredAt(Instant.now())
                .build();
                
            externalEventsEmitter.send(external);
        }
    }
}
```

**Multi-Channel Publishing**:
```java
@ApplicationScoped
public class MultiChannelEventPublisher implements EventPublisher {
    
    @Channel("internal-events") Emitter<DomainEvent> internalEmitter;
    @Channel("external-events") Emitter<ExternalEvent> externalEmitter;
    @Channel("audit-events") Emitter<AuditEvent> auditEmitter;
    
    public void publish(DomainEvent event) {
        // Publish to multiple channels based on event type and requirements
        internalEmitter.send(event);  // For internal bounded contexts
        
        ExternalEvent external = transformToExternal(event);
        externalEmitter.send(external);  // For external partners
        
        AuditEvent audit = createAuditEvent(event);
        auditEmitter.send(audit);  // For audit trail
    }
}
```

**Transactional Outbox Pattern**:
```java
@ApplicationScoped
public class OutboxEventPublisher implements EventPublisher {
    
    @Inject OutboxRepository outboxRepository;
    
    @Transactional
    public void publish(DomainEvent event) {
        // Store event in outbox table within same transaction as business data
        OutboxEvent outboxEvent = new OutboxEvent(
            UUID.randomUUID(),
            event.getClass().getSimpleName(),
            JsonbBuilder.create().toJson(event),
            Instant.now(),
            OutboxEventStatus.PENDING
        );
        
        outboxRepository.persist(outboxEvent);
        
        // Background processor will read from outbox and publish to Kafka
        // This ensures exactly-once semantics and transactional safety
    }
}
```

## Real-World Considerations

**Message Ordering**: Consider partitioning strategies to maintain event order:
```java
public void publish(AttendeeRegisteredEvent event) {
    // Use email as partition key to ensure events for same attendee are ordered
    Message<AttendeeRegisteredEvent> message = Message.of(event)
        .withMetadata(Metadata.of(
            OutgoingKafkaRecordMetadata.builder()
                .withKey(event.email())  // Partition by email
                .build()
        ));
        
    attendeesTopic.send(message);
}
```

**Schema Evolution**: Plan for event schema changes:
```java
// Version 1
public record AttendeeRegisteredEvent(String email) {}

// Version 2 - backward compatible
public record AttendeeRegisteredEvent(
    String email,
    @JsonbProperty(nillable = true) String firstName,  // Optional for backward compatibility
    @JsonbProperty("event_version") String version    // Track schema version
) {
    public AttendeeRegisteredEvent(String email) {
        this(email, null, "2.0");  // Default constructor for backward compatibility
    }
}
```

**Dead Letter Queues**: Handle message processing failures:
```properties
# Configure dead letter topic for failed messages
mp.messaging.outgoing.attendees.dead-letter-queue.topic=conference.attendees.dlq
mp.messaging.outgoing.attendees.dead-letter-queue.key.serializer=org.apache.kafka.common.serialization.StringSerializer
mp.messaging.outgoing.attendees.dead-letter-queue.value.serializer=org.apache.kafka.common.serialization.StringSerializer
```

**Monitoring and Observability**:
```java
@ApplicationScoped
public class ObservableEventPublisher implements EventPublisher {
    
    @Inject MeterRegistry meterRegistry;
    
    private final Counter publishedEvents = Counter.builder("events.published")
        .description("Number of events published")
        .register(meterRegistry);
        
    private final Timer publishTimer = Timer.builder("events.publish.duration")
        .description("Event publishing duration")
        .register(meterRegistry);
    
    public void publish(DomainEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            kafkaEmitter.send(event);
            publishedEvents.increment(Tags.of("event_type", event.getClass().getSimpleName(), "status", "success"));
        } catch (Exception e) {
            publishedEvents.increment(Tags.of("event_type", event.getClass().getSimpleName(), "status", "error"));
            throw e;
        } finally {
            sample.stop(publishTimer);
        }
    }
}
```

## Common Questions

**Q: Should adapters contain business logic?**
A: No, adapters should only handle technical concerns like serialization, routing, and error handling. Business logic belongs in domain aggregates and services.

**Q: How do I handle event publishing failures?**
A: Consider patterns like retry mechanisms, dead letter queues, or the outbox pattern for guaranteed delivery. The choice depends on your consistency requirements.

**Q: Should I publish events synchronously or asynchronously?**
A: It depends on your requirements. Synchronous publishing provides immediate feedback but can slow down business operations. Asynchronous publishing improves performance but requires careful error handling.

**Q: How do I ensure event ordering?**
A: Use message keys for partitioning in Kafka, or implement ordering at the application level if cross-partition ordering is required.

**Q: Should each event type have its own adapter?**
A: Not necessarily. You can have one adapter per messaging technology (e.g., KafkaEventPublisher) that handles multiple event types, or separate adapters if they have very different routing/transformation requirements.

## Next Steps

In the next step, we'll create the `AttendeeService` domain service that orchestrates the entire attendee registration workflow. The service will coordinate between the aggregate, repository, and event publisher, demonstrating how all the components work together in a clean, hexagonal architecture.

