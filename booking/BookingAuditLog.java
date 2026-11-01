package booking;

import java.util.ArrayList, java.util.List;

// this is simply a minimal booking-event record to satisfy the C1 operation contract from Iteration 1 
public class BookingAuditLog {
    private final List<String> events = new ArrayList<>();

    public void recordBooked(String appointmentId) {
        // just to clarify that no clinical details are stored in audit events
        events.add("APPOINTMENT_BOOKED " + appointmentId);
    }

    public List<String> getEvents() {
        return new ArrayList<>(events);
    }
}
