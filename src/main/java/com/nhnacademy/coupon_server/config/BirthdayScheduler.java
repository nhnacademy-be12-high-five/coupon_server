package com.nhnacademy.coupon_server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class BirthdayScheduler {

    private final JobLauncher jobLauncher;
    private final Job birthdayCouponJob;

    @Scheduled(cron = "0 0 0 1 * *")
    @SchedulerLock(name = "birthday_coupon_issue_lock", lockAtLeastFor = "PT30S", lockAtMostFor = "PT10M")
    public void autoIssueBirthdayCoupons() {
        log.info("생일 쿠폰 자동 발급 배치 시작");

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("executedAt", LocalDateTime.now().toString())
                    .toJobParameters();

            jobLauncher.run(birthdayCouponJob, jobParameters);

        } catch (Exception e) {
            log.error("생일 쿠폰 발급 배치 실행 중 오류 발생", e);
        }
    }
}