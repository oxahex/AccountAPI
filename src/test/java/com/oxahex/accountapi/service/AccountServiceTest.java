package com.oxahex.accountapi.service;

import com.oxahex.accountapi.domain.Account;
import com.oxahex.accountapi.domain.AccountUser;
import com.oxahex.accountapi.dto.AccountDto;
import com.oxahex.accountapi.exception.AccountException;
import com.oxahex.accountapi.repository.AccountRepository;
import com.oxahex.accountapi.repository.AccountUserRepository;
import com.oxahex.accountapi.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("계좌 생성 - 성공")
    void createAccount() {
        // given: 유저(userId: 12, name: "oxahex")
        AccountUser user = AccountUser.builder().id(12L).name("oxahex").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000001").build()));

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000009").build());      // 비정상 값

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        // when: 유저(userId: 12, name: "oxahex")가 계좌를 생성함
        AccountDto accountDto = accountService.createAccount(1L, 10000L);

        // then: 생성된 계좌의 유저 id = 12, accountNumber = 1000000002
        verify(accountRepository, times(1)).save(captor.capture());     // 1번 save -> capture
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000002", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("계좌 생성(기존 계좌가 없는 경우) - 성공")
    void createFirstAccount() {
        // given: 유저(userId: 1, name: "oxahex")
        AccountUser user = AccountUser.builder().id(1L).name("oxahex").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000009").build());      // 비정상 값

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        // when: 유저(userId: 1, name: "oxahex")가 첫 번째 계좌를 생성함
        AccountDto accountDto = accountService.createAccount(1L, 10000L);

        // then: 생성된 계좌의 유저 id = 1, accountNumber = 1000000000
        verify(accountRepository, times(1)).save(captor.capture());     // 1번 save -> capture
        assertEquals(1L, accountDto.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("계좌 생성 실패 - 유저 없음")
    void createAccount_UserNotFound() {
        // given: 유저 없음
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when: 유저가 없는데 계좌 생성 시도
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 10000L));

        // then: Exception USER_NOT_FOUND
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 생성 실패 - 최대 생성 가능 계좌 수 초과")
    void createAccount_MaxAccountIs10() {
        // given: 유저가 가지고 있는 계좌가 10개일 때
        AccountUser user = AccountUser.builder()
                .id(15L).name("oxahex").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);

        // when: 해당 유저가 계좌를 생성 시도함
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 10000L));

        // then: Exception MAX_ACCOUNT_PER_USER_10
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }
}