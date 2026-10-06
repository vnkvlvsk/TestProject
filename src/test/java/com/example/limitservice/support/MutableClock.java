package com.example.limitservice.support;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class MutableClock extends Clock {

    private volatile Instant instant = Instant.now();

    public void setTime(OffsetDateTime datetime) {
        this.instant = datetime.toInstant();
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
