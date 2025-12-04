package com.nhnacademy.coupon_server.controller;

import com.nhnacademy.coupon_server.controller.apidocs.MemberCouponDocs;
import com.nhnacademy.coupon_server.dto.response.CouponCalculationResponseDto;
import com.nhnacademy.coupon_server.dto.response.CouponResponseDto;
import com.nhnacademy.coupon_server.dto.response.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.dto.request.CouponCalculationRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponCancelRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponUseRequestDto;
import com.nhnacademy.coupon_server.dto.request.UserCouponIssueRequestDto;
import com.nhnacademy.coupon_server.service.CouponService;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class MemberCouponController implements MemberCouponDocs {

    private final MemberCouponService memberCouponService;
    private final CouponService couponService;

    @Override
    public ResponseEntity<Void> issueCoupon(@RequestHeader("X-USER-ID") Long memberId, UserCouponIssueRequestDto requestDto) {
        memberCouponService.issueCouponByUser(memberId, requestDto.getCouponId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<Page<MemberCouponResponseDto>> getCouponsByUserId(@RequestHeader("X-USER-ID") Long memberId, Pageable pageable) {
        Page<MemberCouponResponseDto> responseDtos = memberCouponService.findCouponByUserId(memberId, pageable);
        return ResponseEntity.ok(responseDtos);
    }

    @Override
    public ResponseEntity<Page<CouponResponseDto>> getIssuableCoupons(Pageable pageable) {
        Page<CouponResponseDto> responseDtos = couponService.findIssuableCoupons(pageable);
        return ResponseEntity.ok(responseDtos);
    }

    @Override
    public ResponseEntity<List<MemberCouponResponseDto>> getUsableCoupons(@RequestHeader("X-USER-ID") Long memberId) {
        List<MemberCouponResponseDto> responseDtos = memberCouponService.findUsableCoupons(memberId);
        return ResponseEntity.ok(responseDtos);
    }

    @Override
    public ResponseEntity<CouponCalculationResponseDto> calculateCoupon(@RequestHeader("X-USER-ID") Long memberId, CouponCalculationRequestDto requestDto) {
        CouponCalculationResponseDto responseDto = memberCouponService.calculateDiscount(memberId, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @Override
    public ResponseEntity<Void> useCoupon(@RequestHeader("X-USER-ID") Long memberId, MemberCouponUseRequestDto requestDto) {
        memberCouponService.useCoupon(memberId, requestDto);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> cancelCouponUsage(@RequestHeader("X-USER-ID") Long memberId, MemberCouponCancelRequestDto requestDto) {
        memberCouponService.cancelCouponUsage(memberId, requestDto);
        return ResponseEntity.ok().build();
    }
}
