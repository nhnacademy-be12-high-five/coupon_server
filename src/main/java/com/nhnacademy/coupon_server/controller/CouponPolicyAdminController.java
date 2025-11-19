package com.nhnacademy.coupon_server.controller;

import com.nhnacademy.coupon_server.controller.apidocs.CouponPolicyAdminDocs;
import com.nhnacademy.coupon_server.dto.CouponPolicyRequestDto;
import com.nhnacademy.coupon_server.dto.CouponPolicyResponseDto;
import com.nhnacademy.coupon_server.service.CouponPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/coupon-policy")
public class CouponPolicyAdminController implements CouponPolicyAdminDocs {
    private final CouponPolicyService couponPolicyService;

    @PostMapping
    public ResponseEntity<CouponPolicyResponseDto> createPolicy(@Valid @RequestBody CouponPolicyRequestDto couponPolicyRequestDto) {
        CouponPolicyResponseDto responseDto = couponPolicyService.create(couponPolicyRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping
    public ResponseEntity<List<CouponPolicyResponseDto>> getAllPolicies() {
        List<CouponPolicyResponseDto> policies = couponPolicyService.findAll();
        return ResponseEntity.ok(policies);
    }
}
