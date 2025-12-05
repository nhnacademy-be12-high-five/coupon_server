package com.nhnacademy.coupon_server.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponCalculationRequestDto {
    @NotNull(message = "쿠폰 ID는 필수 입니다.")
    private Long couponId;

    @NotNull(message = "주문 총 금액은 필수입니다.")
    @Min(value = 0, message = "주문 금액은 0원 이상이어야 합니다.")
    private Long totalOrderPrice;
}
