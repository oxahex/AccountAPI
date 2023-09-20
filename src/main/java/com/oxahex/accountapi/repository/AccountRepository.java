package com.oxahex.accountapi.repository;

import com.oxahex.accountapi.domain.Account;
import com.oxahex.accountapi.domain.AccountUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findFirstByOrderByIdDesc();   // 가장 마지막에 생성된 Account를 가져옴

    Integer countByAccountUser(AccountUser accountUser);    // accountUser가 생성한 총 계좌 수 반환

    Optional<Account> findByAccountNumber(String accountNumber);    // 계좌 번호로 특정 계좌 데이터 가져옴

    List<Account> findByAccountUser(AccountUser accountUser);   // 유저와 연결된 계좌를 모두 가져옴
}