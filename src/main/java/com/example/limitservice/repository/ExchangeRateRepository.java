package com.example.limitservice.repository;

import java.time.LocalDate;
import java.util.Optional;

import com.example.limitservice.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByCurrencyShortnameAndRateDate(String currencyShortname, LocalDate rateDate);
}
