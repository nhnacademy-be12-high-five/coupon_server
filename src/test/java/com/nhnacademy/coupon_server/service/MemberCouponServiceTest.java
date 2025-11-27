package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.calculator.CouponDateCalculator;
import com.nhnacademy.coupon_server.dto.coupon.CouponCalculationRequestDto;
import com.nhnacademy.coupon_server.dto.coupon.CouponCalculationResponseDto;
import com.nhnacademy.coupon_server.dto.coupon.MemberCouponCancelRequestDto;
import com.nhnacademy.coupon_server.dto.coupon.MemberCouponUseRequestDto;
import com.nhnacademy.coupon_server.dto.memberCoupon.MemberCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.memberCoupon.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
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

import java.lang.reflect.Member;
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

    @Test
    @DisplayName("사용자 쿠폰 발급 성공")
    void issueCouponByUserSuccess() {
        Long userId = 1L;
        Long couponId = 100L;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationDate = now.plusDays(30);

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .issueCount(100)
                .issuedStartAt(now.minusDays(1))
                .issuedEndAt(now.plusDays(1))
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.countByCouponId(couponId)).thenReturn(99L);
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(false);
        when(dateCalculator.calculateExpiration(coupon)).thenReturn(expirationDate);

        memberCouponService.issueCouponByUser(userId, couponId);

        ArgumentCaptor<MemberCoupon> captor = ArgumentCaptor.forClass(MemberCoupon.class);
        verify(memberCouponRepository).save(captor.capture());

        MemberCoupon savedCoupon = captor.getValue();
        Assertions.assertEquals(userId, savedCoupon.getUserId());
        Assertions.assertEquals(Status.ISSUED, savedCoupon.getStatus());
        Assertions.assertEquals(expirationDate, savedCoupon.getExpiredAt());
    }

    @Test
    @DisplayName("주문 시 적용 가능한 쿠폰 목록 조회 성공")
    void findUsableCOuponsSuccess() {
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();

        Coupon mockCoupon = Coupon.builder()
                .id(1L)
                .couponName("할인 쿠폰")
                .build();

        MemberCoupon validMemberCoupon = MemberCoupon.builder()
                .id(1L)
                .userId(userId)
                .coupon(mockCoupon)
                .status(Status.ISSUED)
                .issueAt(now.minusDays(1))
                .expiredAt(now.plusDays(10))
                .build();

        when(memberCouponRepository.findAllByUserIdAndStatusAndExpiredAtAfter(eq(userId), eq(Status.ISSUED), any(LocalDateTime.class))).thenReturn(List.of(validMemberCoupon));

        List<MemberCouponResponseDto> result = memberCouponService.findUsableCoupons(userId);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("할인 쿠폰", result.get(0).getCouponName());

        verify(memberCouponRepository, times(1)).findAllByUserIdAndStatusAndExpiredAtAfter(eq(userId), eq(Status.ISSUED), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("쿠폰 사용 처리 성공")
    void useCouponSuccess() {
        Long userId = 1L;
        Long couponId = 100L;
        Long orderId = 20251127L;

        MemberCouponUseRequestDto requestDto = new MemberCouponUseRequestDto(couponId, orderId);

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .id(1L)
                .userId(userId)
                .status(Status.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        when(memberCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(Optional.of(memberCoupon));

        memberCouponService.useCoupon(userId, requestDto);
        Assertions.assertEquals(Status.USED, memberCoupon.getStatus());
        Assertions.assertNotNull(memberCoupon.getUsedAt());
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 이미 사용된 쿠폰")
    void useCouponFailureAlreadyUsed() {
        Long userId = 1L;
        Long couponId = 100L;
        MemberCouponUseRequestDto requestDto = new MemberCouponUseRequestDto(couponId, userId);
        MemberCoupon usedCoupon = MemberCoupon.builder()
                .status(Status.USED)
                .build();

        when(memberCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(Optional.of(usedCoupon));
        Assertions.assertThrows(IllegalStateException.class, () -> memberCouponService.useCoupon(userId, requestDto));
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 유효 기간 만료")
    void useCouponFailureExpired() {
        Long userId = 1L;
        Long couponId = 100L;
        MemberCouponUseRequestDto requestDto = new MemberCouponUseRequestDto(couponId, userId);

        MemberCoupon expiredCoupon = MemberCoupon.builder()
                .status(Status.ISSUED)
                .expiredAt(LocalDateTime.now().minusDays(1))
                .build();

        when(memberCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(Optional.of(expiredCoupon));
        Assertions.assertThrows(IllegalStateException.class, () -> memberCouponService.useCoupon(userId, requestDto));
    }

    @Test
    @DisplayName("쿠폰 사용 취소(복구) 성공")
    void cancelCouponUsageSuccess() {
        Long userId = 1L;
        Long couponId = 100L;
        Long orderId = 20251127L;

        MemberCouponCancelRequestDto requestDto = new MemberCouponCancelRequestDto(couponId, orderId);

        MemberCoupon usedCoupon = MemberCoupon.builder()
                .userId(userId)
                .status(Status.USED)
                .orderId(orderId)
                .usedAt(LocalDateTime.now())
                .build();

        when(memberCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(Optional.of(usedCoupon));

        memberCouponService.cancelCouponUsage(userId, requestDto);

        Assertions.assertEquals(Status.ISSUED, usedCoupon.getStatus());
        Assertions.assertNull(usedCoupon.getUsedAt(), "사용 취소 시 사용 일시는 null이어야 합니다.");
        Assertions.assertNull(usedCoupon.getOrderId(), "사용 취소 시 주문 ID는 null이어야 합니다.");
    }
    @Test
    @DisplayName("쿠폰 사용 취소 실패 - 아직 사용 안 한 쿠폰")
    void cancelCouponUsageFailureNotUsed() {
        Long userId = 1L;
        Long couponId = 100L;
        MemberCouponCancelRequestDto requestDto = new MemberCouponCancelRequestDto(couponId, 123L);

        MemberCoupon issuedCoupon = MemberCoupon.builder()
                .status(Status.ISSUED)
                .build();

        when(memberCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.of(issuedCoupon));

        Assertions.assertThrows(IllegalStateException.class, () -> memberCouponService.cancelCouponUsage(userId, requestDto)
        );
    }

    @Test
    @DisplayName("사용자 쿠폰 발급 실패 - 발급 시작 기간 전")
    void issueCouponByUserFailureBeforeStart() {
        Long userId = 1L;
        Long couponId = 100L;
        Coupon coupon = Coupon.builder()
                .id(couponId)
                .issuedStartAt(LocalDateTime.now().plusDays(1)) // 내일부터 발급 가능
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                memberCouponService.issueCouponByUser(userId, couponId)
        );
    }

    @Test
    @DisplayName("사용자 쿠폰 발급 실패 - 발급 종료 기간 지남")
    void issueCouponByUserFailureAfterEnd() {
        Long userId = 1L;
        Long couponId = 100L;
        Coupon coupon = Coupon.builder()
                .id(couponId)
                .issuedEndAt(LocalDateTime.now().minusDays(1)) // 어제 종료됨
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                memberCouponService.issueCouponByUser(userId, couponId)
        );
    }

    @Test
    @DisplayName("사용자 쿠폰 발급 실패 - 발급 수량 소진")
    void issueCouponByUserFailureCountExhausted() {
        Long userId = 1L;
        Long couponId = 100L;
        Coupon coupon = Coupon.builder()
                .id(couponId)
                .issueCount(100) // 총 100개
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.countByCouponId(couponId)).thenReturn(100L); // 이미 100개 발급됨

        Assertions.assertThrows(IllegalStateException.class, () ->
                memberCouponService.issueCouponByUser(userId, couponId)
        );
    }

    @Test
    @DisplayName("할인 계산 성공 - 정액 할인 (FIXED)")
    void calculateDiscountFixed() {
        Long userId = 1L;
        Long couponId = 100L;
        Long orderPrice = 30000L;
        Long discountVal = 5000L;

        CouponPolicy policy = CouponPolicy.builder()
                .discountType(DiscountType.FIXED)
                .discountValue(discountVal)
                .minOrderValue(10000L)
                .build();

        Coupon coupon = Coupon.builder().couponPolicy(policy).build();

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .status(Status.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        when(memberCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.of(memberCoupon));

        CouponCalculationRequestDto req = new CouponCalculationRequestDto(couponId, orderPrice);
        CouponCalculationResponseDto res = memberCouponService.calculateDiscount(userId, req);

        Assertions.assertEquals(discountVal, res.getDiscountAmount());
        Assertions.assertEquals(orderPrice - discountVal, res.getFinalPrice());
    }

    @Test
    @DisplayName("할인 계산 성공 - 정률 할인 (PERCENTAGE)")
    void calculateDiscountPercentage() {
        Long userId = 1L;
        Long couponId = 100L;
        Long orderPrice = 20000L;
        Long discountPercent = 10L;

        CouponPolicy policy = CouponPolicy.builder()
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(discountPercent)
                .build();

        Coupon coupon = Coupon.builder().couponPolicy(policy).build();
        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .status(Status.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        when(memberCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.of(memberCoupon));

        CouponCalculationRequestDto req = new CouponCalculationRequestDto(couponId, orderPrice);
        CouponCalculationResponseDto res = memberCouponService.calculateDiscount(userId, req);

        Assertions.assertEquals(2000L, res.getDiscountAmount());
        Assertions.assertEquals(18000L, res.getFinalPrice());
    }

    @Test
    @DisplayName("할인 계산 성공 - 최대 할인 한도 적용")
    void calculateDiscountMaxLimit() {
        Long userId = 1L;
        Long couponId = 100L;
        Long orderPrice = 100000L;

        CouponPolicy policy = CouponPolicy.builder()
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(50L)
                .maxDiscountValue(10000L)
                .build();

        Coupon coupon = Coupon.builder().couponPolicy(policy).build();
        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .status(Status.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        when(memberCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.of(memberCoupon));

        CouponCalculationRequestDto req = new CouponCalculationRequestDto(couponId, orderPrice);
        CouponCalculationResponseDto res = memberCouponService.calculateDiscount(userId, req);

        Assertions.assertEquals(10000L, res.getDiscountAmount());
    }

    @Test
    @DisplayName("할인 계산 실패 - 최소 주문 금액 미달")
    void calculateDiscountFailureMinOrderValue() {
        Long userId = 1L;
        Long couponId = 100L;
        Long orderPrice = 5000L;

        CouponPolicy policy = CouponPolicy.builder()
                .minOrderValue(10000L)
                .build();

        Coupon coupon = Coupon.builder().couponPolicy(policy).build();
        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .status(Status.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        when(memberCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.of(memberCoupon));

        CouponCalculationRequestDto req = new CouponCalculationRequestDto(couponId, orderPrice);

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                memberCouponService.calculateDiscount(userId, req)
        );
    }

    @Test
    @DisplayName("쿠폰 사용 취소 실패 - 주문 ID 불일치")
    void cancelCouponUsageFailureOrderIdMismatch() {
        Long userId = 1L;
        Long couponId = 100L;
        Long requestOrderId = 999L;
        Long actualOrderId = 111L;

        MemberCouponCancelRequestDto req = new MemberCouponCancelRequestDto(couponId, requestOrderId);

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .status(Status.USED)
                .orderId(actualOrderId)
                .build();

        when(memberCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .thenReturn(Optional.of(memberCoupon));

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                memberCouponService.cancelCouponUsage(userId, req)
        );
    }
}
