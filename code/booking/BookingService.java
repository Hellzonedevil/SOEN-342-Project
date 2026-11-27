package booking;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** GRASP Controller for patient search, availability lookup, and appointment management. */
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
    public List<TimeInterval> findAvailableSlots(String professionalId, String serviceId, TimeInterval requestedPeriod) {
        verifyReferences(professionalId, serviceId);
        if (requestedPeriod == null) {
            throw new BookingException("INVALID_BOOKING_DATA");
        }
        List<TimeInterval> candidates = availability.findAvailableSlots(professionalId, serviceId, requestedPeriod);
        List<TimeInterval> result = new ArrayList<>();
        for (TimeInterval slot : candidates) {
            if (slot != null && !appointments.hasConflict(professionalId, slot)) {
                result.add(slot);
            }
        }
        return result;
    }

    //C1: validate, then create and confirm exactly one appointment. For now, availability is checked again even if a prior search returned this slot.
    // Iteration I feedback: no endsAt parameter, the end time is derived from the duration of the selected service.
    public Appointment bookAppointment(String patientId, String professionalId, String serviceId, LocalDateTime startsAt, String referralId) {
        Patient patient = patients.findById(patientId);
        if (patient == null) {
            throw new BookingException("PATIENT_NOT_FOUND");
        }
        verifyReferences(professionalId, serviceId);
        int durationMinutes = catalog.getDurationMinutes(serviceId);
        if (startsAt == null || durationMinutes <= 0) {
            throw new BookingException("INVALID_BOOKING_DATA");
        }
        TimeInterval proposed = new TimeInterval(startsAt, startsAt.plusMinutes(durationMinutes));

        // here, the referral validation precedes the creation. Also the referral is optional only if the selected medical service does not require one
        if (catalog.requiresReferral(serviceId) && (referralId == null || referralId.isBlank())) {
            throw new BookingException("REFERRAL_REQUIRED");
        }
        if (referralId != null && !referralId.isBlank() && !catalog.referralIsEligible(referralId, patientId, professionalId, serviceId)) {
            throw new BookingException("INVALID_REFERRAL");
        }

        // here basically both the schedule and already-booked appointments must allow the interval
        if (!availability.isAvailable(professionalId, serviceId, proposed) || appointments.hasConflict(professionalId, proposed)) {
            throw new BookingException("SLOT_UNAVAILABLE");
        }

        Appointment booked = appointments.create(patient, professionalId, serviceId, proposed, referralId);
        auditLog.recordBooked(booked.getAppointmentId());
        return booked;
    }

    /** Cancels a confirmed appointment while retaining its record for history. */
    public Appointment cancelAppointment(String appointmentId) {
        Appointment appointment = appointments.findById(appointmentId);
        if (appointment == null) {
            throw new BookingException("APPOINTMENT_NOT_FOUND");
        }
        if (!"CONFIRMED".equals(appointment.getStatus())) {
            throw new BookingException("APPOINTMENT_NOT_CANCELLABLE");
        }
        appointments.cancel(appointmentId);
        auditLog.recordCancelled(appointmentId);
        return appointments.findById(appointmentId);
    }

    /** Moves a confirmed appointment only when its new interval remains available. */
    public Appointment rescheduleAppointment(String appointmentId, LocalDateTime newStartsAt) {
        Appointment appointment = appointments.findById(appointmentId);
        if (appointment == null) {
            throw new BookingException("APPOINTMENT_NOT_FOUND");
        }
        if (!"CONFIRMED".equals(appointment.getStatus())) {
            throw new BookingException("APPOINTMENT_NOT_RESCHEDULABLE");
        }
        int durationMinutes = catalog.getDurationMinutes(appointment.getServiceId());
        if (newStartsAt == null || durationMinutes <= 0) {
            throw new BookingException("INVALID_BOOKING_DATA");
        }
        TimeInterval proposed = new TimeInterval(newStartsAt, newStartsAt.plusMinutes(durationMinutes));
        if (!availability.isAvailable(appointment.getProfessionalId(), appointment.getServiceId(), proposed) || appointments.hasConflict(appointment.getProfessionalId(), proposed, appointment.getAppointmentId())) {
            throw new BookingException("SLOT_UNAVAILABLE");
        }
        appointments.reschedule(appointmentId, proposed);
        auditLog.recordRescheduled(appointmentId);
        return appointments.findById(appointmentId);
    }

    private void verifyReferences(String professionalId, String serviceId) {
        if (!catalog.professionalExists(professionalId) || !catalog.serviceExists(serviceId)) {
            throw new BookingException("INVALID_BOOKING_DATA");
        }
    }
}