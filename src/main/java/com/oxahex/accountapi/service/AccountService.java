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
}