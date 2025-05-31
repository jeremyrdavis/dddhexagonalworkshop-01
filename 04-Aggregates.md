# Step 4: Aggregates

## TL;DR

Implement the `Attendee` aggregate:

```java
package dddhexagonalworkshop.conference.attendees.domain.aggregates;

import dddhexagonalworkshop.conference.attendees.domain.events.AttendeeRegisteredEvent;
import dddhexagonalworkshop.conference.attendees.domain.services.AttendeeRegistrationResult;

/**
 * "An AGGREGATE is a cluster of associated objects that we treat as a unit for the purpose of data changes. Each AGGREGATE has a root and a boundary. The boundary defines what is inside the AGGREGATE. The root is a single, specific ENTITY contained in the AGGREGATE. The root is the only member of the AGGREGATE that outside objects are allowed to hold references to, although objects within the boundary may hold references to each other."
 * Eric Evans, Domain-Driven Design: Tackling Complexity in the Heart of Software, 2003
 */
public class Attendee {

    String email;

    private Attendee(String email) {
    }

    public static AttendeeRegistrationResult registerAttendee(String email) {
        // Here you would typically perform some business logic, like checking if the attendee already exists
        // and then create an event to publish.
        Attendee attendee = new Attendee(email);
        AttendeeRegisteredEvent event = new AttendeeRegisteredEvent(email);
        return new AttendeeRegistrationResult(attendee, event);
    }

    public String getEmail(){
        return email;
    }
}
```


## Learning Objectives

- Understand Aggregates as the core building blocks of Domain-Driven Design
- Implement the Attendee aggregate with business logic and invariant enforcement
- Apply the concept of aggregate roots and consistency boundaries
- Connect Commands, business logic, and Result objects through aggregate methods

## What We Are Building

An Attendee aggregate that encapsulates the business logic for attendee registration and maintains consistency within the attendee bounded context.

## Why Aggregates ~~Matter~~ Are the Heart of DDD

_Aggregates solve the most critical problem in business software: where does the business logic live?_

Domain-Driven Design exists to solve a fundamental problem: where does the business logic live? In many (if not most) applications, business rules get scattered across layers, making them impossible to find, understand, or change safely. Aggregates solve this by creating a single, authoritative home for all business logic related to a specific business concept.  Every business operation flows through aggregates, every business rule is enforced by aggregates, and every significant business state change originates from aggregates.

## Implementation

Aggregates are the core objects in Domain-Driven Design. An Aggregate represents the most important object or objects in our Bounded Context. We're implementing the Attendees Bounded Context, and the Attendee is the most important object in this context.
An Aggregate both represents the real-world object (a conference attendee in this case) and encapsulates all of the invariants, or business logic, associated with the object.

```java
package dddhexagonalworkshop.conference.attendees.domain.aggregates;

import dddhexagonalworkshop.conference.attendees.domain.events.AttendeeRegisteredEvent;
import dddhexagonalworkshop.conference.attendees.domain.services.AttendeeRegistrationResult;

/**
 * "An AGGREGATE is a cluster of associated objects that we treat as a unit for the purpose of data changes. Each AGGREGATE has a root and a boundary. The boundary defines what is inside the AGGREGATE. The root is a single, specific ENTITY contained in the AGGREGATE. The root is the only member of the AGGREGATE that outside objects are allowed to hold references to, although objects within the boundary may hold references to each other."
 * Eric Evans, Domain-Driven Design: Tackling Complexity in the Heart of Software, 2003
 */
public class Attendee {

    String email;

    private Attendee(String email) {
    }

    public static AttendeeRegistrationResult registerAttendee(String email) {
        // Here you would typically perform some business logic, like checking if the attendee already exists
        // and then create an event to publish.
        Attendee attendee = new Attendee(email);
        AttendeeRegisteredEvent event = new AttendeeRegisteredEvent(email);
        return new AttendeeRegistrationResult(attendee, event);
    }

    public String getEmail(){
        return email;
    }
}
```

##  Key Design Decisions

**Static Factory Method:** registerAttendee() is static because it represents creating a new attendee, not operating on an existing one. This is a common pattern for aggregate creation.

**Private Constructor:** The constructor is private to force all creation through the factory method, ensuring business rules are always applied.

**Business Logic Location:** All validation and business rules are in the aggregate, not scattered across services or controllers.

### Testing Your Implementation
After implementing the aggregate, verify it works correctly:

```bash
mvn test -Dtest=AttendeeTest
```

## Deeper Dive

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

