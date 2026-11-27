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

public class ProfessionalRepository {
    private final Map<String, HealthcareProfessional> professionals = new LinkedHashMap<>();
    private final Connection connection;
    private int nextGeneratedId = 1;

    public ProfessionalRepository() {
        this(null);
    }

    public ProfessionalRepository(Connection connection) {
        this.connection = connection;
    }

    public void add(HealthcareProfessional professional) {
        if (professional == null || isBlank(professional.getProfessionalId())) {
            throw new IllegalArgumentException("Professional must have an identifier");
        }
        if (existsById(professional.getProfessionalId())) {
            throw new IllegalArgumentException("Duplicate professional identifier");
        }
        if (connection == null) {
            professionals.put(professional.getProfessionalId(), professional);
            return;
        }
        String sql = "INSERT INTO professional (professional_id, first_name, last_name, title, specialty_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, professional.getProfessionalId());
            statement.setString(2, professional.getFirstName());
            statement.setString(3, professional.getLastName());
            statement.setString(4, professional.getTitle());
            statement.setString(5, professional.getSpecialty() == null ? null : professional.getSpecialty().getSpecialtyId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("Could not save professional " + professional.getProfessionalId(), exception);
        }
    }

    public HealthcareProfessional findById(String professionalId) {
        if (connection == null) {
            return professionals.get(professionalId);
        }
        String sql = "SELECT p.professional_id, p.first_name, p.last_name, p.title, s.specialty_id, s.name AS specialty_name FROM professional p LEFT JOIN specialty s ON p.specialty_id = s.specialty_id WHERE p.professional_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, professionalId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? readProfessional(result) : null;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Could not find professional " + professionalId, exception);
        }
    }

    public boolean existsById(String professionalId) {
        if (connection == null) {
            return professionals.containsKey(professionalId);
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM professional WHERE professional_id = ?")) {
            statement.setString(1, professionalId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Could not check professional " + professionalId, exception);
        }
    }

    public HealthcareProfessional findOrCreate(String firstName, String lastName, String title) {
        return findOrCreate(firstName, lastName, title, null);
    }

    /** this is being used by the CSV import: reuses a professional with the same name and title, otherwise creates one */
    public HealthcareProfessional findOrCreate(String firstName, String lastName, String title, Specialty specialty) {
        String normalizedFirstName = normalizeRequired(firstName, "First name");
        String normalizedLastName = normalizeRequired(lastName, "Last name");
        String normalizedTitle = normalizeRequired(title, "Title");

        for (HealthcareProfessional professional : getAll()) {
            if (professional.getFirstName() != null
                    && professional.getLastName() != null
                    && professional.getTitle() != null
                    && professional.getFirstName().equalsIgnoreCase(normalizedFirstName)
                    && professional.getLastName().equalsIgnoreCase(normalizedLastName)
                    && professional.getTitle().equalsIgnoreCase(normalizedTitle)) {
                return professional;
            }
        }

        String professionalId;
        do {
            professionalId = "PRO-" + nextGeneratedId++;
        } while (existsById(professionalId));

        HealthcareProfessional created = new HealthcareProfessional(
                professionalId, normalizedFirstName, normalizedLastName, normalizedTitle, specialty);
        add(created);
        return created;
    }
    
    public List<HealthcareProfessional> getAll() {
        if (connection == null) {
            return new ArrayList<>(professionals.values());
        }
        List<HealthcareProfessional> result = new ArrayList<>();
        String sql = "SELECT p.professional_id, p.first_name, p.last_name, p.title, s.specialty_id, s.name AS specialty_name FROM professional p LEFT JOIN specialty s ON p.specialty_id = s.specialty_id ORDER BY p.rowid";
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                result.add(readProfessional(rows));
            }
            return result;
        } catch (SQLException exception) {
            throw new PersistenceException("Could not list professionals", exception);
        }
    }

    private HealthcareProfessional readProfessional(ResultSet row) throws SQLException {
        String specialtyId = row.getString("specialty_id");
        Specialty specialty = specialtyId == null ? null : new Specialty(specialtyId, row.getString("specialty_name"));
        return new HealthcareProfessional(row.getString("professional_id"), row.getString("first_name"), row.getString("last_name"), row.getString("title"), specialty);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeRequired(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
