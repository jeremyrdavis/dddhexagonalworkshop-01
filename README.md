# Iteration 01: End to end DDD

## DDD Concepts: Commands, Events, Aggregates, Domain Services, Repositories, Entities

### Overview

**Introduction to Iteration 1**

In this iteration, we will cover the basics of Domain-Driven Design by implementing a basic workflow for registering a conference attendee. We will create the following DDD constructs:

- Aggregate
- Domain Service
- Domain Event
- Command
- Adapter
- Entity
- Repository

We will also use the `Hexangonal Architecture`, or `Ports and Adapters` pattern to integrate with external systems, ensuring a clean separation of concerns.

The basic project structure is already set up for you. The project is structured as follows:

```text
dddhexagonalworkshop
├── conference
│   └── attendees
│       ├── domain
│       │   ├── aggregates
│       │   │   └── Attendee.java
│       │   ├── events
│       │   │   └── AttendeeRegisteredEvent.java
│       │   ├── services
│       │   │   ├── AttendeeRegistrationResult.java
│       │   │   └── AttendeeService.java
│       │   │   └── RegisterAttendeeCommand.java
│       │   └── valueobjects
│       ├── infrastructure
│       │   ├── AttendeeEndpoint.java
│       │   ├── AttendeeDTO.java
│       │   └── AttendeeEventPublisher.java
│       └── persistence
│           ├── AttendeeEntity.java
│           └── AttendeeRepository.java
```

As you progress through the workshop, you will fill in the missing pieces of code in the appropriate packages. The workshop authors have stubbed out the classes so that you can focus on the Domain Driven Design concepts as much as possible and Java and framework concepts as little as possible.
You can type in the code line by line or copy and paste the code provided into your IDE. You can also combine the approaches as you see fit. The goal is to understand the concepts and how they fit together in a DDD context.

**Quarkus**

Quarkus, https://quarkus.io, is a modern Java framework designed for building cloud-native applications. It provides a set of tools and libraries that make it easy to develop, test, and deploy applications. In this workshop, we will leverage Quarkus to implement our DDD concepts and build a RESTful API for registering attendees.
The project uses Quarkus, a Java framework that provides built-in support for REST endpoints, JSON serialization, and database access. Quarkus also features a `Dev Mode` that automatically spins up external dependencies like Kafka and PostgreSQL, allowing you to focus on writing code without worrying about the underlying infrastructure.

**Steps:**

In this first iteration, we will implement the basic workflow for registering an attendee. The steps are as follows:

1. Create an `AttendeeRegisteredEvent` that records the important functionality in this subdomain and can be used to integrate with the rest of the system.
2. Create a `RegisterAttendeeCommand` that triggers the registration workflow.
3. Implement an Adapter in the form of a REST Endpoint, `AttendeeEndpoint` with a POST method.
4. Implement a Data Transfer Object, `AttendeeDTO`, to return the attendee's details to the UI.
5. Implement a Domain Service, `AttendeeService`, to orchestration the registration process.
6. Implement an `Attendee` Aggregate to isolate invariants (business logic) from the rest of the application.
7. Implement a Domain Event, `AttendeeRegisteredEvent`, that will be published when an attendee is successfully registered.
8. Implement a Repository interface, `AttendeeRepository`, that defines methods for saving and retrieving attendees.
9. Create an Entity, `AttendeeEntity`, to persist instances of the `Attendee` entity in a database.
10. Create an Adapter, `AttendeeEventPublisher`, that sends events to Kafka to propagate changes to the rest of the system.

By the end of Iteration 1, you'll have a solid foundation in DDD concepts and a very basic working application.

Let's get coding!

## Step 1: Events

### Learning Objectives

- Understand the role of Domain Events in capturing business-significant occurrences
- Implement an AttendeeRegisteredEvent to notify other parts of the system

### What You'll Build

An AttendeeRegisteredEvent record that captures the fact that an attendee has successfully registered for the conference.

### Why Domain Events Matter

- Domain Events solve several critical problems in distributed systems:
- Business Communication: Events represent facts that have already happened in the business domain. Events are immutable statements of truth.
- System Decoupling: When an attendee registers, multiple things might need to happen:
 -- Send a welcome email
 -- Update conference capacity
 -- Notify the billing system
 -- Generate a badge
  Without events, the AttendeeService would need to know about all these concerns, creating tight coupling. With events, each system can independently listen for AttendeeRegisteredEvent and react appropriately.
- Audit Trail: Events naturally create a history of what happened in your system, which is valuable for debugging, compliance, and business analytics.

### Implementation

A Domain Event is a record of some business-significant occurrence in a Bounded Context. It's obviously significant that an attendee has registered because that's how conferences make money, but it's also significant because other parts of the system need to respond to the registration.
For this iteration, we'll use a minimal event with only the attendee's email address. Update  `AttendeeRegisteredEvent.java` with the email:

```java
package dddhexagonalworkshop.conference.attendees.domain.events;

public record AttendeeRegisteredEvent(String email) {
}
```

***Note:*** Deleting and recreating the file is fine if you prefer to start fresh. Just ensure the package structure matches the one in the project.

### Key Design Decisions

**Why a record?** Records are perfect for events because:
- They're immutable by default (events should never change)
- They provide automatic equals/hashCode implementation
- They're concise and readable

**Why only email?** In this iteration, we're keeping it simple. In real systems, you might include:
- Timestamp of registration
- Attendee ID
- Conference ID
- Registration type (early bird, regular, etc.)

### Testing Your Implementation

After implementing the event, verify it compiles and the basic structure is correct.  There is a JUnit test, `AttendeeRegisteredEventTest.java` which can be run in your IDE or from the commd line with:

```bash
mvn test -Dtest=AttendeeRegisteredEventTest
```

The test should pass, confirming that the `AttendeeRegisteredEvent` record is correctly defined and can be instantiated with an email address.

## Step 2: Commands

### Learning Objectives
- Understand how Commands encapsulate business intentions and requests for action
- Distinguish between Commands (can fail) and Events (facts that occurred)
- Implement a RegisterAttendeeCommand to capture attendee registration requests
- Apply the Command pattern to create a clear contract for business operations

### You'll Build

A `RegisterAttendeeCommand` record that encapsulates all the data needed to request attendee registration for the conference.

### Why Commands Matter

Commands solve several important problems in business applications:
- Clear Business Intent: Commands explicitly represent what a user or system wants to accomplish. `RegisterAttendeeCommand` clearly states "I want to register this person for the conference" rather than having loose parameters floating around.
- Validation Boundary: Commands provide a natural place to validate input before it reaches your business logic:

```java
// Instead of scattered validation
if (email == null || email.isEmpty()) { ... }
if (!email.contains("@")) { ... }

// Commands centralize validation rules
public record RegisterAttendeeCommand(String email) {
    public RegisterAttendeeCommand {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        // Additional validation logic here
    }
}
```

- Immutability: Commands are immutable objects that can't be accidentally modified as they pass through your system. This prevents bugs and makes the code easier to reason about.
- Failure Handling: Unlike events (which represent facts), commands can be rejected. Your business logic can validate a command and decide whether to process it or reject it with a clear error message.

### Commands vs Events: A Critical Distinction

| Aspect | Commands | Events |
|--------|----------|--------|
| **Nature** | Intention/Request | Fact/What happened |
| **Can fail?** | Yes | No (already happened) |
| **Mutability** | Immutable | Immutable |
| **Tense** | Imperative ("Register") | Past tense ("Registered") |
| **Example** | RegisterAttendeeCommand | AttendeeRegisteredEvent |

Think of it like ordering food:
- **Command**: "I want to order a burger" (restaurant might be out of burgers)
- **Event**: "Customer ordered a burger at 2:15 PM" (this definitely happened)

### Implementation

Commands are objects that encapsulate a request to perform an action. The `RegisterAttendeeCommand` will encapsulate the data needed to register an attendee, which in this iteration is just the email address.

The `RegisterAttendeeCommand` is located in the `dddhexagonalworkshop.conference.attendees.domain.services` package because it's part of the AttendeeService's API. This placement follows DDD principles where commands are associated with the services that process them.

```java
package dddhexagonalworkshop.conference.attendees.domain.services;

/**
 * Command representing a request to register an attendee for the conference.
 * Commands encapsulate the intent to perform a business operation and can
 * be validated, queued, or rejected before processing.
 */
public record RegisterAttendeeCommand(String email) {
    
    /**
     * Compact constructor for validation.
     * This runs automatically when the record is created.
     */
    public RegisterAttendeeCommand {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        
        // Basic email format validation
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Email must contain @ symbol");
        }
    }
}
```

### Key Design Decisions

**Why a record?** Records are perfect for commands because:
- They're immutable by default (commands shouldn't change after creation)
- They provide automatic equals/hashCode (useful for deduplication)
- They're concise and focus on data rather than behavior
- The compact constructor enables validation at creation time

**Why only email?** We're starting simple to focus on the DDD concepts. In real systems, registration might include:
- First and last name
- Phone number
- Company information
- Dietary restrictions
- T-shirt size

**Package placement?** Commands live with the service that processes them, not in a separate "commands" package. This keeps related concepts together.

### Testing Your Implementation

After implementing the event, verify it compiles and the basic structure is correct.  There is a JUnit test, `RegisterAttendeeCommandTest.java` which can be run in your IDE or from the commd line with:

```bash
mvn test -Dtest=RegisterAttendeeCommandTest
```

## Connection to Other Components

This command will be:
1. **Received** by the `AttendeeEndpoint` from HTTP requests
2. **Processed** by the `AttendeeService` to orchestrate registration
3. **Validated** automatically when created (thanks to the compact constructor)
4. **Used** to create the `Attendee` aggregate and trigger business logic

We have not yet implemented the `Attendee`, `AttendeeService`, or `AttendeeEndpoint` yet, but we will in the next steps.

## Real-World Considerations

**Command Validation**: In production systems, you might want more sophisticated validation:
- Email format validation using regex
- Cross-field validation for complex commands

**Command Handling**: Commands often go through pipelines:
```
HTTP Request → Command → Validation → Authorization → Business Logic → Events
```

**Command Sourcing**: Some systems store commands as well as events, creating a complete audit trail of what was requested vs. what actually happened.

## Common Questions

**Q: Should commands contain behavior or just data?**
A: Primarily data, but validation logic in the constructor is acceptable. Complex business logic belongs in aggregates or domain services.

**Q: Can one command trigger multiple events?**
A: Absolutely! Registering an attendee might trigger `AttendeeRegisteredEvent`, `PaymentRequestedEvent`, and `WelcomeEmailQueuedEvent`.

**Q: What if I need to change a command after it's created?**
A: You don't! Commands are immutable. Instead, create a new command with the updated data. This immutability prevents bugs and makes the system more predictable.

### Next Steps

In the next step, we'll create the `AttendeeRegistrationResult` that will package together the outputs of processing this command - both the created `Attendee` and the `AttendeeRegisteredEvent` that needs to be published.### Step 2: Adapters

The `Ports and Adapters` pattern, also known as `Hexagonal Architecture`, is a design pattern that separates the core business logic from external systems. The phrase was coined by Alistair Cockburn in the early 2000s.

The pattern fits well with Domain-Driven Design (DDD) because it allows the domain model to remain pure and focused on business logic, while the adapters handle the technical details. This allows the core application to remain independent of the technologies used for input/output, such as databases, message queues, or REST APIs.

Adapters are components that translate between the domain model and external systems or frameworks. In the context of a REST endpoint, an adapter handles the conversion of HTTP requests to commands that the domain model can process, and vice versa for responses. We don't need to manually convert the JSON to and from Java objects, as Quarkus provides built-in support for this using Jackson.

Complete the AttendeeEndpoint in the `dddhexagonalworkshop.conference.attendees.infrastructure` package.

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
## Step 3: Returning Multiple Objects

***Note:*** This step is not specific to Domain Driven Design.  This is simply a useful coding practice.

### Learning Objectives

- Understand how Result objects cleanly package multiple outputs from domain operations
- Implement AttendeeRegistrationResult to encapsulate both domain state and events

### What You'll Build

An AttendeeRegistrationResult Record that packages together both the created Attendee aggregate and the AttendeeRegisteredEvent that needs to be published.

### Why Result Objects Matter
- Multiple Return Values: Operations often need to return more than one thing. When an attendee registers, we need:
  -- The created Attendee (to persist to database)
  -- The AttendeeRegisteredEvent (to publish to other systems)
  -- Type Safety: Rather than returning generic collections or maps, result objects provide compile-time safety and clear naming.
  -- Potentially validation results, warnings, or metadata

We need to create an object that holds both the Attendee aggregate and the AttendeeRegisteredEvent that were created during the registration process.

```java
package dddhexagonalworkshop.conference.attendees.domain.services;
import dddhexagonalworkshop.conference.attendees.domain.aggregates.Attendee;
import dddhexagonalworkshop.conference.attendees.domain.events.AttendeeRegisteredEvent;

/**
* Result object that packages the outputs of attendee registration.
* Contains both the domain state (Attendee) and the domain event
* (AttendeeRegisteredEvent) that need different handling by the service layer.
  */
  public record AttendeeRegistrationResult(Attendee attendee, AttendeeRegisteredEvent attendeeRegisteredEvent) {

  /**
    * Compact constructor for validation.
    * Ensures both components are present since registration should
    * always produce both an attendee and an event.
      */
      public AttendeeRegistrationResult {
          if (attendee == null) {
          throw new IllegalArgumentException("Attendee cannot be null");
          }
          if (attendeeRegisteredEvent == null) {
          throw new IllegalArgumentException("AttendeeRegisteredEvent cannot be null");
      }
  }
}
```
***Note:*** The `AttendeeRegistrationResult` will not compile until we implement the `Attendee` aggregate. This is intentional to guide you through the process of building the domain model step by step.  It will compile after the next step when we implement the `Attendee` aggregate.

### Key Design Decisions

**Why a record?** Records are perfect for result objects because:
- They're immutable (results shouldn't change after creation)
- They provide automatic equals/hashCode for testing
- They have clear, readable toString() methods
- Component accessors are automatically generated

**Why validate in constructor?** Since this represents a successful operation, both components should always be present. The validation ensures we catch programming errors early.
**Package placement?** Result objects live with the service that uses them, since they're part of the service's API contract.
**Naming convention?** Result objects typically follow the pattern [Operation]Result (e.g., AttendeeRegistrationResult, PaymentProcessingResult).

### Testing Your Implementation
The `AttendeeRegistrationResult` record is tested indirectly through the `AttendeeService` tests. Once you implement the `Attendee` aggregate in the next step, the `AttendeeRegistrationResult` will be used in the service layer, and you can run the tests to verify its functionality.

## Step 4: Aggregates

### Learning Objectives

- Understand Aggregates as the core building blocks of Domain-Driven Design
- Implement the Attendee aggregate with business logic and invariant enforcement
- Apply the concept of aggregate roots and consistency boundaries
- Connect Commands, business logic, and Result objects through aggregate methods

### What You'll Build

An Attendee aggregate that encapsulates the business logic for attendee registration and maintains consistency within the attendee bounded context.

### Why Aggregates Are the Heart of DDD
- Aggregates solve the most critical problem in business software: where does the business logic live?
Scattered Logic Problem: Without aggregates, business rules end up scattered across:
```java
 ❌ Business logic scattered everywhere
// In the controller
if (email.isEmpty()) throw new ValidationException("Email required");

// In the service  
if (existingAttendees.contains(email)) throw new DuplicateException("Already registered");

// In the repository
if (attendee.getStatus() == null) attendee.setStatus("PENDING");

// In random utility classes
if (!EmailValidator.isValid(email)) throw new InvalidEmailException();
Aggregate Solution: All business logic for a concept lives in one place:
java// ✅ All attendee business logic centralized in the Attendee aggregate
public class Attendee {
public static AttendeeRegistrationResult registerAttendee(String email) {
// All validation, business rules, and invariants enforced here
validateEmail(email);
checkBusinessRules(email);

        Attendee attendee = new Attendee(email);
        AttendeeRegisteredEvent event = new AttendeeRegisteredEvent(email);
        
        return new AttendeeRegistrationResult(attendee, event);
    }
}
```

### Core Aggregate Concepts

**Consistency Boundary:** An aggregate defines what data must be consistent together. For attendees:
- Email must be valid and unique within the conference
- Registration status must be coherent with payment status
- Badge information must match attendee details

**Aggregate Root:** The single entry point for accessing the aggregate. Other objects can only reference the aggregate through its root (the Attendee itself), never reaching into internal objects directly.

**Business Invariants:** Rules that must always be true:
- An attendee must have a valid email
- An attendee can only be registered once per conference
- Registration must create both attendee record and notification event

### Implementation

Aggregates are the core objects in Domain-Driven Design. An Aggregate represents the most important object or objects in our Bounded Context. We're implementing the Attendees Bounded Context, and the Attendee is the most important object in this context.
An Aggregate both represents the real-world object (a conference attendee in this case) and encapsulates all of the invariants, or business logic, associated with the object.

```java
package dddhexagonalworkshop.conference.attendees.domain.aggregates;

import dddhexagonalworkshop.conference.attendees.domain.events.AttendeeRegisteredEvent;
import dddhexagonalworkshop.conference.attendees.domain.services.AttendeeRegistrationResult;

/**
 * "An AGGREGATE is a cluster of associated objects that we treat as a unit for the purpose of data changes. Each AGGREGATE has a root and a boundary. The boundary defines what is inside the AGGREGATE. The root is a single, specific ENTITY contained in the AGGREGATE. The root is the only member of the AGGREGATE that outside objects are allowed to hold references to, although objects within the boundary may hold references to each other."
 * Eric Evans, Domain-Driven Design: Tackling Complexity in the Heart of Software, 2003
 *
 * Attendee aggregate root - represents a conference attendee and encapsulates
 * all business logic related to attendee management. This is the consistency
 * boundary for attendee-related operations.
 */
public class Attendee {

    private final String email;

    /**
     * Private constructor - aggregates control their own creation
     * to ensure invariants are always maintained.
     */
    private Attendee(String email) {
        this.email = email;
    }

    /**
     * Factory method for registering a new attendee.
     * This is the primary business operation that processes the registration
     * command and returns everything needed by the application layer.
     *
     * @param email The attendee's email address
     * @return AttendeeRegistrationResult containing the attendee and event
     */
    public static AttendeeRegistrationResult registerAttendee(String email) {
        // Business logic and invariant enforcement
        validateEmailForRegistration(email);

        // Create the aggregate instance
        Attendee attendee = new Attendee(email);

        // Create the domain event
        AttendeeRegisteredEvent event = new AttendeeRegisteredEvent(email);

        // Return both outputs packaged together
        return new AttendeeRegistrationResult(attendee, event);
    }

    /**
     * Encapsulates business rules for email validation.
     * This is where domain-specific validation logic lives.
     */
    private static void validateEmailForRegistration(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required for registration");
        }

        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Email must be a valid email address");
        }

        // Additional business rules could go here:
        // - Check against blocked domains
        // - Validate email format against conference requirements
        // - Check for corporate email requirements
    }

    /**
     * Getter for email - aggregates control access to their data
     */
    public String getEmail() {
        return email;
    }

    /**
     * Equality based on business identity (email in this case)
     * Two attendees are the same if they have the same email
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attendee attendee = (Attendee) o;
        return email.equals(attendee.email);
    }

    @Override
    public int hashCode() {
        return email.hashCode();
    }

    @Override
    public String toString() {
        return "Attendee{email='" + email + "'}";
    }
}
```

###  Key Design Decisions

**Static Factory Method:** registerAttendee() is static because it represents creating a new attendee, not operating on an existing one. This is a common pattern for aggregate creation.
**Private Constructor:** The constructor is private to force all creation through the factory method, ensuring business rules are always applied.
**Business Logic Location:** All validation and business rules are in the aggregate, not scattered across services or controllers.

### Testing Your Implementation
After implementing the aggregate, verify it works correctly:

```bash
mvn test -Dtest=AttendeeTest
```

### Aggregate Patterns in Practice

**Aggregate Size:** Keep aggregates small and focused. Our Attendee aggregate only handles attendee-specific concerns, not conference-wide logic.

**Consistency Boundaries:**

✅ Within aggregate: Strong consistency (all changes happen together)

❌ Between aggregates: Eventual consistency (use events to synchronize)

**Command Handling:** Each business operation typically maps to one aggregate method:
- RegisterAttendeeCommand → Attendee.registerAttendee()
- UpdateAttendeeCommand → Attendee.updateContactInfo()
- CancelRegistrationCommand → Attendee.cancelRegistration()

### Real-World Considerations
**Performance:** Aggregates should be designed for the most common access patterns. Don't load huge object graphs if you only need basic information.
**Concurrency:** In production, you'll need to handle concurrent modifications using techniques like optimistic locking or event sourcing.
**Evolution:** As business rules change, aggregates evolve. The centralized logic makes changes easier to implement and test.

### Common Questions
**Q:** Should aggregates have dependencies on other aggregates?
**A:** No! Aggregates should not directly reference other aggregates. Use domain services or events for cross-aggregate operations.
**Q:** How big should an aggregate be?
**A:** As small as possible while maintaining consistency. If you find yourself loading lots of data you don't need, consider splitting the aggregate.
**Q:** Can aggregates call external services?
**A:** Generally no. Aggregates should contain pure business logic. Use domain services for operations that need external dependencies.
**Q:** Should aggregates be mutable or immutable?
**A:** It depends. For event-sourced systems, immutable aggregates work well. For traditional CRUD, controlled mutability (like our example) is common.

### Next Steps
In the next step, we'll create the AttendeeEntity that will persist an instance of an Attendee.

## Step 5: Entities

In Domain-Driven Design, all persistence is handled by repositories, and before we create the repository, we need a persistence entity. Entities represent specific instances of domain objects with database identities.

### Learning Objectives

- Understand the difference between Domain Aggregates and Persistence Entities
- Implement AttendeeEntity for database persistence using JPA annotations
- Apply the separation between domain logic and persistence concerns
- Connect domain aggregates to database storage through persistence entities

### What You'll Build

An AttendeeEntity JPA entity that represents how attendee data is stored in the database, separate from the domain logic in the Attendee aggregate.

### Why Separate Persistence Entities?

This separation solves several critical problems in domain-driven applications:
Domain Purity: Your domain aggregates stay focused on business logic without being polluted by persistence concerns:

❌ Domain aggregate mixed with persistence concerns

```java

@Entity @Table(name = "attendees")
public class Attendee {
@Id @GeneratedValue
private Long id;  // Database concern, not business concern

    @Column(name = "email_address", length = 255)
    private String email;  // Database annotations in domain model
    
    public static AttendeeRegistrationResult registerAttendee(String email) {
        // Business logic mixed with persistence annotations
    }
}
```
✅ Clean separation of concerns

```java
// Domain Aggregate (business logic only)
public class Attendee {
private final String email;

    public static AttendeeRegistrationResult registerAttendee(String email) {
        // Pure business logic, no persistence concerns
    }
}
...

// Persistence Entity (database concerns only)
@Entity @Table(name = "attendees")
public class AttendeeEntity {
@Id @GeneratedValue
private Long id;

    @Column(name = "email")
    private String email;
    
    // No business logic, just persistence mapping
}
```
- Technology Independence: Your domain model isn't tied to any specific database or ORM framework. You could switch from JPA to MongoDB without changing your business logic.
- Testing Simplicity: Domain logic can be tested without database setup, while persistence logic can be tested separately with database integration tests.
- Evolution Independence: Database schema changes don't require changes to domain logic, and business rule changes don't require database migrations.

### Entities vs Aggregates: Key Differences

Understanding the distinction between Domain Aggregates and Persistence Entities is crucial for proper DDD implementation. Here's a detailed comparison:

| Aspect      | Domain Aggregate             | Persistence Entity           |
| ----------- | ---------------------------- | ---------------------------- |
| Purpose     | Business logic & rules       | Data storage mapping         |
| Dependencies| Pure Java, domain concepts   | JPA, database annotations    |
| Identity    | Business identity (email)    | Technical identity (database ID) |
| Lifecycle   | Created by business operations | Created/loaded by ORM        |
| Mutability  | Controlled by business rules | Managed by persistence framework |
| Testing     | Unit tests, no database      | Integration tests with database |

### Deep Dive: Purpose and Responsibility

Domain Aggregates are responsible for:

- Enforcing business invariants and rules
- Encapsulating domain logic and behavior
- Maintaining consistency within their boundary
- Creating domain events when state changes occur
- Providing a clean API for business operations

```java
// Domain Aggregate - focused on business behavior
public class Attendee {
    public static AttendeeRegistrationResult registerAttendee(String email) {
        validateBusinessRules(email);        // Business logic
        checkConferenceCapacity();           // Business invariant
        Attendee attendee = new Attendee(email);
        AttendeeRegisteredEvent event = new AttendeeRegisteredEvent(email);
        return new AttendeeRegistrationResult(attendee, event);
    }
}
```

Persistence Entities are responsible for:

- Mapping domain data to database structures
- Handling ORM framework requirements
- Managing database relationships and constraints
- Providing efficient data access patterns
- Supporting query optimization

```java
// Persistence Entity - focused on data mapping
@Entity @Table(name = "attendee")
public class AttendeeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "email", unique = true, nullable = false)
    private String email;
    
    // No business logic - just data mapping
}
```
### Why This Separation Matters

**Flexibility:** Business logic can evolve independently of persistence technology. You could switch from JPA to MongoDB without changing domain code.
**Testability:** Business logic can be tested quickly without database setup, while persistence logic gets thorough integration testing.
**Performance:** Persistence entities can be optimized for database access patterns without compromising domain model clarity.
**Team Organization:** Domain experts can focus on aggregates, while database specialists optimize entities.
**Technology Evolution:** Framework updates or database changes don't ripple into business logic.

### Implementation

```java
package dddhexagonalworkshop.conference.attendees.persistence;

import jakarta.persistence.*;

/**
* JPA Entity for persisting Attendee data to the database.
* This class is purely concerned with data storage and mapping,
* containing no business logic. It serves as the bridge between
* our domain model and the relational database.
  */
  @Entity
  @Table(name = "attendee")
  public class AttendeeEntity {

  /**
    * Database primary key - technical identity for persistence.
    * This is different from business identity (email) in the domain model.
      */
      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

  /**
    * Business data mapped to database column.
    * Keep column names simple and clear.
      */
      @Column(name = "email", nullable = false, unique = true)
      private String email;

  /**
    * Default no-argument constructor required by Hibernate/JPA.
    * Protected visibility prevents direct instantiation while
    * allowing framework access.
      */
      protected AttendeeEntity() {
      // Required by JPA specification
      }

  /**
    * Constructor for creating new entity instances.
    * Package-private to control creation within persistence layer.
    *
    * @param email The attendee's email address
      */
      protected AttendeeEntity(String email) {
      this.email = email;
      }

  /**
    * Getter for database ID.
    * Protected because external code shouldn't depend on database IDs.
      */
      protected Long getId() {
      return id;
      }

  /**
    * Getter for email.
    * Protected to keep access controlled within persistence layer.
      */
      protected String getEmail() {
          return email;
      }

    /**
     * Setter for email.
     * Protected to keep access controlled within persistence layer.
     */
    protected void setEmail(String email) {
        this.email = email;
    }

  /**
    * String representation for debugging.
      */
      @Override
      public String toString() {
          return "AttendeeEntity{" +
          "id=" + id +
          ", email='" + email + '\'' +
          '}';
      }
  }
```

### Key Design Decisions
**Protected Constructors:** The default constructor is required by JPA, while the parameterized constructor allows controlled creation. Both are protected to limit access.
**Protected Methods:** Getters and setters are protected, not public. Only the repository layer should interact with entities directly.
**No Business Logic:** The entity contains no validation or business rules - that's the aggregate's responsibility.
**Simple Mapping:** We start with basic column mapping. Real applications might have more complex relationships and constraints.

### Database Schema

This entity will create a database table like:

```sql
    CREATE TABLE attendee (

      id BIGINT AUTO_INCREMENT PRIMARY KEY,
          email VARCHAR(255) NOT NULL UNIQUE
          );
```
The unique constraint on email ensures data integrity at the database level, complementing the business rules in the aggregate.

### Testing Your Implementation 

We will test the `AttendeeEntity` in the `AttendeeRepositoryTest.java` class, which will build in the next step.

### Connection to Other Components

This entity will be:

- Created by the AttendeeRepository when converting from domain aggregates
- Persisted to the database using JPA/Hibernate
- Loaded from the database when retrieving attendees
- Converted back to domain aggregates by the repository

Mapping Patterns

- Simple Mapping: Our current approach with basic fields and annotations.
Complex Relationships: Real applications might have:

```java
@Entity
public class AttendeeEntity {
@OneToMany(mappedBy = "attendee", cascade = CascadeType.ALL)
private List<RegistrationEntity> registrations;

    @Embedded
    private AddressEntity address;
    
    @Enumerated(EnumType.STRING)
    private AttendeeStatus status;
}
```

Value Objects: Embedded objects for complex data:

```java
@Embeddable
public class AddressEntity {
private String street;
private String city;
private String zipCode;
}
```

### Real-World Considerations

Performance: Use appropriate fetch strategies and indexing:
java@Index(name = "idx_attendee_email", columnList = "email")
@Table(name = "attendee", indexes = {@Index(name = "idx_attendee_email", columnList = "email")})
Auditing: Track creation and modification times:
java@CreationTimestamp
private LocalDateTime createdAt;

@UpdateTimestamp
private LocalDateTime updatedAt;
Versioning: Handle concurrent modifications:
java@Version
private Long version;
Soft Deletes: Instead of physical deletion:
java@Column(name = "deleted_at")
private LocalDateTime deletedAt;

### Common Questions

**Q:** Why not just use the domain aggregate as a JPA entity?
**A:** It violates single responsibility principle and couples domain logic to persistence technology. Changes in business rules would require database considerations and vice versa.

**Q:** Should entities contain validation logic?
**A:** No, validation belongs in the domain aggregate. Entities are just data containers for persistence.

**Q:** Can entities reference other entities?
**A:** Yes, but keep relationships simple and consider the performance implications of joins and lazy loading.

**Q:** How do I handle complex domain objects with many fields?
**A:** Start simple and evolve. Use embedded objects (@Embeddable) for value objects and separate entities for other aggregates.

**Q:** Should I use the same entity for reading and writing?
**A:** For simple cases, yes. For complex scenarios, consider separate read/write models (CQRS pattern).

### Next Steps
In the next step, we'll create the AttendeeRepository that bridges between our domain aggregates and these persistence entities. The repository will handle converting Attendee aggregates to AttendeeEntity objects for storage, and vice versa for retrieval, maintaining the clean separation between domain and persistence concerns.

### Step 6: Repositories

Complete the AttendeeRepository by adding a method to persist attendees.

You have probably noticed that our 'AttendeeRepository`implements`PanacheRepository<AttendeeEntity>`.  `PanacheRepository' provides methods for manipulating JPA Entities; however, following DDD guidelines, the Repository will be the only persistence class that other objects can access. To this end we need methods that take an `Attendee` and convert it to an `AttendeeEntity`.

```java
package dddhexagonalworkshop.conference.attendees.persistence;

import dddhexagonalworkshop.conference.attendees.domain.aggregates.Attendee;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

public class AttendeeRepository implements PanacheRepository<AttendeeEntity> {


  public void persist(Attendee aggregate) {
    // transform the aggregate to an entity
    AttendeeEntity attendeeEntity = fromAggregate(aggregate);
    persist(attendeeEntity);
  }

  private AttendeeEntity fromAggregate(Attendee attendee) {
    AttendeeEntity entity = new AttendeeEntity(attendee.getEmail());
    return entity;
  }
}
```















### Step : Data Transfer Objects (DTOs)

We also need to create a simple DTO (Data Transfer Object) to represent the attendee in the response. Data Transfer Objects are used to transfer data between layers, especially when the data structure is different from the domain model, which is why we are using it here.

```java
package dddhexagonalworkshop.conference.attendees.infrastructure;

public record AttendeeDTO(String email) {
}
```

### Step 4: Domain Services

Domain Services implement functionality that does not have a natural home in another object. Services also typically operate on or interact with multiple other objects. Services are named after the functionality they provide.

Our `AttendeeService` coordinates the workflow for registering an attendee. The `AttendeeService` calls the `Attendee` Aggregate and the deals with the results.

Implement the method, "registerAttendee" so that it takes a RegisterAttendeeCommand and returns an `AttendeeRegisteredResult`. Do not worry if the class does not compile immediately. We will implement the other objects in the next steps.

```java
package domain.services;

import dddhexagonalworkshop.conference.attendees.infrastructure.AttendeeDTO;
import jakarta.enterprise.context.ApplicationScoped;

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

In this implementation the `Attendee` aggregate has no knowledge of the persistence framework.

### Step 10: Another Adapter

- Create the AttendeeEventPublisher
  - create a single method, "publish" that takes an AttendeeRegisteredEvent
  - implement the method by sending the event to Kafka

```java
package dddhexagonalworkshop.conference.attendees.infrastructure;

import dddhexagonalworkshop.conference.attendees.domain.events.AttendeeRegisteredEvent;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class AttendeeEventPublisher {

  @Channel("attendees")
  public Emitter<AttendeeRegisteredEvent> attendeesTopic;

  public void publish(AttendeeRegisteredEvent attendeeRegisteredEvent) {
    attendeesTopic.send(attendeeRegisteredEvent);
  }
}
```

### Step 11: Complete the registration process

Update the AttendeeEndpoint to return the AttendeeDTO

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

## DDD Workshop Tutorial Summary

In this iteration we implemented multiple Domain-Driven Design (DDD) concepts while building a microservice for the attendee subdomain of a conference registration system. We used core DDD patterns including Commands, Events, Aggregates, Domain Services, Repositories, Entities, and Adapters while maintaining clean separation of concerns through the Ports and Adapters pattern.

### Step-by-Step Implementation

#### Step 1: Commands

What was built: RegisterAttendeeCommand record with email field
Key learning: Commands encapsulate requests to perform actions, are immutable, and represent intentions to change system state (unlike Events which are facts that already happened)

#### Step 2: Adapters (REST Endpoint)

What was built: AttendeeEndpoint with POST method for attendee registration
Key learning: Adapters translate between domain models and external systems, keeping core business logic independent of technical frameworks like REST APIs

#### Step 3: Data Transfer Objects

What was built: AttendeeDTO record for API responses
Key learning: DTOs facilitate data transfer between layers when structure differs from domain models

#### Step 4: Domain Services

What was built: AttendeeService with registerAttendee method
Key learning: Domain Services implement functionality that doesn't naturally belong in other objects and coordinate workflows across multiple domain objects

#### Step 5: Aggregates

What was built: Attendee aggregate with registerAttendee static method
Key learning: Aggregates are core DDD objects that represent real-world entities and encapsulate all business logic/invariants for their bounded context

#### Step 6: Events

What was built: AttendeeRegisteredEvent record with email field
Key learning: Domain Events record business-significant occurrences and enable system-wide notifications of important state changes

#### Step 7: Result Objects

What was built: AttendeeRegistrationResult record holding both Attendee and Event
Key learning: Result objects cleanly package multiple outputs from domain operations

#### Step 8: Entities

What was built: AttendeeEntity JPA entity for database persistence
Key learning: Entities represent specific instances of domain objects with identities, separate from business logic aggregates

#### Step 9: Repositories

What was built: AttendeeRepository implementing PanacheRepository
Key learning: Repositories handle all persistence operations and convert between domain aggregates and persistence entities, maintaining domain purity

#### Step 10: Event Publishing Adapter

What was built: AttendeeEventPublisher for Kafka integration
Key learning: Event publishers are adapters that propagate domain events to external messaging systems

#### Step 11: Integration Completion

What was completed: Full integration in AttendeeEndpoint
Key learning: All components work together to complete the registration workflow

### Architecture Patterns Demonstrated

Hexagonal Architecture/Ports and Adapters: Clean separation between core business logic and external systems (REST, Kafka, Database)
Domain-Driven Design: Business logic encapsulated in aggregates, clear bounded contexts, and domain-centric design
Event-Driven Architecture: System components communicate through domain events rather than direct coupling
Key Takeaways

Commands represent intentions; Events represent facts
Aggregates contain business logic and maintain consistency
Adapters isolate technical concerns from domain logic
Repositories abstract persistence details from the domain
Domain Services orchestrate complex workflows across multiple objects

### Key points

**_Hexagonal Architecture/Ports and Adapters_**: The AttendeeEndpoint is an _Outgoing Port_ for the registering attendees. In our case the _Adaper_ is the Jackson library, which is built into Quarkus, and handles converting JSON to Java objects and vice versa.  
The AttendeeEventPubliser is also an Adapter that sends events to Kafka, which is another Port in our architecture.  
The AttendeeRepository is a Port that allows us to persist the AttendeeEntity to a database.

**_Aggregates_** Business logic is implemented in an Aggregate, Attendee. The Aggregate is responsible for creating the AttendeeEntity and the AttendeeRegisteredEvent.

**_Commands_** we use a Command object, RegisterAttendeeCommand, to encapsulate the data needed to register an attendee. Commands are different from Events because Commands can fail or be rejected, while Events are statements of fact that have already happened.

**_Events_** we use an Event, AttendeeRegisteredEvent, to notify other parts of the system that an attendee has been registered. Events are statements of fact that have already happened and cannot be changed.
