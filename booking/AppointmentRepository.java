package booking;

import java.util.ArrayList, java.util.List;

// this basically represents the storage for the prototype's appointments in memory (also im making with the idea that no database is required in Iteration II)
public class AppointmentRepository {
    private final List<Appointment> appointments = new ArrayList<>();
    private int nextId = 1;

    public boolean hasConflict(String professionalId, TimeInterval proposed) {
        for (Appointment appointment : appointments) {
            if (!"CANCELLED".equals(appointment.getStatus()) && appointment.getProfessionalId().equals(professionalId) && appointment.getInterval().overlaps(proposed)) {
                return true;
            }
        }
        return false;
    }

    // nnote that this is being called only after BookingService has validated the booking
    public Appointment create(Patient patient, String professionalId, String locationId, String serviceId, TimeInterval interval, String referralId) {
        Appointment appointment = new Appointment("APT-" + nextId++, patient, professionalId, locationId, serviceId, interval, referralId);
        appointments.add(appointment);
        return appointment;
    }

    public List<Appointment> getAll() {
        return new ArrayList<>(appointments);
    }
}
