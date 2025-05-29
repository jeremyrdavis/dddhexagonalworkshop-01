package dddhexagonalworkshop.conference.attendees.domain.services;

import dddhexagonalworkshop.conference.attendees.domain.aggregates.Attendee;
import dddhexagonalworkshop.conference.attendees.domain.events.AttendeeRegisteredEvent;

/**
 * This object is used to return the result of an attendee registration and contains the objects created by the Aggregate.
 */
public record AttendeeRegistrationResult() {
}
