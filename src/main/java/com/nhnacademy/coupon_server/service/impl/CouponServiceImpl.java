package com.nhnacademy.coupon_server.service.impl;

import com.nhnacademy.coupon_server.dto.request.CouponRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponCountDto;
import com.nhnacademy.coupon_server.dto.response.CouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.CouponStatus;
import com.nhnacademy.coupon_server.entity.state.CouponType;
import com.nhnacademy.coupon_server.exception.CouponPolicyNotFoundException;
import com.nhnacademy.coupon_server.exception.CouponServerException;
import com.nhnacademy.coupon_server.exception.ErrorCode;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.couponPolicy.CouponPolicyRepository;
import com.nhnacademy.coupon_server.repository.memberCoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {
    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final RedisTemplate<Object, Object> redisTemplate;

    @Override
    @Transactional
    public CouponResponseDto create(CouponRequestDto couponRequestDto) {
        log.info("쿠폰 템플릿 생성 요청 - 정책 ID: {}, 이름: {}", couponRequestDto.getId(), couponRequestDto.getCouponName());

        CouponPolicy couponPolicy = couponPolicyRepository.findById(couponRequestDto.getId())
                .orElseThrow(() -> new CouponPolicyNotFoundException());

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

        if (savedCoupon.getIssueCount() != null) {
            String countKey = "coupon:count:" + savedCoupon.getId();
            redisTemplate.opsForValue().set(countKey, String.valueOf(savedCoupon.getIssueCount()));
            if (savedCoupon.getIssuedEndAt() != null) {
                redisTemplate.expireAt(countKey, Timestamp.valueOf(savedCoupon.getIssuedEndAt().plusDays(1)));
            }
        }
        return CouponResponseDto.fromEntity(savedCoupon);
    }

    @Override
    public List<CouponResponseDto> findAll() {
        log.info("모든 쿠폰 템플릿 조회 요청");
        List<Coupon> coupons = couponRepository.findAll();

        if (coupons.isEmpty()) {
            return List.of();
        }
        List<Long> couponIds = coupons.stream().map(Coupon::getId).toList();

        Map<Long, Long> issuedCountMap = memberCouponRepository.countByCouponIdIn(couponIds).stream()
                .collect(Collectors.toMap(
                        CouponCountDto::getCouponId,
                        CouponCountDto::getCount
                ));

        return coupons.stream()
                .map(coupon -> CouponResponseDto.fromEntity(
                        coupon,
                        issuedCountMap.getOrDefault(coupon.getId(), 0L)
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CouponResponseDto> findAll(Pageable pageable){
        Page<Coupon> couponPage = couponRepository.findAll(pageable);
        if (couponPage.isEmpty()){
            return Page.empty(pageable);
        }
        List<Long> couponIds = couponPage.getContent().stream()
                .map(Coupon::getId)
                .toList();

        Map<Long, Long> issuedCountMap = memberCouponRepository.countByCouponIdIn(couponIds).stream()
                .collect(Collectors.toMap(
                        CouponCountDto::getCouponId,
                        CouponCountDto::getCount
                ));
        return couponPage.map(coupon ->
                CouponResponseDto.fromEntity(
                        coupon,
                        issuedCountMap.getOrDefault(coupon.getId(), 0L)
                ));
    }

    @Override
    public Page<CouponResponseDto> findIssuableCoupons(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<Coupon> coupons = couponRepository.findIssuableCoupons(
                now, CouponPolicyStatus.ACTIVE, CouponType.NORMAL, pageable
        );

        if (coupons.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> couponIds = coupons.getContent().stream()
                .map(Coupon::getId)
                .toList();

        Map<Long, Long> issuedCountMap = memberCouponRepository.countByCouponIdIn(couponIds).stream()
                .collect(Collectors.toMap(
                        CouponCountDto::getCouponId,
                        CouponCountDto::getCount
                ));

        List<CouponResponseDto> filteredList = coupons.stream()
                .map(coupon -> CouponResponseDto.fromEntity(
                        coupon,
                        issuedCountMap.getOrDefault(coupon.getId(), 0L) // issuedCount 전달
                ))
                .filter(dto -> dto.getRemainingCount() == null || dto.getRemainingCount() > 0)
                .toList();

        return new PageImpl<>(filteredList, pageable, filteredList.size());
    }

//    @Override
//    public Page<CouponResponseDto> getCoupons(Pageable pageable) {
//        Page<Coupon> coupons = couponRepository.findAll(pageable);
//
//        return coupons.map(coupon -> {
//            long issuedCount = memberCouponRepository.countByCouponId(coupon.getId());
//
//            Integer remainingCount = null;
//            if (coupon.getIssueCount() != null) {
//                remainingCount = coupon.getIssueCount() - (int) issuedCount;
//            }
//
//            return CouponResponseDto.fromEntity(coupon, remainingCount);
//        });
//    }

    @Override
    public List<CouponResponseDto> findCouponsByBookId(Long bookId) {
        // 1. 입력값 검증
        if (bookId == null || bookId <= 0) {
            throw new CouponServerException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Coupon> coupons = couponRepository.findByBookIdAndStatus(
                bookId,
                CouponPolicyStatus.ACTIVE,
                LocalDateTime.now()
        );

        return coupons.stream()
                .map(coupon -> {
                    // 2. 남은 수량 계산 (다른 메서드와 로직 통일)
                    Integer remainingCount = null;
                    if (coupon.getIssueCount() != null) {
                        long issuedCount = memberCouponRepository.countByCouponId(coupon.getId());
                        remainingCount = Math.max(0, coupon.getIssueCount() - (int) issuedCount);
                    }
                    // remainingCount를 전달하여 DTO 생성 (SOLD_OUT 상태 등 반영됨)
                    return CouponResponseDto.fromEntity(coupon, remainingCount);
                })
                .toList();
    }

    @Override
    @Transactional
    public void updateCouponStatus(Long couponId, CouponStatus status) {
        Coupon coupon = couponRepository.findById(couponId).orElseThrow(() -> new CouponServerException(ErrorCode.COUPON_NOT_FOUND));
        try {
            CouponStatus newStatus = CouponStatus.valueOf(status.toString());
            CouponStatus oldStatus = coupon.getStatus();

            // 1. 멱등성 검사: 이미 같은 상태라면 무시
            if (oldStatus == newStatus) {
                log.info("쿠폰 상태 변경 무시 (이미 {} 상태임) - CouponId: {}", newStatus, couponId);
                return;
            }

            // 2. 상태 변경
            coupon.updateStatus(newStatus);

            // 3. 재발행(INACTIVE -> ACTIVE) 시 Redis 재고 동기화 로직
            if (oldStatus == CouponStatus.INACTIVE && newStatus == CouponStatus.ACTIVE) {
                restoreRedisStock(coupon);
            }

        } catch (IllegalArgumentException e) {
            throw new CouponServerException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void restoreRedisStock(Coupon coupon) {
        if (coupon.getIssueCount() == null) return; // 무제한 쿠폰은 스킵

        String countKey = "coupon:count:" + coupon.getId();
        // 키가 없을 때만 복구 (이미 있으면 기존 수량 유지)
        if (Boolean.FALSE.equals(redisTemplate.hasKey(countKey))) {
            long issuedCount = memberCouponRepository.countByCouponId(coupon.getId());
            long remainingCount = Math.max(0, coupon.getIssueCount() - issuedCount);

            redisTemplate.opsForValue().set(countKey, String.valueOf(remainingCount));

            // 만료 시간 재설정 (쿠폰 종료일 + 1일)
            if (coupon.getIssuedEndAt() != null) {
                long ttl = java.sql.Timestamp.valueOf(coupon.getIssuedEndAt().plusDays(1)).getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    redisTemplate.expire(countKey, ttl, TimeUnit.MILLISECONDS);
                }
            }
            log.info("재발행 쿠폰 Redis 재고 복구 완료 - CouponId: {}, Count: {}", coupon.getId(), remainingCount);
        }
    }
}