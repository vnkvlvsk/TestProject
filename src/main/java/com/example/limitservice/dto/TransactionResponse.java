package com.example.limitservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.example.limitservice.entity.ExpenseCategory;
import com.example.limitservice.entity.Transaction;
import com.example.limitservice.entity.TransactionStatus;

public record TransactionResponse(
        Long id,
        String accountFrom,
        String accountTo,
        String currencyShortname,
        BigDecimal sum,
        ExpenseCategory expenseCategory,
        OffsetDateTime datetime,
        BigDecimal sumUsd,
        Boolean limitExceeded,
        TransactionStatus status
) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountFrom(),
                transaction.getAccountTo(),
                transaction.getCurrencyShortname(),
                transaction.getSum(),
                transaction.getExpenseCategory(),
                transaction.getDatetime(),
                transaction.getSumUsd(),
                transaction.getLimitExceeded(),
                transaction.getStatus());
    }
}
