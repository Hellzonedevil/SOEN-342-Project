package booking;

import persistence.PersistenceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Stores appointments in memory or in SQLite, preserving the existing repository API. */
public class AppointmentRepository {
    private final List<Appointment> appointments = new ArrayList<>();
    private final Connection connection;
    private final PatientRepository patients;
    private int nextId = 1;

    public AppointmentRepository() {
        this(null, null);
    }

    public AppointmentRepository(Connection connection, PatientRepository patients) {
        if (connection != null && patients == null) {
            throw new IllegalArgumentException("Persistent appointments require a patient repository");
        }
        this.connection = connection;
        this.patients = patients;
        if (connection != null) {
            nextId = readNextId();
        }
    }

    public boolean hasConflict(String professionalId, TimeInterval proposed) {
        return hasConflict(professionalId, proposed, null);
    }

    public boolean hasConflict(String professionalId, TimeInterval proposed, String excludedAppointmentId) {
        if (connection != null) {
            String sql = "SELECT 1 FROM appointment WHERE professional_id = ? AND status <> 'CANCELLED' "
                    + "AND starts_at < ? AND ? < ends_at "
                    + "AND (? IS NULL OR appointment_id <> ?) LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, professionalId);
                statement.setString(2, proposed.getEndsAt().toString());
                statement.setString(3, proposed.getStartsAt().toString());
                statement.setString(4, excludedAppointmentId);
                statement.setString(5, excludedAppointmentId);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            } catch (SQLException exception) {
                throw new PersistenceException("Could not check for appointment conflicts", exception);
            }
        }
        for (Appointment appointment : appointments) {
            if (!"CANCELLED".equals(appointment.getStatus()) && appointment.getProfessionalId().equals(professionalId) && appointment.getInterval().overlaps(proposed)) {
                return true;
            }
        }
        return false;
    }

    // BookingService validates references, schedule, and conflicts before creating the record.
    public Appointment create(Patient patient, String professionalId, String locationId, String serviceId,
            TimeInterval interval, String referralId) {
        String appointmentId = "APT-" + nextId;
        Appointment appointment = new Appointment(appointmentId, patient, professionalId, locationId, serviceId,
                interval, referralId);
        if (connection != null) {
            String sql = "INSERT INTO appointment "
                    + "(appointment_id, patient_id, professional_id, location_id, service_id, starts_at, ends_at, "
                    + "referral_id, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, appointmentId);
                statement.setString(2, patient.getPatientId());
                statement.setString(3, professionalId);
                statement.setString(4, locationId);
                statement.setString(5, serviceId);
                statement.setString(6, interval.getStartsAt().toString());
                statement.setString(7, interval.getEndsAt().toString());
                statement.setString(8, referralId);
                statement.setString(9, appointment.getStatus());
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new PersistenceException("Could not save appointment " + appointmentId, exception);
            }
        } else {
            appointments.add(appointment);
        }
        nextId++;
        return appointment;
    }

    public Appointment create(Patient patient, String professionalId, String serviceId, TimeInterval interval,
            String referralId) {
        return create(patient, professionalId, null, serviceId, interval, referralId);
    }

    public List<Appointment> getAll() {
        if (connection != null) {
            List<Appointment> result = new ArrayList<>();
            String sql = "SELECT appointment_id, patient_id, professional_id, location_id, service_id, starts_at, "
                    + "ends_at, referral_id, status FROM appointment ORDER BY rowid";
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
                while (rows.next()) {
                    result.add(readAppointment(rows));
                }
                return result;
            } catch (SQLException exception) {
                throw new PersistenceException("Could not list appointments", exception);
            }
        }
        return new ArrayList<>(appointments);
    }

    public Appointment findById(String appointmentId) {
        if (connection == null) {
            for (Appointment appointment : appointments) {
                if (appointment.getAppointmentId().equals(appointmentId)) {
                    return appointment;
                }
            }
            return null;
        }
        String sql = "SELECT appointment_id, patient_id, professional_id, location_id, service_id, starts_at, "
                + "ends_at, referral_id, status FROM appointment WHERE appointment_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, appointmentId);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? readAppointment(row) : null;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Could not find appointment " + appointmentId, exception);
        }
    }

    public void cancel(String appointmentId) {
        if (connection == null) {
            Appointment appointment = findById(appointmentId);
            appointment.cancel();
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE appointment SET status = 'CANCELLED' WHERE appointment_id = ? AND status = 'CONFIRMED'")) {
            statement.setString(1, appointmentId);
            if (statement.executeUpdate() != 1) {
                throw new PersistenceException("Appointment is no longer available to cancel: " + appointmentId);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Could not cancel appointment " + appointmentId, exception);
        }
    }

    public void reschedule(String appointmentId, TimeInterval newInterval) {
        if (connection == null) {
            findById(appointmentId).reschedule(newInterval);
            return;
        }
        String sql = "UPDATE appointment SET starts_at = ?, ends_at = ? "
                + "WHERE appointment_id = ? AND status = 'CONFIRMED'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newInterval.getStartsAt().toString());
            statement.setString(2, newInterval.getEndsAt().toString());
            statement.setString(3, appointmentId);
            if (statement.executeUpdate() != 1) {
                throw new PersistenceException("Appointment is no longer available to reschedule: " + appointmentId);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Could not reschedule appointment " + appointmentId, exception);
        }
    }

    private int readNextId() {
        try (Statement statement = connection.createStatement();
                ResultSet row = statement.executeQuery(
                        "SELECT COALESCE(MAX(CAST(SUBSTR(appointment_id, 5) AS INTEGER)), 0) + 1 "
                                + "FROM appointment WHERE appointment_id LIKE 'APT-%'")) {
            return row.next() ? row.getInt(1) : 1;
        } catch (SQLException exception) {
            throw new PersistenceException("Could not read the next appointment identifier", exception);
        }
    }

    private Appointment readAppointment(ResultSet row) throws SQLException {
        String patientId = row.getString("patient_id");
        Patient patient = patients.findById(patientId);
        if (patient == null) {
            throw new PersistenceException("Appointment references missing patient " + patientId);
        }
        TimeInterval interval = new TimeInterval(LocalDateTime.parse(row.getString("starts_at")),
                LocalDateTime.parse(row.getString("ends_at")));
        return new Appointment(row.getString("appointment_id"), patient, row.getString("professional_id"),
                row.getString("location_id"), row.getString("service_id"), interval, row.getString("referral_id"),
                row.getString("status"));
    }
}
