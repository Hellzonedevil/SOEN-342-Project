package booking;

// this basically represents a confirmed appointment produced by C1, while other statuses belong to later use cases
public class Appointment {
    private final String appointmentId;
    private final Patient patient;
    private final String professionalId;
    private final String locationId;
    private final String serviceId;
    private final TimeInterval interval;
    private final String referralId; // this may potentially be null when not required
    private final String status;

    public Appointment(String appointmentId, Patient patient, String professionalId, String locationId, String serviceId, TimeInterval interval, String referralId) {
        this.appointmentId = appointmentId;
        this.patient = patient;
        this.professionalId = professionalId;
        this.locationId = locationId;
        this.serviceId = serviceId;
        this.interval = interval;
        this.referralId = referralId;
        this.status = "CONFIRMED";
    }

    public String getAppointmentId() { return appointmentId; }
    public Patient getPatient() { return patient; }
    public String getProfessionalId() { return professionalId; }
    public String getLocationId() { return locationId; }
    public String getServiceId() { return serviceId; }
    public TimeInterval getInterval() { return interval; }
    public String getReferralId() { return referralId; }
    public String getStatus() { return status; }
}
