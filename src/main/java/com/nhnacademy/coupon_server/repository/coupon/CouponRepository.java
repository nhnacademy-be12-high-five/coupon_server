package com.nhnacademy.coupon_server.repository.coupon;

import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.CouponType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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
            "JOIN FETCH c.couponPolicy cp " +
            "WHERE cp.comment = :comment " +
            "AND cp.status = :status " +
            "ORDER BY c.id DESC")
    List<Coupon> findCouponsByCommentAndStatus(
            @Param("comment") Comment comment,
            @Param("status") CouponPolicyStatus status,
            Pageable pageable
    );

    @Query("SELECT DISTINCT c FROM Coupon c " +
            "JOIN FETCH c.couponPolicy cp " +
            "JOIN cp.usableBooks b " +
            "WHERE b.bookId = :bookId " +
            "AND cp.status = :status " +
            "AND (c.issuedStartAt IS NULL OR c.issuedStartAt <= :now) " +
            "AND (c.issuedEndAt IS NULL OR c.issuedEndAt >= :now)")
    List<Coupon> findByBookIdAndStatus(@Param("bookId") Long bookId,
                              @Param("status") CouponPolicyStatus status,
                              @Param("now") LocalDateTime now);
}
