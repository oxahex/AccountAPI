package com.oxahex.accountapi.service;

import com.oxahex.accountapi.aop.AccountLockIdInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LockAopAspect {
    private final LockService lockService;

    /**
     * pjp 동작 전 후에 Lock 취득 시도, 취득 여부와 관계 없이 Lock을 해제 처리
     */
    @Around("@annotation(com.oxahex.accountapi.aop.AccountLock) && args(request)")
    public Object aroundMethod(
            ProceedingJoinPoint pjp,
            AccountLockIdInterface request
    ) throws Throwable {

        // Lock 취득 시도
        lockService.lock(request.getAccountNumber());

        try {
            return  pjp.proceed();
        } finally {
            // Lock 해제
            lockService.unlock(request.getAccountNumber());
        }
    }
}