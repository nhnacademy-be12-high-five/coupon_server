package com.nhnacademy.coupon_server.listener;

import com.nhnacademy.coupon_server.dto.message.CouponIssueMessage;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CouponMessageListener {
    private final MemberCouponService memberCouponService;

    @RabbitListener(queues = "coupon-welcome-queue")
    public void receiveWelcomeCouponRequest(CouponIssueMessage message) {
        log.info("RabbitMQ 웰컴 쿠폰 발급 요청 수신 - UserId: {}", message.getMemberId());

        try {
            memberCouponService.issueWelcomeCoupon(message.getMemberId());
        } catch (Exception e) {
            log.error("웰컴 쿠폰 발급 실패 - UserId: {}, Error: {}", message.getMemberId(), e.getMessage(), e);
            if (e instanceof TransientDataAccessException || e instanceof AmqpException) {
                throw e;
            }
        }
    }

    @RabbitListener(queues = "coupon-issue-queue")
    public void receiveIssueCouponRequest(CouponIssueMessage message) {
        log.info("RabbitMQ 일반 쿠폰 발급 요청 수신 - UserId: {}, CouponId: {}", message.getMemberId(), message.getCouponId());
        try {
            memberCouponService.createMemberCoupon(message.getMemberId(), message.getCouponId());
        } catch (Exception e) {
            log.error("쿠폰 발급 DB 저장 실패 - User: {}, CouponId: {}", message.getMemberId(), message.getCouponId(), e);
        }
    }

}
