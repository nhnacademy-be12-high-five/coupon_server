package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.CouponPolicyRequestDto;
import com.nhnacademy.coupon_server.dto.CouponPolicyResponseDto;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.repository.CouponPolicyRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CouponPolicyServiceTest {
    @Autowired
    private CouponPolicyService couponPolicyService;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Test
    @DisplayName("새로운 쿠폰 정책 생성")
    void testCreatePolicy() {
        CouponPolicyRequestDto couponPolicyRequestDto = CouponPolicyRequestDto.builder()
                .name("테스트 웰컴 쿠폰")
                .comment(Comment.WELCOME)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .maxDiscountValue(10000L)
                .minPayValue(50000L)
                .maxDiscountValue(10000L)
                .build();

        CouponPolicyResponseDto responseDto = couponPolicyService.create(couponPolicyRequestDto);

        Assertions.assertThat(responseDto).isNotNull();
        Assertions.assertThat(responseDto.getName()).isEqualTo("테스트 웰컴 쿠폰");
        Assertions.assertThat(responseDto.getComment()).isEqualTo(Comment.WELCOME);
        Assertions.assertThat(responseDto.getId()).isNotNull();

        CouponPolicy couponPolicy = couponPolicyRepository.findById(responseDto.getId()).orElse(null);

        Assertions.assertThat(couponPolicy).isNotNull();
        Assertions.assertThat(couponPolicy.getName()).isEqualTo(responseDto.getName());
        Assertions.assertThat(couponPolicy.getDiscountValue()).isEqualTo(responseDto.getDiscountValue());
    }

    private CouponPolicyResponseDto createTestPolicy(String name) {
        CouponPolicyRequestDto requestDto = CouponPolicyRequestDto.builder()
                .name(name)
                .comment(Comment.EVENT)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .minPayValue(0L)
                .maxDiscountValue(1000L)
                .build();
        return couponPolicyService.create(requestDto);
    }

    @Test
    @DisplayName("모든 쿠폰 정책 조회 성공")
    void testFindAllPolicy() {
        couponPolicyRepository.deleteAllInBatch();

        createTestPolicy("정책 A");
        createTestPolicy("B");
        createTestPolicy("C");

        List<CouponPolicyResponseDto> responseDtoList = couponPolicyService.findAll();
        Assertions.assertThat(responseDtoList).isNotNull();
        Assertions.assertThat(responseDtoList).hasSize(3);
        Assertions.assertThat(responseDtoList).extracting("name").contains("정책 A", "B", "C");
    }
}
