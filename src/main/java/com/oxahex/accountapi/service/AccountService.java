package com.oxahex.accountapi.service;

import com.oxahex.accountapi.domain.Account;
import com.oxahex.accountapi.domain.AccountUser;
import com.oxahex.accountapi.dto.AccountDto;
import com.oxahex.accountapi.exception.AccountException;
import com.oxahex.accountapi.repository.AccountRepository;
import com.oxahex.accountapi.repository.AccountUserRepository;
import com.oxahex.accountapi.type.AccountStatus;
import com.oxahex.accountapi.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    /**
     * 계좌를 생성합니다.
     * <p>
     * 요청이 들어온 사용자가 실제로 DB에 존재하는지 확인하고, 계좌 번호를 생성하고, 계좌를 DB에 저장합니다.
     * @param userId 유저 아이디
     * @param initialBalance 계좌 생성 시 기본 잔액
     * @return AccountDto(생성한 계좌의 정보)를 반환
     */
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {
        // 사용자가 있는지 조회
        // type은 기본적으로 Optional임
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

        // Validation
        validateCreateAccount(accountUser);

        // 계좌 번호를 생성하고(없으면 기본값)
        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> Integer.parseInt(account.getAccountNumber()) + 1 + "")
                .orElse("1000000000");

        // 계좌를 저장하고, 그 정보(Entity)를 받음. -> DTO로 변환해서 반환
        // Entity 데이터를 DTO로 변환해 반환
        return AccountDto.fromEntity(
                accountRepository.save(
                        Account.builder()
                                .accountUser(accountUser)
                                .accountStatus(AccountStatus.IN_USE)
                                .accountNumber(newAccountNumber)
                                .balance(initialBalance)
                                .registeredAt(LocalDateTime.now())
                                .build()
                )
        );
    }

    private void validateCreateAccount(AccountUser accountUser) {
        if (accountRepository.countByAccountUser(accountUser) >= 10) {
            throw new AccountException(ErrorCode.MAX_ACCOUNT_PER_USER_10);
        }
    }

    /**
     * 계좌를 삭제 합니다.
     * <p>
     * 사용자 ID와 계좌의 소유주 ID가 같고, 계좌가 존재하는 경우, 이미 해지된 계좌가 아니고, 계좌에 잔액이 없는 경우 계좌의 상태를 변경(UNREGISTERED), 해지 일시를 업데이트 합니다.
     * @param userId 유저 아이디
     * @param accountNumber 해지하려는 계좌의 계좌번호
     * @return 해지된 계좌의 정보
     */
    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        // 사용자가 있는지 조회
        // type은 기본적으로 Optional임
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

        // Account 조회
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        // Validation
        validateDeleteAccount(accountUser, account);

        // 계좌 상태 업데이트, 해지 일자 수정
        account.setAccountStatus(AccountStatus.UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        // Test 위해서 추가한 부분(captor 데이터 보려고): 로직과 관련 없음
        accountRepository.save(account);

        return AccountDto.fromEntity(account);
    }

    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        // 유저와, 계좌 소유주가 다름
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }

        // 이미 해지된 계좌인 경우
        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }

        // 계좌에 잔액이 있는 경우
        if (account.getBalance() > 0) {
            throw new AccountException(ErrorCode.BALANCE_NOT_EMPTY);
        }
    }

    /**
     * 특정 유저의 계좌 목록을 조회합니다.
     * <p>
     * 유저가 존재하는 경우, 해당 유저와 연결된 계좌를 가져와 반환합니다.
     * @param userId
     * @return 사용자와 연결된 계좌 리스트
     */
    @Transactional
    public List<AccountDto> getAccountsByUserId(Long userId) {
        // 사용자가 있는지 조회
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

        // 해당 사용자와 연결된 계좌를 모두 조회
        List<Account> accounts = accountRepository.findByAccountUser(accountUser);

        // AccountDto 타입으로 변환해 반환
        return accounts.stream()
                .map(AccountDto::fromEntity)
                .collect(Collectors.toList());
    }
}