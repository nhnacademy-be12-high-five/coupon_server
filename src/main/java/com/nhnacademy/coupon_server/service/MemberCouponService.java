package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.memberCoupon.MemberCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.memberCoupon.MemberCouponResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberCouponService {
    Page<MemberCouponResponseDto> findAll(Pageable pageable);

    void issueCouponByAdmin(MemberCouponIssueRequestDto requestDto);

    void issueCouponByUser(Long userId, Long couponId);

    Page<MemberCouponResponseDto> findCouponByUserId(Long userId, Pageable pageable);
}
