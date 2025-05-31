# Step 5: Entities

In Domain-Driven Design, all persistence is handled by repositories, but before we create the repository, we need a persistence entity. Entities represent specific instances of domain objects with database identities.

## tl;dr
Complete the `AttendeeEntity` class with the following code:

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

## Learning Objectives

- Understand the difference between Domain Aggregates and Persistence Entities
- Implement AttendeeEntity for database persistence using JPA annotations
- Apply the separation between domain logic and persistence concerns
- Connect domain aggregates to database storage through persistence entities

## What We Are Building

An AttendeeEntity JPA entity that represents how attendee data is stored in the database, separate from the domain logic in the Attendee aggregate.

## Why Entities Matter

- Technology Independence: Your domain model isn't tied to any specific database or ORM framework. You could switch from JPA to MongoDB without changing your business logic.
- Testing Simplicity: Domain logic can be tested without database setup, while persistence logic can be tested separately with database integration tests.
- Evolution Independence: Database schema changes don't require changes to domain logic, and business rule changes don't require database migrations.

Here is an example:

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

## Implementation

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

## Key Design Decisions

**Protected Constructors:** The default constructor is required by JPA, while the parameterized constructor allows controlled creation. Both are protected to limit access.

**Protected Methods:** Getters and setters are protected, not public. Only the repository layer should interact with entities directly.

**No Business Logic:** The entity contains no validation or business rules - that's the aggregate's responsibility.

**Simple Mapping:** We start with basic column mapping. Real applications might have more complex relationships and constraints.

## A Deeper Dive into Entities

### Entities vs Aggregates: Key Differences

Understanding the distinction between Domain Aggregates and Persistence Entities is crucial for proper DDD implementation. Here's a detailed comparison:

| Aspect      | Domain Aggregate               | Persistence Entity           |
| ----------- | ------------------------------ | ---------------------------- |
| Purpose     | Business logic & rules         | Data storage mapping         |
| Dependencies| Pure Java, domain concepts     | JPA, database annotations    |
| Identity    | Business identity (email)      | Technical identity (database ID) |
| Lifecycle   | Created by business operations | Created/loaded by ORM        |
| Mutability  | Controlled by business rules   | Managed by persistence framework |
| Testing     | Unit tests, no database        | Integration tests with database |

## Purpose and Responsibility

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

## Why This Separation Matters

**Flexibility:** Business logic can evolve independently of persistence technology. You could switch from JPA to MongoDB without changing domain code.
**Testability:** Business logic can be tested quickly without database setup, while persistence logic gets thorough integration testing.
**Performance:** Persistence entities can be optimized for database access patterns without compromising domain model clarity.
**Team Organization:** Domain experts can focus on aggregates, while database specialists optimize entities.
**Technology Evolution:** Framework updates or database changes don't ripple into business logic.


## Testing Your Implementation

We will test the `AttendeeEntity` in the `AttendeeRepositoryTest.java` class, which will build in the next step.

## Common Questions

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

