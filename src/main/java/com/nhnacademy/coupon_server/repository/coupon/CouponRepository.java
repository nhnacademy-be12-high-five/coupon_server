package com.nhnacademy.coupon_server.repository.coupon;

import com.nhnacademy.coupon_server.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
}
