package com.nhnacademy.coupon_server.entity;

import com.nhnacademy.coupon_server.entity.state.Status;
import jakarta.persistence.*;
import lombok.*;

import java.security.Timestamp;

@Entity
@Table(name = "member_coupon")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberCoupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_coupon_id")
    private Long id;

    @Column(name = "issue_count", nullable = false)
    private Long issueCount;

    @Column(name = "Status")
    private Status status;

    @Column(name = "issue_date")
    private Timestamp issueDate;

    @Column(name = "valid_from_date")
    private Timestamp validFromDate;

    @Column(name = "valid_to_date")
    private Timestamp validToDate;

    @Column(name = "used_date")
    private Timestamp usedDate;
}
