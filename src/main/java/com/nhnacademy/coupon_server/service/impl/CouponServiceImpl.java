package com.nhnacademy.coupon_server.service.impl;

import com.nhnacademy.coupon_server.dto.coupon.CouponRequestDto;
import com.nhnacademy.coupon_server.dto.coupon.CouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.CouponType;
import com.nhnacademy.coupon_server.exception.CouponPolicyNotFoundException;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.couponPolicy.CouponPolicyRepository;
import com.nhnacademy.coupon_server.repository.memberCoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {
    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;

    @Override
    @Transactional
    public CouponResponseDto create(CouponRequestDto couponRequestDto) {
        log.info("쿠폰 템플릿 생성 요청 - 정책 ID: {}, 이름: {}", couponRequestDto.getId(), couponRequestDto.getCouponName());

        CouponPolicy couponPolicy = couponPolicyRepository.findById(couponRequestDto.getId())
                .orElseThrow(() -> new CouponPolicyNotFoundException("쿠폰 정책을 찾을 수 없습니다. ID -> " + couponRequestDto.getId()));

        Coupon coupon = Coupon.builder()
                .couponPolicy(couponPolicy)
                .couponName(couponRequestDto.getCouponName())
                .description(couponRequestDto.getDescription())
                .issueCount(couponRequestDto.getIssueCount())
                .issueCount(couponRequestDto.getIssueCount())
                .issuedStartAt(couponRequestDto.getIssueStartAt())
                .issuedEndAt(couponRequestDto.getIssueEndAt())
                .validPeriodDate(couponRequestDto.getValidPeriodDate())
                .validEndAt(couponRequestDto.getValidEndAt())
                .couponType(couponRequestDto.getCouponType() != null ? couponRequestDto.getCouponType() : CouponType.NORMAL)
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);
        return CouponResponseDto.fromEntity(savedCoupon);
    }

    @Override
    public List<CouponResponseDto> findAll() {
        log.info("모든 쿠폰 템플릿 조회 요청");

        return couponRepository.findAll().stream()
                .map(coupon -> {
                    Integer remainingCount = null;
                    if (coupon.getIssueCount() != null) {
                        long issuedCount = memberCouponRepository.countByCouponId(coupon.getId());
                        remainingCount = Math.max(0, coupon.getIssueCount() - (int) issuedCount);
                    }
                    return CouponResponseDto.fromEntity(coupon, remainingCount);
                })
                .toList();
    }

    @Override
    public Page<CouponResponseDto> findIssuableCoupons(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<Coupon> coupons = couponRepository.findAllByIssuedStartAtBeforeAndIssuedEndAtAfterAndCouponPolicyStatusAndCouponType(
                now, now, CouponPolicyStatus.ACTIVE, CouponType.NORMAL, pageable
        );
        List<CouponResponseDto> filteredList = coupons.stream()
                .map(coupon -> {
                    Integer remainingCount = null;
                    if (coupon.getIssueCount() != null) {
                        long issueCount = memberCouponRepository.countByCouponId(coupon.getId());
                        remainingCount = Math.max(0, coupon.getIssueCount() - (int) issueCount);
                    }
                    return CouponResponseDto.fromEntity(coupon, remainingCount);
                })
                .filter(dto -> dto.getRemainingCount() == null || dto.getRemainingCount() > 0)
                .toList();

        return new PageImpl<>(filteredList, pageable, filteredList.size());
    }
}
