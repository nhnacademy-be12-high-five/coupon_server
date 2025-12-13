package com.nhnacademy.coupon_server.config;

import com.nhnacademy.coupon_server.batch.BirthdayMemberItemReader;
import com.nhnacademy.coupon_server.batch.BirthdayMemberItemWriter;
import com.nhnacademy.coupon_server.service.client.MemberServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {
    private final MemberServiceClient memberServiceClient;
    private final BirthdayMemberItemWriter birthdayMemberItemWriter;
    private static final int CHUNK_SIZE = 1000;

    @Bean
    public Job birthdayCouponJob(JobRepository jobRepository, Step birthdayCouponStep) {
        return new JobBuilder("birthdayCouponJob", jobRepository)
                .start(birthdayCouponStep)
                .build();
    }

    @Bean
    public Step birthdayCouponStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("birthdayCouponStep", jobRepository)
                .<Long,Long>chunk(CHUNK_SIZE, transactionManager)
                .reader(new BirthdayMemberItemReader(memberServiceClient, CHUNK_SIZE))
                .writer(birthdayMemberItemWriter)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }
}
