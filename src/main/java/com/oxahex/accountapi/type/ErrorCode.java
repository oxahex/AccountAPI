package com.oxahex.accountapi.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND("유저가 없습니다."),
    ACCOUNT_NOT_FOUND("계좌가 없습니다."),
    AMOUNT_EXCEED_BALANCE("거래 금액이 계좌 잔액보다 큽니다."),
    MAX_ACCOUNT_PER_USER_10("생성 가능한 최대 계좌는 10개입니다."),
    USER_ACCOUNT_UN_MATCH("유저와 계좌의 소유주가 다릅니다."),
    ACCOUNT_ALREADY_UNREGISTERED("이미 해지된 계좌입니다."),
    BALANCE_NOT_EMPTY("잔액이 있는 계좌는 해지할 수 없습니다.");

    private final String description;
}