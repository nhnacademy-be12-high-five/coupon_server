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
    @Query(value = "SELECT c FROM Coupon c " +
            "JOIN FETCH c.couponPolicy cp " +
            "WHERE (c.issuedStartAt IS NULL OR c.issuedStartAt <= :now) " + // 시작일이 없거나, 지났으면 OK
            "AND (c.issuedEndAt IS NULL OR c.issuedEndAt >= :now) " +       // 종료일이 없거나(무제한), 아직 안 지났으면 OK
            "AND cp.status = :status " +
            "AND c.couponType = :couponType",
            countQuery = "SELECT COUNT(c) FROM Coupon c " +
                    "JOIN c.couponPolicy cp " +
                    "WHERE (c.issuedStartAt IS NULL OR c.issuedStartAt <= :now) " +
                    "AND (c.issuedEndAt IS NULL OR c.issuedEndAt >= :now) " +
                    "AND cp.status = :status " +
                    "AND c.couponType = :couponType")
    Page<Coupon> findIssuableCoupons(
            @Param("now") LocalDateTime now,
            @Param("status") CouponPolicyStatus status,
            @Param("couponType") CouponType couponType,
            Pageable pageable
    );

    @Query("SELECT DISTINCT c FROM Coupon c " +
            "JOIN FETCH c.couponPolicy cp " +
            "LEFT JOIN cp.usableBooks ub " +
            "LEFT JOIN cp.usableCategories uc " +
            "WHERE cp.status = :status " +
            "AND (c.issuedStartAt IS NULL OR c.issuedStartAt <= :now) " +
            "AND (c.issuedEndAt IS NULL OR c.issuedEndAt >= :now) " +
            "AND (" +
            "   ub.bookId = :bookId " +
            "   OR uc.categoryId IN :categoryIds " +
            "   OR (ub.bookId IS NULL AND uc.categoryId IS NULL)" +
            ")")
    List<Coupon> findCouponsForProduct(
            @Param("bookId") Long bookId,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("status") CouponPolicyStatus status,
            @Param("now") LocalDateTime now
    );

    @Override
    @Query("SELECT c FROM Coupon c JOIN FETCH c.couponPolicy")
    List<Coupon> findAll();

    @Override
    @Query(value = "SELECT c FROM Coupon c JOIN FETCH c.couponPolicy",
            countQuery = "SELECT COUNT(c) FROM Coupon c")
    Page<Coupon> findAll(Pageable pageable);

    @Query("SELECT c FROM Coupon c JOIN FETCH c.couponPolicy WHERE c.couponPolicy.comment = :comment")
    List<Coupon> findByCouponPolicyComment(@Param("comment") Comment comment);

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
