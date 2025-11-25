package com.nhnacademy.coupon_server.dto.memberCoupon;

import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Status;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberCouponResponseDto {
    private Long id;
    private Long userId;
    private Long couponId;
    private String couponName;
    private Status status;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
    private LocalDateTime expiredAt;
    private Long orderId;

    public static MemberCouponResponseDto fromEntity(MemberCoupon memberCoupon) {
        return MemberCouponResponseDto.builder()
                .id(memberCoupon.getId())
                .userId(memberCoupon.getUserId())
                .couponId(memberCoupon.getCoupon().getId())
                .couponName(memberCoupon.getCoupon().getCouponName())
                .status(memberCoupon.getStatus())
                .issuedAt(memberCoupon.getIssueAt())
                .usedAt(memberCoupon.getUsedAt())
                .expiredAt(memberCoupon.getExpiredAt())
                .orderId(memberCoupon.getOrderId())
                .build();
    }
}
