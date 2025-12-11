package com.nhnacademy.coupon_server.exception;

public class DuplicateCouponException extends CouponServerException {
    public DuplicateCouponException(String message) {
        super(ErrorCode.DUPLICATE_COUPON_ISSUE);
    }

    public DuplicateCouponException() {
        super(ErrorCode.DUPLICATE_COUPON_ISSUE);
    }
}
