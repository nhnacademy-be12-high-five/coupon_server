package com.nhnacademy.coupon_server.batch;

import com.nhnacademy.coupon_server.calculator.CouponDateCalculator;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.membercoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class BirthdayMemberItemWriter implements ItemWriter<Long> {

    private final MemberCouponRepository memberCouponRepository;
    private final MemberCouponJdbcRepository memberCouponJdbcRepository;
    private final CouponRepository couponRepository;
    private final CouponDateCalculator dateCalculator;

    private Coupon cachedBirthdayCoupon;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.cachedBirthdayCoupon = fetchBirthdayCoupon();
        if (this.cachedBirthdayCoupon == null) {
            throw new IllegalStateException("활성화된 생일 쿠폰 정책을 찾을 수 없습니다.");
        }
    }

    @Override
    public void write(Chunk<? extends Long> chunk) {
        List<Long> userIds = new ArrayList<>(chunk.getItems());

        if (userIds.isEmpty()) return;

        // 1. [조회 최적화] 이미 발급받은 유저 ID 조회 (IN 절, 1회 쿼리)
        List<Long> alreadyIssuedIds = memberCouponRepository.findUserIdsByCouponIdAndUserIdIn(
                cachedBirthdayCoupon.getId(),
                userIds
        );

        // 2. 중복 제거 (발급 대상만 필터링)
        List<Long> targetUserIds = userIds.stream()
                .filter(id -> !alreadyIssuedIds.contains(id))
                .toList();

        if (targetUserIds.isEmpty()) {
            log.info("이번 청크는 모두 이미 발급된 유저입니다. (Skip)");
            return;
        }

        // 3. Entity 리스트 생성 (메모리 작업)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = dateCalculator.calculateExpiration(cachedBirthdayCoupon); // 배치 내 동일 만료일 적용

        List<MemberCoupon> memberCoupons = targetUserIds.stream()
                .map(userId -> MemberCoupon.builder()
                        .coupon(cachedBirthdayCoupon)
                        .userId(userId)
                        .status(Status.ISSUED)
                        .issueAt(now)
                        .expiredAt(expiredAt)
                        .build())
                .toList();

        // 4. [쓰기 최적화] Bulk Insert 수행 (1회 쿼리)
        memberCouponJdbcRepository.batchInsertMemberCoupons(memberCoupons);

        log.info("Bulk Insert 완료: {}명 (중복 제외됨)", memberCoupons.size());
    }

    private Coupon fetchBirthdayCoupon() {
        List<Coupon> coupons = couponRepository.findCouponsByCommentAndStatus(
                Comment.BIRTHDAY,
                CouponPolicyStatus.ACTIVE,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id"))
        );
        return coupons.isEmpty() ? null : coupons.get(0);
    }
}
