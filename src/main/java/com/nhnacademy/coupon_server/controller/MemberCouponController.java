package com.nhnacademy.coupon_server.controller;

import com.nhnacademy.coupon_server.controller.apidocs.MemberCouponDocs;
import com.nhnacademy.coupon_server.dto.memberCoupon.UserCouponIssueRequestDto;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class MemberCouponController implements MemberCouponDocs {

    private final MemberCouponService memberCouponService;

    @Override
    public ResponseEntity<Void> issueCoupon(Long userId, UserCouponIssueRequestDto requestDto) {
        memberCouponService.issueCouponByUser(userId, requestDto.getCouponId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
