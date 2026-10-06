package com.example.limitservice.dto;

import java.math.BigDecimal;

import com.example.limitservice.entity.ExpenseCategory;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record LimitRequest(
        @NotNull @Pattern(regexp = "\\d{10}", message = "must be 10 digits")
        String account,

        @NotNull
        ExpenseCategory expenseCategory,

        @NotNull @Positive @Digits(integer = 17, fraction = 2)
        BigDecimal limitSum
) {
}
