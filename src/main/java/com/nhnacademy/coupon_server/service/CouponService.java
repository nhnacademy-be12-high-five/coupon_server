package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.coupon.CouponRequestDto;
import com.nhnacademy.coupon_server.dto.coupon.CouponResponseDto;

import java.util.List;

public interface CouponService {
    CouponResponseDto create(CouponRequestDto couponRequestDto);

    List<CouponResponseDto> findAll();
}
