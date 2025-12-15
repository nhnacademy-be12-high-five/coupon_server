package com.nhnacademy.coupon_server.config;

import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.repository.memberCoupon.MemberCouponRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CouponDeleteBatchConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final MemberCouponRepository memberCouponRepository;
    private static final int CHUNK_SIZE = 1000;

    @Bean
    public Job deleteExpiredCouponJob(JobRepository jobRepository, Step deleteExpiredCouponStep) {
        return new JobBuilder("deleteExpiredCouponJob", jobRepository)
                .start(deleteExpiredCouponStep)
                .build();
    }

    @Bean
    public Step deleteExpiredCouponStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager) {
        return new StepBuilder("deleteExpiredCouponStep", jobRepository)
                .<MemberCoupon, MemberCoupon>chunk(CHUNK_SIZE, transactionManager)
                .reader(expiredOrUsedCouponReader())
                .writer(couponDeleteWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<MemberCoupon> expiredOrUsedCouponReader() {
        return new JpaPagingItemReaderBuilder<MemberCoupon>()
                .name("expiredOrUsedCouponReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("SELECT mc FROM MemberCoupon mc WHERE mc.status = :usedStatus OR (mc.status = :issuedStatus AND mc.expiredAt < :now)")
                .parameterValues(Map.of(
                        "usedStatus", Status.USED,
                        "issuedStatus", Status.ISSUED,
                        "now", LocalDateTime.now()
                ))
                .build();
    }

    @Bean
    public ItemWriter<MemberCoupon> couponDeleteWriter() {
        return items -> {
            log.info("삭제 대상 쿠폰 {}건 삭제 진행", items.size());
            memberCouponRepository.deleteAll(items);
        };
    }
}