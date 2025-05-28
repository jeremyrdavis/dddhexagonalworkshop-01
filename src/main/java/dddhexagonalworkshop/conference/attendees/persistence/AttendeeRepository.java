package dddhexagonalworkshop.conference.attendees.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * "A REPOSITORY represents all objects of a certain type as a conceptual set (usually emulated). It acts like a collection, except with more elaborate querying capability. Objects of the appropriate type are added and removed, and the machinery behind the REPOSITORY inserts them or deletes them from the database."
 * Eric Evans, Domain-Driven Design: Tackling Complexity in the Heart of Software, 2003.
 *
 * We are using PanacheRepository from Quarkus, which provides a set of methods to interact with the database.
 */
@ApplicationScoped
public class AttendeeRepository implements PanacheRepository<AttendeeEntity> {
}
