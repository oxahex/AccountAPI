package com.oxahex.accountapi.repository;

import com.oxahex.accountapi.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findFirstByOrderByIdDesc();   // 가장 마지막에 생성된 Account를 가져옴
}