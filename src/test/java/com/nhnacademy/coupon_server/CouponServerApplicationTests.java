package com.nhnacademy.coupon_server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.batch.job.enabled=false"
})
@ActiveProfiles("test")
class CouponServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
