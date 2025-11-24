package com.nhnacademy.coupon_server.dto.coupon;

import com.nhnacademy.coupon_server.entity.Coupon;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CouponResponseDto {
    private Long id;
    private Long couponPolicyId;
    private String couponName;
    private String description;
    private Integer issueCount;
    private LocalDateTime issueStartAt;
    private LocalDateTime issueEndAt;
    private Integer validPeriodDate;
    private LocalDateTime validEndAt;

    public static CouponResponseDto fromEntity(Coupon coupon) {
        return CouponResponseDto.builder()
                .id(coupon.getId())
                .couponPolicyId(coupon.getCouponPolicy().getId())
                .couponName(coupon.getCouponName())
                .description(coupon.getDescription())
                .issueCount(coupon.getIssueCount())
                .issueStartAt(coupon.getIssuedStartAt())
                .issueEndAt(coupon.getIssuedEndAt())
                .validPeriodDate(coupon.getValidPeriodDate())
                .validEndAt(coupon.getValidEndAt())
                .build();
    }
}
