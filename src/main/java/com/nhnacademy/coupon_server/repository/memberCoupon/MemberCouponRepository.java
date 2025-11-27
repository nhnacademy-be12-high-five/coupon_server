package com.nhnacademy.coupon_server.repository.memberCoupon;

import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {
    // 중복 발급 확인
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    // 해당 쿠폰이 현재까지 몇개 발급되었는지 카운트
    long countByCouponId(Long couponId);

    // 특정 사용자의 쿠폰 목록 조회 (페이징)
    Page<MemberCoupon> findByUserId(Long userId, Pageable pageable);

    List<MemberCoupon> findAllByUserIdAndStatusAndExpiredAtAfter(Long userId, Status status, LocalDateTime now);

    @EntityGraph(attributePaths = {"coupon", "coupon.couponPolicy"})
    Optional<MemberCoupon> findByUserIdAndCouponId(Long userId, Long couponId);
}
