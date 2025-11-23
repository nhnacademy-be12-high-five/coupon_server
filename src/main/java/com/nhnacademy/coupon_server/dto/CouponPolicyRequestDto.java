package com.nhnacademy.coupon_server.dto;

import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponPolicyRequestDto {
    @NotBlank(message = "쿠폰 정책 이름은 필수입니다.")
    private String name;

    @NotNull(message = "쿠폰 용도는 필수입니다.")
    private Comment comment;

    @NotNull(message = "할인 유형은 필수입니다.")
    private DiscountType discountType;

    @NotNull(message = "할인 값은 필수입니다.")
    @PositiveOrZero(message = "할인 값은 0 이상이어야 합니다.")
    private Long discountValue;

    @PositiveOrZero(message = "최소 주문 금액은 0 이상이어야 합니다.")
    private Long minOrderValue;

    @PositiveOrZero(message = "최대 할인 금액은 0 이상이어야 합니다.")
    private Long maxDiscountValue;

    private List<Long> targetBookIds;

    private List<Long> targetCategoryIds;
}
