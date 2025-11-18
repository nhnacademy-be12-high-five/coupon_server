package com.nhnacademy.coupon_server.dto;

import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;


@Getter
@Builder
public class CouponPolicyResponseDto {
    private Long id;
    private String name;
    private Comment comment;
    private DiscountType discountType;
    private Long discountValue;
    private Long minPayValue;
    private Long maxDiscountValue;
    private Timestamp createdAt;

    public static CouponPolicyResponseDto fromEntity(CouponPolicy policy) {
        return CouponPolicyResponseDto.builder()
                .id(policy.getId())
                .name(policy.getName())
                .comment(policy.getComment())
                .discountType(policy.getDiscountType())
                .discountValue(policy.getDiscountValue())
                .minPayValue(policy.getMinPayValue())
                .maxDiscountValue(policy.getMaxDiscountValue())
                .createdAt(policy.getCreatedAt())
                .build();
    }

}
