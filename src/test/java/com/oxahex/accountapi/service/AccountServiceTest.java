package com.oxahex.accountapi.service;

import com.oxahex.accountapi.domain.Account;
import com.oxahex.accountapi.domain.AccountUser;
import com.oxahex.accountapi.dto.AccountDto;
import com.oxahex.accountapi.exception.AccountException;
import com.oxahex.accountapi.repository.AccountRepository;
import com.oxahex.accountapi.repository.AccountUserRepository;
import com.oxahex.accountapi.type.AccountStatus;
import com.oxahex.accountapi.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
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

    @Test
    @DisplayName("계좌 삭제 - 성공")
    void deleteAccount() {
        // given: 유저가 있음
        AccountUser user = AccountUser.builder()
                .id(12L).name("oxahex").build();
        // given: 유저 정보를 DB에서 가져옴
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                                .balance(0L)
                        .accountNumber("10000000001").build()));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        // when
        AccountDto accountDto = accountService.deleteAccount(1L, "1234567890");

        // then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("10000000001", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 유저 없음")
    void deleteAccount_UserNotFound() {
        // given: 유저 없음
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when: 유저가 없는데 계좌 생성 시도
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        // then: Exception USER_NOT_FOUND
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 계좌 없음")
    void deleteAccount_AccountNotFound() {
        // given: 유저 있음
        AccountUser user = AccountUser.builder().id(1L).name("oxahex").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        // given: 계좌 없음
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when: 계좌가 없는데 계좌 생성 시도
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        // then: Exception ACCOUNT_NOT_FOUND
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 유저와 계좌 소유주가 다름")
    void deleteAccount_UserUnMatch() {
        // given: userA 유저의 계좌를 삭제하려는데, 계좌의 소유주가 userB
        AccountUser userA = AccountUser.builder().id(1L).name("userA").build();
        AccountUser userB = AccountUser.builder().id(2L).name("userB").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(userA));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(userB)
                        .balance(0L)
                        .accountNumber("1234567890").build()));

        // when: 해지를 요청한 유저(userA)와 계좌 소유주(userB)가 다른데 계좌 해지 시도
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        // then: Exception USER_ACCOUNT_UN_MATCH
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 계좌에 잔액이 남아 있음")
    void deleteAccount_BalanceNotEmpty() {
        // given: 삭제하려는 계좌의 소유주, 잔액이 있는 계좌(10000)
        AccountUser user = AccountUser.builder().id(1L).name("user").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(10000L)
                        .accountNumber("1234567890").build()));

        // when: 잔액이 있는 계좌 삭제 시도
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        // then: Exception BALANCE_NOT_EMPTY
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 이미 해지 처리 된 계좌")
    void deleteAccount_AlreadyUnRegisteredAccount() {
        // given: 삭제하려는 계좌의 소유주, 이미 해지 처리 된 계좌
        AccountUser user = AccountUser.builder().id(1L).name("user").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(0L)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .accountNumber("1234567890").build()));

        // when: 잔액이 있는 계좌 삭제 시도
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        // then: Exception ACCOUNT_ALREADY_UNREGISTERED
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 조회 - 성공")
    void getAccountsByUserId() {
        // given: 유저가 존재함
        AccountUser user = AccountUser.builder().id(1L).name("user").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        // given: 계좌가 3개 존재함
        List<Account> accounts = Arrays.asList(
                Account.builder().accountUser(user).accountNumber("1234567890").balance(10000L).build(),
                Account.builder().accountUser(user).accountNumber("1234567891").balance(20000L).build(),
                Account.builder().accountUser(user).accountNumber("1234567892").balance(30000L).build()
        );
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);

        // when
        List<AccountDto> accountDtos = accountService.getAccountsByUserId(1L);

        // then: 개수, 계좌 정보
        assertEquals(3, accountDtos.size());
        assertEquals("1234567890", accountDtos.get(0).getAccountNumber());
        assertEquals(10000, accountDtos.get(0).getBalance());
        assertEquals("1234567891", accountDtos.get(1).getAccountNumber());
        assertEquals(20000, accountDtos.get(1).getBalance());
        assertEquals("1234567892", accountDtos.get(2).getAccountNumber());
        assertEquals(30000, accountDtos.get(2).getBalance());
    }

    @Test
    @DisplayName("계좌 조회 실패 - 유저 없음")
    void getAccountsByUserId_UserNotFound() {
        // given: 유저 없음
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when: 유저가 없는데 계좌 조회 시도
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));

        // then: Exception USER_NOT_FOUND
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}