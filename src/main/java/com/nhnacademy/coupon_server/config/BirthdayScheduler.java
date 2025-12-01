//package com.nhnacademy.coupon_server.config;
//
//import com.nhnacademy.coupon_server.entity.Coupon;
//import com.nhnacademy.coupon_server.entity.state.Comment;
//import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
//import com.nhnacademy.coupon_server.service.MemberCouponService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDate;
//import java.util.List;
//
//@Slf4j
//@Component
//@EnableScheduling
//@RequiredArgsConstructor
//public class BirthdayScheduler {
//    private final MemberCouponService memberCouponService;
//    private final CouponRepository couponRepository;
//
//    @Scheduled(cron = "0 0 0 1 * *")
//    @Transactional
//    public void autoIssueBirthdayCoupons() {
//        int currentMonth = LocalDate.now().getMonthValue();
//        log.info("{}월 생일 쿠폰 자동 발급 스케줄러 시작", currentMonth);
//
//        List<Coupon> birthdayCoupons = couponRepository.findByCouponPolicyComment(Comment.BIRTHDAY);
//        if (birthdayCoupons.isEmpty()) {
//            log.warn("발급한 생일 쿠폰 템플릿이 없습니다.");
//            return;
//        }
//        Coupon coupon = birthdayCoupons.get(0);
//
//        List<Long> birthdayUserIds = List.of(1L);
//
//        int successCount = 0;
//        for(Long userId : birthdayUserIds) {
//            try {
//                memberCouponService.issueBirthdayCoupon(userId, coupon.getId());
//                successCount++;
//            } catch(Exception e) {
//                log.error("User ID {} 생일 쿠폰 발급 실패 : {}", userId, e.getMessage());
//            }
//        }
//
//        log.info("{}월 생일 쿠폰 발급 완료. 대상: {}명, 성공: {}명", currentMonth,birthdayUserIds.size(), successCount);
//    }
//}
// 수정중