package booking;

import persistence.PersistenceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stores patient records in memory or through the configured SQLite connection. */
public class PatientRepository {
    private final Map<String, Patient> patients = new LinkedHashMap<>();
    private final Connection connection;

    public PatientRepository() {
        this(null);
    }

    public PatientRepository(Connection connection) {
        this.connection = connection;
    }

    public void add(Patient patient) {
        if (patient == null || patient.getPatientId() == null || patient.getPatientId().isBlank()) {
            throw new IllegalArgumentException("Patient must have an identifier");
        }
        if (findById(patient.getPatientId()) != null) {
            throw new IllegalArgumentException("Duplicate patient identifier");
        }
        if (connection == null) {
            patients.put(patient.getPatientId(), patient);
            return;
        }
        String sql = "INSERT INTO patient (patient_id, first_name, last_name, phone_number, insurance_number, date_of_birth) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patient.getPatientId());
            statement.setString(2, patient.getFirstName());
            statement.setString(3, patient.getLastName());
            statement.setString(4, patient.getPhoneNumber());
            statement.setString(5, patient.getInsuranceNumber());
            statement.setString(6, patient.getDateOfBirth() == null ? null : patient.getDateOfBirth().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("Could not save patient " + patient.getPatientId(), exception);
        }
    }

    public Patient findById(String patientId) {
        if (connection == null) {
            return patients.get(patientId);
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT patient_id, first_name, last_name, phone_number, insurance_number, date_of_birthFROM patient WHERE patient_id = ?")) {
            statement.setString(1, patientId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? readPatient(result) : null;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Could not find patient " + patientId, exception);
        }
    }

    public List<Patient> search(PatientSearchCriteria criteria) {
        if (criteria == null || criteria.isEmpty()) {
            throw new BookingException("INVALID_SEARCH_CRITERIA");
        }
        List<Patient> matches = new ArrayList<>();
        for (Patient patient : getAll()) {
            if (criteria.matches(patient)) {
                matches.add(patient);
            }
        }
        return matches;
    }

    public List<Patient> getAll() {
        if (connection == null) {
            return new ArrayList<>(patients.values());
        }
        List<Patient> result = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT patient_id, first_name, last_name, phone_number, insurance_number, date_of_birth FROM patient ORDER BY rowid")) {
            while (rows.next()) {
                result.add(readPatient(rows));
            }
            return result;
        } catch (SQLException exception) {
            throw new PersistenceException("Could not list patients", exception);
        }
    }

    private Patient readPatient(ResultSet row) throws SQLException {
        String dateOfBirth = row.getString("date_of_birth");
        return new Patient(row.getString("patient_id"), row.getString("first_name"), row.getString("last_name"), row.getString("phone_number"), row.getString("insurance_number"), dateOfBirth == null ? null : java.time.LocalDate.parse(dateOfBirth));
    }
}
