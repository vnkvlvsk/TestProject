package com.example.limitservice.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class DateUtilsTest {

    private static final ZoneId UTC = ZoneOffset.UTC;

    @Test
    void startOfMonthIsFirstDayAtMidnight() {
        OffsetDateTime result = DateUtils.startOfMonth(OffsetDateTime.parse("2022-01-13T15:30:00Z"), UTC);

        assertThat(result).isEqualTo(OffsetDateTime.parse("2022-01-01T00:00:00Z"));
    }

    @Test
    void startOfMonthUsesGivenZoneNotOffsetOfTransaction() {
        OffsetDateTime result = DateUtils.startOfMonth(OffsetDateTime.parse("2022-02-01T03:00:00+06:00"), UTC);

        assertThat(result).isEqualTo(OffsetDateTime.parse("2022-01-01T00:00:00Z"));
    }

    @Test
    void startOfNextMonthAfterDecemberIsNextYear() {
        OffsetDateTime result = DateUtils.startOfNextMonth(OffsetDateTime.parse("2022-12-31T23:59:59Z"), UTC);

        assertThat(result).isEqualTo(OffsetDateTime.parse("2023-01-01T00:00:00Z"));
    }

    @Test
    void toLocalDateConvertsToGivenZone() {
        LocalDate result = DateUtils.toLocalDate(OffsetDateTime.parse("2022-01-30T00:00:00+06:00"), UTC);

        assertThat(result).isEqualTo(LocalDate.of(2022, 1, 29));
    }
}
