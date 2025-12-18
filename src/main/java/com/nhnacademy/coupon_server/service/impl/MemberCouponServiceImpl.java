package com.nhnacademy.coupon_server.service.impl;

import com.nhnacademy.coupon_server.calculator.CouponDateCalculator;
import com.nhnacademy.coupon_server.dto.request.CouponCalculationRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponCalculationResponseDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponCancelRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponUseRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponIssueRequestDto;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Override
    public Page<MemberCouponResponseDto> findAll(Pageable pageable) {
        return memberCouponRepository.findAll(pageable).map(MemberCouponResponseDto::fromEntity);
    }

    @Override
    @Transactional
    public void issueCouponByAdmin(MemberCouponIssueRequestDto requestDto) {
        Long userId = requestDto.getUserId();
        Long couponId = requestDto.getCouponId();

        log.info("관리자 수동 발급 요청 - Coupon: {}, User: {}", couponId, userId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> {
                    log.warn("쿠폰을 찾을 수 없음: {}", couponId);
                    return new CouponNotFoundException();
                });

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
                .orElseThrow(() -> {
                    log.warn("발급 대상 쿠폰 없음: {}", couponId);
                    return new CouponNotFoundException();
                });

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getIssuedStartAt() != null && now.isBefore(coupon.getIssuedStartAt())) {
            throw new IllegalArgumentException("아직 발급 가능한 기간이 아닙니다.");
        }
        if (coupon.getIssuedEndAt() != null && now.isAfter(coupon.getIssuedEndAt())) {
            throw new IllegalArgumentException("발급 기간이 지났습니다.");
        }
        if (coupon.getIssueCount() != null) {
            long currentCount = memberCouponRepository.countByCouponId(couponId);
            if (currentCount >= coupon.getIssueCount()) {
                throw new IllegalStateException("수량이 모두 매진되었습니다.");
            }
        }
        if (coupon.getCouponPolicy().getStatus() == CouponPolicyStatus.INACTIVE) {
            throw new IllegalStateException("해당 쿠폰의 정책이 중단되어 더 이상 발급받을 수 없습니다.");
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
        Long memberCouponId = requestDto.getCouponId();
        Long orderPrice = requestDto.getTotalOrderPrice();

        // 1. 발급된 쿠폰 ID(PK)로 조회
        MemberCoupon memberCoupon = memberCouponRepository.findById(memberCouponId)
                .orElseThrow(() -> {
                    log.error("할인 계산 실패 - 존재하지 않는 MemberCoupon ID: {}", memberCouponId);
                    return new CouponNotFoundException();
                });

        // 2. 소유자 검증
        if (!memberCoupon.getUserId().equals(userId)) {
            log.warn("쿠폰 소유자 불일치 - 요청자: {}, 소유자: {}", userId, memberCoupon.getUserId());
            throw new IllegalArgumentException("해당 쿠폰의 소유자가 아닙니다.");
        }

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

    // [수정됨] 쿠폰 사용 처리
    @Override
    @Transactional
    public void useCoupon(Long userId, MemberCouponUseRequestDto requestDto) {
        Long memberCouponId = requestDto.getCouponId();

        // 1. 발급된 쿠폰 ID(PK)로 조회
        MemberCoupon memberCoupon = memberCouponRepository.findById(memberCouponId)
                .orElseThrow(() -> {
                    log.error("쿠폰 사용 실패 - 존재하지 않는 MemberCoupon ID: {}", memberCouponId);
                    return new CouponNotFoundException();
                });

        // 2. 소유자 검증
        if (!memberCoupon.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 쿠폰의 소유자가 아닙니다.");
        }

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
    }

    // [수정됨] 쿠폰 사용 취소
    @Override
    @Transactional
    public void cancelCouponUsage(Long userId, MemberCouponCancelRequestDto requestDto) {
        Long memberCouponId = requestDto.getCouponId();

        // 1. 발급된 쿠폰 ID(PK)로 조회
        MemberCoupon memberCoupon = memberCouponRepository.findById(memberCouponId)
                .orElseThrow(() -> {
                    log.error("쿠폰 취소 실패 - 존재하지 않는 MemberCoupon ID: {}", memberCouponId);
                    return new CouponNotFoundException();
                });

        // 2. 소유자 검증
        if (!memberCoupon.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 쿠폰의 소유자가 아닙니다.");
        }

        if (memberCoupon.getStatus() != Status.USED) {
            throw new IllegalStateException("사용된 상태의 쿠폰만 취소할 수 있습니다.");
        }

        memberCoupon.cancel();
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

        LocalDateTime now = LocalDateTime.now();
        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .userId(memberId)
                .status(Status.ISSUED)
                .issueAt(now)
                .expiredAt(now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59))
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
            log.warn("이미 웰컴 쿠폰이 지급되었습니다. (중복 발급 방지) User: {}", memberId);
        }
    }
}