package com.nhnacademy.coupon_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.coupon_server.dto.memberCoupon.UserCouponIssueRequestDto;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MemberCouponController.class,
        properties = {"spring.cloud.config.enabled=false"}
)
class MemberCouponControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberCouponService memberCouponService;

    @Test
    @DisplayName("사용자 쿠폰 발급 성공")
    void issueCouponSuccess() throws Exception {
        Long userId = 1L;
        Long couponId = 100L;
        UserCouponIssueRequestDto requestDto = new UserCouponIssueRequestDto(couponId);

        doNothing().when(memberCouponService).issueCouponByUser(userId, couponId);

        mockMvc.perform(post("/coupons/issue")
                .header("X-USER-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("사용자 쿠폰 발급 실패 - 유효하지 않은 요청(ID 누락)")
    void issueCouponFailureInvalidRequest() throws Exception {
        UserCouponIssueRequestDto requestDto = new UserCouponIssueRequestDto(null);

        mockMvc.perform(post("/coupons/issue")
                .header("X-USER-ID", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }
}
