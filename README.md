# Iteration 01: End to end DDD

## DDD Concepts: Commands, Events, Aggregates, Domain Services, Repositories, Entities

### Modules

1. [Events](01-Events.md)
2. [Commands](02-Commands.md)
3. [Combining Return Values](03-Combining-Return-Values.md)
4. [Aggregates](04-Aggregate.md)
4. [Domain Service](06-Domain-Service.md)
5. [Command Pattern](07-Command-Pattern.md)
6. [Repository](08-Repository.md)
7. [Data Transfer Objects](09-DTOs.md)
8. [Inbound Adapter](10-Inbound-Adapter.md)

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
