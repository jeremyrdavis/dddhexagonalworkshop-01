# Workshop Workflow

## Iteration 01: End to end DDD
### DDD Concepts: Commands, Events, Aggregates, Domain Services, Repositories, Entities

### Overview

**Introduction:**
In this iteration, we will cover the basics of Domain-Driven Design by implementing a basic workflow for registering a conference attendee. We will create the following DDD constructs:
- Aggregate
- Domain Service
- Domain Event
- Command
- Adapter
- Entity
- Repository

We will alss use the `Hexangonal Architecture`, or `Ports and Adapters` pattern to integrate with external systems, ensuring a clean separation of concerns.

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
As you progress through the workshop, you will fill in the missing pieces of code in the appropriate packages.  The workshop authors have stubbed out the classes so that you can focus on the Domain Driven Design concepts as much as possible and Java and framework concepts as little as possilb. 
You can type in the code line by line or copy and paste the code provided into your IDE. You can also combine the approaches as you see fit. The goal is to understand the concepts and how they fit together in a DDD context.


**Quarkus**

Quarkus, https://quarkus.io, is a modern Java framework designed for building cloud-native applications. It provides a set of tools and libraries that make it easy to develop, test, and deploy applications. In this workshop, we will leverage Quarkus to implement our DDD concepts and build a RESTful API for registering attendees.
The project uses Quarkus, a Java framework that provides built-in support for REST endpoints, JSON serialization, and database access.  Quarkus also features a `Dev Mode` that automatically spins up external dependencies like Kafka and PostgreSQL, allowing you to focus on writing code without worrying about the underlying infrastructure.

**Steps:**

In this first iteration, we will implement the basic workflow for registering an attendee. The steps are as follows:

1. Create a `RegisterAttendeeCommand` with only one, basic property (email).
2. Implement an Adapter in the form of a REST Endpoint, `AttendeeEndpoint` with a POST method.
3. Implement a Service, `AttendeeService` that will orchestration the registration process.
4. Create an `Attendee` entity that represents the attendee in the domain and implements the application's invariants or business rules.
4. Create a Domain Event, `AttendeeRegisteredEvent`, that will be published when an attendee is successfully registered.
5. Create a Repository interface, `AttendeeRepository`, that defines methods for saving and retrieving attendees.
6. Create an Entity, `AttendeeEntity`, to persist instances of the `Attendee` entity in a database.
7. Create an Adapter, `AttendeeEventPublisher`, that sends events to Kafka to propagate changes to the rest of the system.

By the end of Iteration 1, you'll have a solid foundation in DDD concepts and a very basic working application.

Let's get coding!

### Step 1: Commands

Commands are objects that encapsulate a request to perform an action. Commands are immutable and can fail or be rejected.  Commands are closely related to Events, which we will cover later. The difference between the two is that Commands represent an intention to change the state of the system, while Events are statements of fact that have already happened.

We will start by creating a command to register an attendee. This command will encapsulate the data needed to register an attendee, which in this iteration is just the email address.

The `RegisterAttendeeCommand` can be found in the `dddhexagonalworkshop.conference.attendees.domain.services` package because it is part of AttendeeService's API. We will implement the `AttendeeService` later; for now, we will just create the command.

Update the RegisterAttendeeCommand object with a single String, "email."

```java
package dddhexagonalworkshop.conference.attendees.domain.services;

public record RegisterAttendeeCommand(String email) {
}

```

### Adapters

The `Ports and Adapters` pattern, also known as `Hexagonal Architecture`, is a design pattern that separates the core business logic from external systems. The phrase was coined by Alistair Cockburn in the early 2000s.  

The pattern fits well with Domain-Driven Design (DDD) because it allows the domain model to remain pure and focused on business logic, while the adapters handle the technical details.  This allows the core application to remain independent of the technologies used for input/output, such as databases, message queues, or REST APIs.  

Adapters are components that translate between the domain model and external systems or frameworks. In the context of a REST endpoint, an adapter handles the conversion of HTTP requests to commands that the domain model can process, and vice versa for responses. We don't need to manually convert the JSON to and from Java objects, as Quarkus provides built-in support for this using Jackson.

Complete the AttendeeEndpoint in the `dddhexagonalworkshop.conference.attendees.infrastructure` package.

```java
package dddhexagonalworkshop.conference.attendees.infrastructure;

import dddhexagonalworkshop.conference.attendees.api.AttendeeDTO;
import dddhexagonalworkshop.conference.attendees.dddhexagonalworkshop.conference.attendees.domain.services.AttendeeService;
import dddhexagonalworkshop.conference.attendees.dddhexagonalworkshop.conference.attendees.domain.services.RegisterAttendeeCommand;
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

### Data Transfer Objects (DTOs)

We also need to create a simple DTO (Data Transfer Object) to represent the attendee in the response. Data Transfer Objects are used to transfer data between layers, especially when the data structure is different from the domain model, which is why we are using it here.

```java
package dddhexagonalworkshop.conference.attendees.infrastrcture;

public record AttendeeDTO(String email) {
}
```

### Domain Services

- Create the AttendeeService in the attendes/domain/services package
    - create one method, "registerAttendee" that takes a RegisterAttendeeCommand

```java
package domain.services;

import dddhexagonalworkshop.conference.attendees.infrastrcture.AttendeeDTO;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AttendeeService {

    public AttendeeDTO registerAttendee(RegisterAttendeeCommand registerAttendeeAttendeeCommand) {
        // Logic to register an attendee
        // This is a placeholder implementation
        return new AttendeeDTO(registerAttendeeAttendeeCommand.email());
    }
}

```

### Entities

- Create the AttendeeEntity in the attendees/persistence package
    - only one field, "email"

```java
package dddhexagonalworkshop.conference.attendees.persistence;

import dddhexagonalworkshop.conference.attendees.api.AddressDTO;
import dddhexagonalworkshop.conference.attendees.domain.valueobjects.Badge;
import jakarta.persistence.*;

@Entity @Table(name = "attendee")
public class AttendeeEntity {

    @Id @GeneratedValue
    private Long id;

    private String email;

    protected AttendeeEntity() {

    }

    protected AttendeeEntity(String email) {
        this.email = email;
    }

    protected Long getId() {
        return id;
    }

    protected String getEmail() {
        return email;
    }

}
```

### Repositories

- Create the AttendeeRegistredEvent in the domain/events package
    - create a single field, "email"

```java
package dddhexagonalworkshop.conference.attendees.domain.events;

public record AttendeeRegisteredEvent(String email) {
}
```

- Create the AttendeeRegistrationResult in the attendees/domain/services package
    - create two fields, "attendee" and "attendeeRegistrationEvent"

```java
package dddhexagonalworkshop.conference.attendees.domain.services;

import dddhexagonalworkshop.conference.attendees.domain.aggregates.Attendee;
import dddhexagonalworkshop.conference.attendees.domain.events.AttendeeRegisteredEvent;

public record AttendeeRegistrationResult(Attendee attendee, AttendeeRegisteredEvent attendeeRegisteredEvent) {
}
```

- Create the Attendee Aggregate in attendees/domain/aggregates
    - create a single method, "registerAttendee"
    - implement the method
        - by creating an AttendeeEntity and an AttendeeRegistredEvent
        - Create the AttendeeRegistrationResult in the attendees/domain/services package to return the AttendeeEntity and AttendeeRegisteredEvent
```java
package dddhexagonalworkshop.conference.attendees.domain;

public class Attendee {

  String email;

  public static Attendee registerAttendee(String email) {
    Attendee attendee = new Attendee();
    attendee.email = email;
    return attendee;
  }
  
  public String getEmail(){
    return email;
  }
}

```

- Create the AttendeeRepository using Hibernate Panache

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

- Create the AttendeeEventPublisher
    - create a single method, "publish" that takes an AttendeeRegisteredEvent
    - implement the method by sending the event to Kafka

```java
package dddhexagonalworkshop.conference.attendees.infrastrcture;

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

- Update the AttendeeService so that it persists the attendee and publishes the event
    - update the registerAttendee method to return an AttendeeRegistratedResult
    - update the registerAttendee method to call the AttendeeEventPublisher

```java
package dddhexagonalworkshop.conference.attendees.domain.services;

import dddhexagonalworkshop.conference.attendees.domain.aggregates.Attendee;
import dddhexagonalworkshop.conference.attendees.infrastrcture.AttendeeDTO;
import dddhexagonalworkshop.conference.attendees.infrastrcture.AttendeeEventPublisher;
import dddhexagonalworkshop.conference.attendees.persistence.AttendeeRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AttendeeService {

  @Inject
  AttendeeRepository attendeeRepository;

  @Inject
  AttendeeEventPublisher attendeeEventPublisher;

  @Transactional
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

Update the AttendeeEndpoint to return the AttendeeDTO

```java
package dddhexagonalworkshop.conference.attendees.infrastrcture;

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

## Summary
In this first iteration, we have created the basic structure of the Attendee registration micorservice.

### Key points
***Hexagonal Architecture/Ports and Adapters***: The AttendeeEndpoint is a _Port_ for the registering attendees.  In our case the _Adaper_ is the Jackson library, which is built into Quarkus, and handles converting JSON to Java objects and vice versa.  
The AttendeeEventPubliser is also an Adapter that sends events to Kafka, which is another Port in our architecture.  
The AttendeeRepository is a Port that allows us to persist the AttendeeEntity to a database.

***Aggregates*** Business logic is implemented in an Aggregate, Attendee. The Aggregate is responsible for creating the AttendeeEntity and the AttendeeRegisteredEvent.

***Commands*** we use a Command object, RegisterAttendeeCommand, to encapsulate the data needed to register an attendee.  Commands are different from Events because Commands can fail or be rejected, while Events are statements of fact that have already happened.

***Events*** we use an Event, AttendeeRegisteredEvent, to notify other parts of the system that an attendee has been registered.  Events are statements of fact that have already happened and cannot be changed.
