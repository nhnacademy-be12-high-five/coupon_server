package com.nhnacademy.coupon_server.exception;

public class CouponNotFoundException extends CouponServerException {
    public CouponNotFoundException(String message) {
        super(ErrorCode.COUPON_NOT_FOUND);
    }

    public CouponNotFoundException() {
        super(ErrorCode.COUPON_NOT_FOUND);
    }

}
