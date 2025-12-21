package com.nhnacademy.coupon_server.service.impl;

import com.nhnacademy.coupon_server.calculator.CouponDateCalculator;
import com.nhnacademy.coupon_server.dto.request.CouponCalculationRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponCancelRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponUseRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponCalculationResponseDto;
import com.nhnacademy.coupon_server.dto.response.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Page<MemberCouponResponseDto> findAll(Pageable pageable) {
        return memberCouponRepository.findAll(pageable).map(MemberCouponResponseDto::fromEntity);
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
    @Transactional
    public void issueCouponByAdmin(MemberCouponIssueRequestDto requestDto) {
        Long userId = requestDto.getUserId();
        Long couponId = requestDto.getCouponId();

        log.info("관리자 수동 발급 요청 - Coupon: {}, User: {}", couponId, userId);

        Coupon coupon = getCouponOrThrow(couponId);

        if (coupon.getCouponPolicy().getStatus() == CouponPolicyStatus.INACTIVE) {
            throw new IllegalStateException("해당 쿠폰의 정책이 중단되어 발급할 수 없습니다.");
        }
        validateDuplicateAndSave(userId, coupon);
    }

    @Override
    @Transactional
    public void issueCouponByUser(Long userId, Long couponId) {
        log.info("사용자 쿠폰 발급 요청 - Coupon: {}, User: {}", couponId, userId);

        Coupon coupon = getCouponOrThrow(couponId);

        validateCouponIssuance(coupon);

        String countKey = "coupon:count:" + couponId;
        if (coupon.getIssueCount() != null) {
            try {
                Long remainingCount = redisTemplate.opsForValue().decrement(countKey);

                if (remainingCount != null && remainingCount < 0) {
                    redisTemplate.opsForValue().increment(countKey);
                    throw new IllegalStateException("수량이 모두 매진되었습니다.");
                }
            } catch (RedisConnectionFailureException | RedisSystemException e) {
                log.error("Redis 장애 발생으로 쿠폰 발급 중단 - Coupon: {}, User: {}, Error: {}", couponId, userId, e.getMessage());
                throw new IllegalStateException("시스템 오류로 인해 쿠폰 발급을 진행할 수 없습니다.");
            }
        }

        try {
            validateDuplicateAndSave(userId, coupon);
        } catch (Exception e) {
            if (coupon.getIssueCount() != null) {
                redisTemplate.opsForValue().increment(countKey);
            }
            throw e;
        }
    }

    @Override
    public Page<MemberCouponResponseDto> findCouponByUserId(Long userId, Pageable pageable) {
        return memberCouponRepository.findByUserId(userId, pageable).map(MemberCouponResponseDto::fromEntity);
    }

    @Override
    public List<MemberCouponResponseDto> findUsableCoupons(Long userId) {
        return memberCouponRepository.findAllByUserIdAndStatusAndExpiredAtAfter(userId, Status.ISSUED, LocalDateTime.now())
                .stream().map(MemberCouponResponseDto::fromEntity).toList();
    }

    // [수정됨] 쿠폰 할인 계산
    @Override
    public CouponCalculationResponseDto calculateDiscount(Long userId, CouponCalculationRequestDto requestDto) {
        // requestDto.getCouponId()는 MemberCoupon의 ID (PK)입니다.
        // 1. 발급된 쿠폰 ID(PK)로 조회
        MemberCoupon memberCoupon = findAndValidateOwner(requestDto.getCouponId(), userId);

        memberCoupon.validateUsable();

        long discountAmount = memberCoupon.getCoupon().getCouponPolicy().calculateDiscountAmount(requestDto.getTotalOrderPrice());

        return CouponCalculationResponseDto.builder()
                .discountAmount(discountAmount)
                .finalPrice(requestDto.getTotalOrderPrice() - discountAmount)
                .build();
    }

    // [수정됨] 쿠폰 사용 처리
    @Override
    @Transactional
    public void useCoupon(Long userId, MemberCouponUseRequestDto requestDto) {
       MemberCoupon memberCoupon = findAndValidateOwner(requestDto.getCouponId(), userId);

        memberCoupon.use(requestDto.getOrderId());
    }

    // [수정됨] 쿠폰 사용 취소
    @Override
    @Transactional
    public void cancelCouponUsage(Long userId, MemberCouponCancelRequestDto requestDto) {
        MemberCoupon memberCoupon = findAndValidateOwner(requestDto.getCouponId(), userId);

        memberCoupon.cancel();
    }

    private  Coupon getCouponOrThrow(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(CouponNotFoundException::new);
    }

    private void validateDuplicateAndSave(Long userId, Coupon coupon) {
        if (memberCouponRepository.existsByUserIdAndCouponId(userId, coupon.getId())) {
            throw new DuplicateCouponException();
        }
        try {
            saveMemberCoupon(userId, coupon);
        }catch (DataIntegrityViolationException e) {
            throw new DuplicateCouponException();
        }
    }

    private void saveMemberCoupon(Long userId, Coupon coupon) {
        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .userId(userId)
                .status(Status.ISSUED)
                .issueAt(LocalDateTime.now())
                .expiredAt(dateCalculator.calculateExpiration(coupon))
                .build();
        memberCouponRepository.save(memberCoupon);
    }

    private MemberCoupon findAndValidateOwner(Long memberCouponId, Long userId) {
        MemberCoupon memberCoupon = memberCouponRepository.findById(memberCouponId)
                .orElseThrow(CouponNotFoundException::new);
        memberCoupon.validateOwner(userId);
        return memberCoupon;
    }

    private void validateCouponIssuance(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getIssuedStartAt() != null && now.isBefore(coupon.getIssuedStartAt())) {
            throw new IllegalArgumentException("아직 발급 가능한 기간이 아닙니다.");
        }
        if (coupon.getIssuedEndAt() != null && now.isAfter(coupon.getIssuedEndAt())) {
            throw new IllegalArgumentException("발급 기간이 지났습니다.");
        }
        if (coupon.getCouponPolicy().getStatus() == CouponPolicyStatus.INACTIVE) {
            throw new IllegalStateException("해당 쿠폰의 정책이 중단되어 더 이상 발급받을 수 없습니다.");
        }
    }

    @Override
    @Transactional
    public void issueBirthdayCoupon(Long memberId, Long couponId) {
        log.info("생일 쿠폰 발급 요청 - User: {}, Coupon: {}", memberId, couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponNotFoundException());

        if (memberCouponRepository.existsByUserIdAndCouponId(memberId, couponId)) {
            log.warn("이미 생일 쿠폰을 발급받은 회원입니다. User: {}", memberId);
            return;
        }

        saveMemberCoupon(memberId, coupon);
    }

    @Override
    @Transactional
    public void issueWelcomeCoupon(Long memberId) {
        log.info("웰컴 쿠폰 자동 지급 시도 - User: {}", memberId);
        List<Coupon> coupons = couponRepository.findCouponsByCommentAndStatus(
                Comment.WELCOME,
                CouponPolicyStatus.ACTIVE,
                PageRequest.of(0, 1)
        );

        if (coupons.isEmpty()) {
            // 웰컴 쿠폰 정책이 없으면 예외 던짐 (또는 조용히 로그만 남기고 종료)
            throw new CouponNotFoundException();
        }
        Coupon welcomeCoupon = coupons.get(0);

        if (memberCouponRepository.existsByUserIdAndCouponId(memberId, welcomeCoupon.getId())) {
            log.info("이미 웰컴 쿠폰을 받은 회원입니다. User: {}", memberId);
            return;
        }

        saveMemberCoupon(memberId, welcomeCoupon);
    }
}