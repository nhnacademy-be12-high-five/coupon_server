package com.nhnacademy.coupon_server.entity;

import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.CouponType;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "coupon_policy")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "coupon_policy_name", nullable = false)
    @Setter
    private String name;

    @Enumerated(EnumType.STRING)
    private CouponType couponType;

    @Column(name = "discount_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @Setter
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    @Setter
    private Long discountValue;

    @Column(name = "min_order_value")
    @Setter
    private Long minOrderValue;

    @Column(name = "max_discount_value")
    @Setter
    private Long maxDiscountValue;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @OneToMany(mappedBy = "couponPolicy", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter
    @Builder.Default
    @BatchSize(size = 100)
    private Set<CouponPolicyBook> usableBooks = new HashSet<>();

    @OneToMany(mappedBy = "couponPolicy", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter
    @Builder.Default
    private Set<CouponPolicyCategory> usableCategories = new HashSet<>();

    public void disable() {
        this.isActive = false;
    }

    public long calculateDiscountAmount(long orderPrice) {
        if (this.minOrderValue != null && orderPrice < this.minOrderValue) {
            throw new IllegalArgumentException("최소 주문 금액(" + this.minOrderValue + "원)을 충족하지 못했습니다.");
        }
        long discountAmount = this.discountType.calculate(orderPrice, this.discountValue);

        if (this.maxDiscountValue != null && discountAmount > this.maxDiscountValue) {
            discountAmount = this.maxDiscountValue;
        }

        return Math.min(discountAmount, orderPrice);
    }
}
