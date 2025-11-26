package com.nhnacademy.coupon_server.repository.memberCoupon;

import com.nhnacademy.coupon_server.entity.MemberCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {
    // 중복 발급 확인
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    // 해당 쿠폰이 현재까지 몇개 발급되었는지 카운트
    long countByCouponId(Long couponId);
}
