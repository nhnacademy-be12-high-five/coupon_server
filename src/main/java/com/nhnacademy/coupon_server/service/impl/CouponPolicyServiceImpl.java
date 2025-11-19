package com.nhnacademy.coupon_server.service.impl;

import com.nhnacademy.coupon_server.dto.CouponPolicyRequestDto;
import com.nhnacademy.coupon_server.dto.CouponPolicyResponseDto;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.repository.CouponPolicyRepository;
import com.nhnacademy.coupon_server.service.CouponPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CouponPolicyServiceImpl implements CouponPolicyService {
    private final CouponPolicyRepository couponPolicyRepository;

    @Override
    @Transactional
    public CouponPolicyResponseDto create(CouponPolicyRequestDto couponPolicyRequestDto) {
        log.info("새로 만든 쿠폰 정책 -> {}", couponPolicyRequestDto.getName());

        CouponPolicy newPolicy = CouponPolicy.builder()
                .name(couponPolicyRequestDto.getName())
                .comment(couponPolicyRequestDto.getComment())
                .discountType(couponPolicyRequestDto.getDiscountType())
                .discountValue(couponPolicyRequestDto.getDiscountValue())
                .minPayValue(couponPolicyRequestDto.getMinPayValue())
                .maxDiscountValue(couponPolicyRequestDto.getMaxDiscountValue())
                .build();

        CouponPolicy savedPolicy = couponPolicyRepository.save(newPolicy);
        return CouponPolicyResponseDto.fromEntity(savedPolicy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponPolicyResponseDto> findAll() {
        log.info("Fetching all coupon policies");
        return couponPolicyRepository.findAll().stream()
                .map(CouponPolicyResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}
