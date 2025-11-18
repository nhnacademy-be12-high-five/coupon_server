package com.nhnacademy.coupon_server.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coupon_policy_category")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicyCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_policy_category_id")
    private Long id;

    @Column(name = "category_id")
    private Long categoryId;

    @ManyToOne
    @JoinColumn(name = "policy_id")
    private CouponPolicy policy;
}
