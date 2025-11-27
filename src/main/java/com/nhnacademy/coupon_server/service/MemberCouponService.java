package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.coupon.CouponCalculationRequestDto;
import com.nhnacademy.coupon_server.dto.coupon.CouponCalculationResponseDto;
import com.nhnacademy.coupon_server.dto.coupon.MemberCouponCancelRequestDto;
import com.nhnacademy.coupon_server.dto.coupon.MemberCouponUseRequestDto;
import com.nhnacademy.coupon_server.dto.memberCoupon.MemberCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.memberCoupon.MemberCouponResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberCouponService {
    Page<MemberCouponResponseDto> findAll(Pageable pageable);

    void issueCouponByAdmin(MemberCouponIssueRequestDto requestDto);

    void issueCouponByUser(Long userId, Long couponId);

    Page<MemberCouponResponseDto> findCouponByUserId(Long userId, Pageable pageable);

    List<MemberCouponResponseDto> findUsableCoupons(Long userId);

    CouponCalculationResponseDto calculateDiscount(Long userId, CouponCalculationRequestDto requestDto);

    void useCoupon(Long userId, MemberCouponUseRequestDto requestDto);

    void cancelCouponUsage(Long userId, MemberCouponCancelRequestDto requestDto);
}
