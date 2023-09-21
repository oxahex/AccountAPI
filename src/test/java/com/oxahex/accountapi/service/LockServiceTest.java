package com.oxahex.accountapi.service;

import com.oxahex.accountapi.exception.AccountException;
import com.oxahex.accountapi.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LockServiceTest {
    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private LockService lockService;

    @Test
    @DisplayName("Lock 얻어오기 - 성공")
    void getLock() throws InterruptedException {
        // given: tryLock -> 성공
        given(redissonClient.getLock(anyString()))
                .willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any()))
                .willReturn(true);

        // when: lock 요청 문제가 없는 경우
        // then
        assertDoesNotThrow(() -> lockService.lock("123"));
    }

    @Test
    @DisplayName("Lock 얻어오기 - 실패")
    void getLock_Fail() throws InterruptedException {
        // given: tryLock -> 실패
        given(redissonClient.getLock(anyString()))
                .willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any()))
                .willReturn(false);

        // when: lock 요청 문제가 있는 경우 exception 발생
        AccountException exception = assertThrows(AccountException.class,
                () -> lockService.lock("123"));

        // then
        assertEquals(ErrorCode.ACCOUNT_TRANSACTION_LOCK, exception.getErrorCode());
    }
}