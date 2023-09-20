package com.oxahex.accountapi.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND("유저가 없습니다."),
    MAX_ACCOUNT_PER_USER_10("생성 가능한 최대 계좌는 10개입니다.");

    private final String description;
}