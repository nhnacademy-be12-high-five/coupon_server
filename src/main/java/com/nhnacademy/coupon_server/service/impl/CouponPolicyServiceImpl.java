package com.nhnacademy.coupon_server.service.impl;

import com.nhnacademy.coupon_server.dto.CouponPolicyRequestDto;
import com.nhnacademy.coupon_server.dto.CouponPolicyResponseDto;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.CouponPolicyBook;
import com.nhnacademy.coupon_server.entity.CouponPolicyCategory;
import com.nhnacademy.coupon_server.repository.CouponPolicyBookRepository;
import com.nhnacademy.coupon_server.repository.CouponPolicyCategoryRepository;
import com.nhnacademy.coupon_server.repository.CouponPolicyRepository;
import com.nhnacademy.coupon_server.service.CouponPolicyService;
import jakarta.persistence.EntityNotFoundException;
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
    private final CouponPolicyBookRepository couponPolicyBookRepository;
    private final CouponPolicyCategoryRepository couponPolicyCategoryRepository;

    @Override
    @Transactional
    public CouponPolicyResponseDto create(CouponPolicyRequestDto couponPolicyRequestDto) {
        log.info("새로 만든 쿠폰 정책 -> {}", couponPolicyRequestDto.getName());

        CouponPolicy newPolicy = CouponPolicy.builder()
                .name(couponPolicyRequestDto.getName())
                .comment(couponPolicyRequestDto.getComment())
                .discountType(couponPolicyRequestDto.getDiscountType())
                .discountValue(couponPolicyRequestDto.getDiscountValue())
                .minOrderValue(couponPolicyRequestDto.getMinOrderValue())
                .maxDiscountValue(couponPolicyRequestDto.getMaxDiscountValue())
                .build();

        CouponPolicy savedPolicy = couponPolicyRepository.save(newPolicy);
        // 도서 적용 범위 저장 로직
        if (couponPolicyRequestDto.getTargetBookIds() != null && !couponPolicyRequestDto.getTargetBookIds().isEmpty()) {
            List<CouponPolicyBook> bookLinks = couponPolicyRequestDto.getTargetBookIds().stream()
                    .map(bookId -> CouponPolicyBook.builder()
                            .couponPolicy(savedPolicy)
                            .bookId(bookId)
                            .build())
                    .toList();
            couponPolicyBookRepository.saveAll(bookLinks);
            savedPolicy.setUsableBooks(bookLinks);
        }
        // 카테고리 적용 범위 저장 로직
        if (couponPolicyRequestDto.getTargetCategoryIds() != null && !couponPolicyRequestDto.getTargetCategoryIds().isEmpty()) {
            List<CouponPolicyCategory> categoryLinks = couponPolicyRequestDto.getTargetCategoryIds().stream()
                    .map(categoryId -> CouponPolicyCategory.builder()
                            .couponPolicy(savedPolicy)
                            .categoryId(categoryId)
                            .build())
                    .toList();
            couponPolicyCategoryRepository.saveAll(categoryLinks);
            savedPolicy.setUsableCategories(categoryLinks);
        }
        return CouponPolicyResponseDto.fromEntity(savedPolicy);
    }

    @Override
    public List<CouponPolicyResponseDto> findAll() {
        log.info("모든 쿠폰 정책 조회 요청");

        List<CouponPolicy> couponPolicies = couponPolicyRepository.findAll();

        return couponPolicies.stream()
                .map(CouponPolicyResponseDto::fromEntity)
                .toList();
    }

    @Override
    public CouponPolicyResponseDto findById(Long id) {
        log.info("쿠폰 정책 단건 조회 요청 - ID -> {}", id);
        CouponPolicy policy = couponPolicyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("쿠폰 정책을 찾을 수 없습니다."));
        return CouponPolicyResponseDto.fromEntity(policy);
    }

}
