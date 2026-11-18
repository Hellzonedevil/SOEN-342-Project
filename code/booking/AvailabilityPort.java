package booking;

import java.util.List;

/** Availability operations consumed by the booking service. */
public interface AvailabilityPort {
    /** Returns candidate appointment intervals without reserving them. */
    List<TimeInterval> findAvailableSlots(String professionalId, String serviceId, TimeInterval requestedPeriod);

    /** Checks whether the proposed interval fits the professional's published schedule. */
    boolean isAvailable(String professionalId, String serviceId, TimeInterval proposed);
}