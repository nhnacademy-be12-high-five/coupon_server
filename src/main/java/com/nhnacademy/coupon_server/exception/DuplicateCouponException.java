package com.nhnacademy.coupon_server.exception;

public class DuplicateCouponException extends RuntimeException {
    public DuplicateCouponException(String message) {
        super(message);
    }
}
