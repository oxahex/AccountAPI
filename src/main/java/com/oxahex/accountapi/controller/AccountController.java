package com.oxahex.accountapi.controller;

import com.oxahex.accountapi.dto.CreateAccount;
import com.oxahex.accountapi.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/account")
    public CreateAccount.Response createAccount(
            @RequestBody @Valid CreateAccount.Request request) {

        // Controller에서 받아온 데이터로 Account Service 동작
        // Account Service에서 받아온 데이터(AccountDTO)를 CreateAccount.Response 타입으로 변경해 반환
        return CreateAccount.Response.from(
                accountService.createAccount(
                        request.getUserId(),
                        request.getInitialBalance()
                )
        );
    }
}