package com.example.limitservice.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.example.limitservice.entity.ExpenseCategory;
import com.example.limitservice.entity.SpendingLimit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpendingLimitRepository extends JpaRepository<SpendingLimit, Long> {

    Optional<SpendingLimit> findFirstByAccountNumberAndExpenseCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
            String accountNumber, ExpenseCategory expenseCategory, OffsetDateTime datetime);

    List<SpendingLimit> findByAccountNumberOrderByLimitDatetimeAsc(String accountNumber);
}
