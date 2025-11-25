package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.calculator.CouponDateCalculator;
import com.nhnacademy.coupon_server.dto.memberCoupon.MemberCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.memberCoupon.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.exception.DuplicateCouponException;
import com.nhnacademy.coupon_server.exception.GlobalExceptionHandler;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.memberCoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.service.impl.MemberCouponServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberCouponServiceTest {
    @Mock
    private MemberCouponRepository memberCouponRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponDateCalculator dateCalculator;

    private MemberCouponService memberCouponService;
    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @BeforeEach
    public void setUp() {
        memberCouponService = new MemberCouponServiceImpl(memberCouponRepository, couponRepository, dateCalculator);
    }

    @Test
    @DisplayName("쿠폰 발급 성공 - 만료일 계산기 호출 확인")
    void issueCouponSuccess() {
        Long userId = 100L;
        Long couponId = 10L;
        LocalDateTime expectedDate = LocalDateTime.now().plusDays(30);

        Coupon mockCoupon = Coupon.builder().id(couponId).build();

        MemberCouponIssueRequestDto requestDto = MemberCouponIssueRequestDto.builder()
                .userId(userId)
                .couponId(couponId)
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(mockCoupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(false);
        when(dateCalculator.calculateExpiration(mockCoupon)).thenReturn(expectedDate);

        memberCouponService.issueCouponByAdmin(requestDto);

        ArgumentCaptor<MemberCoupon> captor = ArgumentCaptor.forClass(MemberCoupon.class);
        verify(memberCouponRepository).save(captor.capture());

        MemberCoupon savedCoupon = captor.getValue();
        Assertions.assertEquals(userId, savedCoupon.getUserId());
        Assertions.assertEquals(expectedDate, savedCoupon.getExpiredAt());
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 이미 발급된 쿠폰")
    void issueCouponFailure() {
        Long userId = 100L;
        Long couponId = 10L;

        MemberCouponIssueRequestDto requestDto = MemberCouponIssueRequestDto.builder()
                .userId(userId)
                .couponId(couponId)
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(Coupon.builder().build()));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(true);

        // When & Then
        Assertions.assertThrows(DuplicateCouponException.class, () ->
                memberCouponService.issueCouponByAdmin(requestDto)
        );

        verify(memberCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("회원 쿠폰 전체 조회 성공 (페이징)")
    void findAllTestSuccess() {
        Pageable pageable = PageRequest.of(0, 10);

        Coupon mockCoupon = Coupon.builder()
                .id(10L)
                .couponName("신규 가입 쿠폰")
                .build();

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .id(1L)
                .userId(100L)
                .coupon(mockCoupon)
                .status(Status.ISSUED)
                .issueAt(LocalDateTime.now().plusDays(30))
                .build();

        Page<MemberCoupon> pageResult = new PageImpl<>(List.of(memberCoupon));
        when(memberCouponRepository.findAll(pageable)).thenReturn(pageResult);

        Page<MemberCouponResponseDto> result = memberCouponService.findAll(pageable);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals("신규 가입 쿠폰", result.getContent().get(0).getCouponName());
        Assertions.assertEquals(Status.ISSUED, result.getContent().get(0).getStatus());

        verify(memberCouponRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("DuplicateCouponException 처리 테스트 (409)")
    void handleDuplicateCouponException() {
        String errorMessage = "이미 발급된 쿠폰입니다.";
        DuplicateCouponException exception = new DuplicateCouponException(errorMessage);

        ResponseEntity<String> response = globalExceptionHandler.handleDuplicateCouponException(exception);

        Assertions.assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Assertions.assertEquals(errorMessage, response.getBody());
    }
}
