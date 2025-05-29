package dddhexagonalworkshop.conference.attendees.domain.aggregates;

import dddhexagonalworkshop.conference.attendees.domain.services.AttendeeRegistrationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AttendeeTest {

    private final String email = "smeagol@riverfolk.net";

    @Test
    public void testRegisterAttendee_ValidEmail() {
        AttendeeRegistrationResult result = Attendee.registerAttendee(email);

        assertNotNull(result, "Registration result should not be null");
        assertNotNull(result.attendee(), "Attendee should not be null");
        assertNotNull(result.attendeeRegisteredEvent(), "Event should not be null");
        assertEquals(email, result.attendee().getEmail(), "Attendee email should match");
        assertEquals(email, result.attendeeRegisteredEvent().email(), "Event email should match");
    }

    @Test
    public void testRegisterAttendee_InvalidEmail_MissingAtSymbol() {
        String invalidEmail = "testexample.com";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Attendee.registerAttendee(invalidEmail);
        });

        assertEquals("Email must be a valid email address", exception.getMessage(), "Exception message should match");
    }

    @Test
    public void testRegisterAttendee_InvalidEmail_MissingDot() {
        String invalidEmail = "test@examplecom";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Attendee.registerAttendee(invalidEmail);
        });

        assertEquals("Email must be a valid email address", exception.getMessage(), "Exception message should match");
    }

    @Test
    public void testRegisterAttendee_InvalidEmail_NullEmail() {
        String invalidEmail = null;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Attendee.registerAttendee(invalidEmail);
        });

        assertEquals("Email is required for registration", exception.getMessage(), "Exception message should match");
    }

    @Test
    public void testRegisterAttendee_InvalidEmail_BlankEmail() {
        String invalidEmail = "  ";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Attendee.registerAttendee(invalidEmail);
        });

        assertEquals("Email is required for registration", exception.getMessage(), "Exception message should match");
    }

    @Test
    public void testAttendeeEquals_SameEmail() {
        Attendee attendee1 = Attendee.registerAttendee(email).attendee();
        Attendee attendee2 = Attendee.registerAttendee(email).attendee();

        assertEquals(attendee1, attendee2, "Attendees with the same email should be equal");
    }

    @Test
    public void testAttendeeEquals_DifferentEmail() {
        Attendee attendee1 = Attendee.registerAttendee("test1@example.com").attendee();
        Attendee attendee2 = Attendee.registerAttendee("test2@example.com").attendee();

        assertNotEquals(attendee1, attendee2, "Attendees with different emails should not be equal");
    }

    @Test
    public void testAttendeeHashCode_SameEmail() {
        Attendee attendee1 = Attendee.registerAttendee(email).attendee();
        Attendee attendee2 = Attendee.registerAttendee(email).attendee();

        assertEquals(attendee1.hashCode(), attendee2.hashCode(), "Attendees with the same email should have the same hash code");
    }

    @Test
    public void testAttendeeHashCode_DifferentEmail() {
        Attendee attendee1 = Attendee.registerAttendee("test1@example.com").attendee();
        Attendee attendee2 = Attendee.registerAttendee("test2@example.com").attendee();

        assertNotEquals(attendee1.hashCode(), attendee2.hashCode(), "Attendees with different emails should have different hash codes");
    }
}