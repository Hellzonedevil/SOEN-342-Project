package booking;

// this is an integration point for the existing professional/service/referral concepts (can be changed later on but for now i think this is the way to go)
// Iteration II: locationId was removed (Iteration I feedback) and the catalog also provides service durations,
// so the same catalog object can be given to AvailabilityService as its ServiceDurationProvider.
public interface BookingCatalog extends ServiceDurationProvider {
    boolean professionalExists(String professionalId);
    boolean serviceExists(String serviceId);
    boolean requiresReferral(String serviceId);
    boolean referralIsEligible(String referralId, String patientId, String professionalId, String serviceId);
}
