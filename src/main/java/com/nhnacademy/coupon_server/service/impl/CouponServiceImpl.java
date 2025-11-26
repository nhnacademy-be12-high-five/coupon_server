package com.nhnacademy.coupon_server.service.impl;

import com.nhnacademy.coupon_server.dto.coupon.CouponRequestDto;
import com.nhnacademy.coupon_server.dto.coupon.CouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.exception.CouponNotFoundException;
import com.nhnacademy.coupon_server.exception.CouponPolicyNotFoundException;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.couponPolicy.CouponPolicyRepository;
import com.nhnacademy.coupon_server.repository.memberCoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.service.CouponService;
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
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);
        return CouponResponseDto.fromEntity(savedCoupon);
    }

    @Override
    public List<CouponResponseDto> findAll() {
        log.info("모든 쿠폰 템플릿 조회 요청");

        return couponRepository.findAll().stream()
                .map(CouponResponseDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public CouponResponseDto update(Long id, CouponRequestDto couponRequestDto) {
        log.info("쿠폰 템플릿 수정 요청 - ID -> {}", id);

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new CouponNotFoundException("쿠폰 템플릿을 찾을 수 없습니다. ID -> " + id));

        CouponPolicy couponPolicy = couponPolicyRepository.findById(couponRequestDto.getId())
                .orElseThrow(() -> new CouponPolicyNotFoundException("쿠폰 정책을 찾을 수 없습니다, ID -> " + couponRequestDto.getId()));

        coupon.update(
                couponPolicy,
                couponRequestDto.getCouponName(),
                couponRequestDto.getDescription(),
                couponRequestDto.getIssueCount(),
                couponRequestDto.getIssueStartAt(),
                couponRequestDto.getIssueEndAt(),
                couponRequestDto.getValidPeriodDate(),
                couponRequestDto.getValidEndAt()
        );
        return CouponResponseDto.fromEntity(coupon);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("쿠폰 템플릿 삭제 요청 - ID: {}", id);

        if (!couponRepository.existsById(id)) {
            throw new CouponPolicyNotFoundException("삭제할 쿠폰 템플릿이 존재하지 않습니다. ID: " + id);
        }

        couponRepository.deleteById(id);
    }

    @Override
    public Page<CouponResponseDto> findIssuableCoupons(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<Coupon> coupons = couponRepository.findAllByIssuedStartAtBeforeAndIssuedEndAtAfter(now, now, pageable);
        return coupons.map(coupon -> {
            Integer remainingCount = null;
            if (coupon.getIssueCount() != null) {
                long issueCount = memberCouponRepository.countByCouponId(coupon.getId());
                remainingCount = Math.max(0, coupon.getIssueCount() - (int) issueCount);
            }

            return CouponResponseDto.fromEntity(coupon, remainingCount);
        });
    }
}
