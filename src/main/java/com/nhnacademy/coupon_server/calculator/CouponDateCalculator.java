package com.nhnacademy.coupon_server.calculator;

import com.nhnacademy.coupon_server.entity.Coupon;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CouponDateCalculator {
    public LocalDateTime calculateExpiration(Coupon coupon) {
        if (coupon.getValidEndAt() != null) {
            return coupon.getValidEndAt();
        }

        if (coupon.getValidPeriodDate() != null && coupon.getValidPeriodDate() > 0) {
            return LocalDateTime.now().plusDays(coupon.getValidPeriodDate());
        }

        throw new IllegalStateException("쿠폰의 유효기간 정책이 설정되지 않았습니다. (couponId=" + coupon.getId() + ")");
    }
}
