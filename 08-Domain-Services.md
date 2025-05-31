# Step 8: Application Services

## tl;dr

There are three types of Services in DDD: Application Services, Domain Services, and Infrastructure Services.
Domain Services implement functionality that doesn't have a natural home in any single Aggregate. They coordinate workflows across multiple domain objects and handle cross-Aggregate business rules.

```java
package dddhexagonalworkshop.conference.attendees.domain.services;

import dddhexagonalworkshop.conference.attendees.domain.aggregates.Attendee;
import dddhexagonalworkshop.conference.attendees.infrastructure.AttendeeDTO;
import dddhexagonalworkshop.conference.attendees.infrastructure.AttendeeEventPublisher;
import dddhexagonalworkshop.conference.attendees.persistence.AttendeeRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * "The application and domain layers call on the SERVICES provided by the infrastructure layer. When the scope of a SERVICE has been well chosen and its interface well designed, the caller can remain loosely coupled and uncomplicated by the elaborate behavior the SERVICE interface encapsulates."
 * Eric Evans, Domain-Driven Design: Tackling Complexity in the Heart of Software, 2003.
 */

@ApplicationScoped
public class AttendeeService {


    @Inject
    AttendeeRepository attendeeRepository;

    @Inject
    AttendeeEventPublisher attendeeEventPublisher;

    public AttendeeDTO registerAttendee(RegisterAttendeeCommand registerAttendeeAttendeeCommand) {
        // Logic to register an attendee
        AttendeeRegistrationResult result = Attendee.registerAttendee(registerAttendeeAttendeeCommand.email());


        //persist the attendee
        QuarkusTransaction.requiringNew().run(() -> {
            attendeeRepository.persist(result.attendee());
        });

        //notify the system that a new attendee has been registered
        attendeeEventPublisher.publish(result.attendeeRegisteredEvent());

        return new AttendeeDTO(result.attendee().getEmail());
    }
}
```

## Learning Objectives
- **Understand** Domain Services as workflow orchestrators in the domain layer
- **Implement** AttendeeService to coordinate registration business operations
- **Apply** proper separation between domain services and application services
- **Connect** all DDD components through clean service orchestration

## What We're Building
An `AttendeeService` that orchestrates the complete attendee registration workflow, coordinating between aggregates, repositories, and event publishers while maintaining clean domain boundaries.

## Why Domain Services Matter

Domain Services solve the critical problem of **where to put business logic that doesn't naturally belong in any single aggregate** and **how to coordinate complex workflows**:

**The Scattered Coordination Problem**: Without domain services, workflow logic gets scattered:

❌ Workflow logic scattered across layers

```java
@Path("/attendees")
public class AttendeeEndpoint {
    public Response register(RegisterAttendeeCommand cmd) {
        // Validation logic in REST layer
        if (cmd.email() == null) throw new BadRequestException();
        
        // Business logic in REST layer
        AttendeeRegistrationResult result = Attendee.registerAttendee(cmd.email());
        
        // Transaction management in REST layer
        QuarkusTransaction.requiringNew().run(() -> {
            attendeeRepository.persist(result.attendee());
        });
        
        // Event publishing in REST layer
        eventPublisher.publish(result.attendeeRegisteredEvent());
        
        return Response.ok(new AttendeeDTO(result.attendee().getEmail())).build();
    }
}
```

**The Domain Service Solution**: Centralized workflow orchestration:

✅ Clean workflow orchestration in domain layer

```java
@ApplicationScoped
public class AttendeeService {
    public AttendeeDTO registerAttendee(RegisterAttendeeCommand command) {
        // All business workflow logic centralized
        // Proper transaction boundaries
        // Clean separation of concerns
    }
}

@Path("/attendees")
public class AttendeeEndpoint {
    public Response register(RegisterAttendeeCommand cmd) {
        // REST layer only handles HTTP concerns
        AttendeeDTO result = attendeeService.registerAttendee(cmd);
        return Response.ok(result).build();
    }
}
```
### Implementation

Domain Services implement functionality that doesn't have a natural home in any single aggregate. They coordinate workflows across multiple domain objects and handle cross-aggregate business rules.

```java
package dddhexagonalworkshop.conference.attendees.domain.services;

import dddhexagonalworkshop.conference.attendees.domain.aggregates.Attendee;
import dddhexagonalworkshop.conference.attendees.infrastructure.AttendeeDTO;
import dddhexagonalworkshop.conference.attendees.infrastructure.AttendeeEventPublisher;
import dddhexagonalworkshop.conference.attendees.persistence.AttendeeRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * "The application and domain layers call on the SERVICES provided by the infrastructure layer. When the scope of a SERVICE has been well chosen and its interface well designed, the caller can remain loosely coupled and uncomplicated by the elaborate behavior the SERVICE interface encapsulates."
 * Eric Evans, Domain-Driven Design: Tackling Complexity in the Heart of Software, 2003.
 */

@ApplicationScoped
public class AttendeeService {


    @Inject
    AttendeeRepository attendeeRepository;

    @Inject
    AttendeeEventPublisher attendeeEventPublisher;

    public AttendeeDTO registerAttendee(RegisterAttendeeCommand registerAttendeeAttendeeCommand) {
        // Logic to register an attendee
        AttendeeRegistrationResult result = Attendee.registerAttendee(registerAttendeeAttendeeCommand.email());


        //persist the attendee
        QuarkusTransaction.requiringNew().run(() -> {
            attendeeRepository.persist(result.attendee());
        });

        //notify the system that a new attendee has been registered
        attendeeEventPublisher.publish(result.attendeeRegisteredEvent());

        return new AttendeeDTO(result.attendee().getEmail());
    }
}
```

## Key Design Decisions

**Single Responsibility**: Each method has a clear, single purpose - registration, lookup, or cancellation.

**Transaction Boundaries**: `@Transactional` ensures data consistency across repository operations and event publishing.

**Error Handling**: Domain-specific exceptions provide meaningful error messages and maintain abstraction boundaries.

**Logging Strategy**: Structured logging for operational visibility and debugging.

**Separation of Concerns**: Private methods separate validation, persistence, and event publishing concerns.

## Deeper Dive

### Domain Services vs Other Service Types

Understanding the different types of services and their responsibilities is crucial for proper DDD implementation:

### Service Type Comparison

| Aspect                | Domain Service                  | Application Service     | Infrastructure Service |
|-----------------------|---------------------------------|-------------------------|----------------------|
| **Layer**             | Domain                          | Application             | Infrastructure |
| **Purpose**           | Business workflow orchestration | Use case coordination   | Technical operations |
| **Dependencies**      | Domain objects only             | Domain + Infrastructure | External systems |
| **Business Logic**    | Contains business rules         | Minimal business logic  | No business logic |
| **Transaction Scope** | Often transactional             | Manages transactions    | Participates in transactions |
| **Testing**           | Domain-focused unit tests       | Integration tests       | Technical integration tests |

**Domain Service Examples**:

✅ Domain Service - business workflow

```java
public class AttendeeService {
    public AttendeeDTO registerAttendee(RegisterAttendeeCommand cmd) {
        // Business workflow orchestration
        // Cross-aggregate business rules
        // Domain transaction boundaries
    }
    
    public void transferAttendeeToNewConference(String email, ConferenceId newConference) {
        // Complex business workflow involving multiple aggregates
        // Business rules that span aggregates
    }
}
```

**Application Service Examples**:
```java
// Application Service - use case coordination
public class AttendeeApplicationService {
    public void handleAttendeeRegistration(RegisterAttendeeCommand cmd) {
        // Use case orchestration
        attendeeService.registerAttendee(cmd);  // Delegate to domain
        emailService.sendWelcomeEmail(cmd.email());  // Infrastructure coordination
        analyticsService.trackRegistration(cmd);  // Cross-cutting concerns
    }
}
```

**Infrastructure Service Examples**:
```java
// Infrastructure Service - technical operations
public class EmailService {
    public void sendWelcomeEmail(String email) {
        // Pure technical operation
        // No business logic
        // External system integration
    }
}
```

### Domain Service vs Aggregate: Responsibility Boundaries

| Aspect | Aggregate | Domain Service |
|--------|-----------|----------------|
| **Scope** | Single aggregate boundary | Cross-aggregate operations |
| **State** | Maintains aggregate state | Stateless coordination |
| **Invariants** | Enforces internal invariants | Orchestrates aggregate interactions |
| **Lifecycle** | Created, modified, persisted | Executed then discarded |
| **Business Rules** | Rules within aggregate | Rules spanning aggregates |

**What Belongs in Aggregates**:

✅ Single aggregate business rules

```java
public class Attendee {
    public static AttendeeRegistrationResult registerAttendee(String email) {
        validateEmail(email);  // Attendee-specific validation
        checkAttendeeEligibility(email);  // Attendee business rules
        // Create attendee and event
    }
    
    public void updateContactInformation(ContactInfo info) {
        validateContactInfo(info);  // Attendee-specific validation
        this.contactInfo = info;
        // Raise ContactUpdatedEvent
    }
}
```

**What Belongs in Domain Services**:

✅ Cross-aggregate business workflows

```java
public class AttendeeService {
    public AttendeeDTO registerAttendee(RegisterAttendeeCommand cmd) {
        // Check conference capacity (Conference aggregate concern)
        if (!conferenceService.hasAvailableSpots(cmd.conferenceId())) {
            throw new ConferenceFullException();
        }
        
        // Check for duplicate registration (cross-aggregate rule)
        if (attendeeRepository.findByEmail(cmd.email()).isPresent()) {
            throw new DuplicateRegistrationException();
        }
        
        // Create attendee (Attendee aggregate responsibility)
        AttendeeRegistrationResult result = Attendee.registerAttendee(cmd.email());
        
        // Coordinate persistence and events
        persistAttendeeAndPublishEvent(result);
        
        return new AttendeeDTO(result.attendee().getEmail());
    }
}
```

### Transaction Management Patterns

| Pattern | Responsibility | Pros | Cons | Use Case |
|---------|----------------|------|------|----------|
| **Service-Managed** | Service controls transactions | Clear boundaries | Coupling to transaction tech | Simple workflows |
| **Declarative** | Framework manages transactions | Clean code | Less control | Standard CRUD operations |
| **Manual** | Explicit transaction control | Full control | More complex code | Complex workflows |
| **Saga Pattern** | Distributed transaction coordination | Handles failures across services | Complex implementation | Cross-service workflows |

**Service-Managed Transactions**:
```java
public class AttendeeService {
    @Transactional
    public AttendeeDTO registerAttendee(RegisterAttendeeCommand cmd) {
        // Everything in one transaction
        AttendeeRegistrationResult result = Attendee.registerAttendee(cmd.email());
        attendeeRepository.persist(result.attendee());
        eventPublisher.publish(result.attendeeRegisteredEvent());
        return new AttendeeDTO(result.attendee().getEmail());
    }
}
```

**Manual Transaction Management**:
```java
public class AttendeeService {
    public AttendeeDTO registerAttendee(RegisterAttendeeCommand cmd) {
        return transactionManager.executeInTransaction(() -> {
            AttendeeRegistrationResult result = Attendee.registerAttendee(cmd.email());
            attendeeRepository.persist(result.attendee());
            
            // Publish events outside transaction for better performance
            transactionManager.afterCommit(() -> 
                eventPublisher.publish(result.attendeeRegisteredEvent())
            );
            
            return new AttendeeDTO(result.attendee().getEmail());
        });
    }
}
```

## Testing Your Implementation

**Unit Testing the Domain Service**:
```java
@ExtendWith(MockitoExtension.class)
class AttendeeServiceTest {
    
    @Mock AttendeeRepository attendeeRepository;
    @Mock AttendeeEventPublisher eventPublisher;
    
    @InjectMocks AttendeeService attendeeService;
    
    @Test
    void shouldRegisterNewAttendee() {
        // Given
        RegisterAttendeeCommand command = new RegisterAttendeeCommand("new@example.com");
        when(attendeeRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        
        // When
        AttendeeDTO result = attendeeService.registerAttendee(command);
        
        // Then
        assertThat(result.email()).isEqualTo("new@example.com");
        verify(attendeeRepository).persist(any(Attendee.class));
        verify(eventPublisher).publish(any(AttendeeRegisteredEvent.class));
    }
    
    @Test
    void shouldRejectDuplicateRegistration() {
        // Given
        RegisterAttendeeCommand command = new RegisterAttendeeCommand("existing@example.com");
        Attendee existingAttendee = Attendee.registerAttendee("existing@example.com").attendee();
        when(attendeeRepository.findByEmail("existing@example.com"))
            .thenReturn(Optional.of(existingAttendee));
        
        // When & Then
        assertThrows(DuplicateRegistrationException.class, 
            () -> attendeeService.registerAttendee(command));
        
        verify(attendeeRepository, never()).persist(any());
        verify(eventPublisher, never()).publish(any());
    }
    
    @Test
    void shouldHandleRepositoryFailure() {
        // Given
        RegisterAttendeeCommand command = new RegisterAttendeeCommand("test@example.com");
        when(attendeeRepository.findByEmail(any())).thenReturn(Optional.empty());
        doThrow(new RuntimeException("Database error")).when(attendeeRepository).persist(any());
        
        // When & Then
        assertThrows(AttendeeRegistrationException.class,
            () -> attendeeService.registerAttendee(command));
    }
}
```

**Integration Testing with Database and Messaging**:
```java
@QuarkusTest
@TestTransaction
class AttendeeServiceIntegrationTest {
    
    @Inject AttendeeService attendeeService;
    @Inject AttendeeRepository attendeeRepository;
    
    @Test
    void shouldCompleteRegistrationWorkflow() {
        // Given
        RegisterAttendeeCommand command = new RegisterAttendeeCommand("integration@example.com");
        
        // When
        AttendeeDTO result = attendeeService.registerAttendee(command);
        
        // Then
        assertThat(result.email()).isEqualTo("integration@example.com");
        
        // Verify persistence
        Optional<Attendee> persisted = attendeeRepository.findByEmail("integration@example.com");
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getEmail()).isEqualTo("integration@example.com");
        
        // Verify idempotency - second registration should fail
        assertThrows(DuplicateRegistrationException.class,
            () -> attendeeService.registerAttendee(command));
    }
    
    @Test
    void shouldHandleTransactionRollback() {
        // Given - setup to force event publishing failure
        RegisterAttendeeCommand command = new RegisterAttendeeCommand("rollback@example.com");
        
        // When - registration fails due to event publishing
        assertThrows(AttendeeRegistrationException.class,
            () -> attendeeService.registerAttendee(command));
        
        // Then - verify transaction was rolled back
        Optional<Attendee> notPersisted = attendeeRepository.findByEmail("rollback@example.com");
        assertThat(notPersisted).isEmpty();
    }
}
```

**Business Logic Testing**:
```java
@Test
void shouldEnforceBusinessRules() {
    // Test various business scenarios
    RegisterAttendeeCommand validCommand = new RegisterAttendeeCommand("valid@example.com");
    RegisterAttendeeCommand invalidCommand = new RegisterAttendeeCommand("invalid");
    
    // Valid registration should succeed
    AttendeeDTO result = attendeeService.registerAttendee(validCommand);
    assertThat(result.email()).isEqualTo("valid@example.com");
    
    // Invalid email should be rejected at aggregate level
    assertThrows(IllegalArgumentException.class,
        () -> attendeeService.registerAttendee(invalidCommand));
    
    // Duplicate registration should be rejected at service level
    assertThrows(DuplicateRegistrationException.class,
        () -> attendeeService.registerAttendee(validCommand));
}
```

### Connection to Other Components

This service will be:
1. **Called** by the `AttendeeEndpoint` to handle registration requests
2. **Use** the `Attendee` aggregate for business logic
3. **Coordinate** with `AttendeeRepository` for persistence
4. **Publish** events through `AttendeeEventPublisher`
5. **Return** `AttendeeDTO` objects to the presentation layer

### Advanced Domain Service Patterns

**Saga Orchestration** for complex workflows:
```java
@ApplicationScoped
public class ConferenceRegistrationSaga {
    
    public void handleAttendeeRegistration(RegisterAttendeeCommand cmd) {
        SagaTransaction saga = sagaManager.start("attendee-registration", cmd.email());
        
        try {
            // Step 1: Register attendee
            AttendeeDTO attendee = attendeeService.registerAttendee(cmd);
            saga.recordSuccess("attendee-created", attendee);
            
            // Step 2: Reserve conference spot
            ConferenceSpot spot = conferenceService.reserveSpot(cmd.conferenceId());
            saga.recordSuccess("spot-reserved", spot);
            
            // Step 3: Process payment
            PaymentResult payment = paymentService.processPayment(cmd.paymentInfo());
            saga.recordSuccess("payment-processed", payment);
            
            saga.complete();
            
        } catch (Exception e) {
            saga.compensate();  // Rollback all completed steps
            throw new RegistrationSagaException("Registration workflow failed", e);
        }
    }
}
```

**Domain Event Handling** within services:
```java
@ApplicationScoped
public class AttendeeService {
    
    @Observes AttendeeRegisteredEvent event {
        // React to attendee registration
        badgeService.createBadge(event.email());
        welcomeService.scheduleWelcomeEmail(event.email());
    }
    
    @Observes ConferenceCapacityReachedEvent event {
        // React to conference being full
        waitlistService.activateWaitlist(event.conferenceId());
    }
}
```

**Specification Pattern** for complex business rules:
```java
@ApplicationScoped
public class AttendeeService {
    
    public AttendeeDTO registerAttendee(RegisterAttendeeCommand cmd) {
        // Use specifications for complex business rules
        RegistrationEligibilitySpec eligibilitySpec = new RegistrationEligibilitySpec(
            new NotAlreadyRegisteredSpec(attendeeRepository),
            new ConferenceNotFullSpec(conferenceService),
            new ValidRegistrationPeriodSpec(clock)
        );
        
        if (!eligibilitySpec.isSatisfiedBy(cmd)) {
            throw new RegistrationNotAllowedException(
                eligibilitySpec.getViolationReasons(cmd));
        }
        
        // Proceed with registration
        return performRegistration(cmd);
    }
}
```

**Policy Pattern** for configurable business rules:
```java
@ApplicationScoped
public class AttendeeService {
    
    @Inject RegistrationPolicy registrationPolicy;
    
    public AttendeeDTO registerAttendee(RegisterAttendeeCommand cmd) {
        // Apply configurable business policies
        PolicyResult policyResult = registrationPolicy.evaluate(cmd);
        
        if (!policyResult.isAllowed()) {
            throw new PolicyViolationException(policyResult.getReasons());
        }
        
        // Apply any policy-driven modifications
        RegisterAttendeeCommand modifiedCommand = policyResult.applyModifications(cmd);
        
        return performRegistration(modifiedCommand);
    }
}
```

### Real-World Considerations

**Performance Optimization**:
```java
@ApplicationScoped
public class OptimizedAttendeeService {
    
    @Inject @ConfigProperty(name = "registration.batch.size") int batchSize;
    
    public List<AttendeeDTO> registerMultipleAttendees(List<RegisterAttendeeCommand> commands) {
        // Batch processing for better performance
        return commands.stream()
            .collect(Collectors.groupingBy(cmd -> cmd.email().hashCode() % batchSize))
            .values()
            .parallelStream()
            .flatMap(batch -> processBatch(batch).stream())
            .collect(Collectors.toList());
    }
    
    private List<AttendeeDTO> processBatch(List<RegisterAttendeeCommand> batch) {
        return transactionManager.executeInTransaction(() -> {
            // Process entire batch in single transaction
            return batch.stream()
                .map(this::registerAttendee)
                .collect(Collectors.toList());
        });
    }
}
```

**Circuit Breaker Pattern** for external dependencies:
```java
@ApplicationScoped
public class ResilientAttendeeService {
    
    @Inject CircuitBreaker eventPublisherCircuitBreaker;
    
    public AttendeeDTO registerAttendee(RegisterAttendeeCommand cmd) {
        AttendeeRegistrationResult result = Attendee.registerAttendee(cmd.email());
        attendeeRepository.persist(result.attendee());
        
        // Use circuit breaker for event publishing
        eventPublisherCircuitBreaker.call(() -> {
            eventPublisher.publish(result.attendeeRegisteredEvent());
            return null;
        }).recover(throwable -> {
            // Fallback: store event for later retry
            outboxRepository.storeForRetry(result.attendeeRegisteredEvent());
            return null;
        });
        
        return new AttendeeDTO(result.attendee().getEmail());
    }
}
```

**Caching Strategies**:
```java
@ApplicationScoped
public class CachedAttendeeService {
    
    @CacheResult(cacheName = "attendee-lookup")
    public Optional<AttendeeDTO> findAttendeeByEmail(String email) {
        return attendeeRepository.findByEmail(email)
            .map(attendee -> new AttendeeDTO(attendee.getEmail()));
    }
    
    @CacheInvalidate(cacheName = "attendee-lookup")
    public AttendeeDTO registerAttendee(RegisterAttendeeCommand cmd) {
        // Registration invalidates cache
        return performRegistration(cmd);
    }
}
```

### Common Questions

**Q: What's the difference between Domain Services and Application Services?**
A: Domain Services contain business logic and operate on domain objects. Application Services coordinate use cases and handle cross-cutting concerns like security, transaction management, and external service integration.

**Q: Should Domain Services be stateless?**
A: Yes, Domain Services should be stateless and focus on coordinating operations rather than maintaining state. State belongs in aggregates.

**Q: When should I create a new Domain Service vs adding methods to an existing one?**
A: Create a new service when you have a distinct set of related business operations. Keep services focused on a single area of business functionality.

**Q: Can Domain Services call other Domain Services?**
A: Yes, but be careful about circular dependencies and consider whether the logic belongs in a higher-level orchestrating service instead.

**Q: Should Domain Services handle validation?**
A: Domain Services should handle cross-aggregate validation and business rules, but delegate single-aggregate validation to the aggregates themselves.

## Next Steps

In the next step, we'll create the `AttendeeEndpoint` REST adapter that serves as the inbound adapter for our hexagonal architecture. The endpoint will receive HTTP requests, convert them to commands, delegate to our domain service, and return appropriate HTTP responses, completing the end-to-end registration workflow.

