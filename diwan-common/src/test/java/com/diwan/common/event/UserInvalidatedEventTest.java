package com.diwan.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Guards the wire contract of the Kafka event: old messages must stay readable, new fields must be ignorable. */
class UserInvalidatedEventTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void newEventsCarryVersionIdAndTimestamp() {
        UserInvalidatedEvent e = new UserInvalidatedEvent(7L, "DELETED");
        assertEquals(UserInvalidatedEvent.CURRENT_VERSION, e.getVersion());
        assertNotNull(e.getEventId());
        assertNotNull(e.getOccurredAt());
        assertNotEquals(e.getEventId(), new UserInvalidatedEvent(7L, "DELETED").getEventId());
    }

    @Test
    void roundTrips() throws Exception {
        UserInvalidatedEvent e = new UserInvalidatedEvent(7L, "DEACTIVATED");
        UserInvalidatedEvent back = mapper.readValue(mapper.writeValueAsString(e), UserInvalidatedEvent.class);
        assertEquals(e.getEventId(), back.getEventId());
        assertEquals(e.getOccurredAt(), back.getOccurredAt());
        assertEquals(7L, back.getUserId());
        assertEquals("DEACTIVATED", back.getReason());
    }

    @Test
    void messagesFromBeforeVersioningStillDeserialize() throws Exception {
        UserInvalidatedEvent old = mapper.readValue("{\"userId\":5,\"reason\":\"DELETED\"}", UserInvalidatedEvent.class);
        assertEquals(5L, old.getUserId());
        assertEquals(0, old.getVersion());
        assertNull(old.getEventId());
    }

    @Test
    void unknownFieldsFromNewerProducersAreIgnored() throws Exception {
        UserInvalidatedEvent e = mapper.readValue(
                "{\"version\":2,\"userId\":5,\"reason\":\"DELETED\",\"tenant\":\"x\"}", UserInvalidatedEvent.class);
        assertEquals(2, e.getVersion());
        assertEquals(5L, e.getUserId());
    }
}
