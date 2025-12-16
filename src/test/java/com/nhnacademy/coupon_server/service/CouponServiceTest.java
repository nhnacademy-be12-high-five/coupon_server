//package com.nhnacademy.coupon_server.service;
//
//import com.nhnacademy.coupon_server.calculator.CouponDateCalculator;
//import com.nhnacademy.coupon_server.dto.request.CouponRequestDto;
//import com.nhnacademy.coupon_server.dto.response.CouponResponseDto;
//import com.nhnacademy.coupon_server.entity.Coupon;
//import com.nhnacademy.coupon_server.entity.CouponPolicy;
//import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
//import com.nhnacademy.coupon_server.entity.state.CouponType;
//import com.nhnacademy.coupon_server.entity.state.DiscountType;
//import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
//import com.nhnacademy.coupon_server.repository.couponPolicy.CouponPolicyRepository;
//import com.nhnacademy.coupon_server.repository.memberCoupon.MemberCouponRepository;
//import com.nhnacademy.coupon_server.service.impl.CouponServiceImpl;
//import com.nhnacademy.coupon_server.service.impl.MemberCouponServiceImpl;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.redis.core.StringRedisTemplate;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class CouponServiceTest {
//
//    @Mock
//    private CouponRepository couponRepository;
//    @Mock
//    private CouponPolicyRepository couponPolicyRepository;
//    @Mock
//    private MemberCouponRepository memberCouponRepository;
//    @Mock
//    private MemberCouponService memberCouponService;
//    @Mock
//    private CouponDateCalculator dateCalculator;
//    @Mock
//    private StringRedisTemplate redisTemplate;
//    @Mock
//    private RabbitTemplate rabbitTemplate;
//
//
//    private CouponServiceImpl couponService;
//
//    @BeforeEach
//    void setUp() {
//        couponService = new CouponServiceImpl(
//                couponRepository,
//                couponPolicyRepository,
//                memberCouponRepository
//        );
//    }
//
//    @Test
//    @DisplayName("쿠폰 템플릿 생성 성공")
//    void createCouponSuccess() {
//        Long policyId = 1L;
//        CouponPolicy mockPolicy = CouponPolicy.builder()
//                .id(policyId)
//                .name("Test Coupon Policy")
//                .discountType(DiscountType.FIXED)
//                .build();
//
//        CouponRequestDto requestDto = CouponRequestDto.builder()
//                .id(policyId)
//                .couponName("Summer Sale")
//                .issueCount(100)
//                .issueStartAt(LocalDateTime.now())
//                .issueEndAt(LocalDateTime.now().plusDays(7))
//                .validPeriodDate(30)
//                .build();
//
//        Coupon mockCoupon = Coupon.builder()
//                .id(100L)
//                .couponPolicy(mockPolicy)
//                .couponName(requestDto.getCouponName())
//                .build();
//
//        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(mockPolicy));
//        when(couponRepository.save(any(Coupon.class))).thenReturn(mockCoupon);
//
//        CouponResponseDto responseDto = couponService.create(requestDto);
//
//        Assertions.assertNotNull(responseDto);
//        assertEquals(100L, responseDto.getId());
//        assertEquals("Summer Sale", responseDto.getCouponName());
//        assertEquals(policyId, responseDto.getCouponPolicyId());
//    }
//
//    @Test
//    @DisplayName("모든 쿠폰 템플릿 조회 성공")
//    void findAllCouponSuccess() {
//        Long policyId = 1L;
//        CouponPolicy mockPolicy = CouponPolicy.builder()
//                .id(policyId)
//                .name("Test Policy")
//                .build();
//
//        Coupon coupon1 = Coupon.builder()
//                .id(1L)
//                .couponPolicy(mockPolicy)
//                .couponName("Coupon 1")
//                .build();
//
//        Coupon coupon2 = Coupon.builder()
//                .id(2L)
//                .couponPolicy(mockPolicy)
//                .couponName("Coupon 2")
//                .build();
//
//        when(couponRepository.findAll()).thenReturn(List.of(coupon1, coupon2));
//
//        List<CouponResponseDto> responseDtoList = couponService.findAll();
//
//        assertEquals(2, responseDtoList.size());
//        assertEquals("Coupon 1", responseDtoList.get(0).getCouponName());
//        assertEquals("Coupon 2", responseDtoList.get(1).getCouponName());
//        assertEquals(policyId, responseDtoList.get(0).getCouponPolicyId());
//    }
//
//    @Test
//    @DisplayName("발급 가능한 쿠폰 조회 - 잔여 수량 계산 확인")
//    void findIssuableCouponSuccess() {
//        Long policyId = 1L;
//        CouponPolicy policy = CouponPolicy.builder()
//                .id(policyId)
//                .name("Test Policy")
//                .discountType(DiscountType.FIXED)
//                .discountValue(1000L)
//                .build();
//
//        Coupon limitedCoupon = Coupon.builder()
//                .id(1L)
//                .couponPolicy(policy)
//                .couponName("Limited Coupon")
//                .issueCount(100)
//                .issuedStartAt(LocalDateTime.now().minusDays(1))
//                .issuedEndAt(LocalDateTime.now().plusDays(1))
//                .build();
//
//        Coupon unlimitedCoupon = Coupon.builder()
//                .id(2L)
//                .couponPolicy(policy)
//                .couponName("무제한 쿠폰")
//                .issueCount(null)
//                .issuedStartAt(LocalDateTime.now().minusDays(1))
//                .issuedEndAt(LocalDateTime.now().plusDays(1))
//                .build();
//
//        Pageable pageable = PageRequest.of(0, 10);
//        List<Coupon> coupons = List.of(limitedCoupon, unlimitedCoupon);
//        Page<Coupon> couponPage = new PageImpl<>(coupons);
//
//        when(couponRepository.findAllByIssuedStartAtBeforeAndIssuedEndAtAfterAndCouponPolicyStatusAndCouponType(any(), any(), eq(CouponPolicyStatus.ACTIVE), eq(CouponType.NORMAL), eq(pageable))).thenReturn(couponPage);
//
//        when(memberCouponRepository.countByCouponId(1L)).thenReturn(10L);
//        Page<CouponResponseDto> result = couponService.findIssuableCoupons(pageable);
//
//        List<CouponResponseDto> responseDtoList = result.getContent();
//        assertEquals(2, responseDtoList.size());
//
//        assertEquals("Limited Coupon", responseDtoList.get(0).getCouponName());
//        assertEquals(90, responseDtoList.get(0).getRemainingCount());
//
//        assertEquals("무제한 쿠폰", responseDtoList.get(1).getCouponName());
//        Assertions.assertNull(responseDtoList.get(1).getRemainingCount());
//    }
//
//    @Test
//    @DisplayName("쿠폰 목록 조회 시나리오 - 각 상황별(정상, 대기, 만료, 소진, 비활성) 상태 계산 검증")
//    void getCoupons_StatusCalculation_Scenario() {
//        // Given
//        LocalDateTime now = LocalDateTime.now();
//
//        // 1. [ACTIVE] 정상 발급 가능 (기간 내, 수량 넉넉)
//        Coupon activeCoupon = createMockCoupon(1L, "정상 쿠폰", CouponPolicyStatus.ACTIVE,
//                now.minusDays(1), now.plusDays(1), 100);
//
//        // 2. [WAITING] 발급 대기 (시작일이 미래)
//        Coupon waitingCoupon = createMockCoupon(2L, "대기 쿠폰", CouponPolicyStatus.ACTIVE,
//                now.plusDays(1), now.plusDays(7), 100);
//
//        // 3. [EXPIRED] 기간 만료 (종료일이 과거)
//        Coupon expiredCoupon = createMockCoupon(3L, "만료 쿠폰", CouponPolicyStatus.ACTIVE,
//                now.minusDays(10), now.minusDays(1), 100);
//
//        // 4. [SOLD_OUT] 소진 (수량 10개 설정, 10개 모두 발급되었다고 가정)
//        Coupon soldOutCoupon = createMockCoupon(4L, "소진 쿠폰", CouponPolicyStatus.ACTIVE,
//                now.minusDays(1), now.plusDays(1), 10);
//
//        // 5. [INACTIVE] 정책 비활성화
//        Coupon inactiveCoupon = createMockCoupon(5L, "비활성 쿠폰", CouponPolicyStatus.INACTIVE,
//                now.minusDays(1), now.plusDays(1), 100);
//
//        List<Coupon> couponList = List.of(activeCoupon, waitingCoupon, expiredCoupon, soldOutCoupon, inactiveCoupon);
//        Page<Coupon> couponPage = new PageImpl<>(couponList);
//
//        // Mocking 1: 쿠폰 목록 조회
//        when(couponRepository.findAll(any(Pageable.class))).thenReturn(couponPage);
//
//        // Mocking 2: [수정됨] MemberCouponRepository를 통해 발급 수량 조회
//        // 소진된 쿠폰(ID 4)은 10장이 발행되었는데, 10장이 발급되었다고 설정 (잔여 = 10 - 10 = 0)
//        when(memberCouponRepository.countByCouponId(4L)).thenReturn(10L);
//
//        // 나머지 쿠폰은 0장이 발급되었다고 설정 (잔여 100)
//        when(memberCouponRepository.countByCouponId(argThat(id -> id != 4L))).thenReturn(0L);
//
//        // When
//        Page<CouponResponseDto> result = couponService.getCoupons(PageRequest.of(0, 10));
//
//        // Then
//        List<CouponResponseDto> content = result.getContent();
//
//        // 상태값 검증
//        // 1. ACTIVE
//        assertEquals("ACTIVE", content.get(0).getStatus(), "정상 쿠폰은 ACTIVE 여야 합니다.");
//        // 2. WAITING
//        assertEquals("WAITING", content.get(1).getStatus(), "시작 전 쿠폰은 WAITING 여야 합니다.");
//        // 3. EXPIRED
//        assertEquals("EXPIRED", content.get(2).getStatus(), "종료된 쿠폰은 EXPIRED 여야 합니다.");
//        // 4. SOLD_OUT (발행량 10 - 발급량 10 = 잔여 0)
//        assertEquals("SOLD_OUT", content.get(3).getStatus(), "수량이 소진된 쿠폰은 SOLD_OUT 여야 합니다.");
//        // 5. INACTIVE
//        assertEquals("INACTIVE", content.get(4).getStatus(), "정책이 비활성인 쿠폰은 INACTIVE 여야 합니다.");
//    }
//
//    private Coupon createMockCoupon(Long id, String name, CouponPolicyStatus policyStatus,
//                                    LocalDateTime start, LocalDateTime end, Integer count) {
//        CouponPolicy policy = CouponPolicy.builder()
//                .status(policyStatus)
//                .build();
//
//        return Coupon.builder()
//                .id(id)
//                .couponName(name)
//                .couponPolicy(policy)
//                .issuedStartAt(start)
//                .issuedEndAt(end)
//                .issueCount(count)
//                .couponType(CouponType.NORMAL)
//                .build();
//    }
//
//    @Test
//    @DisplayName("전체 쿠폰 조회 시 잔여 수량 계산 로직 검증 (정상, 초과, 무제한)")
//    void findAll_RemainingCountCalculation() {
//        Coupon normalCoupon = Coupon.builder()
//                .id(1L)
//                .couponName("정상 쿠폰")
//                .issueCount(100)
//                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
//                .build();
//
//        Coupon overIssuedCoupon = Coupon.builder()
//                .id(2L)
//                .couponName("초과 발급된 쿠폰")
//                .issueCount(100)
//                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
//                .build();
//
//        Coupon unlimitedCoupon = Coupon.builder()
//                .id(3L)
//                .couponName("무제한 쿠폰")
//                .issueCount(null)
//                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
//                .build();
//
//        when(couponRepository.findAll()).thenReturn(List.of(normalCoupon, overIssuedCoupon, unlimitedCoupon));
//
//        when(memberCouponRepository.countByCouponId(1L)).thenReturn(30L);
//
//        when(memberCouponRepository.countByCouponId(2L)).thenReturn(120L);
//
//        List<CouponResponseDto> result = couponService.findAll();
//
//        Assertions.assertEquals(3, result.size());
//
//        CouponResponseDto normalDto = result.stream().filter(c -> c.getId().equals(1L)).findFirst().get();
//        Assertions.assertEquals(70, normalDto.getRemainingCount());
//
//        CouponResponseDto overIssuedDto = result.stream().filter(c -> c.getId().equals(2L)).findFirst().get();
//        Assertions.assertEquals(0, overIssuedDto.getRemainingCount());
//
//        CouponResponseDto unlimitedDto = result.stream().filter(c -> c.getId().equals(3L)).findFirst().get();
//        Assertions.assertNull(unlimitedDto.getRemainingCount());
//    }
//}
