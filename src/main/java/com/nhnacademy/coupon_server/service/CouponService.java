package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.coupon.CouponRequestDto;
import com.nhnacademy.coupon_server.dto.coupon.CouponResponseDto;

public interface CouponService {
    CouponResponseDto create(CouponRequestDto couponRequestDto);
}
