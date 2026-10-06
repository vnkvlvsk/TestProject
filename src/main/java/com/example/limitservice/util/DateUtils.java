package com.example.limitservice.util;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class DateUtils {

    private DateUtils() {
    }

    public static OffsetDateTime startOfMonth(OffsetDateTime datetime, ZoneId zone) {
        return startOfMonthInZone(datetime, zone).toOffsetDateTime();
    }

    public static OffsetDateTime startOfNextMonth(OffsetDateTime datetime, ZoneId zone) {
        return startOfMonthInZone(datetime, zone).plusMonths(1).toOffsetDateTime();
    }

    public static LocalDate toLocalDate(OffsetDateTime datetime, ZoneId zone) {
        return datetime.atZoneSameInstant(zone).toLocalDate();
    }

    private static ZonedDateTime startOfMonthInZone(OffsetDateTime datetime, ZoneId zone) {
        return datetime.atZoneSameInstant(zone)
                .toLocalDate()
                .withDayOfMonth(1)
                .atStartOfDay(zone);
    }
}
