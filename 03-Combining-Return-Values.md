# Step 3: Combining Return Values

***Note:*** This step is not specific to Domain Driven Design.  This is simply a useful coding practice.

## TL;DR

Update AttendeeRegistrationResult so that it is constructed with both the Attendee aggregate and the AttendeeRegisteredEvent:

```java
package dddhexagonalworkshop.conference.attendees.domain.services;

import dddhexagonalworkshop.conference.attendees.domain.aggregates.Attendee;
import dddhexagonalworkshop.conference.attendees.domain.events.AttendeeRegisteredEvent;

/**
 * This object is used to return the result of an attendee registration and contains the objects created by the Aggregate.
 */
public record AttendeeRegistrationResult(Attendee attendee, AttendeeRegisteredEvent attendeeRegisteredEvent) {
}
```

## Learning Objectives

- Understand how cleanly package multiple outputs from domain operations
- Implement AttendeeRegistrationResult to encapsulate both domain state and events

## What We Are Building

An AttendeeRegistrationResult Record that packages together both the created Attendee aggregate and the AttendeeRegisteredEvent that needs to be published.

## Why Combining Return Values Matters
 
- Multiple Return Values: Operations often need to return more than one thing. When an attendee registers, we need to create both the Attendee, `Attendee`, (the domain state that represents the attendee), and an event, `AttendeeRegisteredEvent`, (to publish to other systems)

Let's create an object that holds both the Attendee aggregate and the AttendeeRegisteredEvent that were created during the registration process.

```java
package dddhexagonalworkshop.conference.attendees.domain.services;

import dddhexagonalworkshop.conference.attendees.domain.aggregates.Attendee;
import dddhexagonalworkshop.conference.attendees.domain.events.AttendeeRegisteredEvent;

/**
 * This object is used to return the result of an attendee registration and contains the objects created by the Aggregate.
 */
public record AttendeeRegistrationResult(Attendee attendee, AttendeeRegisteredEvent attendeeRegisteredEvent) {
}
```
***Note:*** The `AttendeeRegistrationResult` will not compile until we implement the `Attendee` aggregate. This is intentional to guide you through the process of building the domain model step by step.  It will compile after the next step when we implement the `Attendee` aggregate.

## Key Design Decisions

**Why a record?** Records are perfect for result objects because:
- They're immutable (results shouldn't change after creation)
- Component accessors are automatically generated

**Package placement?** Result objects live with the service that uses them, since they're part of the service's API contract.
**Naming convention?** Follow the pattern [Operation]Result (e.g., AttendeeRegistrationResult, PaymentProcessingResult).

## Testing Your Implementation
The `AttendeeRegistrationResult` record is tested indirectly through the `AttendeeService` tests. Once you implement the `Attendee` aggregate in the next step, the `AttendeeRegistrationResult` will be used in the service layer, and you can run the tests to verify its functionality.

