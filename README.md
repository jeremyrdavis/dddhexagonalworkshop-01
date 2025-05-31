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

1. Events: Create an `AttendeeRegisteredEvent` that records the important functionality in this subdomain and can be used to integrate with the rest of the system.
2. Commands: Commands trigger events.  Create a `RegisterAttendeeCommand` that triggers the registration workflow.
3. Returning Multiple Objects: Nothing to do with DDD here just a useful way to return multiple objects from a method invocation. Create a class, `AttendeeRegistrationResult`.
4. Aggregates: Implement an Aggregate, `Attendee`.  Aggregates are at the heart of DDD.
5. Entities: create an Entity, `AttendeeEntity`, to persist instances of the `Attendee` aggregate in a database.
6. Repositories: create a Repository, `AttendeeRepository`, that defines methods for saving and retrieving attendees.
5. Incomoming Adapters: Implement an `Attendee` Aggregate to isolate invariants (business logic) from the rest of the application.
6. Outgoing Adapters: Implement an `AttendeeEventPublisher` that sends events to Kafka to propagate changes to the rest of the system.
7. Data Transfer Objects (DTOs): Create an `AttendeeDTO` to transfer data between the REST endpoint and the domain model.
8. Domain Services: Implement a Domain Service, `AttendeeService`, to orchestration the registration process.

By the end of Iteration 1, you'll have a solid foundation in DDD concepts and a very basic working application.

Let's get coding!

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

❌ Business logic scattered everywhere

```java
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

## Step 6: Repositories

#### Learning Objectives

- Understand the Repository pattern as the bridge between domain and persistence
- Implement AttendeeRepository that converts between aggregates and entities
- Apply domain-driven persistence patterns while maintaining clean architecture
- Connect domain aggregates to database storage through proper abstraction layers

### What You'll Build

An AttendeeRepository that handles all persistence operations for attendees, converting between domain Attendee aggregates and persistence AttendeeEntity objects while maintaining clean separation of concerns.

### Why Repositories Are Essential in DDD

Repositories solve the fundamental problem of how domain objects interact with persistence without being contaminated by database concerns:
The Persistence Pollution Problem: Without repositories, domain logic gets mixed with database code:

❌ Domain service polluted with persistence concerns

```java

@ApplicationScoped
public class AttendeeService {
@Inject EntityManager entityManager;

    public AttendeeDTO registerAttendee(RegisterAttendeeCommand command) {
        // Domain logic mixed with JPA code
        AttendeeEntity entity = new AttendeeEntity(command.email());
        entityManager.persist(entity);  // Database concerns in domain service
        entityManager.flush();
        
        // More JPA code mixed with business logic
        return new AttendeeDTO(entity.getEmail());
    }
}
```

The Repository Solution: Repositories encapsulate all persistence logic:

✅ Clean separation through repository pattern

```java
@ApplicationScoped
public class AttendeeService {
@Inject AttendeeRepository attendeeRepository;  // Domain interface

    public AttendeeDTO registerAttendee(RegisterAttendeeCommand command) {
        // Pure domain logic
        AttendeeRegistrationResult result = Attendee.registerAttendee(command.email());
        
        // Repository handles all persistence concerns
        attendeeRepository.persist(result.attendee());
        
        return new AttendeeDTO(result.attendee().getEmail());
    }
}
```

### Repository Pattern: Core Concepts

**Collection Abstraction:** Repositories make persistence feel like working with an in-memory collection of domain objects. You add, remove, and find aggregates without thinking about SQL or database details.

**Domain Interface:** The repository interface is defined in the domain layer, expressing operations in business terms, not database terms.

**Implementation Separation:** The actual persistence implementation can use JPA, MongoDB, files, or any other technology without affecting domain code.

**Aggregate Boundary:** Each aggregate root gets its own repository. You don't have repositories for every entity - only for aggregates you need to retrieve independently.

### Deep Dive: Repository Responsibilities vs Other Patterns

#### Repository vs Data Access Object (DAO)

| Aspect | Repository (DDD) | DAO (Data Access) |
|--------|------------------|-------------------|
| Focus | Domain aggregates | Database tables/entities |
| Interface | Business operations | CRUD operations |
| Conversion | Aggregate ⟷ Entity | Entity ⟷ Database |
| Query Language | Domain concepts | SQL/Database terms |
| Scope | Per aggregate root | Per table/entity |

#### Repository Example (Domain-focused):

```java
public interface AttendeeRepository {
    void persist(Attendee attendee);                    // Business operation
    Optional<Attendee> findByEmail(String email);       // Business query
    List<Attendee> findRegisteredAttendees();           // Business concept
    void remove(Attendee attendee);                     // Business operation
}
```

#### DAO Example (Database-focused):

```java
public interface AttendeeDAO {
    void insert(AttendeeEntity entity);                 // Database operation
    AttendeeEntity selectById(Long id);                 // Database query
    List<AttendeeEntity> selectAll();                   // Database operation
    void update(AttendeeEntity entity);                 // Database operation
    void delete(Long id);                               // Database operation
}
```

#### Repository vs Service Layer

| Aspect | Repository | Service |
|--------|------------|---------|
| Purpose | Persistence abstraction | Business workflow orchestration |
| Scope | Single aggregate type | Cross-aggregate operations |
| Dependencies | Database/ORM only | Repositories, external services |
| Transaction | Usually single operations | Often manages transactions |
| Domain Events | Not responsible for events | Publishes domain events |


### What Belongs in Repository:

✅ Persistence operations for Attendee aggregate

```java
@ApplicationScoped
public class AttendeeRepository {
    public void persist(Attendee attendee) { ... }
    public Optional<Attendee> findByEmail(String email) { ... }
    public void remove(Attendee attendee) { ... }
}
```

#### What Belongs in Service:

✅ Business workflow orchestration

```java
    @ApplicationScoped  
public class AttendeeService {

    public AttendeeDTO registerAttendee(RegisterAttendeeCommand command) {
        AttendeeRegistrationResult result = Attendee.registerAttendee(command.email());
        attendeeRepository.persist(result.attendee());              // Use repository
        attendeeEventPublisher.publish(result.attendeeRegisteredEvent()); // Publish events
        return new AttendeeDTO(result.attendee().getEmail());
    }
}
```
#### Repository vs Active Record

| Aspect | Repository Pattern | Active Record Pattern |
| ------ | ----------------- | -------------------- |
| Object Responsibility | Aggregate = business logic only | Object = business logic + persistence |
| Testability | Easy to mock/stub | Harder to test (database coupled) |
| Separation of Concerns | Clean separation | Mixed concerns |
| Framework Dependency | Isolated in repository | Throughout domain objects |

**Repository Pattern (Separation):**

```java
// Domain aggregate - pure business logic
public class Attendee {
public static AttendeeRegistrationResult registerAttendee(String email) {
// Only business logic, no persistence
}
}

// Repository - handles persistence separately
public class AttendeeRepository {
public void persist(Attendee attendee) { ... }
}
```

**Active Record Pattern (Mixed):**

```java
// Active Record - business logic + persistence mixed
public class Attendee extends ActiveRecord {
    public static AttendeeRegistrationResult registerAttendee(String email) {
        Attendee attendee = new Attendee(email);
        attendee.save();  // Persistence mixed with business logic
        return result;
    }
}
```

### Implementation

Repositories represent all objects of a certain type as a conceptual set (usually emulated). They act like collections, except with more elaborate querying capability. Objects of the appropriate type are added and removed, and the machinery behind the repository inserts them or deletes them from the database.

```java
package dddhexagonalworkshop.conference.attendees.persistence;

import dddhexagonalworkshop.conference.attendees.domain.aggregates.Attendee;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
* Repository for managing Attendee aggregate persistence.
*
* This class bridges between the domain model (Attendee) and the persistence
* model (AttendeeEntity), handling all conversion and database operations.
* The repository implements the domain's persistence interface while using
* Quarkus Panache for the actual database operations.
  */
  @ApplicationScoped
  public class AttendeeRepository implements PanacheRepository<AttendeeEntity> {

  /**
    * Persists an Attendee aggregate to the database.
    * Converts the domain aggregate to a persistence entity and saves it.
    *
    * @param aggregate The Attendee domain aggregate to persist
      */
      public void persist(Attendee aggregate) {
      // Convert domain aggregate to persistence entity
      AttendeeEntity attendeeEntity = fromAggregate(aggregate);

      // Use inherited Panache method to persist
      persist(attendeeEntity);
      }

  /**
    * Finds an attendee by their email address.
    * Returns a domain aggregate, not a persistence entity.
    *
    * @param email The email address to search for
    * @return Optional containing the Attendee aggregate if found
      */
      public Optional<Attendee> findByEmail(String email) {
      // Query using persistence entity
      Optional<AttendeeEntity> entityOpt = find("email", email).firstResultOptional();

      // Convert persistence entity back to domain aggregate
      return entityOpt.map(this::toAggregate);
      }

  /**
    * Removes an attendee from the database.
    *
    * @param attendee The Attendee aggregate to remove
      */
      public void remove(Attendee attendee) {
      // Find the corresponding entity and delete it
      find("email", attendee.getEmail())
      .firstResultOptional()
      .ifPresent(this::delete);
      }

  /**
    * Converts a domain Attendee aggregate to an AttendeeEntity for persistence.
    * This is where domain concepts are mapped to database structures.
    *
    * @param attendee The domain aggregate
    * @return The persistence entity
      */
      private AttendeeEntity fromAggregate(Attendee attendee) {
      return new AttendeeEntity(attendee.getEmail());
      }

  /**
    * Converts an AttendeeEntity from the database to a domain Attendee aggregate.
    * This reconstitutes the domain object from persisted data.
    *
    * @param entity The persistence entity
    * @return The domain aggregate
      */
      private Attendee toAggregate(AttendeeEntity entity) {
      // Note: In a real system, you might need a factory method on Attendee
      // to reconstruct from persisted state, since registerAttendee() is for new attendees
      return Attendee.fromPersistedData(entity.getEmail());
      }
      }

```

### Key Design Decisions

**Aggregate-to-Entity Conversion:** The fromAggregate() and toAggregate() methods handle all conversion logic, keeping domain and persistence models separate.

**Domain Interface:** Methods are named using business terminology (findByEmail, persist) rather than database terminology (selectByEmail, insert).

**Error Handling:** Repository methods throw domain exceptions, not database exceptions, maintaining the abstraction.

**Panache Integration:** We extend PanacheRepository<AttendeeEntity> to get basic CRUD operations while adding our domain-specific methods.

**Conversion Responsibility:** The repository is responsible for all conversion between domain and persistence models.

### Next Steps

In the next section we will create a second Outbound Adaper, `AttendeeEventPublisher` to send messages to the rest of the system.

## Step 7: Outbound Adapters for Events

### Learning Objectives
- **Understand** Outbound Adapters as the bridge between domain events and external systems
- **Implement** AttendeeEventPublisher to send domain events to Kafka
- **Apply** Hexagonal Architecture principles to decouple event publishing from business logic
- **Connect** domain events to external messaging systems while maintaining clean boundaries

### What You'll Build
An `AttendeeEventPublisher` adapter that publishes `AttendeeRegisteredEvent` domain events to Kafka, enabling other bounded contexts and systems to react to attendee registrations.

### Why Outbound Adapters Are Critical

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

### Hexagonal Architecture: Ports and Adapters Deep Dive

Understanding the relationship between Ports and Adapters is crucial for proper implementation:

#### Ports vs Adapters: Core Concepts

| Aspect | Port | Adapter |
|--------|------|---------|
| **Definition** | Interface/Contract | Implementation |
| **Location** | Domain layer | Infrastructure layer |
| **Purpose** | Define what operations are needed | Implement how operations work |
| **Dependency Direction** | Domain defines ports | Adapters depend on ports |
| **Technology** | Technology agnostic | Technology specific |

#### Inbound vs Outbound Adapters

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

#### Event Publishing Patterns Comparison

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

### Implementation

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

## Step 8: Domain Services

### Learning Objectives
- **Understand** Domain Services as workflow orchestrators in the domain layer
- **Implement** AttendeeService to coordinate registration business operations
- **Apply** proper separation between domain services and application services
- **Connect** all DDD components through clean service orchestration

### What You'll Build
An `AttendeeService` that orchestrates the complete attendee registration workflow, coordinating between aggregates, repositories, and event publishers while maintaining clean domain boundaries.

### Why Domain Services Are Essential

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

### Domain Services vs Other Service Types: Deep Dive

Understanding the different types of services and their responsibilities is crucial for proper DDD implementation:

#### Service Type Comparison

| Aspect | Domain Service | Application Service | Infrastructure Service |
|--------|----------------|-------------------|----------------------|
| **Layer** | Domain | Application | Infrastructure |
| **Purpose** | Business workflow orchestration | Use case coordination | Technical operations |
| **Dependencies** | Domain objects only | Domain + Infrastructure | External systems |
| **Business Logic** | Contains business rules | Minimal business logic | No business logic |
| **Transaction Scope** | Often transactional | Manages transactions | Participates in transactions |
| **Testing** | Domain-focused unit tests | Integration tests | Technical integration tests |

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

#### Domain Service vs Aggregate: Responsibility Boundaries

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

#### Transaction Management Patterns

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

### Key Design Decisions

**Single Responsibility**: Each method has a clear, single purpose - registration, lookup, or cancellation.

**Transaction Boundaries**: `@Transactional` ensures data consistency across repository operations and event publishing.

**Error Handling**: Domain-specific exceptions provide meaningful error messages and maintain abstraction boundaries.

**Logging Strategy**: Structured logging for operational visibility and debugging.

**Separation of Concerns**: Private methods separate validation, persistence, and event publishing concerns.

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

## Step 9: Data Transfer Objects (DTOs)

### Learning Objectives
- **Understand** DTOs as the boundary between domain and presentation layers
- **Implement** AttendeeDTO for JSON serialization in REST responses
- **Apply** proper separation between domain models and external representations
- **Connect** domain services to REST endpoints through well-designed data contracts

### What You'll Build
An `AttendeeDTO` record that represents attendee data for JSON serialization, providing a stable external API contract independent of internal domain model changes.

### Why DTOs Are Essential

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

### DTOs vs Other Data Representation Patterns: Deep Dive

Understanding different approaches to data representation helps choose the right pattern for each scenario:

#### Data Representation Pattern Comparison

| Pattern | Purpose | Layer | Mutability | Serialization | Use Case |
|---------|---------|-------|------------|---------------|----------|
| **Domain Aggregate** | Business logic & state | Domain | Controlled by business rules | Not intended for external use | Core business operations |
| **Persistence Entity** | Database mapping | Infrastructure | ORM-managed | Database-specific formats | Data storage & retrieval |
| **Data Transfer Object** | External data contracts | Presentation | Immutable | JSON/XML optimized | API responses & requests |
| **View Model** | UI-specific data | Presentation | UI-framework specific | UI binding formats | User interface rendering |
| **Event Payload** | Inter-service communication | Infrastructure | Immutable | Message-specific formats | Async messaging |

#### DTO Types and Responsibilities

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

#### DTO Design Patterns

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

### Implementation

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



## Step 10: Wrapping Up With an Inbound Adapter (REST Endpoint)

### Learning Objectives
- **Understand** Inbound Adapters as the entry point for external requests into the domain
- **Implement** AttendeeEndpoint as a REST adapter using JAX-RS
- **Apply** Hexagonal Architecture principles to decouple HTTP concerns from business logic
- **Complete** the end-to-end DDD workflow from HTTP request to domain operation

### What You'll Build
An `AttendeeEndpoint` REST controller that serves as the inbound adapter, handling HTTP requests, delegating to domain services, and returning JSON responses while maintaining clean architectural boundaries.

### Why Inbound Adapters Are Critical

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

### Hexagonal Architecture: Inbound vs Outbound Deep Dive

Understanding the flow of data through hexagonal architecture is crucial for proper adapter implementation:

#### Adapter Flow Patterns

| Flow Type | Direction | Purpose | Examples | Initiator |
|-----------|-----------|---------|----------|-----------|
| **Inbound (Primary)** | External → Domain | Receive requests | REST, GraphQL, CLI, Events | External systems |
| **Outbound (Secondary)** | Domain → External | Send commands/queries | Database, Messaging, Email | Domain logic |

#### Inbound Adapter Responsibilities

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

##### REST Endpoint Patterns Comparison

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

### Implementation

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
