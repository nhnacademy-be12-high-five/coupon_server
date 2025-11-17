package com.nhnacademy.coupon_server.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coupon_policy_book")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicyBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_policy_book_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private CouponPolicy couponPolicyId;

//    @ManyToOne(fetch = FetchType.LAZY)
    private Long bookId;
}
