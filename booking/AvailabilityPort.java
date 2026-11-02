package booking;

import java.util.List;

// NOTE: WHOVER IS MEMBER#2 SHOULD SUPPLY AN IMPLEMENTATION USING THE SHHARED AVAILABILITY DATA (so technically you may change the logic of the interface to your liking, but i believe this is the most straightforward way to do it)
public interface AvailabilityPort {
    // thsi essentially returns the candidate/available slots (they dont reserve a booking)
    List<TimeInterval> findAvailableSlots(String professionalId, String locationId, String serviceId, TimeInterval requestedPeriod);

    // this one simpky checks whether a selected interval is available immediately before BookingService creates an appointment
    boolean isAvailable(String professionalId, String locationId, String serviceId, TimeInterval proposed);
}