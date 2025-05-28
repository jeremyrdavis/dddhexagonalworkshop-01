# Workshop Workflow

## Iteration 01: End to end DDD
### DDD Concepts: Commands, Events, Aggregates, Domain Services, Repositories, Entities

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
3. Implement a Data Transfer Object, `AttendeeDTO`, to return the attendee's details to the UI.
4. Implement a Domain Service, `AttendeeService`, to orchestration the registration process.
5. Implement an `Attendee` Aggregate to isolate invariants (business logic) from the rest of the application.
6. Implement a Domain Event, `AttendeeRegisteredEvent`, that will be published when an attendee is successfully registered.
7. Implement a Repository interface, `AttendeeRepository`, that defines methods for saving and retrieving attendees.
8. Create an Entity, `AttendeeEntity`, to persist instances of the `Attendee` entity in a database.
9. Create an Adapter, `AttendeeEventPublisher`, that sends events to Kafka to propagate changes to the rest of the system.

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

### Step 2: Adapters

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

### Step 3: Data Transfer Objects (DTOs)

We also need to create a simple DTO (Data Transfer Object) to represent the attendee in the response. Data Transfer Objects are used to transfer data between layers, especially when the data structure is different from the domain model, which is why we are using it here.

```java
package dddhexagonalworkshop.conference.attendees.infrastrcture;

public record AttendeeDTO(String email) {
}
```

### Step 4: Domain Services

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

### Step 5: Aggregates

Aggregates are the core objects in Domain Driven Design.  An Aggregate represents the most important object or objects in our bounded context.
We are implementing the Attendees Bounded Context, and the Attendee is the most important object in this Bounded Context.

An Aggregate both represents the real world object, a conference attendee in this case, and encapsulates all of the invariants, or business logic, associated with the object.  In this iteration, we will implement business logic to notify the rest of the system about an attendee's registration.

Update the Attendee Aggregate in attendees/domain/aggregates so that the `registerAttendee` method creates and returns an instance of `Attendee` and an `AttendeeRegisteredEvent` that will be used to notify the rest of the system.  We will also need to create an object to hold both the `Attendee` and `AttendeeRegisteredEvent`, `AttendeeRegistrationResult`.  Do not worry if your class does not compile immediately.  We will create the other objects in the next step.

Implement the `registerAttendee` method by creating an AttendeeEntity and an AttendeeRegistredEvent.  Instantiate an `AttendeeRegistrationResult` with the newly created objects and return the `AttendeeRegistrationResult`.

```java
package dddhexagonalworkshop.conference.attendees.domain;

public class Attendee {

  String email;

  public static AttendeeRegistrationResult registerAttendee(String email, String firstName, String lastName, Address address) {
      // Here you would typically perform some business logic, like checking if the attendee already exists
      // and then create an event to publish.
      Attendee attendee = new Attendee(email, firstName, lastName, address);
      AttendeeRegisteredEvent event = new AttendeeRegisteredEvent(email, attendee.getFullName());
      return new AttendeeRegistrationResult(attendee, event);
  }

  public String getEmail(){
    return email;
  }
}

```


### Step 5: Entities

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

### Step 7: Events

A Domain Event is a record of some business-significant occurrence in a Bounded Context.  It is obviously significant that an attendee has registered because that one way conferences make money, but it is also signfificant because other parts of the system need to respond to the registration.

For this iteration, we will use a minimal event with only the attendee's email address.  Update the AttendeeRegistrationEvent with the email:

```java
package dddhexagonalworkshop.conference.attendees.domain.events;

public record AttendeeRegisteredEvent(String email) {
}
```

### Step 8: Creating Multiple Objects 

We need to create multiple objects when an attendee registers.  First, we need an `Attendee`.  Second, we need an `AttendeeRegisteredEvent`.  We will use an `AttendeeRegistrationResult` to hold both the `Attendee` and `AttendeeRegisteredEvent`. 

Complete the `AttendeeRegistrationResult` by adding an `Attendee` and an `AttendeeRegisteredEvent`.

```java
package dddhexagonalworkshop.conference.attendees.domain.services;

import dddhexagonalworkshop.conference.attendees.domain.aggregates.Attendee;
import dddhexagonalworkshop.conference.attendees.domain.events.AttendeeRegisteredEvent;

public record AttendeeRegistrationResult(Attendee attendee, AttendeeRegisteredEvent attendeeRegisteredEvent) {
}
```


### Step 6: Repositories

Complete the AttendeeRepository by adding a method to persist attendees.

You have probably noticed that our 'AttendeeRepository` implements `PanacheRepository<AttendeeEntity>`.  `PanacheRepository' provides methods for manipulating JPA Entities; however, following DDD guidelines, the Repository will be the only persistence class that other ojbects can access.  To this end we need methods that take an `Attendee` and convert it to an `AttendeeEntity`.

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

In this implementation the `Attendee` aggregate has no knowledge of the persistence framework.



### Step 10: Another Adapter
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

### Step 11: Complete the registration process

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
