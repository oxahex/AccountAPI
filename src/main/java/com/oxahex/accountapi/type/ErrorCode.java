package com.oxahex.accountapi.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND("유저가 없습니다.");

    private final String description;
}