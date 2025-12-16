package com.nhnacademy.coupon_server.service.impl;

import com.nhnacademy.coupon_server.calculator.CouponDateCalculator;
import com.nhnacademy.coupon_server.config.RabbitMqConfig;
import com.nhnacademy.coupon_server.dto.message.CouponIssueMessage;
import com.nhnacademy.coupon_server.dto.request.CouponCalculationRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponCancelRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponUseRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponCalculationResponseDto;
import com.nhnacademy.coupon_server.dto.response.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.exception.CouponNotFoundException;
import com.nhnacademy.coupon_server.exception.DuplicateCouponException;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.memberCoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberCouponServiceImpl implements MemberCouponService {
    private final MemberCouponRepository memberCouponRepository;
    private final CouponRepository couponRepository;
    private final CouponDateCalculator dateCalculator;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public Page<MemberCouponResponseDto> findAll(Pageable pageable) {
        Page<MemberCoupon> memberCoupons = memberCouponRepository.findAll(pageable);
        return memberCoupons.map(MemberCouponResponseDto::fromEntity);
    }

    @Override
    @Transactional
    public void issueCouponByAdmin(MemberCouponIssueRequestDto requestDto) {
        Long userId = requestDto.getUserId();
        Long couponId = requestDto.getCouponId();

        log.info("관리자 수동 발급 요청 - Coupon: {}, User: {}", couponId, userId);

        Coupon coupon = couponRepository.findById(couponId).orElseThrow(CouponNotFoundException::new);
        if (coupon.getCouponPolicy().getStatus() == CouponPolicyStatus.INACTIVE) {
            throw new IllegalStateException("해당 쿠폰의 정책이 중단되어 발급할 수 없습니다.");
        }
        if (memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new DuplicateCouponException();
        }

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .userId(userId)
                .status(Status.ISSUED)
                .issueAt(LocalDateTime.now())
                .expiredAt(dateCalculator.calculateExpiration(coupon))
                .build();

        try {
            memberCouponRepository.save(memberCoupon);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateCouponException();
        }
    }

    @Override
    @Transactional
    public void issueCouponByUser(Long userId, Long couponId) {
        log.info("사용자 쿠폰 발급 요청 - Coupon: {}, User: {}", couponId, userId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(CouponNotFoundException::new);

        validateCouponValidity(coupon);
        String issuedUserKey = "issued:users:" + couponId;
        String countKey = "coupon:count:" + couponId;
        Long isAdded = redisTemplate.opsForSet().add(issuedUserKey, String.valueOf(userId));

        if (coupon.getIssuedEndAt() != null) {
            redisTemplate.expireAt(issuedUserKey,
                    java.sql.Timestamp.valueOf(coupon.getIssuedEndAt().plusDays(1)));
        }

        if ((isAdded != null) && (isAdded == 0)) {
            throw new DuplicateCouponException();
        }

        if (coupon.getIssueCount() != null) {


            // 원자적 감소 (Atomic Decrement)
            Long remainingCount = redisTemplate.opsForValue().decrement(countKey);

            // 재고 부족 체크 (0 미만이면 매진)
            if (remainingCount != null && remainingCount < 0) {
                // 롤백: 감소시킨 값을 다시 증가시켜 원복
                redisTemplate.opsForValue().increment(countKey);
                // 유저 중복 체크 내역도 롤백
                redisTemplate.opsForSet().remove(issuedUserKey, String.valueOf(userId));

                throw new IllegalStateException("수량이 모두 매진되었습니다.");
            }
        }

        try {
            CouponIssueMessage message = new CouponIssueMessage(userId, couponId);
            rabbitTemplate.convertAndSend(RabbitMqConfig.COUPON_ISSUE_QUEUE, message);
            log.info("쿠폰 발급 요청 큐 적재 완료 - User: {}, Coupon: {}", userId, couponId);
        } catch (Exception e) {
            log.error("메시지 큐 전송 실패, Redis 롤백 수행 - User: {}, Coupon: {}", userId, couponId, e);
            redisTemplate.opsForSet().remove(issuedUserKey, String.valueOf(userId));
            if (coupon.getIssueCount() != null) {
                redisTemplate.opsForValue().increment(countKey);
            }
            throw new RuntimeException("쿠폰 발급 요청 실패", e);
        }
    }

    @Override
    @Transactional
    public void createMemberCoupon(Long userId, Long couponId) {
        log.info("DB 저장 시작 - Coupon: {}, User: {}", couponId, userId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(CouponNotFoundException::new);

        if (memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new DuplicateCouponException();
        }

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .userId(userId)
                .status(Status.ISSUED)
                .issueAt(LocalDateTime.now())
                .expiredAt(dateCalculator.calculateExpiration(coupon))
                .build();

        memberCouponRepository.save(memberCoupon);
    }

    @Override
    public Page<MemberCouponResponseDto> findCouponByUserId(Long userId, Pageable pageable) {
        Page<MemberCoupon> memberCoupons = memberCouponRepository.findByUserId(userId, pageable);
        return memberCoupons.map(MemberCouponResponseDto::fromEntity);
    }

    @Override
    public List<MemberCouponResponseDto> findUsableCoupons(Long userId) {
        List<MemberCoupon> memberCoupons = memberCouponRepository.findAllByUserIdAndStatusAndExpiredAtAfter(userId, Status.ISSUED, LocalDateTime.now());
        return memberCoupons.stream().map(MemberCouponResponseDto::fromEntity).toList();
    }

    @Override
    public CouponCalculationResponseDto calculateDiscount(Long userId, CouponCalculationRequestDto requestDto) {
        Long couponId = requestDto.getCouponId();
        Long orderPrice = requestDto.getTotalOrderPrice();

        MemberCoupon memberCoupon = memberCouponRepository.findByUserIdAndCouponId(userId, couponId)
                .orElseThrow(CouponNotFoundException::new);

        if (memberCoupon.getStatus() != Status.ISSUED) {
            throw new IllegalStateException("이미 사용했거나 만료된 쿠폰입니다.");
        }
        if (memberCoupon.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("유효 기간이 지난 쿠폰입니다.");
        }

        CouponPolicy policy = memberCoupon.getCoupon().getCouponPolicy();

        if (policy.getMinOrderValue() != null && orderPrice < policy.getMinOrderValue()) {
            throw new IllegalArgumentException("최소 주문 금액(" + policy.getMinOrderValue() + "원)을 충족하지 못했습니다.");
        }

        long discountAmount = 0;
        switch (policy.getDiscountType()) {
            case FIXED -> discountAmount = policy.getDiscountValue();
            case PERCENTAGE -> discountAmount = (orderPrice * policy.getDiscountValue()) / 100;
            default -> throw new IllegalStateException("알 수 없는 할인 타입입니다.");
        }

        if (policy.getMaxDiscountValue() != null && discountAmount > policy.getMaxDiscountValue()) {
            discountAmount = policy.getMaxDiscountValue();
        }

        discountAmount = Math.min(discountAmount, orderPrice);

        return CouponCalculationResponseDto.builder()
                .discountAmount(discountAmount)
                .finalPrice(orderPrice - discountAmount)
                .build();
    }

    @Override
    @Transactional
    public void useCoupon(Long userId, MemberCouponUseRequestDto requestDto) {
        Long couponId = requestDto.getCouponId();

        MemberCoupon memberCoupon = memberCouponRepository.findByUserIdAndCouponId(userId, couponId)
                .orElseThrow(CouponNotFoundException::new);

        if (memberCoupon.getStatus() == Status.USED) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }
        if (memberCoupon.getStatus() != Status.ISSUED) {
            throw new IllegalStateException("사용할 수 없는 상태의 쿠폰입니다. (상태: " + memberCoupon.getStatus() + ")");
        }
        if (memberCoupon.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("유효 기간이 지난 쿠폰입니다.");
        }

        memberCoupon.use(requestDto.getOrderId());
        // MemberCoupon 엔티티에 setOrderId가 없다면 추가 필요 (Lombok @Setter가 있다면 가능)
        // memberCoupon.setOrderId(requestDto.getOrderId());
    }

    @Override
    @Transactional
    public void cancelCouponUsage(Long userId, MemberCouponCancelRequestDto requestDto) {
        Long couponId = requestDto.getCouponId();
        Long orderId = requestDto.getOrderId();

        MemberCoupon memberCoupon = memberCouponRepository.findByUserIdAndCouponId(userId, couponId)
                .orElseThrow(CouponNotFoundException::new);

        if (memberCoupon.getStatus() != Status.USED) {
            throw new IllegalStateException("사용된 상태의 쿠폰만 취소할 수 있습니다.");
        }
        if (memberCoupon.getOrderId() != null && !memberCoupon.getOrderId().equals(orderId)) {
            throw new IllegalArgumentException("해당 주문에서 사용된 쿠폰이 아닙니다.");
        }

        memberCoupon.cancel();
    }

    @Override
    @Transactional
    public void issueBirthdayCoupon(Long memberId, Long couponId) {
        log.info("생일 쿠폰 발급 요청 - User: {}, Coupon: {}", memberId, couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(CouponNotFoundException::new);

        if (memberCouponRepository.existsByUserIdAndCouponId(memberId, couponId)) {
            log.warn("이미 생일 쿠폰을 발급받은 회원입니다. User: {}", memberId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59);

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .userId(memberId)
                .status(Status.ISSUED)
                .issueAt(now)
                .expiredAt(endOfMonth)
                .build();
        try {
            memberCouponRepository.save(memberCoupon);
        } catch (DataIntegrityViolationException e) {
            log.warn("이미 생일 쿠폰을 발급받은 회원입니다. (중복 발급 방지) User: {}", memberId);
        }
    }

    @Override
    @Transactional
    public void issueWelcomeCoupon(Long memberId) {
        log.info("웰컴 쿠폰 자동 지급 시도 - User: {}", memberId);
        List<Coupon> coupons = couponRepository.findCouponsByCommentAndStatus(
                Comment.WELCOME,
                CouponPolicyStatus.ACTIVE,
                PageRequest.of(0, 1) // 0페이지에서 1개만 조회 (= LIMIT 1)
        );

        if (coupons.isEmpty()) {
            throw new CouponNotFoundException();
        }
        Coupon welcomeCoupon = coupons.get(0);

        if (memberCouponRepository.existsByUserIdAndCouponId(memberId, welcomeCoupon.getId())) {
            log.info("이미 웰컴 쿠폰을 받은 회원입니다. User: {}", memberId);
            return;
        }

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(welcomeCoupon)
                .userId(memberId)
                .status(Status.ISSUED)
                .issueAt(LocalDateTime.now())
                .expiredAt(dateCalculator.calculateExpiration(welcomeCoupon))
                .build();

        try {
            memberCouponRepository.save(memberCoupon);
            log.info("웰컴 쿠폰 지급 완료! User: {}, Coupon: {}", memberId, welcomeCoupon.getCouponName());
        } catch (DataIntegrityViolationException e) {
            // 이미 다른 스레드나 트랜잭션에서 발급함 -> 성공으로 간주하고 종료
            log.warn("이미 웰컴 쿠폰이 지급되었습니다. (중복 발급 방지) User: {}", memberId);
        }
    }

    private void validateCouponValidity(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getIssuedStartAt() != null && now.isBefore(coupon.getIssuedStartAt())) {
            throw new IllegalArgumentException("아직 발급 가능한 기간이 아닙니다.");
        }
        if (coupon.getIssuedEndAt() != null && now.isAfter(coupon.getIssuedEndAt())) {
            throw new IllegalArgumentException("발급 기간이 지났습니다.");
        }
        if (coupon.getCouponPolicy().getStatus() == CouponPolicyStatus.INACTIVE) {
            throw new IllegalStateException("해당 쿠폰의 정책이 중단되어 발급받을 수 없습니다.");
        }
    }
}
