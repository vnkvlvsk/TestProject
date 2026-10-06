package com.example.limitservice.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    public static final String USD = "USD";
    private static final int MONEY_SCALE = 2;

    private MoneyUtils() {
    }

    public static BigDecimal toUsd(BigDecimal sum, BigDecimal rate) {
        return sum.divide(rate, MONEY_SCALE, RoundingMode.HALF_EVEN);
    }
}
