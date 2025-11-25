package com.nhnacademy.coupon_server.repository.memberCoupon;

import com.nhnacademy.coupon_server.entity.MemberCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
}
