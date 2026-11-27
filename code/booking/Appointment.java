package booking;

// A booked appointment, including its current schedule and booking status. */
public class Appointment {
    private final String appointmentId;
    private final Patient patient;
    private final String professionalId;
    private final String locationId;
    private final String serviceId;
    private TimeInterval interval;
    private final String referralId; // this may potentially be null when not required
    private String status;

    // Basically, new bookings start confirmed; SQLite uses the other constructor to restore a saved status.
    public Appointment(String appointmentId, Patient patient, String professionalId, String locationId, String serviceId, TimeInterval interval, String referralId) {
        this(appointmentId, patient, professionalId, locationId, serviceId, interval, referralId, "CONFIRMED");
    }

    Appointment(String appointmentId, Patient patient, String professionalId, String locationId, String serviceId, TimeInterval interval, String referralId, String status) {
        this.appointmentId = appointmentId;
        this.patient = patient;
        this.professionalId = professionalId;
        this.locationId = locationId;
        this.serviceId = serviceId;
        this.interval = interval;
        this.referralId = referralId;
        this.status = status;
    }

    public String getAppointmentId() { return appointmentId; }
    public Patient getPatient() { return patient; }
    public String getProfessionalId() { return professionalId; }
    public String getLocationId() { return locationId; }
    public String getServiceId() { return serviceId; }
    public TimeInterval getInterval() { return interval; }
    public String getReferralId() { return referralId; }
    public String getStatus() { return status; }

    void cancel() {
        status = "CANCELLED";
    }

    void reschedule(TimeInterval newInterval) {
        interval = newInterval;
    }
}
