package persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens the shared SQLite connection and creates the small schema used by the repositories.
 * The application uses one connection so the model stays easy to follow for this prototype.
 */
public class SqliteDatabase implements AutoCloseable {
    private final Connection connection;

    public SqliteDatabase(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("Database file is required");
        }

        Connection opened = null;
        try {
            Path absoluteFile = file.toAbsolutePath();
            Path parent = absoluteFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            opened = DriverManager.getConnection("jdbc:sqlite:" + absoluteFile);
            try (Statement statement = opened.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            connection = opened;
            createSchema();
        } catch (SQLException | IOException exception) {
            if (opened != null) {
                try {
                    opened.close();
                } catch (SQLException closeException) {
                    exception.addSuppressed(closeException);
                }
            }
            throw new PersistenceException(
                    "Could not open or initialize the SQLite database: " + exception.getMessage(), exception);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    private void createSchema() throws SQLException {
        String[] statements = {
            "CREATE TABLE IF NOT EXISTS patient ("
                    + "patient_id TEXT PRIMARY KEY, first_name TEXT, last_name TEXT, "
                    + "phone_number TEXT, insurance_number TEXT, date_of_birth TEXT)",
            "CREATE TABLE IF NOT EXISTS specialty ("
                    + "specialty_id TEXT PRIMARY KEY, name TEXT NOT NULL COLLATE NOCASE UNIQUE)",
            "CREATE TABLE IF NOT EXISTS professional ("
                    + "professional_id TEXT PRIMARY KEY, first_name TEXT, last_name TEXT, "
                    + "title TEXT, specialty_id TEXT REFERENCES specialty(specialty_id))",
            "CREATE TABLE IF NOT EXISTS service ("
                    + "service_id TEXT PRIMARY KEY, name TEXT, duration_minutes INTEGER NOT NULL "
                    + "CHECK (duration_minutes > 0), requires_referral INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS availability_slot ("
                    + "slot_id TEXT PRIMARY KEY, professional_id TEXT NOT NULL "
                    + "REFERENCES professional(professional_id), starts_at TEXT NOT NULL, ends_at TEXT NOT NULL, "
                    + "CHECK (starts_at < ends_at))",
            "CREATE INDEX IF NOT EXISTS availability_professional_time "
                    + "ON availability_slot(professional_id, starts_at, ends_at)",
            "CREATE TABLE IF NOT EXISTS appointment ("
                    + "appointment_id TEXT PRIMARY KEY, patient_id TEXT NOT NULL REFERENCES patient(patient_id), "
                    + "professional_id TEXT NOT NULL REFERENCES professional(professional_id), "
                    + "location_id TEXT, service_id TEXT NOT NULL REFERENCES service(service_id), "
                    + "starts_at TEXT NOT NULL, ends_at TEXT NOT NULL, referral_id TEXT, "
                    + "status TEXT NOT NULL CHECK (status IN ('CONFIRMED', 'CANCELLED')), "
                    + "CHECK (starts_at < ends_at))",
            "CREATE INDEX IF NOT EXISTS appointment_professional_time "
                    + "ON appointment(professional_id, status, starts_at, ends_at)"
        };
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException exception) {
            throw new PersistenceException("Could not close the SQLite database", exception);
        }
    }
}
