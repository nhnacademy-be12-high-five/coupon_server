package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.calculator.CouponDateCalculator;
import com.nhnacademy.coupon_server.dto.message.CouponIssueMessage;
import com.nhnacademy.coupon_server.dto.request.CouponCalculationRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponCalculationResponseDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponCancelRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponUseRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.response.ErrorResponse;
import com.nhnacademy.coupon_server.dto.response.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.exception.CouponNotFoundException;
import com.nhnacademy.coupon_server.exception.DuplicateCouponException;
import com.nhnacademy.coupon_server.exception.ErrorCode;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberCouponServiceTest {
    @Mock
    private MemberCouponRepository memberCouponRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private CouponDateCalculator dateCalculator;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private MemberCouponServiceImpl memberCouponService;
    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @BeforeEach
    public void setUp() {
        memberCouponService = new MemberCouponServiceImpl(
                memberCouponRepository,
                couponRepository,
                dateCalculator,
                redisTemplate,
                rabbitTemplate
        );

    }

    // ==========================================
    // 1. 관리자 발급 (issueCouponByAdmin)
    // ==========================================
    @Test
    @DisplayName("관리자 발급 성공")
    void issueCouponByAdmin_Success() {
        Long userId = 100L;
        Long couponId = 10L;
        LocalDateTime expectedDate = LocalDateTime.now().plusDays(30);

        MemberCouponIssueRequestDto requestDto = MemberCouponIssueRequestDto.builder()
                .userId(userId)
                .couponId(couponId)
                .build();

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(false);
        when(dateCalculator.calculateExpiration(coupon)).thenReturn(expectedDate);

        memberCouponService.issueCouponByAdmin(requestDto);

        ArgumentCaptor<MemberCoupon> captor = ArgumentCaptor.forClass(MemberCoupon.class);
        verify(memberCouponRepository).save(captor.capture());

        MemberCoupon savedCoupon = captor.getValue();
        Assertions.assertEquals(userId, savedCoupon.getUserId());
        Assertions.assertEquals(expectedDate, savedCoupon.getExpiredAt());
    }

    @Test
    @DisplayName("관리자 발급 실패 - 이미 발급된 쿠폰 (Logical Check)")
    void issueCouponByAdmin_Failure_Duplicate() {
        Long userId = 100L;
        Long couponId = 10L;
        MemberCouponIssueRequestDto requestDto = MemberCouponIssueRequestDto.builder().userId(userId).couponId(couponId).build();

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(true);

        Assertions.assertThrows(DuplicateCouponException.class, () ->
                memberCouponService.issueCouponByAdmin(requestDto)
        );
        verify(memberCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 발급 실패 - 동시성 제약조건 위반 (DataIntegrityViolationException)")
    void issueCouponByAdmin_Failure_Concurrency() {
        Long userId = 100L;
        Long couponId = 10L;
        MemberCouponIssueRequestDto requestDto = new MemberCouponIssueRequestDto(couponId, userId);

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(false);
        when(dateCalculator.calculateExpiration(coupon)).thenReturn(LocalDateTime.now());

        doThrow(DataIntegrityViolationException.class).when(memberCouponRepository).save(any(MemberCoupon.class));

        Assertions.assertThrows(DuplicateCouponException.class, () ->
                memberCouponService.issueCouponByAdmin(requestDto)
        );
    }

    // ==========================================
    // 2. 사용자 발급 (issueCouponByUser)
    // ==========================================
    @Test
    @DisplayName("사용자 발급 성공")
    void issueCouponByUser_Success() {
        Long userId = 1L;
        Long couponId = 100L;
        Coupon coupon = Coupon.builder()
                .id(couponId)
                .issueCount(100)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(1))
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(setOperations.add(anyString(), anyString())).thenReturn(1L);

        when(valueOperations.decrement(anyString())).thenReturn(99L);

        memberCouponService.issueCouponByUser(userId, couponId);

        verify(rabbitTemplate, times(1)).convertAndSend(eq("high-five-coupon-issue-queue"), any(CouponIssueMessage.class));
        verify(memberCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("사용자 발급 실패 - 동시성 제약조건 위반")
    void issueCouponByUser_Failure_Concurrency() {
        Long userId = 1L;
        Long couponId = 100L;
        LocalDateTime now = LocalDateTime.now();

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .issueCount(100)
                .issuedStartAt(now.minusDays(1))
                .issuedEndAt(now.plusDays(1))
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        when(setOperations.add(anyString(), anyString())).thenReturn(0L);

        Assertions.assertThrows(DuplicateCouponException.class, () ->
                memberCouponService.issueCouponByUser(userId, couponId)
        );

        verify(rabbitTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("사용자 발급 실패 - 발급 기간 아님 (Before Start)")
    void issueCouponByUser_Fail_BeforeStart() {
        Long couponId = 10L;
        Coupon coupon = Coupon.builder().issuedStartAt(LocalDateTime.now().plusDays(1)).build();
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        Assertions.assertThrows(IllegalArgumentException.class, () -> memberCouponService.issueCouponByUser(1L, couponId));
    }

    @Test
    @DisplayName("사용자 발급 실패 - 수량 매진")
    void issueCouponByUser_Fail_SoldOut() {
        Long userId = 1L;
        Long couponId = 10L;

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(1))
                .issueCount(100)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(setOperations.add(anyString(), anyString())).thenReturn(1L);

        when(valueOperations.setIfAbsent(anyString(), anyString())).thenReturn(true);
        when(valueOperations.decrement(anyString())).thenReturn(-1L);

        Assertions.assertThrows(IllegalStateException.class, () ->
                memberCouponService.issueCouponByUser(userId, couponId)
        );
    }

    // ==========================================
    // 3. 웰컴 쿠폰 발급 (issueWelcomeCoupon)
    // ==========================================
    @Test
    @DisplayName("웰컴 쿠폰 발급 성공")
    void issueWelcomeCoupon_Success() {
        Long userId = 1L;
        Coupon welcomeCoupon = Coupon.builder().id(100L).couponName("Welcome").build();

        when(couponRepository.findCouponsByCommentAndStatus(eq(Comment.WELCOME), eq(CouponPolicyStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(List.of(welcomeCoupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, 100L)).thenReturn(false);
        when(dateCalculator.calculateExpiration(welcomeCoupon)).thenReturn(LocalDateTime.now().plusDays(30));

        memberCouponService.issueWelcomeCoupon(userId);

        verify(memberCouponRepository).save(any(MemberCoupon.class));
    }

    @Test
    @DisplayName("웰컴 쿠폰 발급 - 동시성 이슈 발생 시 예외 삼킴 (성공 처리)")
    void issueWelcomeCoupon_DataIntegrity_SwallowException() {
        Long userId = 1L;
        Coupon welcomeCoupon = Coupon.builder().id(100L).couponName("Welcome").build();

        when(couponRepository.findCouponsByCommentAndStatus(eq(Comment.WELCOME), eq(CouponPolicyStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(List.of(welcomeCoupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, 100L)).thenReturn(false);
        when(dateCalculator.calculateExpiration(welcomeCoupon)).thenReturn(LocalDateTime.now().plusDays(30));

        doThrow(DataIntegrityViolationException.class).when(memberCouponRepository).save(any(MemberCoupon.class));

        assertDoesNotThrow(() -> memberCouponService.issueWelcomeCoupon(userId));
    }

    @Test
    @DisplayName("웰컴 쿠폰 발급 - 진행 중인 웰컴 쿠폰 없음")
    void issueWelcomeCoupon_NotFound() {
        when(couponRepository.findCouponsByCommentAndStatus(any(), any(), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        Assertions.assertThrows(CouponNotFoundException.class, () -> memberCouponService.issueWelcomeCoupon(1L));
    }

    // ==========================================
    // 4. 생일 쿠폰 발급 (issueBirthdayCoupon)
    // ==========================================
    @Test
    @DisplayName("생일 쿠폰 발급 성공")
    void issueBirthdayCoupon_Success() {
        Long userId = 1L;
        Long couponId = 100L;
        Coupon coupon = Coupon.builder().id(couponId).build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(false);

        memberCouponService.issueBirthdayCoupon(userId, couponId);

        verify(memberCouponRepository).save(any(MemberCoupon.class));
    }

    @Test
    @DisplayName("생일 쿠폰 발급 - 동시성 이슈 발생 시 예외 삼킴")
    void issueBirthdayCoupon_DataIntegrity_SwallowException() {
        Long userId = 1L;
        Long couponId = 100L;
        Coupon coupon = Coupon.builder().id(couponId).build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(false);

        doThrow(DataIntegrityViolationException.class).when(memberCouponRepository).save(any(MemberCoupon.class));

        assertDoesNotThrow(() -> memberCouponService.issueBirthdayCoupon(userId, couponId));
    }

    // ==========================================
    // 5. 기타 기능 (조회, 계산, 사용, 취소)
    // ==========================================
    @Test
    @DisplayName("회원 쿠폰 전체 조회 성공")
    void findAll_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Coupon coupon = Coupon.builder()
                .couponName("Test")
                .couponPolicy(CouponPolicy.builder().discountType(DiscountType.FIXED).discountValue(1000L).build())
                .build();
        Page<MemberCoupon> page = new PageImpl<>(List.of(MemberCoupon.builder().coupon(coupon).build()));

        when(memberCouponRepository.findAll(pageable)).thenReturn(page);

        Page<MemberCouponResponseDto> result = memberCouponService.findAll(pageable);
        Assertions.assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("할인 계산 성공")
    void calculateDiscount_Success() {
        Long userId = 1L;
        Long couponId = 10L;
        CouponPolicy policy = CouponPolicy.builder()
                .discountType(DiscountType.FIXED).discountValue(2000L).minOrderValue(5000L).build();
        Coupon coupon = Coupon.builder().couponPolicy(policy).build();
        MemberCoupon mc = MemberCoupon.builder().coupon(coupon).status(Status.ISSUED).expiredAt(LocalDateTime.now().plusDays(1)).build();

        when(memberCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(Optional.of(mc));

        CouponCalculationResponseDto res = memberCouponService.calculateDiscount(userId, new CouponCalculationRequestDto(couponId, 10000L));
        Assertions.assertEquals(2000L, res.getDiscountAmount());
    }

    @Test
    @DisplayName("쿠폰 사용 성공")
    void useCoupon_Success() {
        Long userId = 1L;
        Long couponId = 10L;
        MemberCoupon mc = MemberCoupon.builder().status(Status.ISSUED).expiredAt(LocalDateTime.now().plusDays(1)).build();

        when(memberCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(Optional.of(mc));

        memberCouponService.useCoupon(userId, new MemberCouponUseRequestDto(couponId, 123L));
        Assertions.assertEquals(Status.USED, mc.getStatus());
    }

    @Test
    @DisplayName("쿠폰 취소 성공")
    void cancelCouponUsage_Success() {
        Long userId = 1L;
        Long couponId = 10L;
        Long orderId = 123L;
        MemberCoupon mc = MemberCoupon.builder().status(Status.USED).orderId(orderId).build();

        when(memberCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(Optional.of(mc));

        memberCouponService.cancelCouponUsage(userId, new MemberCouponCancelRequestDto(couponId, orderId));
        Assertions.assertEquals(Status.ISSUED, mc.getStatus());
        Assertions.assertNull(mc.getOrderId());
    }

    @Test
    @DisplayName("GlobalExceptionHandler 테스트")
    void handleExceptionTest() {
        DuplicateCouponException ex = new DuplicateCouponException();

        ResponseEntity<ErrorResponse> res = globalExceptionHandler.handleCouponServerException(ex);

        Assertions.assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
        Assertions.assertEquals(ErrorCode.DUPLICATE_COUPON_ISSUE.getCode(), res.getBody().getCode());
    }
}