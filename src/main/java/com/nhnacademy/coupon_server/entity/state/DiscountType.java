package com.nhnacademy.coupon_server.entity.state;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RequiredArgsConstructor
public enum DiscountType {
    FIXED {
        @Override
        public long calculate(long orderPrice, long discountValue) {
            return discountValue;
        }
    },
    PERCENTAGE {
        @Override
        public long calculate(long orderPrice, long discountValue) {
            return BigDecimal.valueOf(orderPrice)
                    .multiply(BigDecimal.valueOf(discountValue))
                    .divide(BigDecimal.valueOf(100), RoundingMode.DOWN)
                    .longValue();
        }
    };

    public abstract long calculate(long orderPrice, long discountValue);
}
