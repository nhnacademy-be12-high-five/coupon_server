package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.CouponPolicyRequestDto;
import com.nhnacademy.coupon_server.dto.CouponPolicyResponseDto;

import java.util.List;

public interface CouponPolicyService {
    CouponPolicyResponseDto create(CouponPolicyRequestDto couponPolicyRequestDto);
}
