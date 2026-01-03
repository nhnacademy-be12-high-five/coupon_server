package com.nhnacademy.coupon_server.entity.state;

public enum CouponType {
    NORMAL("일반 쿠폰"),
    WELCOME("웰컴 쿠폰"), // 기존 Comment.WELCOME 대체
    BIRTHDAY("생일 쿠폰"), // 기존 Comment.BIRTHDAY 대체
    BOOK_SPECIFIC("도서 전용 쿠폰"),
    CATEGORY_SPECIFIC("카테고리 전용 쿠폰");

    private final String description;

    CouponType(String description) { this.description = description; }
}
