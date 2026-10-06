package com.example.limitservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.example.limitservice.entity.ExpenseCategory;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record TransactionRequest(
        @NotNull @Pattern(regexp = "\\d{10}", message = "must be 10 digits")
        String accountFrom,

        @NotNull @Pattern(regexp = "\\d{10}", message = "must be 10 digits")
        String accountTo,

        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "must be ISO 4217 code, for example KZT")
        String currencyShortname,

        @NotNull @Positive @Digits(integer = 17, fraction = 2)
        BigDecimal sum,

        @NotNull
        ExpenseCategory expenseCategory,

        @NotNull
        OffsetDateTime datetime
) {
}
