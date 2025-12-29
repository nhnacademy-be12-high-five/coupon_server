package com.nhnacademy.coupon_server.batch;

import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.couponPolicy.CouponPolicyRepository;
import com.nhnacademy.coupon_server.repository.memberCoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.service.client.MemberServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test") // H2 DB 등 테스트 환경 설정 프로파일 사용
class BirthdayCouponLoadTest {
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private CouponRepository couponRepository;
    
    @Autowired
    private CouponPolicyRepository couponPolicyRepository; // 정책 저장을 위해 필요하다고 가정

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @MockitoBean
    private MemberServiceClient memberServiceClient;

    @Autowired
    private Job birthdayCouponJob;

    private static final int TOTAL_USERS = 1_000_000; // 부하 테스트를 위한 유저 수 (조절 가능)
    private static final int CHUNK_SIZE = 1000;    // BatchConfig의 CHUNK_SIZE와 일치

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(birthdayCouponJob);
        memberCouponRepository.deleteAll();
        couponRepository.deleteAll();
        couponPolicyRepository.deleteAll();

        // 1. 생일 쿠폰 정책 및 쿠폰 데이터 생성 (Writer가 조회할 수 있도록)
        // 실제 엔티티 구조에 맞춰 빌더를 수정해 주세요.
        CouponPolicy policy = CouponPolicy.builder()
                .name("생일 축하 쿠폰")
                .status(CouponPolicyStatus.ACTIVE)
                .discountType(DiscountType.FIXED)
                .comment(Comment.BIRTHDAY) // 핵심 조건
                .discountValue(1000L)
                .maxDiscountValue(1000L)
                .minOrderValue(5000L)
                .build();
        couponPolicyRepository.save(policy);

        Coupon coupon = Coupon.builder()
                .couponPolicy(policy)
                .couponName("생일 축하 쿠폰")
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(1))
                .validPeriodDate(30)
                .build();
        couponRepository.save(coupon);

        // 2. MemberServiceClient Mocking (대량의 유저 데이터 시뮬레이션)
        mockMemberServiceResponse();


    }

    private void mockMemberServiceResponse() {
        int totalPages = TOTAL_USERS / CHUNK_SIZE;

        for (int page = 0; page < totalPages; page++) {
            List<Long> userIds = LongStream.range(page * CHUNK_SIZE, (page + 1) * CHUNK_SIZE)
                    .boxed()
                    .collect(Collectors.toList());

            // 페이지별로 userIds 리스트 반환
            given(memberServiceClient.getBirthdayUserId(anyInt(), eq(page), eq(CHUNK_SIZE)))
                    .willReturn(userIds);
        }

        // 마지막 페이지 이후 빈 리스트 반환 (Reader 종료 조건)
        given(memberServiceClient.getBirthdayUserId(anyInt(), eq(totalPages), eq(CHUNK_SIZE)))
                .willReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("대량의 생일자 쿠폰 발급 부하 테스트")
    void performanceTestBirthdayCouponIssuance() throws Exception {
        // given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("executedAt", String.valueOf(System.currentTimeMillis()))
                .addString("type", "birthday_load_test")
                .toJobParameters();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        stopWatch.stop();

        // then
        // 1. 배치가 성공적으로 완료되었는지 확인
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // 2. 모든 유저에게 쿠폰이 발급되었는지 DB count로 확인
        long issuedCount = memberCouponRepository.count();
        assertThat(issuedCount).isEqualTo(TOTAL_USERS);

        // 3. 성능 지표 출력
        System.out.println("=========================================");
        System.out.println("처리된 유저 수: " + TOTAL_USERS);
        System.out.println("총 소요 시간: " + stopWatch.getTotalTimeSeconds() + "초");
        System.out.println("초당 처리 건수(TPS): " + (TOTAL_USERS / stopWatch.getTotalTimeSeconds()));
        System.out.println("=========================================");
    }
}