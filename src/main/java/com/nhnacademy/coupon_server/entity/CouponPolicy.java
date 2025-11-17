package com.nhnacademy.coupon_server.entity;

import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coupon_policy")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_policy_id")
    private Long id;

    @Column(name = "coupon_policy_name", nullable = false)
    private String name;

    @Column(name = "comment", nullable = false)
    @Enumerated(EnumType.STRING)
    private Comment comment;

    @Column(name = "discount_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private Long discountValue;

    @Column(name = "min_pay_value")
    private Long minPayValue;

    @Column(name = "max_discount_value")
    private Long maxDiscountValue;

    @Column(name = "coupon_created_at")
    private Timestamp createdAt;

    @Column(name = "coupon_update_at")
    private Timestamp updatedAt;

    @OneToMany(mappedBy = "couponPolicy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CouponPolicyBook> usableBooks = new ArrayList<>();

    @OneToMany(mappedBy = "couponPolicy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CouponPolicyCategory> usableCategories = new ArrayList<>();
}
