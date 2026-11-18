package booking;

/** Adapter for the service catalog's appointment-duration lookup for Sarvesh! Use if u think it's needed */
public interface ServiceDurationProvider {
    int getDurationMinutes(String serviceId);
}
