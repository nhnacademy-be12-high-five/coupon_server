package com.nhnacademy.coupon_server.config;

import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import com.nhnacademy.coupon_server.service.client.MemberServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BirthdaySchedulerTest {
    @Mock
    private MemberCouponService memberCouponService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private MemberServiceClient memberServiceClient;

    @InjectMocks
    private BirthdayScheduler birthdayScheduler;

    @Test
    @DisplayName("정상 동작: 생일 쿠폰 정책으로 인한 생일자가 있으면 발급 성공")
    void autoIssueBirthdayCouponSuccess(){
        Long couponId = 1L;
        Coupon coupon = Coupon.builder()
                .id(couponId)
                .couponName("생일 쿠폰")
                .build();

        when(couponRepository.findByCouponPolicyComment(Comment.BIRTHDAY)).thenReturn(List.of(coupon));

        List<Long> birthdayUserIds = List.of(100L, 200L);
        when(memberServiceClient.getBirthdayUserId(anyInt())).thenReturn(birthdayUserIds);
        birthdayScheduler.autoIssueBirthdayCoupons();

        verify(memberServiceClient, times(1)).getBirthdayUserId(LocalDateTime.now().getMonthValue());
        verify(memberCouponService).issueBirthdayCoupon(100L, couponId);
        verify(memberCouponService).issueBirthdayCoupon(200L, couponId);
    }

    @Test
    @DisplayName("실패: 생일 쿠폰 정책 존재하지 않음")
    void autoIssueBirthdayCouponFailureNoCouponPolicy(){
        when(couponRepository.findByCouponPolicyComment(Comment.BIRTHDAY)).thenReturn(Collections.emptyList());
        birthdayScheduler.autoIssueBirthdayCoupons();
        verify(memberServiceClient, never()).getBirthdayUserId(anyInt());
        verify(memberCouponService, never()).issueBirthdayCoupon(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: 생일자가 존재하지 않음")
    void autoIssueBirthdayCouponFailureNoBirthdayUsers(){
        Coupon coupon = Coupon.builder()
                .id(1L)
                .build();
        when(couponRepository.findByCouponPolicyComment(Comment.BIRTHDAY)).thenReturn(List.of(coupon));
        when(memberServiceClient.getBirthdayUserId(anyInt())).thenReturn(Collections.emptyList());

        birthdayScheduler.autoIssueBirthdayCoupons();
        verify(memberCouponService, never()).issueCouponByUser(anyLong(), anyLong());
    }

    @Test
    @DisplayName("예외 처리: Member Server 통신 실패")
    void autoIssueBirthdayCouponsMemberServerFail() {
        Coupon coupon = Coupon.builder().id(1L).build();

        when(couponRepository.findByCouponPolicyComment(Comment.BIRTHDAY)).thenReturn(List.of(coupon));

        when(memberServiceClient.getBirthdayUserId(anyInt())).thenThrow(new RuntimeException("Connection Refused"));

        birthdayScheduler.autoIssueBirthdayCoupons();

        verify(memberCouponService, never()).issueBirthdayCoupon(anyLong(), anyLong());
    }

    @Test
    @DisplayName("예외 처리: 특정 회원 발급 실패 시에도 다른 회원은 계속 진행")
    void autoIssueBirthdayCouponsPartialFailure() {
        Long couponId = 1L;
        Coupon coupon = Coupon.builder().id(couponId).build();

        when(couponRepository.findByCouponPolicyComment(Comment.BIRTHDAY)).thenReturn(List.of(coupon));

        List<Long> birthdayUserIds = List.of(100L, 200L, 300L);
        when(memberServiceClient.getBirthdayUserId(anyInt())).thenReturn(birthdayUserIds);

        doNothing().when(memberCouponService).issueBirthdayCoupon(100L, couponId);
        doThrow(new RuntimeException("Already Issued")).when(memberCouponService).issueBirthdayCoupon(200L, couponId);
        doNothing().when(memberCouponService).issueBirthdayCoupon(300L, couponId);

        birthdayScheduler.autoIssueBirthdayCoupons();

        verify(memberCouponService).issueBirthdayCoupon(100L, couponId);
        verify(memberCouponService).issueBirthdayCoupon(200L, couponId);
        verify(memberCouponService).issueBirthdayCoupon(300L, couponId);
    }

}
