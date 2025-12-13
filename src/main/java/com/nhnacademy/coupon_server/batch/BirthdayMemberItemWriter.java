package com.nhnacademy.coupon_server.batch;

import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
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

import java.util.List;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class BirthdayMemberItemWriter implements ItemWriter<Long> {
    private final MemberCouponService memberCouponService;
    private final CouponRepository couponRepository;

    private Long cachedBirthdayCouponId;
    private int failureCount = 0;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.cachedBirthdayCouponId = null;
        this.failureCount = 0;
    }

    @Override
    public void write(Chunk<? extends Long> chunk) {
        if (cachedBirthdayCouponId == null) {
            cachedBirthdayCouponId = fetchBirthdayCouponId();
            if (cachedBirthdayCouponId == null) {
                throw new IllegalStateException("활성화된 생일 쿠폰 정책을 찾을 수 없습니다.");
            }
        }

        for (Long userId : chunk) {
            memberCouponService.issueBirthdayCoupon(userId, cachedBirthdayCouponId);
        }
        log.info("이번 청크 작업 완료: {}명 처리 성공", chunk.size());
    }

    private Long fetchBirthdayCouponId() {
        List<Coupon> coupons = couponRepository.findCouponsByCommentAndStatus(
                Comment.BIRTHDAY,
                CouponPolicyStatus.ACTIVE,
                PageRequest.of(0, 1, Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))
        );
        if (coupons.isEmpty()) {
            return null;
        }
        return coupons.get(0).getId();
    }
}
