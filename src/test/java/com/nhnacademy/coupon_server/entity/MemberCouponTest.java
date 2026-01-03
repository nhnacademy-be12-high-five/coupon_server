package com.nhnacademy.coupon_server.entity;

import com.nhnacademy.coupon_server.entity.state.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberCouponTest {

    @Test
    @DisplayName("쿠폰 사용 가능 검증 성공 - 정상 상태(ISSUED) 및 기간 내")
    void validateUsable_Success() {
        MemberCoupon coupon = MemberCoupon.builder()
                .status(Status.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(1)) // 미래 날짜
                .build();

        assertThatCode(coupon::validateUsable).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("쿠폰 사용 가능 검증 성공 - 만료일이 없는 경우 (Null Safe)")
    void validateUsable_Success_NoExpiration() {
        MemberCoupon coupon = MemberCoupon.builder()
                .status(Status.ISSUED)
                .expiredAt(null) // 만료일 없음
                .build();

        assertThatCode(coupon::validateUsable).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("쿠폰 사용 가능 검증 실패 - 이미 사용된 쿠폰 (USED)")
    void validateUsable_Fail_Used() {
        MemberCoupon coupon = MemberCoupon.builder()
                .status(Status.USED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        assertThatThrownBy(coupon::validateUsable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 사용했거나 사용할 수 없는 쿠폰입니다.");
    }

    @Test
    @DisplayName("쿠폰 사용 가능 검증 실패 - 상태가 만료됨 (EXPIRED)")
    void validateUsable_Fail_StatusExpired() {
        MemberCoupon coupon = MemberCoupon.builder()
                .status(Status.EXPIRED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        assertThatThrownBy(coupon::validateUsable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 사용했거나 사용할 수 없는 쿠폰입니다.");
    }

    @Test
    @DisplayName("쿠폰 사용 가능 검증 실패 - 유효 기간 지남 (Date check)")
    void validateUsable_Fail_DateExpired() {
        MemberCoupon coupon = MemberCoupon.builder()
                .status(Status.ISSUED)
                .expiredAt(LocalDateTime.now().minusSeconds(1)) // 과거 날짜
                .build();

        assertThatThrownBy(coupon::validateUsable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("유효 기간이 지난 쿠폰입니다.");
    }
}