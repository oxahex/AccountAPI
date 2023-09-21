package com.oxahex.accountapi.controller;

import com.oxahex.accountapi.aop.AccountLock;
import com.oxahex.accountapi.dto.CancelBalance;
import com.oxahex.accountapi.dto.QueryTransactionResponse;
import com.oxahex.accountapi.dto.UseBalance;
import com.oxahex.accountapi.exception.AccountException;
import com.oxahex.accountapi.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/transaction/use")
    @AccountLock
    public UseBalance.Response useBalance(
            @Valid @RequestBody UseBalance.Request request) throws InterruptedException {

        try {
            Thread.sleep(5000L);
            return UseBalance.Response.from(
                    transactionService.useBalance(
                            request.getUserId(),
                            request.getAccountNumber(),
                            request.getAmount()
                    )
            );
        } catch (AccountException e) {
            log.error("Failed to use balance.");

            // 실패 시 실패 데이터 업데이트
            transactionService.saveFailedUseTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );

            throw e;
        }
    }

    @PostMapping("/transaction/cancel")
    @AccountLock
    public CancelBalance.Response cancelBalance(
            @Valid @RequestBody CancelBalance.Request request) {

        try {
            return CancelBalance.Response.from(
                    transactionService.cancelBalance(
                            request.getTransactionId(),
                            request.getAccountNumber(),
                            request.getAmount()
                    )
            );
        } catch (AccountException e) {
            log.error("Failed to cancel balance.");

            // 실패 시 실패 데이터 업데이트
            transactionService.saveFailedCancelTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );

            throw e;
        }
    }

    @GetMapping("/transaction/{transactionId}")
    public QueryTransactionResponse queryTransaction(
            @PathVariable String transactionId) {

        return QueryTransactionResponse.from(
                transactionService.queryTransaction(transactionId)
        );
    }
}