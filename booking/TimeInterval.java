package booking;

import java.time.LocalDateTime;

// so basically this the time interval whcich im expecting to be shared with Member 2's availability module
public class TimeInterval {
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;

    public TimeInterval(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("Start must be earlier than end");
        }
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public LocalDateTime getStartsAt() { return startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }

    // as suggested by AI, i went with with half-open intervals, which is somehting like this: [start, end). Also keep in mind that adjacent bookings do not conflict.
    public boolean overlaps(TimeInterval other) {
        return startsAt.isBefore(other.endsAt) && other.startsAt.isBefore(endsAt);
    }
}
