package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.coupon.CouponRequestDto;
import com.nhnacademy.coupon_server.dto.coupon.CouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.exception.CouponNotFoundException;
import com.nhnacademy.coupon_server.exception.CouponPolicyNotFoundException;
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
import static org.mockito.Mockito.*;

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

    @Test
    @DisplayName("쿠폰 템플릿 수정 성공")
    void updateCouponSuccess() {
        Long couponId = 100L;
        Long policyId = 1L;

        CouponPolicy policy = CouponPolicy.builder()
                .id(policyId)
                .name("기존 정책")
                .build();

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .couponPolicy(policy)
                .couponName("기존 이름")
                .issueCount(10)
                .build();

        CouponRequestDto updateReq = CouponRequestDto.builder()
                .id(policyId)
                .couponName("새로운 이름")
                .issueCount(20)
                .issueStartAt(LocalDateTime.now())
                .issueEndAt(LocalDateTime.now().plusDays(1))
                .validPeriodDate(30)
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));

        CouponResponseDto responseDto = couponService.update(couponId, updateReq);

        Assertions.assertEquals("새로운 이름", responseDto.getCouponName());
        Assertions.assertEquals(20, responseDto.getIssueCount());
    }

    @Test
    @DisplayName("쿠폰 템플릿 수정 실패 - 존재하지 않는 쿠폰")
    void updateCouponNotFound() {
        Long couponId = 999L;
        CouponRequestDto req = CouponRequestDto.builder().id(1L).build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.empty());

        Assertions.assertThrows(CouponNotFoundException.class, () -> couponService.update(couponId, req));
    }

    @Test
    @DisplayName("쿠폰 템플릿 삭제 성공")
    void deleteCouponSuccess() {
        Long couponId = 100L;
        when(couponRepository.existsById(couponId)).thenReturn(true);
        doNothing().when(couponRepository).deleteById(couponId);

        couponService.delete(couponId);

        verify(couponRepository, times(1)).existsById(couponId);
        verify(couponRepository, times(1)).deleteById(couponId);
    }

    @Test
    @DisplayName("쿠폰 템플릿 삭제 실패 - 존재하지 않는 ID")
    void deleteCouponNotFound() {
        Long couponId = 999L;
        when(couponRepository.existsById(couponId)).thenReturn(false);
        Assertions.assertThrows(CouponPolicyNotFoundException.class, () -> couponService.delete(couponId));
        verify(couponRepository, never()).deleteById(couponId);
    }
}
