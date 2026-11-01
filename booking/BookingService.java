package booking;

import java.time.LocalDateTime, java.util.ArrayList, java.util.List;

/** Note: GRASP Controller for the three system operations of Iteration 1 critical use case C1.
 *  also only the booking portion of the Iteration 2 proof of concept is implemented here
 */
public class BookingService {
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final AvailabilityPort availability;
    private final BookingCatalog catalog;
    private final BookingAuditLog auditLog;

    public BookingService(PatientRepository patients, AppointmentRepository appointments, AvailabilityPort availability, BookingCatalog catalog, BookingAuditLog auditLog) {
        this.patients = patients;
        this.appointments = appointments;
        this.availability = availability;
        this.catalog = catalog;
        this.auditLog = auditLog;
    }

    // C1: read-only patient search. Basically the Caller is assumed to be an authorized clinic staff
    public List<Patient> searchPatients(PatientSearchCriteria criteria) {
        return patients.search(criteria);
    }

    //C1:available candidate intervals. Note also that searching never reserves an appointment
    public List<TimeInterval> findAvailableSlots(String professionalId, String locationId, String serviceId, TimeInterval requestedPeriod) {
        verifyReferences(professionalId, locationId, serviceId);
        if (requestedPeriod == null) {
            throw new BookingException("INVALID_BOOKING_DATA");
        }
        List<TimeInterval> candidates = availability.findAvailableSlots(professionalId, locationId, serviceId, requestedPeriod);
        List<TimeInterval> result = new ArrayList<>();
        for (TimeInterval slot : candidates) {
            if (slot != null && !appointments.hasConflict(professionalId, slot)) {
                result.add(slot);
            }
        }
        return result;
    }

    //C1: validate, then create and confirm exactly one appointment. For now, availability is checked again even if a prior search returned this slot.
    public Appointment bookAppointment(String patientId, String professionalId, String locationId, String serviceId, LocalDateTime startsAt, LocalDateTime endsAt, String referralId) {
        Patient patient = patients.findById(patientId);
        if (patient == null) {
            throw new BookingException("PATIENT_NOT_FOUND");
        }
        verifyReferences(professionalId, locationId, serviceId);
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new BookingException("INVALID_BOOKING_DATA");
        }
        TimeInterval proposed = new TimeInterval(startsAt, endsAt);

        // here, the referral validation precedes the creation. Also the referral is optional only if the selected medical service does not require one
        if (catalog.requiresReferral(serviceId) && (referralId == null || referralId.isBlank())) {
            throw new BookingException("REFERRAL_REQUIRED");
        }
        if (referralId != null && !referralId.isBlank() && !catalog.referralIsEligible(referralId, patientId, professionalId, serviceId)) {
            throw new BookingException("INVALID_REFERRAL");
        }

        // here basically both the schedule and already-booked appointments must allow the interval
        if (!availability.isAvailable(professionalId, locationId, serviceId, proposed) || appointments.hasConflict(professionalId, proposed)) {
            throw new BookingException("SLOT_UNAVAILABLE");
        }

        Appointment booked = appointments.create(patient, professionalId, locationId, serviceId, proposed, referralId);
        auditLog.recordBooked(booked.getAppointmentId());
        return booked;
    }

    private void verifyReferences(String professionalId, String locationId, String serviceId) {
        if (!catalog.professionalExists(professionalId) || !catalog.locationExists(locationId) || !catalog.serviceExists(serviceId)) {
            throw new BookingException("INVALID_BOOKING_DATA");
        }
    }
}