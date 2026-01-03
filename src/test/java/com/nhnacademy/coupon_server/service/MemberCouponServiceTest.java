package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.calculator.CouponDateCalculator;
import com.nhnacademy.coupon_server.dto.request.CouponCalculationRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponCancelRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponUseRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponCalculationResponseDto;
import com.nhnacademy.coupon_server.dto.response.ErrorResponse;
import com.nhnacademy.coupon_server.dto.response.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.entity.state.Status;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
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
    private CouponDateCalculator dateCalculator;

    @Mock
    private StringRedisTemplate redisTemplate;

    private MemberCouponServiceImpl memberCouponService;
    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @BeforeEach
    public void setUp() {

        memberCouponService = new MemberCouponServiceImpl(memberCouponRepository, couponRepository, dateCalculator, redisTemplate);
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

    // ==========================================
    // 2. 사용자 발급 (issueCouponByUser)
    // [변경] Redis/RabbitMQ 제거 -> DB 직접 저장 검증
    // ==========================================
    @Test
    @DisplayName("사용자 발급 성공 - Lua Script 실행 결과 1 반환")
    void issueCouponByUser_Success() {
        Long userId = 1L;
        Long couponId = 100L;
        LocalDateTime expectedDate = LocalDateTime.now().plusDays(7);

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .issueCount(100)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(1))
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(dateCalculator.calculateExpiration(coupon)).thenReturn(expectedDate);

        // [핵심 변경] Redis Lua Script 실행 Mocking
        // execute(script, keys, args) 호출 시 1L(성공)을 반환하도록 설정
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(1L);

        memberCouponService.issueCouponByUser(userId, couponId);

        // DB 저장이 호출되었는지 검증
        verify(memberCouponRepository, times(1)).save(any(MemberCoupon.class));
    }

    @Test
    @DisplayName("사용자 발급 실패 - 이미 발급된 유저 (Redis Lua Script 반환값 -1)")
    void issueCouponByUser_Failure_Duplicate_Redis() {
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

        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(-1L);

        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId))
                .thenReturn(true);

        Assertions.assertThrows(DuplicateCouponException.class, () ->
                memberCouponService.issueCouponByUser(userId, couponId)
        );

        verify(memberCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("사용자 발급 실패 - 발급 기간 아님 (Before Start)")
    void issueCouponByUser_Fail_BeforeStart() {
        Long couponId = 10L;
        Coupon coupon = Coupon.builder().issuedStartAt(LocalDateTime.now().plusDays(1)).build(); // 미래 시작
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        Assertions.assertThrows(IllegalArgumentException.class, () -> memberCouponService.issueCouponByUser(1L, couponId));
    }

    @Test
    @DisplayName("사용자 발급 실패 - 수량 매진 (Redis Lua Script 반환값 0)")
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

        // [핵심 변경] Lua Script가 0(매진) 또는 null 반환
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(0L);

        Assertions.assertThrows(IllegalStateException.class, () ->
                memberCouponService.issueCouponByUser(userId, couponId)
        );
    }

    @Test
    @DisplayName("사용자 발급 실패 - Redis 연결 장애 발생")
    void issueCouponByUser_Fail_RedisError() {
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

        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenThrow(new RedisConnectionFailureException("Redis Connection Failed"));

        Assertions.assertThrows(RedisConnectionFailureException.class, () ->
                memberCouponService.issueCouponByUser(userId, couponId)
        );
    }

    // ==========================================
    // 3. 웰컴 쿠폰 및 생일 쿠폰
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
    @DisplayName("생일 쿠폰 발급 - 동시성 이슈 발생 시 예외 삼킴")
    void issueBirthdayCoupon_DataIntegrity_SwallowException() {
        Long userId = 1L;
        Long couponId = 100L;
        Coupon coupon = Coupon.builder().id(couponId).build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(true);
        // 말일 계산이 로직 내에 포함되어 있음 -> 별도 Mocking 불필요 (Repository 호출만 확인)
        assertDoesNotThrow(() -> memberCouponService.issueBirthdayCoupon(userId, couponId));
        verify(memberCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("내부 생성용 (createMemberCoupon) 성공")
    void createMemberCoupon_Success() {
        Long userId = 1L;
        Long couponId = 50L;
        Coupon coupon = Coupon.builder().id(couponId).build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(false);
        when(dateCalculator.calculateExpiration(coupon)).thenReturn(LocalDateTime.now().plusDays(10));

        memberCouponService.createMemberCoupon(userId, couponId);

        verify(memberCouponRepository).save(any(MemberCoupon.class));
    }


    // ==========================================
    // 4. 조회, 계산, 사용, 취소 (PK 기반 로직 변경 반영)
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
        Long memberCouponId = 55L; // DTO에 담기는 ID는 MemberCoupon의 PK임
        Long orderPrice = 10000L;

        CouponPolicy policy = CouponPolicy.builder()
                .discountType(DiscountType.FIXED).discountValue(2000L).minOrderValue(5000L).build();
        Coupon coupon = Coupon.builder().couponPolicy(policy).build();

        // MemberCoupon 생성 (소유자 userId 설정 필수)
        MemberCoupon mc = MemberCoupon.builder()
                .id(memberCouponId)
                .userId(userId)
                .coupon(coupon)
                .status(Status.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        // 변경: findByUserIdAndCouponId -> findById
        when(memberCouponRepository.findById(memberCouponId)).thenReturn(Optional.of(mc));

        CouponCalculationResponseDto res = memberCouponService.calculateDiscount(
                userId,
                new CouponCalculationRequestDto(memberCouponId, orderPrice)
        );
        Assertions.assertEquals(2000L, res.getDiscountAmount());
        Assertions.assertEquals(8000L, res.getFinalPrice());
    }

    @Test
    @DisplayName("할인 계산 실패 - 소유자 불일치")
    void calculateDiscount_Fail_OwnerMismatch() {
        Long userId = 1L;
        Long otherUserId = 2L;
        Long memberCouponId = 55L;

        MemberCoupon mc = MemberCoupon.builder()
                .id(memberCouponId)
                .userId(otherUserId) // 다른 사람 소유
                .build();

        when(memberCouponRepository.findById(memberCouponId)).thenReturn(Optional.of(mc));

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                memberCouponService.calculateDiscount(userId, new CouponCalculationRequestDto(memberCouponId, 10000L))
        );
    }

    @Test
    @DisplayName("쿠폰 사용 성공")
    void useCoupon_Success() {
        Long userId = 1L;
        Long memberCouponId = 55L;

        MemberCoupon mc = MemberCoupon.builder()
                .id(memberCouponId)
                .userId(userId)
                .status(Status.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        when(memberCouponRepository.findById(memberCouponId)).thenReturn(Optional.of(mc));

        memberCouponService.useCoupon(userId, new MemberCouponUseRequestDto(memberCouponId, 123L));

        Assertions.assertEquals(Status.USED, mc.getStatus());
        Assertions.assertEquals(123L, mc.getOrderId());
    }

    @Test
    @DisplayName("쿠폰 취소 성공")
    void cancelCouponUsage_Success() {
        Long userId = 1L;
        Long memberCouponId = 55L;
        Long orderId = 123L;

        MemberCoupon mc = MemberCoupon.builder()
                .id(memberCouponId)
                .userId(userId)
                .status(Status.USED)
                .orderId(orderId)
                .build();

        when(memberCouponRepository.findById(memberCouponId)).thenReturn(Optional.of(mc));

        memberCouponService.cancelCouponUsage(userId, new MemberCouponCancelRequestDto(memberCouponId, orderId));

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