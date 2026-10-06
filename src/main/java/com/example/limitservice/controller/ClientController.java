package com.example.limitservice.controller;

import java.util.List;

import com.example.limitservice.dto.ExceededTransactionResponse;
import com.example.limitservice.dto.LimitRequest;
import com.example.limitservice.dto.LimitResponse;
import com.example.limitservice.service.LimitService;
import com.example.limitservice.service.TransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Client")
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientController {

    private static final String ACCOUNT_REGEXP = "\\d{10}";

    private final LimitService limitService;
    private final TransactionService transactionService;

    @PostMapping("/limits")
    @ResponseStatus(HttpStatus.CREATED)
    public LimitResponse createLimit(@Valid @RequestBody LimitRequest request) {
        return LimitResponse.from(limitService.createLimit(request));
    }

    @GetMapping("/limits")
    public List<LimitResponse> getLimits(@RequestParam @Pattern(regexp = ACCOUNT_REGEXP) String account) {
        return limitService.getLimits(account).stream()
                .map(LimitResponse::from)
                .toList();
    }

    @GetMapping("/transactions/exceeded")
    public List<ExceededTransactionResponse> getExceededTransactions(
            @RequestParam @Pattern(regexp = ACCOUNT_REGEXP) String account) {
        return transactionService.getExceededTransactions(account);
    }
}
