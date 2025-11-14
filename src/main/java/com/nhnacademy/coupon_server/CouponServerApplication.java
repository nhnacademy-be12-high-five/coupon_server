package com.nhnacademy.coupon_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CouponServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CouponServerApplication.class, args);
    }

}
