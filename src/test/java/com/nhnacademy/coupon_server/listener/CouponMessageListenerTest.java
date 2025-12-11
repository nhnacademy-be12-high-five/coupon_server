package com.nhnacademy.coupon_server.listener;

import com.nhnacademy.coupon_server.dto.message.CouponIssueMessage;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.dao.TransientDataAccessException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponMessageListenerTest {

    @Mock
    private MemberCouponService memberCouponService;

    @InjectMocks
    private CouponMessageListener couponMessageListener;

    @Test
    @DisplayName("RabbitMQ 메시지 수신 성공 - 정상적으로 쿠폰 발급 서비스 호출")
    void receiveWelcomeCouponRequestSuccess() {
        Long memberId = 1L;
        CouponIssueMessage message = new CouponIssueMessage(memberId);

        doNothing().when(memberCouponService).issueWelcomeCoupon(memberId);

        couponMessageListener.receiveWelcomeCouponRequest(message);

        verify(memberCouponService, times(1)).issueWelcomeCoupon(memberId);
    }

    @Test
    @DisplayName("일반 예외 발생 (RuntimeException) - 로그를 남기고 예외를 삼켜야 함 (ACK 처리)")
    void receiveWelcomeCouponRequestGeneralException() {
        Long memberId = 1L;
        CouponIssueMessage message = new CouponIssueMessage(memberId);

        doThrow(new RuntimeException("일반 오류 발생")).when(memberCouponService).issueWelcomeCoupon(memberId);

        assertDoesNotThrow(() -> couponMessageListener.receiveWelcomeCouponRequest(message));

        verify(memberCouponService, times(1)).issueWelcomeCoupon(memberId);
    }

    @Test
    @DisplayName("재시도 가능한 예외 (TransientDataAccessException) - 예외를 다시 던져야 함 (NACK/Retry)")
    void receiveWelcomeCouponRequestTransientDataAccessException() {
        Long memberId = 1L;
        CouponIssueMessage message = new CouponIssueMessage(memberId);

        TransientDataAccessException transientException = mock(TransientDataAccessException.class);
        doThrow(transientException).when(memberCouponService).issueWelcomeCoupon(memberId);

        assertThrows(TransientDataAccessException.class, () ->
                couponMessageListener.receiveWelcomeCouponRequest(message)
        );

        verify(memberCouponService, times(1)).issueWelcomeCoupon(memberId);
    }

    @Test
    @DisplayName("재시도 가능한 예외 (AmqpException) - 예외를 다시 던져야 함 (NACK/Retry)")
    void receiveWelcomeCouponRequest_AmqpException() {
        Long memberId = 1L;
        CouponIssueMessage message = new CouponIssueMessage(memberId);

        doThrow(new AmqpException("MQ 연결 오류")).when(memberCouponService).issueWelcomeCoupon(memberId);

        assertThrows(AmqpException.class, () ->
                couponMessageListener.receiveWelcomeCouponRequest(message)
        );

        verify(memberCouponService, times(1)).issueWelcomeCoupon(memberId);
    }
}