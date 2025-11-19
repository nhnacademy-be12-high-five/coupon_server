package com.nhnacademy.coupon_server.entity;

import com.nhnacademy.coupon_server.entity.state.Status;
import jakarta.persistence.*;
import lombok.*;

import java.security.Timestamp;

@Entity
@Table(name = "member_coupon")
@Getter
//@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class MemberCoupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_coupon_id")
    private Long id;

    @Setter
    @Column(name = "issue_count", nullable = false)
    private Long issueCount;

    @Setter
    @Column(name = "Status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Setter
    @Column(name = "issue_date")
    private Timestamp issueDate;

    @Setter
    @Column(name = "valid_from_date")
    private Timestamp validFromDate;

    @Setter
    @Column(name = "valid_to_date")
    private Timestamp validToDate;

    @Setter
    @Column(name = "used_date")
    private Timestamp usedDate;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_policy_id")
    private CouponPolicy couponPolicy;

    @Column(name = "order_id")
    private Long orderId;
}
