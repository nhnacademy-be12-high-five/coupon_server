package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.coupon.CouponRequestDto;
import com.nhnacademy.coupon_server.dto.coupon.CouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.couponPolicy.CouponPolicyRepository;
import com.nhnacademy.coupon_server.service.impl.CouponServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    private CouponServiceImpl couponService;

    @BeforeEach
    void setUp() {
        couponService = new CouponServiceImpl(couponPolicyRepository, couponRepository);
    }

    @Test
    @DisplayName("쿠폰 템플릿 생성 성공")
    void createCouponSuccess() {
        Long policyId = 1L;
        CouponPolicy mockPolicy = CouponPolicy.builder()
                .id(policyId)
                .name("Test Coupon Policy")
                .build();

        CouponRequestDto requestDto = CouponRequestDto.builder()
                .id(policyId)
                .couponName("Summer Sale")
                .issueCount(100)
                .issueStartAt(LocalDateTime.now())
                .issueEndAt(LocalDateTime.now().plusDays(7))
                .validPeriodDate(30)
                .build();

        Coupon mockCoupon = Coupon.builder()
                .id(100L)
                .couponPolicy(mockPolicy)
                .couponName(requestDto.getCouponName())
                .build();

        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(mockPolicy));
        when(couponRepository.save(any(Coupon.class))).thenReturn(mockCoupon);

        CouponResponseDto responseDto = couponService.create(requestDto);

        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(100L, responseDto.getId());
        Assertions.assertEquals("Summer Sale", responseDto.getCouponName());
        Assertions.assertEquals(policyId, responseDto.getCouponPolicyId());
    }

    @Test
    @DisplayName("모든 쿠폰 템플릿 조회 성공")
    void findAllCouponSuccess() {
        Long policyId = 1L;
        CouponPolicy mockPolicy = CouponPolicy.builder()
                .id(policyId)
                .name("Test Policy")
                .build();

        Coupon coupon1 = Coupon.builder()
                .id(1L)
                .couponPolicy(mockPolicy)
                .couponName("Coupon 1")
                .build();

        Coupon coupon2 = Coupon.builder()
                .id(2L)
                .couponPolicy(mockPolicy)
                .couponName("Coupon 2")
                .build();

        when(couponRepository.findAll()).thenReturn(List.of(coupon1, coupon2));

        List<CouponResponseDto> responseDtoList = couponService.findAll();

        Assertions.assertEquals(2, responseDtoList.size());
        Assertions.assertEquals("Coupon 1", responseDtoList.get(0).getCouponName());
        Assertions.assertEquals("Coupon 2", responseDtoList.get(1).getCouponName());
        Assertions.assertEquals(policyId, responseDtoList.get(0).getCouponPolicyId());
    }
}
