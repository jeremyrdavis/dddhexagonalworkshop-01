package dddhexagonalworkshop.conference.attendees.domain.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AttendeeRegisteredEventTest {

    @Test
    public void testAttendeeRegisteredEventContainsAllFields() {
        AttendeeRegisteredEvent event = new AttendeeRegisteredEvent("gandalfthegrey@istari.net");
        assertNotNull(event);
        assertNotNull(event.email());
        assertEquals("gandalfthegrey@istari.net", event.email());
    }
}
