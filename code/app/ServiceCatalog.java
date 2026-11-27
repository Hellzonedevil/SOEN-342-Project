package app;

import booking.Specialty;
import persistence.PersistenceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Catalog of clinic services and specialties, backed by memory or the shared SQLite connection. */
public class ServiceCatalog {
    private final Map<String, Service> services = new LinkedHashMap<>();
    private final Map<String, Specialty> specialtiesByName = new LinkedHashMap<>();
    private final Connection connection;
    private int nextSpecialtyId = 1;

    public ServiceCatalog() {
        this(null);
    }

    public ServiceCatalog(Connection connection) {
        this.connection = connection;
        if (connection != null) {
            nextSpecialtyId = readNextSpecialtyId();
        }
    }

    public void addService(Service service) {
        if (connection == null) {
            services.put(service.getServiceId(), service);
            return;
        }
        String sql = "INSERT INTO service (service_id, name, duration_minutes, requires_referral) "
                + "VALUES (?, ?, ?, ?) ON CONFLICT(service_id) DO UPDATE SET "
                + "name = excluded.name, duration_minutes = excluded.duration_minutes, "
                + "requires_referral = excluded.requires_referral";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, service.getServiceId());
            statement.setString(2, service.getName());
            statement.setInt(3, service.getDurationMinutes());
            statement.setInt(4, service.isRequiresReferral() ? 1 : 0);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("Could not save service " + service.getServiceId(), exception);
        }
    }

    public Service findService(String serviceId) {
        if (serviceId == null) {
            return null;
        }
        String id = serviceId.trim();
        if (connection == null) {
            return services.get(id);
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT service_id, name, duration_minutes, requires_referral FROM service WHERE service_id = ?")) {
            statement.setString(1, id);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? readService(row) : null;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Could not find service " + id, exception);
        }
    }

    public List<Service> getAllServices() {
        if (connection == null) {
            return new ArrayList<>(services.values());
        }
        List<Service> result = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT service_id, name, duration_minutes, requires_referral FROM service ORDER BY rowid")) {
            while (rows.next()) {
                result.add(readService(rows));
            }
            return result;
        } catch (SQLException exception) {
            throw new PersistenceException("Could not list services", exception);
        }
    }

    public Specialty findSpecialty(String specialtyId) {
        if (connection != null) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT specialty_id, name FROM specialty WHERE specialty_id = ?")) {
                statement.setString(1, specialtyId);
                try (ResultSet row = statement.executeQuery()) {
                    return row.next() ? new Specialty(row.getString("specialty_id"), row.getString("name")) : null;
                }
            } catch (SQLException exception) {
                throw new PersistenceException("Could not find specialty " + specialtyId, exception);
            }
        }
        for (Specialty s : specialtiesByName.values()) {
            if (s.getSpecialtyId().equals(specialtyId)) {
                return s;
            }
        }
        return null;
    }

    /** Used by the CSV import: the same specialty name always maps to the same Specialty object. */
    public Specialty findOrCreateSpecialty(String name) {
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (connection != null) {
            String query = "SELECT specialty_id, name FROM specialty WHERE name = ? COLLATE NOCASE";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, name.trim());
                try (ResultSet row = statement.executeQuery()) {
                    if (row.next()) {
                        return new Specialty(row.getString("specialty_id"), row.getString("name"));
                    }
                }
            } catch (SQLException exception) {
                throw new PersistenceException("Could not find specialty " + name, exception);
            }
            String specialtyId = "SPC-" + nextSpecialtyId++;
            Specialty created = new Specialty(specialtyId, name.trim());
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO specialty (specialty_id, name) VALUES (?, ?)")) {
                statement.setString(1, created.getSpecialtyId());
                statement.setString(2, created.getName());
                statement.executeUpdate();
                return created;
            } catch (SQLException exception) {
                throw new PersistenceException("Could not save specialty " + name, exception);
            }
        }
        Specialty existing = specialtiesByName.get(key);
        if (existing != null) {
            return existing;
        }
        Specialty created = new Specialty("SPC-" + nextSpecialtyId++, name.trim());
        specialtiesByName.put(key, created);
        return created;
    }

    private int readNextSpecialtyId() {
        try (Statement statement = connection.createStatement();
                ResultSet row = statement.executeQuery(
                        "SELECT COALESCE(MAX(CAST(SUBSTR(specialty_id, 5) AS INTEGER)), 0) + 1 "
                                + "FROM specialty WHERE specialty_id LIKE 'SPC-%'")) {
            return row.next() ? row.getInt(1) : 1;
        } catch (SQLException exception) {
            throw new PersistenceException("Could not read the next specialty identifier", exception);
        }
    }

    private Service readService(ResultSet row) throws SQLException {
        return new Service(row.getString("service_id"), row.getString("name"), row.getInt("duration_minutes"),
                row.getInt("requires_referral") != 0);
    }
}
