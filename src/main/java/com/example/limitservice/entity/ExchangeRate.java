package com.example.limitservice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exchange_rates")
@Getter
@Setter
@NoArgsConstructor
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "currency_shortname", nullable = false)
    private String currencyShortname;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(name = "close_rate", nullable = false)
    private BigDecimal closeRate;

    @Column(name = "close_date", nullable = false)
    private LocalDate closeDate;
}
