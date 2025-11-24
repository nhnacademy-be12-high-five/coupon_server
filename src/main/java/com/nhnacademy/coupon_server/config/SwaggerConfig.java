package com.nhnacademy.coupon_server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gateway API")
                        .description("백엔드 서비스(Account/Task)로 요청을 중계하는 API 게이트웨이입니다.")
                        .version("v1.0"));
    }
}