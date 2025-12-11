package com.nhnacademy.coupon_server.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CouponServerException extends RuntimeException {
    private final ErrorCode errorCode;
    public CouponServerException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
