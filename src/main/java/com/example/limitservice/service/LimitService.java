package com.example.limitservice.service;

import java.time.OffsetDateTime;
import java.util.List;

import com.example.limitservice.dto.LimitRequest;
import com.example.limitservice.entity.ExpenseCategory;
import com.example.limitservice.entity.SpendingLimit;

public interface LimitService {

    SpendingLimit createLimit(LimitRequest request);

    List<SpendingLimit> getLimits(String accountNumber);

    SpendingLimit getActiveLimit(String accountNumber, ExpenseCategory category, OffsetDateTime datetime);
}
