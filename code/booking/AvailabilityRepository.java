package booking;

import persistence.PersistenceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AvailabilityRepository {
    private final List<AvailabilitySlot> slots = new ArrayList<>();
    private final Connection connection;
    private int nextId = 1;

    public AvailabilityRepository() {
        this(null);
    }

    public AvailabilityRepository(Connection connection) {
        this.connection = connection;
        if (connection != null) {
            nextId = readNextId();
        }
    }

    public AvailabilitySlot create(String professionalId, TimeInterval interval) {
        return createAll(professionalId, Collections.singletonList(interval)).get(0);
    }

    public List<AvailabilitySlot> createAll(String professionalId, List<TimeInterval> intervals) {
        if (professionalId == null || professionalId.isBlank() || intervals == null) {
            throw new AvailabilityException("INVALID_AVAILABILITY_DATA");
        }

        List<TimeInterval> validated = new ArrayList<>();
        for (TimeInterval interval : intervals) {
            if (interval == null) {
                throw new AvailabilityException("INVALID_AVAILABILITY_DATA");
            }
            if (hasOverlap(professionalId, interval)) {
                throw new AvailabilityException("AVAILABILITY_OVERLAP");
            }
            for (TimeInterval earlier : validated) {
                if (earlier.overlaps(interval)) {
                    throw new AvailabilityException("AVAILABILITY_OVERLAP");
                }
            }
            validated.add(interval);
        }

        List<AvailabilitySlot> created = new ArrayList<>();
        if (connection == null) {
            for (TimeInterval interval : validated) {
                AvailabilitySlot slot = new AvailabilitySlot("AVL-" + nextId++, professionalId, interval);
                slots.add(slot);
                created.add(slot);
            }
            return created;
        }

        boolean originalAutoCommit = true;
        boolean transactionStarted = false;
        try {
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            transactionStarted = true;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO availability_slot (slot_id, professional_id, starts_at, ends_at) "
                            + "VALUES (?, ?, ?, ?)")) {
                int id = nextId;
                for (TimeInterval interval : validated) {
                    String slotId = "AVL-" + id++;
                    statement.setString(1, slotId);
                    statement.setString(2, professionalId);
                    statement.setString(3, interval.getStartsAt().toString());
                    statement.setString(4, interval.getEndsAt().toString());
                    statement.executeUpdate();
                    created.add(new AvailabilitySlot(slotId, professionalId, interval));
                }
            }
            connection.commit();
            nextId += created.size();
        } catch (SQLException exception) {
            if (transactionStarted) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }
            throw new PersistenceException("Could not save availability", exception);
        } finally {
            if (transactionStarted) {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                } catch (SQLException exception) {
                    throw new PersistenceException("Could not restore the SQLite connection", exception);
                }
            }
        }
        return created;
    }

    public boolean hasOverlap(String professionalId, TimeInterval proposed) {
        if (connection != null) {
            String sql = "SELECT 1 FROM availability_slot WHERE professional_id = ? "
                    + "AND starts_at < ? AND ? < ends_at LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, professionalId);
                statement.setString(2, proposed.getEndsAt().toString());
                statement.setString(3, proposed.getStartsAt().toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            } catch (SQLException exception) {
                throw new PersistenceException("Could not check availability overlap", exception);
            }
        }
        for (AvailabilitySlot slot : slots) {
            if (slot.getProfessionalId().equals(professionalId) && slot.getInterval().overlaps(proposed)) {
                return true;
            }
        }
        return false;
    }

    public List<AvailabilitySlot> findByProfessionalId(String professionalId) {
        if (connection != null) {
            List<AvailabilitySlot> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT slot_id, professional_id, starts_at, ends_at "
                            + "FROM availability_slot WHERE professional_id = ? ORDER BY rowid")) {
                statement.setString(1, professionalId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        result.add(readSlot(rows));
                    }
                }
                return result;
            } catch (SQLException exception) {
                throw new PersistenceException("Could not find availability for " + professionalId, exception);
            }
        }
        List<AvailabilitySlot> result = new ArrayList<>();
        for (AvailabilitySlot slot : slots) {
            if (slot.getProfessionalId().equals(professionalId)) {
                result.add(slot);
            }
        }
        return result;
    }

    public List<AvailabilitySlot> getAll() {
        if (connection != null) {
            List<AvailabilitySlot> result = new ArrayList<>();
            try (Statement statement = connection.createStatement();
                    ResultSet rows = statement.executeQuery(
                            "SELECT slot_id, professional_id, starts_at, ends_at "
                                    + "FROM availability_slot ORDER BY rowid")) {
                while (rows.next()) {
                    result.add(readSlot(rows));
                }
                return result;
            } catch (SQLException exception) {
                throw new PersistenceException("Could not list availability", exception);
            }
        }
        return new ArrayList<>(slots);
    }

    private int readNextId() {
        try (Statement statement = connection.createStatement();
                ResultSet row = statement.executeQuery(
                        "SELECT COALESCE(MAX(CAST(SUBSTR(slot_id, 5) AS INTEGER)), 0) + 1 "
                                + "FROM availability_slot WHERE slot_id LIKE 'AVL-%'")) {
            return row.next() ? row.getInt(1) : 1;
        } catch (SQLException exception) {
            throw new PersistenceException("Could not read the next availability identifier", exception);
        }
    }

    private AvailabilitySlot readSlot(ResultSet row) throws SQLException {
        TimeInterval interval = new TimeInterval(LocalDateTime.parse(row.getString("starts_at")),
                LocalDateTime.parse(row.getString("ends_at")));
        return new AvailabilitySlot(row.getString("slot_id"), row.getString("professional_id"), interval);
    }
}
