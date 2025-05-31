# Step 2: Commands

## TL;DR

Add a String email parameter to RegisterAttendeeCommand:

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

_Note_: We are only adding a single parameter, "email," because this first iteration is a quick overview.

## Learning Objectives
- Understand how Commands encapsulate business intentions and requests for action
- Distinguish between Commands (can fail) and Events (facts that occurred)
- Implement a RegisterAttendeeCommand to capture attendee registration requests

## What We Are Building

A `RegisterAttendeeCommand` record that encapsulates all the data needed to request attendee registration for the conference.

## Why Commands Matter

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

_Note_: Validation in a command should be lightweight and focused on ensuring the command is valid before processing.  Complex validation logic driven by business rules should be handled in the Aggregate.  For example, it is fine to validate that the email is not blank or does not contain an '@' symbol, but more complex rules like checking if the attendee is already registered should be handled in the `Attendee` aggregate.

- Immutability: Commands are immutable objects that can't be accidentally modified as they pass through your system. This prevents bugs and makes the code easier to reason about.
- Failure Handling: Unlike events (which represent facts), commands can be rejected. Your business logic can validate a command and decide whether to process it or reject it with a clear error message.

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

## Deeper Dive

### Commands vs Events: A Critical Distinction

| Aspect         | Commands                | Events                    |
|----------------|----------------------------------------|------------|
| **Nature**     | Intention/Request       | Fact/What happened        |
| **Can fail?**  | Yes                     | No (already happened)     |
| **Mutability** | Immutable               | Immutable                 |
| **Tense**      | Imperative ("Register") | Past tense ("Registered") |
| **Example**    | RegisterAttendeeCommand | AttendeeRegisteredEvent |

Think of it like ordering food:

- **Command**: "I want to order a burger" (restaurant might be out of burgers)

- **Event**: "Customer ordered a burger at 2:15 PM" (this definitely happened)


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
