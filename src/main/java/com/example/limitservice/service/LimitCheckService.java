package com.example.limitservice.service;

import java.math.BigDecimal;

import com.example.limitservice.entity.Transaction;

public interface LimitCheckService {

    Transaction checkAndSave(Transaction transaction, BigDecimal rateToUsd);
}
