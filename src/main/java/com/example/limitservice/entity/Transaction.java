package com.example.limitservice.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_from", nullable = false)
    private String accountFrom;

    @Column(name = "account_to", nullable = false)
    private String accountTo;

    @Column(name = "currency_shortname", nullable = false)
    private String currencyShortname;

    @Column(name = "sum", nullable = false)
    private BigDecimal sum;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_category", nullable = false)
    private ExpenseCategory expenseCategory;

    @Column(name = "datetime", nullable = false)
    private OffsetDateTime datetime;

    @Column(name = "sum_usd")
    private BigDecimal sumUsd;

    @Column(name = "limit_exceeded")
    private Boolean limitExceeded;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;
}
