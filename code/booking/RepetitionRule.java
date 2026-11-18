package booking;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class RepetitionRule {
    private final RecurrenceType type;
    private final Set<DayOfWeek> weekdays;
    private final int dayOfMonth;

    public RepetitionRule(RecurrenceType type, Set<DayOfWeek> weekdays, int dayOfMonth) {
        if (type == null) {
            throw new AvailabilityException("INVALID_AVAILABILITY_DATA");
        }
        if (type == RecurrenceType.WEEKLY && (weekdays == null || weekdays.isEmpty())) {
            throw new AvailabilityException("INVALID_AVAILABILITY_DATA");
        }
        if (type == RecurrenceType.MONTHLY && (dayOfMonth < 1 || dayOfMonth > 31)) {
            throw new AvailabilityException("INVALID_AVAILABILITY_DATA");
        }

        this.type = type;
        this.weekdays = weekdays == null || weekdays.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(weekdays));
        this.dayOfMonth = dayOfMonth;
    }

    public static RepetitionRule weekly(Set<DayOfWeek> weekdays) {
        return new RepetitionRule(RecurrenceType.WEEKLY, weekdays, 0);
    }

    public static RepetitionRule monthly(int dayOfMonth) {
        return new RepetitionRule(RecurrenceType.MONTHLY, Collections.<DayOfWeek>emptySet(), dayOfMonth);
    }

    public RecurrenceType getType() { return type; }
    public Set<DayOfWeek> getWeekdays() { return weekdays; }
    public int getDayOfMonth() { return dayOfMonth; }

    public boolean matches(LocalDate date) {
        if (date == null) {
            return false;
        }
        if (type == RecurrenceType.WEEKLY) {
            return weekdays.contains(date.getDayOfWeek());
        }
        return date.getDayOfMonth() == dayOfMonth;
    }
}
