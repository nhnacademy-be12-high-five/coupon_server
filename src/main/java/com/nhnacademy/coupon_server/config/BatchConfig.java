package com.nhnacademy.coupon_server.config;

import com.nhnacademy.coupon_server.batch.BirthdayMemberItemReader;
import com.nhnacademy.coupon_server.batch.BirthdayMemberItemWriter;
import com.nhnacademy.coupon_server.service.client.MemberServiceClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.SocketTimeoutException;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {
    private final MemberServiceClient memberServiceClient;
    private final BirthdayMemberItemWriter birthdayMemberItemWriter;
    private static final int CHUNK_SIZE = 1000;
    @Value("${batch.retry.initial-interval:1000}") private long retryInitialInterval;
    @Value("${batch.retry.multiplier:2.0}") private double retryMultiplier;
    @Value("${batch.retry.max-interval:10000}") private long retryMaxInterval;
    @Value("${batch.retry.max-attempts:3}") private int retryMaxAttempts;

    @Bean
    @StepScope
    public BirthdayMemberItemReader birthdayMemberItemReader() {
        return new BirthdayMemberItemReader(memberServiceClient, CHUNK_SIZE);
    }

    @Bean
    public Job birthdayCouponJob(JobRepository jobRepository, Step birthdayCouponStep) {
        return new JobBuilder("birthdayCouponJob", jobRepository)
                .start(birthdayCouponStep)
                .build();
    }

    @Bean
    public Step birthdayCouponStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   BirthdayMemberItemReader birthdayMemberItemReader) {

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retryInitialInterval); // 최초 1초 대기
        backOffPolicy.setMultiplier(retryMultiplier);        // 2배씩 증가
        backOffPolicy.setMaxInterval(retryMaxInterval);    // 최대 10초까지 대기

        return new StepBuilder("birthdayCouponStep", jobRepository)
                .<Long,Long>chunk(CHUNK_SIZE, transactionManager)
                .reader(birthdayMemberItemReader)
                .writer(birthdayMemberItemWriter)
                .faultTolerant()
                .retryLimit(retryMaxAttempts)
                .retry(FeignException.class)
                .retry(SocketTimeoutException.class)
                .backOffPolicy(backOffPolicy)
                .build();
    }
}
