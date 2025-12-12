package com.nhnacademy.coupon_server.controller;

import com.nhnacademy.coupon_server.controller.apidocs.CouponDocs;
import com.nhnacademy.coupon_server.dto.response.CouponResponseDto;
import com.nhnacademy.coupon_server.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController implements CouponDocs {

    private final CouponService couponService;

    @Override
    public ResponseEntity<List<CouponResponseDto>> getBookCoupons(@PathVariable(name = "book-id") Long bookId) {
        List<CouponResponseDto> coupons = couponService.findCouponsByBookId(bookId);
        return ResponseEntity.ok(coupons);
    }
}