package com.example.limitservice.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyUtilsTest {

    @Test
    void convertsSumToUsd() {
        BigDecimal result = MoneyUtils.toUsd(new BigDecimal("250000.00"), new BigDecimal("500.00"));

        assertThat(result).isEqualByComparingTo("500.00");
        assertThat(result.scale()).isEqualTo(2);
    }

    @Test
    void roundsToTwoDigits() {
        BigDecimal result = MoneyUtils.toUsd(new BigDecimal("10000.45"), new BigDecimal("477.3412"));

        assertThat(result).isEqualTo(new BigDecimal("20.95"));
    }
}
