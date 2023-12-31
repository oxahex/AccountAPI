package com.oxahex.accountapi.service;

import com.oxahex.accountapi.domain.Account;
import com.oxahex.accountapi.domain.AccountUser;
import com.oxahex.accountapi.domain.Transaction;
import com.oxahex.accountapi.dto.TransactionDto;
import com.oxahex.accountapi.exception.AccountException;
import com.oxahex.accountapi.repository.AccountRepository;
import com.oxahex.accountapi.repository.AccountUserRepository;
import com.oxahex.accountapi.repository.TransactionRepository;
import com.oxahex.accountapi.type.AccountStatus;
import com.oxahex.accountapi.type.ErrorCode;
import com.oxahex.accountapi.type.TransactionResultType;
import com.oxahex.accountapi.type.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    /**
     * 거래 정보 저장 및 사용자 계좌의 잔액 업데이트
     * @param userId 유저 아이디
     * @param accountNumber 거래하려는 계좌의 계좌번호
     * @param amount 거래 금액
     * @return 거래 내역에 대한 정보
     */
    @Transactional
    public TransactionDto useBalance(
            Long userId, String accountNumber, Long amount) {

        // 유저 정보와 계좌 정보를 가져옴
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        // Validation
        validateUseBalance(accountUser, account, amount);

        // 계좌 잔액 - 거래 금액
        account.useBalance(amount);

        // 변경 사항(잔액 변경) DB 업데이트 후 DTO로 변환해 반환
        return TransactionDto.fromEntity(
                saveAndGetTransaction(TransactionType.USE, TransactionResultType.S, account, amount)
        );
    }

    private void validateUseBalance(AccountUser accountUser, Account account, Long amount) {
        // 사용자 ID와 계좌 소유주 ID가 다른 경우
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }

        // 계좌가 이미 해지 상태인 경우
        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }

        // 거래 금액이 잔액보다 큰 경우
        if (account.getBalance() < amount) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }

        // TODO: 거래 금액이 너무 작거나 큰 경우
    }

    @Transactional
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(TransactionType.USE, TransactionResultType.F, account, amount);
    }


    /**
     * 거래 취소
     * <P> 거래 취소 유효성 판단 후, 기존 금액으로 롤백
     * @param transactionId 취소하려는 거래의 ID
     * @param accountNumber 거래가 일어난 계좌 번호
     * @param amount 취소 금액
     * @return 거래 취소 정보 데이터
     */
    @Transactional
    public TransactionDto cancelBalance(
            String transactionId, String accountNumber, Long amount) {

        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        // Validation
        validateCancelBalance(transaction, account, amount);

        // 계좌 잔액 + 취소 금액
        account.cancelBalance(amount);

        // 변경 사항(잔액 변경) DB 업데이트 후 DTO로 변환해 반환
        return TransactionDto.fromEntity(
                saveAndGetTransaction(TransactionType.CANCEL, TransactionResultType.S, account, amount)
        );
    }


    @Transactional
    public void saveFailedCancelTransaction(
            String accountNumber, Long amount
    ) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(TransactionType.CANCEL, TransactionResultType.F, account, amount);
    }

    private void validateCancelBalance(Transaction transaction, Account account, Long amount) {
        // 거래와 계좌가 일치하지 않는 경우
        if (!Objects.equals(transaction.getAccount().getId(), account.getId())) {
            throw new AccountException(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH);
        }

        // 거래 금액과 거래 취소 금액이 다른 경우(부분 취소 불가 정책)
        if (!Objects.equals(transaction.getAmount(), amount)) {
            throw new AccountException(ErrorCode.CANCEL_MUST_FULLY);
        }

        // 1년이 넘은 거래를 취소 시도하는 경우
        if (transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))) {
            throw new AccountException(ErrorCode.TOO_OLD_ORDER_TO_CANCEL);
        }

        // 계좌가 이미 해지 상태인 경우
        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }
    }

    @Transactional
    public Transaction saveAndGetTransaction(
            TransactionType transactionType,
            TransactionResultType transactionResultType,
            Account account,
            Long amount)
    {
        return transactionRepository.save(
                Transaction.builder()
                        .transactionType(transactionType)
                        .transactionResultType(transactionResultType)
                        .account(account)
                        .amount(amount)
                        .balanceSnapShot(account.getBalance())
                        .transactionId(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build()
        );
    }

    /**
     * 특정 거래 내역 조회
     * <p> 거래 ID 존재 여부 확인 후 거래 내역 정보 반환
     * @param transactionId 조회하고자 하는 거래 ID
     * @return 거래 내역 정보
     */
    @Transactional
    public TransactionDto queryTransaction(String transactionId) {
        // transaction id 로 거래가 있는지 확인 후 반환
        return TransactionDto.fromEntity(transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND)));
    }
}