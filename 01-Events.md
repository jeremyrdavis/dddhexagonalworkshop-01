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

