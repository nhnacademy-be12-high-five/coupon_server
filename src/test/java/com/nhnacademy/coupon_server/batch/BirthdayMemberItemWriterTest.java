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

    @Test
    @DisplayName("BeforeStep 실행 시 캐시 데이터(couponId)가 초기화되어, 재실행 시 다시 조회해야 한다")
    void beforeStep_ResetsCache() {
        Chunk<Long> chunk = new Chunk<>(List.of(1L));
        Coupon coupon = Coupon.builder().id(123L).build();

        when(couponRepository.findCouponsByCommentAndStatus(any(), any(), any()))
                .thenReturn(List.of(coupon));

        writer.write(chunk);
        verify(couponRepository, times(1)).findCouponsByCommentAndStatus(any(), any(), any());

        writer.beforeStep(mock(org.springframework.batch.core.StepExecution.class));

        writer.write(chunk);

        verify(couponRepository, times(2)).findCouponsByCommentAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("활성화된 생일 쿠폰 정책을 찾을 수 없으면 IllegalStateException 발생")
    void write_ThrowsException_WhenNoPolicyFound() {
        Chunk<Long> chunk = new Chunk<>(List.of(100L));

        when(couponRepository.findCouponsByCommentAndStatus(any(), any(), any()))
                .thenReturn(List.of());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                writer.write(chunk)
        );
    }
}