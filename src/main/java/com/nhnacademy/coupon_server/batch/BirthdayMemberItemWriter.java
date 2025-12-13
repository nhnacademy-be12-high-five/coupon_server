package com.nhnacademy.coupon_server.batch;

import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BirthdayMemberItemWriter implements ItemWriter<Long> {
    private final MemberCouponService memberCouponService;
    private final CouponRepository couponRepository;

    private Long cachedBirthdayCouponId;

    @Override
    public void write(Chunk<? extends Long> chunk) {
        if (cachedBirthdayCouponId == null) {
            cachedBirthdayCouponId = fetchBirthdayCouponId();
            if (cachedBirthdayCouponId == null) {
                throw new IllegalStateException("활성화된 생일 쿠폰 정책을 찾을 수 없습니다.");
            }
        }

        for (Long userId : chunk) {
            try {
                memberCouponService.issueBirthdayCoupon(userId, cachedBirthdayCouponId);
            } catch (Exception e) {
                log.error("생일 쿠폰 발급 실패 - UserId: {}, Error: {}", userId, e.getMessage());
                // 개별 실패가 전체 배치를 멈추지 않게 하려면 여기서 try-catch
            }
        }
        log.info("이번 청크 작업 완료: {}명 처리 시도", chunk.size());
    }

    private Long fetchBirthdayCouponId() {
        List<Coupon> coupons = couponRepository.findCouponsByCommentAndStatus(Comment.BIRTHDAY, CouponPolicyStatus.ACTIVE, PageRequest.of(0,1));
        if (coupons.isEmpty()) {
            return null;
        }
        return coupons.get(0).getId();
    }
}
