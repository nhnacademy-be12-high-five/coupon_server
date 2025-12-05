package com.nhnacademy.coupon_server.repository.coupon;

import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.CouponType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Page<Coupon> findAllByIssuedStartAtBeforeAndIssuedEndAtAfterAndCouponPolicyStatusAndCouponType(
            LocalDateTime now1,
            LocalDateTime now2,
            CouponPolicyStatus status,
            CouponType couponType,
            Pageable pageable
    );

    List<Coupon> findByCouponPolicyComment(Comment comment);

    @Query("SELECT c FROM Coupon c " +
            "JOIN c.couponPolicy cp " +
            "WHERE cp.comment = 'WELCOME' " +
            "AND cp.status = 'ACTIVE' " +
            "ORDER BY c.id DESC LIMIT 1")
    Optional<Coupon> findWelcomeCoupon();
}
