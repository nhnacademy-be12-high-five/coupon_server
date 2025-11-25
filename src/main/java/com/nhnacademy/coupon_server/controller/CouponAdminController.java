package com.nhnacademy.coupon_server.controller;

import com.nhnacademy.coupon_server.controller.apidocs.CouponAdminDocs;
import com.nhnacademy.coupon_server.dto.coupon.CouponRequestDto;
import com.nhnacademy.coupon_server.dto.coupon.CouponResponseDto;
import com.nhnacademy.coupon_server.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
@Slf4j
public class CouponAdminController implements CouponAdminDocs {
    private final CouponService couponService;

    @Override
    public ResponseEntity<CouponResponseDto> createCoupon(CouponRequestDto couponRequestDto) {
        log.info("관리자 쿠폰 템플릿 생성 요청 - 정책 ID: {}, 이름: {}", couponRequestDto.getId(), couponRequestDto.getCouponName());
        CouponResponseDto couponResponseDto = couponService.create(couponRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(couponResponseDto);
    }

    @Override
    public ResponseEntity<List<CouponResponseDto>> findAllCoupons() {
        List<CouponResponseDto> couponList = couponService.findAll();
        return ResponseEntity.status(HttpStatus.OK).body(couponList);
    }

    @Override
    public ResponseEntity<CouponResponseDto> updateCoupon(Long couponId, CouponRequestDto couponRequestDto) {
        log.info("관리자 쿠폰 템플릿 수정 요청 - ID -> {}", couponId);
        CouponResponseDto responseDto = couponService.update(couponId, couponRequestDto);
        return ResponseEntity.ok(responseDto);
    }

    @Override
    public ResponseEntity<Void> deleteCoupon(Long couponId) {
        log.info("관리자 쿠폰 삭제 요청 - ID -> {}", couponId);
        couponService.delete(couponId);
        return ResponseEntity.noContent().build();
    }
}
