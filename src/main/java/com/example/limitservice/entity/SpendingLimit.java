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
@Table(name = "spending_limits")
@Getter
@Setter
@NoArgsConstructor
public class SpendingLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_category", nullable = false)
    private ExpenseCategory expenseCategory;

    @Column(name = "limit_sum", nullable = false)
    private BigDecimal limitSum;

    @Column(name = "limit_currency_shortname", nullable = false)
    private String limitCurrencyShortname;

    @Column(name = "limit_datetime", nullable = false)
    private OffsetDateTime limitDatetime;
}
