package dddhexagonalworkshop.conference.attendees.domain.services;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RegisterAttendeeCommandTest {

    @Test
    public void testRegisterAttendeeCommandCreation() {
        RegisterAttendeeCommand command = new RegisterAttendeeCommand("sauron@barad-dur.gov");
        assertNotNull(command, "The command should not be null");
        assertEquals("sauron@barad-dur.gov", command.email(), "The email should match");
    }

    @Test
    public void testRegisterAttendeeCommandEmail() {
        RegisterAttendeeCommand command = new RegisterAttendeeCommand("sauron@barad-dur.gov");
        assertEquals("sauron@barad-dur.gov", command.email(), "The email should be sauron@barad-dur.gov");
    }

    @Test
    public void testRegisterAttendeeCommandInvalidEmail() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new RegisterAttendeeCommand("invalid.email");
        });
        assertEquals("Email must contain @ symbol", exception.getMessage());
    }

    @Test
    public void testRegisterAttendeeCommandBlankEmail() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new RegisterAttendeeCommand("");
        });
        assertEquals("Email cannot be null or blank", exception.getMessage());
    }
}