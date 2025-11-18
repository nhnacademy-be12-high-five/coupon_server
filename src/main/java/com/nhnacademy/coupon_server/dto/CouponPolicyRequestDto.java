package com.nhnacademy.coupon_server.dto;

import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CouponPolicyRequestDto {
    @NotBlank(message = "쿠폰 정책 이름은 필수입니다.")
    private String name;

    @NotBlank(message = "쿠폰 용도는 필수입니다.")
    private Comment comment;

    @NotBlank(message = "할인 유형은 필수입니다.")
    private DiscountType discountType;

    @NotBlank(message = "할인 값은 필수입니다.")
    @Positive(message = "할인 값은 양수이어야 합니다.")
    private Long discountValue;

    @PositiveOrZero(message = "최소 주문 금액은 0 이상이어야 합니다.")
    private Long minPayValue;

    @PositiveOrZero(message = "최대 할인 금액은 0이상 커야 합니다.")
    private Long maxDiscountValue;
}
