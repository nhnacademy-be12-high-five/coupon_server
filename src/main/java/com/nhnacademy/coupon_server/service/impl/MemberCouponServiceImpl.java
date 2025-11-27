package com.nhnacademy.coupon_server.service.impl;

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
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.exception.CouponNotFoundException;
import com.nhnacademy.coupon_server.exception.DuplicateCouponException;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.memberCoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
        Page<MemberCoupon> memberCoupons = memberCouponRepository.findAll(pageable);
        return memberCoupons.map(MemberCouponResponseDto::fromEntity);
    }

    @Override
    @Transactional
    public void issueCouponByAdmin(MemberCouponIssueRequestDto requestDto) {
        Long userId = requestDto.getUserId();
        Long couponId = requestDto.getCouponId();

        log.info("관리자 수동 발급 요청 - Coupon: {}, User: {}", couponId, userId);

        Coupon coupon = couponRepository.findById(couponId).orElseThrow(() -> new CouponNotFoundException("존재하지 않는 쿠폰입니다. ID: " + couponId));
        if (memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new DuplicateCouponException("이미 해당 쿠폰을 보유하고 있는 회원입니다.");
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
    public void issueCouponByUser(Long userId, Long couponId) {
        log.info("사용자 쿠폰 발급 요청 - Coupon: {}, User: {}", couponId, userId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponNotFoundException("존재하지 않는 쿠폰입니다. ID: " + couponId));

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
                throw new IllegalStateException("발급 기간이 지났습니다.");
            }
        }

        if (memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new DuplicateCouponException("이미 해당 쿠폰을 발급받으셨습니다.");
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
                .orElseThrow(() -> new CouponNotFoundException("회원이 보유한 쿠폰이 아니거나 존재하지 않습니다."));

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
                .orElseThrow(() -> new CouponNotFoundException("회원이 보유한 쿠폰이 아니거나 존재하지 않습니다. ID: " + couponId));

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
                .orElseThrow(() -> new CouponNotFoundException("회원이 보유한 쿠폰이 아니거나 존재하지 않습니다. ID: " + couponId));

        if (memberCoupon.getStatus() != Status.USED) {
            throw new IllegalStateException("사용된 상태의 쿠폰만 취소할 수 있습니다.");
        }
        if (memberCoupon.getOrderId() != null && !memberCoupon.getOrderId().equals(orderId)) {
            throw new IllegalArgumentException("해당 주문에서 사용된 쿠폰이 아닙니다.");
        }

        memberCoupon.cancel();
    }
}
