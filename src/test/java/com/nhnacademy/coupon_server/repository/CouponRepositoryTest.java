package com.nhnacademy.coupon_server.repository;

import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CouponRepositoryTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private TestEntityManager entityManager;

    private CouponPolicy couponPolicy;

    @BeforeEach
    void setUp() {
        couponPolicy = CouponPolicy.builder()
                .name("테스트 정책")
                .comment(Comment.EVENT)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .minOrderValue(5000L)
                .maxDiscountValue(1000L)
                .build();
        entityManager.persist(couponPolicy);
    }

    @Test
    @DisplayName("현재 발급 가능한 기간의 쿠폰만 조회한다")
    void findAllByIssuedStartAtBeforeAndIssuedEndAtAfter() {
        LocalDateTime now = LocalDateTime.now();
        Coupon activeCoupon = Coupon.builder()
                .couponPolicy(couponPolicy)
                .couponName("발급 가능 쿠폰")
                .issueCount(100)
                .issuedStartAt(now.minusDays(1))
                .issuedEndAt(now.plusDays(1))
                .validPeriodDate(30)
                .build();
        Coupon futureCoupon = Coupon.builder()
                .couponPolicy(couponPolicy)
                .couponName("미래 쿠폰")
                .issueCount(100)
                .issuedStartAt(now.plusDays(1))
                .issuedEndAt(now.plusDays(2))
                .validPeriodDate(30)
                .build();

        Coupon expiredCoupon = Coupon.builder()
                .couponPolicy(couponPolicy)
                .couponName("만료된 쿠폰")
                .issueCount(100)
                .issuedStartAt(now.minusDays(2))
                .issuedEndAt(now.minusDays(1))
                .validPeriodDate(30)
                .build();

        entityManager.persist(activeCoupon);
        entityManager.persist(futureCoupon);
        entityManager.persist(expiredCoupon);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Coupon> result = couponRepository.findAllByIssuedStartAtBeforeAndIssuedEndAtAfterAndCouponPolicyStatus(now, now, CouponPolicyStatus.ACTIVE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCouponName()).isEqualTo("발급 가능 쿠폰");
    }
}