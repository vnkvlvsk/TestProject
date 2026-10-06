package com.example.limitservice.repository;

import java.util.Optional;

import com.example.limitservice.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AccountRepository extends JpaRepository<Account, String> {

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO accounts (account_number) VALUES (:accountNumber) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    void createIfNotExists(@Param("accountNumber") String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByIdForUpdate(@Param("accountNumber") String accountNumber);
}
