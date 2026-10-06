package com.example.limitservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.example.limitservice.entity.ExpenseCategory;
import com.example.limitservice.entity.SpendingLimit;

public record LimitResponse(
        Long id,
        String account,
        ExpenseCategory expenseCategory,
        BigDecimal limitSum,
        String limitCurrencyShortname,
        OffsetDateTime limitDatetime
) {

    public static LimitResponse from(SpendingLimit limit) {
        return new LimitResponse(
                limit.getId(),
                limit.getAccountNumber(),
                limit.getExpenseCategory(),
                limit.getLimitSum(),
                limit.getLimitCurrencyShortname(),
                limit.getLimitDatetime());
    }
}
