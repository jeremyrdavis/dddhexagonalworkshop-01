package dddhexagonalworkshop.conference.attendees.persistence;

import jakarta.persistence.*;

/**
 * "An Entity models an individual thing. Each Entity has a unique identity in that you can
 * distinguish its individuality from among all other Entities of the same or a different type."
 * Vaugn Vernon, Domain-Driven Design Distilled, 2016
 *
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

}
