package com.oxahex.accountapi.service;

import com.oxahex.accountapi.dto.UseBalance;
import com.oxahex.accountapi.exception.AccountException;
import com.oxahex.accountapi.type.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LockAopAspectTest {
    @Mock
    private LockService lockService;

    @Mock
    ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    private LockAopAspect lockAopAspect;

    @Test
    @DisplayName("Lock, Unlock - 성공")
    void lockAndUnlock() throws Throwable {
        // given: 잔액 사용 Request가 있음
        ArgumentCaptor<String> lockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unlockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);
        UseBalance.Request request =
                new UseBalance.Request(123L, "1234567890", 1000L);

        // when: AopAspect 동작
        lockAopAspect.aroundMethod(proceedingJoinPoint, request);

        // then
        verify(lockService, times(1))
                .lock(lockArgumentCaptor.capture());
        verify(lockService, times(1))
                .unlock(unlockArgumentCaptor.capture());
        assertEquals("1234567890", lockArgumentCaptor.getValue());
        assertEquals("1234567890", unlockArgumentCaptor.getValue());
    }

    @Test
    @DisplayName("Lock, Unlock 성공 - Exception 발생 시에도")
    void lockAndUnlock_EvenIfThrow() throws Throwable {
        // given: 잔액 사용 Request가 있음
        ArgumentCaptor<String> lockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unlockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);
        UseBalance.Request request =
                new UseBalance.Request(123L, "54321", 1000L);
        given(proceedingJoinPoint.proceed())
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        // when: 메서드 동작 시 예외 발생
        assertThrows(AccountException.class,
                () -> lockAopAspect.aroundMethod(proceedingJoinPoint, request));

        // then: 예외가 있더라도 Lock, Unlock 동작은 영향 받지 않음
        verify(lockService, times(1))
                .lock(lockArgumentCaptor.capture());
        verify(lockService, times(1))
                .unlock(unlockArgumentCaptor.capture());
        assertEquals("54321", lockArgumentCaptor.getValue());
        assertEquals("54321", unlockArgumentCaptor.getValue());
    }
}