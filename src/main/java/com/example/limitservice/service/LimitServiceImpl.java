package com.example.limitservice.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import com.example.limitservice.config.LimitProperties;
import com.example.limitservice.dto.LimitRequest;
import com.example.limitservice.entity.ExpenseCategory;
import com.example.limitservice.entity.SpendingLimit;
import com.example.limitservice.repository.AccountRepository;
import com.example.limitservice.repository.SpendingLimitRepository;
import com.example.limitservice.util.DateUtils;
import com.example.limitservice.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimitServiceImpl implements LimitService {

    private final SpendingLimitRepository limitRepository;
    private final AccountRepository accountRepository;
    private final LimitProperties limitProperties;
    private final Clock clock;

    @Override
    public SpendingLimit createLimit(LimitRequest request) {
        accountRepository.createIfNotExists(request.account());

        SpendingLimit limit = new SpendingLimit();
        limit.setAccountNumber(request.account());
        limit.setExpenseCategory(request.expenseCategory());
        limit.setLimitSum(request.limitSum());
        limit.setLimitCurrencyShortname(MoneyUtils.USD);
        limit.setLimitDatetime(OffsetDateTime.now(clock));

        SpendingLimit saved = limitRepository.save(limit);
        log.info("New limit {} USD for account {} and category {}",
                saved.getLimitSum(), saved.getAccountNumber(), saved.getExpenseCategory());
        return saved;
    }

    @Override
    public List<SpendingLimit> getLimits(String accountNumber) {
        return limitRepository.findByAccountNumberOrderByLimitDatetimeAsc(accountNumber);
    }

    @Override
    public SpendingLimit getActiveLimit(String accountNumber, ExpenseCategory category, OffsetDateTime datetime) {
        return limitRepository
                .findFirstByAccountNumberAndExpenseCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                        accountNumber, category, datetime)
                .orElseGet(() -> createDefaultLimit(accountNumber, category, datetime));
    }

    private SpendingLimit createDefaultLimit(String accountNumber, ExpenseCategory category, OffsetDateTime datetime) {
        SpendingLimit limit = new SpendingLimit();
        limit.setAccountNumber(accountNumber);
        limit.setExpenseCategory(category);
        limit.setLimitSum(limitProperties.defaultLimitSum());
        limit.setLimitCurrencyShortname(MoneyUtils.USD);
        limit.setLimitDatetime(DateUtils.startOfMonth(datetime, limitProperties.zone()));

        log.info("No limit for account {} and category {}, creating default {} USD",
                accountNumber, category, limit.getLimitSum());
        return limitRepository.save(limit);
    }
}
