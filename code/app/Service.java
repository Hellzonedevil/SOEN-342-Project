package app;

/** A medical service offered by the clinic; its duration fixes the length of an appointment. */
public class Service {
    private final String serviceId;
    private final String name;
    private final int durationMinutes;
    private final boolean requiresReferral;

    public Service(String serviceId, String name, int durationMinutes, boolean requiresReferral) {
        if (serviceId == null || serviceId.isBlank() || durationMinutes <= 0) {
            throw new IllegalArgumentException("Service needs an identifier and a positive duration");
        }
        this.serviceId = serviceId;
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.requiresReferral = requiresReferral;
    }

    public String getServiceId() { return serviceId; }
    public String getName() { return name; }
    public int getDurationMinutes() { return durationMinutes; }
    public boolean isRequiresReferral() { return requiresReferral; }
}
