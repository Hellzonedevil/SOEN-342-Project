package booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AvailabilityRule {
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final RepetitionRule repetitionRule;
    private final LocalTime startTime;
    private final LocalTime endTime;

    public AvailabilityRule(LocalDate startDate, LocalDate endDate, RepetitionRule repetitionRule,
            LocalTime startTime, LocalTime endTime) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)
                || repetitionRule == null || startTime == null || endTime == null
                || !startTime.isBefore(endTime)) {
            throw new AvailabilityException("INVALID_AVAILABILITY_DATA");
        }
        this.startDate = startDate;
        this.endDate = endDate;
        this.repetitionRule = repetitionRule;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public RepetitionRule getRepetitionRule() { return repetitionRule; }

    public List<TimeInterval> generateIntervals() {
        List<TimeInterval> intervals = new ArrayList<>();
        for (LocalDate date = startDate; ; date = date.plusDays(1)) {
            if (repetitionRule.matches(date)) {
                intervals.add(new TimeInterval(LocalDateTime.of(date, startTime), LocalDateTime.of(date, endTime)));
            }
            if (date.equals(endDate)) {
                break;
            }
        }
        return intervals;
    }
}
