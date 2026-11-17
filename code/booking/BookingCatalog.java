package booking;

// this is an integration point for the existing professional/location/service/referral concepts (can be changed later on but for now i think this is the way to go)
public interface BookingCatalog {
    boolean professionalExists(String professionalId);
    boolean locationExists(String locationId);
    boolean serviceExists(String serviceId);
    boolean requiresReferral(String serviceId);
    boolean referralIsEligible(String referralId, String patientId, String professionalId, String serviceId);
}