package com.nhnacademy.coupon_server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false"})
@ActiveProfiles("local")
class CouponServerApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("main() 메서드 실행 커버리지 테스트")
    void main() {
        assertDoesNotThrow(() -> {
            CouponServerApplication.main(new String[]{
                    "--spring.cloud.config.enabled=false",
                    "--spring.profiles.active=local"
            });
        });
    }
}
