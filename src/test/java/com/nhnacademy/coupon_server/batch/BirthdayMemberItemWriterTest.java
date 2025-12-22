package com.nhnacademy.coupon_server.batch;

import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BirthdayMemberItemWriterTest {

    @Mock
    private MemberCouponService memberCouponService;

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private BirthdayMemberItemWriter writer;

    @Test
    @DisplayName("Writer 실행 시 쿠폰 정책을 한 번만 조회하고(캐싱), 청크 내 모든 회원에게 쿠폰을 발급해야 한다")
    void writeSuccessWithCaching() {
        Chunk<Long> chunk = new Chunk<>(List.of(100L, 101L, 102L));
        Coupon birthdayCoupon = Coupon.builder().id(999L).build();

        when(couponRepository.findCouponsByCommentAndStatus(any(), any(), any()))
                .thenReturn(List.of(birthdayCoupon));

        writer.write(chunk);

        verify(couponRepository, times(1)).findCouponsByCommentAndStatus(any(), any(), any());

        verify(memberCouponService, times(1)).issueBirthdayCoupon(100L, 999L);
        verify(memberCouponService, times(1)).issueBirthdayCoupon(101L, 999L);
        verify(memberCouponService, times(1)).issueBirthdayCoupon(102L, 999L);
    }
}