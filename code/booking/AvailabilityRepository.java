package booking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AvailabilityRepository {
    private final List<AvailabilitySlot> slots = new ArrayList<>();
    private int nextId = 1;

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
        for (TimeInterval interval : validated) {
            AvailabilitySlot slot = new AvailabilitySlot("AVL-" + nextId++, professionalId, interval);
            slots.add(slot);
            created.add(slot);
        }
        return created;
    }

    public boolean hasOverlap(String professionalId, TimeInterval proposed) {
        for (AvailabilitySlot slot : slots) {
            if (slot.getProfessionalId().equals(professionalId) && slot.getInterval().overlaps(proposed)) {
                return true;
            }
        }
        return false;
    }

    public List<AvailabilitySlot> findByProfessionalId(String professionalId) {
        List<AvailabilitySlot> result = new ArrayList<>();
        for (AvailabilitySlot slot : slots) {
            if (slot.getProfessionalId().equals(professionalId)) {
                result.add(slot);
            }
        }
        return result;
    }

    public List<AvailabilitySlot> getAll() {
        return new ArrayList<>(slots);
    }
}
