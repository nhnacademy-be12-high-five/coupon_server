package com.nhnacademy.coupon_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.coupon_server.dto.CouponPolicyRequestDto;
import com.nhnacademy.coupon_server.dto.CouponPolicyResponseDto;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.service.CouponPolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CouponPolicyAdminController.class,
        properties = {"spring.cloud.config.enabled=false"}
)
@DisplayName("CouponPolicyAdminController 컨트롤러 테스트")
class CouponPolicyAdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponPolicyService couponPolicyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("쿠폰 정책 생성 성공")
    void createCouponPolicySuccess() throws Exception {
        CouponPolicyRequestDto requestDto = CouponPolicyRequestDto.builder()
                .name("신규 가입 쿠폰")
                .comment(Comment.WELCOME)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .minOrderValue(10000L)
                .maxDiscountValue(1000L)
                .build();

        CouponPolicyResponseDto responseDto = CouponPolicyResponseDto.builder()
                .id(1L)
                .name("신규 가입 쿠폰")
                .comment(Comment.WELCOME)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .minOrderValue(10000L)
                .maxDiscountValue(1000L)
                .build();

        when(couponPolicyService.create(any(CouponPolicyRequestDto.class))).thenReturn(responseDto)
                .thenReturn(responseDto);

        mockMvc.perform(post("/admin/coupon-policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("신규 가입 쿠폰"))
                .andExpect(jsonPath("$.discountType").value("FIXED"))
                .andExpect(jsonPath("$.discountValue").value(1000L));
    }

    @Test
    @DisplayName("모든 쿠폰 정책 조회 성공")
    void getAllPolicies_success() throws Exception {
        CouponPolicyResponseDto policy1 = CouponPolicyResponseDto.builder()
                .id(1L)
                .name("정책1")
                .comment(Comment.EVENT)
                .build();

        CouponPolicyResponseDto policy2 = CouponPolicyResponseDto.builder()
                .id(2L)
                .name("정책2")
                .comment(Comment.BIRTHDAY)
                .build();

        when(couponPolicyService.findAll())
                .thenReturn(List.of(policy1, policy2));

        mockMvc.perform(get("/admin/coupon-policy")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("정책1"));
    }
}
