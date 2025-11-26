package com.nhnacademy.coupon_server.controller;

import com.nhnacademy.coupon_server.controller.apidocs.MemberCouponDocs;
import com.nhnacademy.coupon_server.dto.coupon.CouponResponseDto;
import com.nhnacademy.coupon_server.dto.memberCoupon.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.dto.memberCoupon.UserCouponIssueRequestDto;
import com.nhnacademy.coupon_server.service.CouponService;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class MemberCouponController implements MemberCouponDocs {

    private final MemberCouponService memberCouponService;
    private final CouponService couponService;

    @Override
    public ResponseEntity<Void> issueCoupon(Long userId, UserCouponIssueRequestDto requestDto) {
        memberCouponService.issueCouponByUser(userId, requestDto.getCouponId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<Page<MemberCouponResponseDto>> getCouponsByUserId(@PathVariable Long memberId, Pageable pageable) {
        Page<MemberCouponResponseDto> responseDtos = memberCouponService.findCouponByUserId(memberId, pageable);
        return ResponseEntity.ok(responseDtos);
    }

    @Override
    public ResponseEntity<Page<CouponResponseDto>> getIssuableCoupons(Pageable pageable) {
        Page<CouponResponseDto> responseDtos = couponService.findIssuableCoupons(pageable);
        return ResponseEntity.ok(responseDtos);
    }
}
