package com.nhnacademy.coupon_server.repository.coupon;

import com.nhnacademy.coupon_server.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Page<Coupon> findAllByIssuedStartAtBeforeAndIssuedEndAtAfter(LocalDateTime now1, LocalDateTime now2, Pageable pageable);
}
