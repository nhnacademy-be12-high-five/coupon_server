package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.coupon.CouponRequestDto;
import com.nhnacademy.coupon_server.dto.coupon.CouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.CouponType;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.couponPolicy.CouponPolicyRepository;
import com.nhnacademy.coupon_server.repository.memberCoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.service.impl.CouponServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponPolicyRepository couponPolicyRepository;
    @Mock
    private MemberCouponRepository memberCouponRepository;

    private CouponServiceImpl couponService;

    @BeforeEach
    void setUp() {
        couponService = new CouponServiceImpl(couponPolicyRepository, couponRepository, memberCouponRepository);
    }

    @Test
    @DisplayName("쿠폰 템플릿 생성 성공")
    void createCouponSuccess() {
        Long policyId = 1L;
        CouponPolicy mockPolicy = CouponPolicy.builder()
                .id(policyId)
                .name("Test Coupon Policy")
                .discountType(DiscountType.FIXED)
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

    @Test
    @DisplayName("발급 가능한 쿠폰 조회 - 잔여 수량 계산 확인")
    void findIssuableCouponSuccess() {
        Long policyId = 1L;
        CouponPolicy policy = CouponPolicy.builder()
                .id(policyId)
                .name("Test Policy")
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .build();

        Coupon limitedCoupon = Coupon.builder()
                .id(1L)
                .couponPolicy(policy)
                .couponName("Limited Coupon")
                .issueCount(100)
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(1))
                .build();

        Coupon unlimitedCoupon = Coupon.builder()
                .id(2L)
                .couponPolicy(policy)
                .couponName("무제한 쿠폰")
                .issueCount(null)
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(1))
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        List<Coupon> coupons = List.of(limitedCoupon, unlimitedCoupon);
        Page<Coupon> couponPage = new PageImpl<>(coupons);

        when(couponRepository.findAllByIssuedStartAtBeforeAndIssuedEndAtAfterAndCouponPolicyStatusAndCouponType(any(), any(), eq(CouponPolicyStatus.ACTIVE), eq(CouponType.NORMAL), eq(pageable))).thenReturn(couponPage);

        when(memberCouponRepository.countByCouponId(1L)).thenReturn(10L);
        Page<CouponResponseDto> result = couponService.findIssuableCoupons(pageable);

        List<CouponResponseDto> responseDtoList = result.getContent();
        Assertions.assertEquals(2, responseDtoList.size());

        Assertions.assertEquals("Limited Coupon", responseDtoList.get(0).getCouponName());
        Assertions.assertEquals(90, responseDtoList.get(0).getRemainingCount());

        Assertions.assertEquals("무제한 쿠폰", responseDtoList.get(1).getCouponName());
        Assertions.assertNull(responseDtoList.get(1).getRemainingCount());
    }
}
