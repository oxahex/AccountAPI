package com.oxahex.accountapi.service;

import com.oxahex.accountapi.domain.Account;
import com.oxahex.accountapi.domain.AccountUser;
import com.oxahex.accountapi.domain.Transaction;
import com.oxahex.accountapi.dto.TransactionDto;
import com.oxahex.accountapi.exception.AccountException;
import com.oxahex.accountapi.repository.AccountRepository;
import com.oxahex.accountapi.repository.AccountUserRepository;
import com.oxahex.accountapi.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.oxahex.accountapi.type.AccountStatus.*;
import static com.oxahex.accountapi.type.ErrorCode.*;
import static com.oxahex.accountapi.type.TransactionResultType.*;
import static com.oxahex.accountapi.type.TransactionType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    public static final long ACCOUNT_BALANCE = 10000L;
    public static final long USE_AMOUNT = 1000L;
    public static final long CANCEL_AMOUNT = 2000L;
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("잔액 사용 - 성공")
    void useBalance() {
        // given: 유저와, 유저의 계좌(10000) 확인
        AccountUser user = AccountUser.builder()
                .id(1L).name("oxahex").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(ACCOUNT_BALANCE)
                .accountNumber("1234567890").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // given: 거래 결과가 저장됨
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(USE_AMOUNT)
                        .balanceSnapShot(1000L).build()
                );

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when: 1000원 사용
        TransactionDto transactionDto = transactionService.useBalance(
                1L, "1234567890", USE_AMOUNT
        );

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        // then: 계좌에 9000원 남음
        assertEquals(ACCOUNT_BALANCE - USE_AMOUNT, captor.getValue().getBalanceSnapShot());
        // then: 거래 결과 == 성공
        assertEquals(S, transactionDto.getTransactionResultType());
        // then: 거래 타입 == USE
        assertEquals(USE, transactionDto.getTransactionType());
        // then: 거래 금액 == 1000
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 유저 없음")
    void useBalance_UserNotFound() {
        // given: 유저가 없음
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when: 유저가 없는 경우(사용 시도 전에 Exception 처리)
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", USE_AMOUNT));

        // then: Exception USER_NOT_FOUND
        assertEquals(USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 계좌 없음")
    void useBalance_AccountNotFound() {
        // given: 유저 있음
        AccountUser user = AccountUser.builder().id(1L).name("oxahex").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        // given: 계좌 없음
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when: 계좌가 없는데 잔액 사용 시도
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", USE_AMOUNT));

        // then: Exception ACCOUNT_NOT_FOUND
        assertEquals(ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 유저와 계좌 소유주가 다름")
    void useBalance_UserUnMatch() {
        // given: userA 유저가 잔액을 사용하려는데, 계좌의 소유주가 userB
        AccountUser userA = AccountUser.builder().id(1L).name("userA").build();
        AccountUser userB = AccountUser.builder().id(2L).name("userB").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(userA));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(userB)
                        .balance(0L)
                        .accountNumber("1234567890").build()));

        // when: 잔액 사용을 요청한 유저(userA)와 계좌 소유주(userB)가 다른데 잔액 사용 시도
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        // then: Exception USER_ACCOUNT_UN_MATCH
        assertEquals(USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 이미 해지 처리 된 계좌")
    void useBalance_AlreadyUnRegisteredAccoun() {
        // given: 계좌의 소유주와 유저 일치하나, 이미 해지 처리 된 계좌
        AccountUser user = AccountUser.builder().id(1L).name("user").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(0L)
                        .accountStatus(UNREGISTERED)
                        .accountNumber("1234567890").build()));

        // when: 이미 해지 처리 된 계좌의 잔액 사용 시도
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        // then: Exception ACCOUNT_ALREADY_UNREGISTERED
        assertEquals(ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 거래 금액이 잔액보다 큼")
    void useBalance_AmountExceedBalance() {
        // given: 거래 금액이 잔액보다 큼
        AccountUser user = AccountUser.builder()
                .id(1L).name("oxahex").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(100L)
                .accountNumber("1234567890").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when: 잔액이 100원인 계좌에서 10000원 사용 시도
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 10000L));

        // then: Exception AMOUNT_EXCEED_BALANCE
        assertEquals(AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
        verify(transactionRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("잔액 사용 실패 트랜잭션 저장 - 성공")
    void saveFailedUseTransaction() {
        // given: 유저와, 유저의 계좌(10000) 확인
        AccountUser user = AccountUser.builder()
                .id(1L).name("oxahex").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(ACCOUNT_BALANCE)
                .accountNumber("1234567890").build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // given: 거래 결과가 저장됨
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(USE_AMOUNT)
                        .balanceSnapShot(ACCOUNT_BALANCE).build()
                );

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when: 1000원 사용 시도 했으나 실패
        transactionService.saveFailedUseTransaction("1234567890", 1000L);

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        // then: 거래 금액 == 1000
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        // then: 계좌에 10000원 남음(실패로 차감되지 않음)
        assertEquals(ACCOUNT_BALANCE, captor.getValue().getBalanceSnapShot());
        // then: 거래 결과 == 성공
        assertEquals(F, captor.getValue().getTransactionResultType());
    }

    @Test
    @DisplayName("잔액 취소 - 성공")
    void cancelBalance() {
        // given: 계좌 확인
        Account account = Account.builder()
                .accountStatus(IN_USE)
                .balance(ACCOUNT_BALANCE)
                .accountNumber("1234567890").build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapShot(ACCOUNT_BALANCE).build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        // given: 거래 결과가 저장됨
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(CANCEL)
                        .transactionResultType(S)
                        .transactionId("transactionIdForCancel")
                        .transactedAt(LocalDateTime.now())
                        .amount(CANCEL_AMOUNT)
                        .balanceSnapShot(ACCOUNT_BALANCE).build()
                );

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when: 1000원 사용
        TransactionDto transactionDto = transactionService.cancelBalance(
                "transactionId", "1234567890", CANCEL_AMOUNT
        );

        // then: transaction 결과 capture
        verify(transactionRepository, times(1)).save(captor.capture());
        // then: 계좌에 12000원 남음
        assertEquals(ACCOUNT_BALANCE + CANCEL_AMOUNT, captor.getValue().getBalanceSnapShot());
        // then: 거래 결과 == 성공
        assertEquals(S, transactionDto.getTransactionResultType());
        // then: 거래 타입 == CANCEL
        assertEquals(CANCEL, transactionDto.getTransactionType());
        // then: 거래 금액 == 1000
        assertEquals(CANCEL_AMOUNT, captor.getValue().getAmount());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 해당 계좌 없음")
    void cancelBalance_AccountNotFount() {
        // given: 기존 잔액 사용 transaction 있음
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));

        // given: 계좌 없음
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when: 계좌가 없는데 기존 잔액 사용 취소 시도
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("1234567890", "1234567890", CANCEL_AMOUNT));

        // then: Exception ACCOUNT_NOT_FOUND
        assertEquals(ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 해당 거래 없음")
    void cancelBalance_TransactionNotFound() {
        // given: 기존 잔액 사용 transaction 없음
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when: 기존 거래가 없는데 잔액 사용 취소 시도
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("1234567890", "1234567890", CANCEL_AMOUNT));

        // then: Exception ACCOUNT_NOT_FOUND
        assertEquals(TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 거래가 일어난 계좌와 요청 계좌가 다름")
    void cancelBalance_TransactionAccountUnMatch() {
        // given: 계좌A, 계좌B, 기존 거래 정보가 주어짐
        AccountUser user = AccountUser.builder().build();
        Account accountA = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(ACCOUNT_BALANCE)
                .accountNumber("1234567890").build();
        Account accountB = Account.builder()
                .id(2L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(ACCOUNT_BALANCE)
                .accountNumber("1234567891").build();

        Transaction transaction = Transaction.builder()
                .account(accountA)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapShot(ACCOUNT_BALANCE)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountB));

        // when: 계좌 A에서 일어난 잔액 사용을 계좌 B에서 취소 요청
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567891", CANCEL_AMOUNT)
        );

        // then: Exception TRANSACTION_ACCOUNT_UN_MATCH
        assertEquals(TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 거래 금액과 취소 금액이 다름")
    void cancelBalance_CancelMustFully() {
        // given: 계좌와 기존 거래 정보가 주어짐
        AccountUser user = AccountUser.builder().build();
        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(ACCOUNT_BALANCE)
                .accountNumber("1234567890").build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT + 1000L)
                .balanceSnapShot(ACCOUNT_BALANCE)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when: 실제 사용한 잔액과 취소하려는 금액이 다름
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567891", CANCEL_AMOUNT)
        );

        // then: Exception CANCEL_MUST_FULLY
        assertEquals(CANCEL_MUST_FULLY, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 1년이 지난 거래는 취소할 수 없음")
    void cancelBalance_TooOldOrderToCancel() {
        // given: 계좌와 2년 전 거래가 주어짐
        AccountUser user = AccountUser.builder().build();
        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(ACCOUNT_BALANCE)
                .accountNumber("1234567890").build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .amount(CANCEL_AMOUNT)
                .balanceSnapShot(ACCOUNT_BALANCE)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when: 1년 이상 지난 거래를 취소 요청
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1234567891", CANCEL_AMOUNT)
        );

        // then: Exception TOO_OLD_ORDER_TO_CANCEL
        assertEquals(TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());
    }
}