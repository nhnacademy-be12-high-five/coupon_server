package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.coupon.CouponRequestDto;
import com.nhnacademy.coupon_server.dto.coupon.CouponResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CouponService {

    // 쿠폰 템플릿 생성
    CouponResponseDto create(CouponRequestDto couponRequestDto);

    // 쿠폰 템플릿 목록 조회
    List<CouponResponseDto> findAll();

    //발급 가능한 쿠폰 목록 조회
    Page<CouponResponseDto> findIssuableCoupons(Pageable pageable);
}
