package com.nhnacademy.coupon_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.coupon_server.dto.coupon.CouponResponseDto;
import com.nhnacademy.coupon_server.dto.memberCoupon.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.dto.memberCoupon.UserCouponIssueRequestDto;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.exception.DuplicateCouponException;
import com.nhnacademy.coupon_server.service.CouponService;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    @MockitoBean
    private CouponService couponService;

    @Test
    @DisplayName("사용자 쿠폰 발급 성공 (201)")
    void issueCouponSuccess() throws Exception {
        Long userId = 1L;
        Long couponId = 100L;
        UserCouponIssueRequestDto requestDto = new UserCouponIssueRequestDto(couponId);

        doNothing().when(memberCouponService).issueCouponByUser(userId, couponId);

        mockMvc.perform(post("/coupons/issue")
                .header("X-USER-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("사용자 쿠폰 발급 실패 - 이미 발급된 쿠폰 (409)")
    void issueCouponFailureDuplicateCoupon() throws Exception {
        Long userId = 1L;
        Long couponId = 100L;
        UserCouponIssueRequestDto requestDto = new UserCouponIssueRequestDto(couponId);

        doThrow(new DuplicateCouponException("이미 해당 쿠폰을 발급받으셨습니다."))
                .when(memberCouponService).issueCouponByUser(userId, couponId);

        mockMvc.perform(post("/coupons/issue")
                .header("X-USER-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("사용자 쿠폰 발급 실패 - 발급 기간 아님/수량 소진 (400)")
    void issueCouponFailureBadRequest() throws Exception {
        Long userId = 1L;
        Long couponId = 100L;
        UserCouponIssueRequestDto requestDto = new UserCouponIssueRequestDto(couponId);

        doThrow(new IllegalArgumentException("아직 발급 가능한 기간이 아닙니다."))
                .when(memberCouponService).issueCouponByUser(userId, couponId);

        mockMvc.perform(post("/coupons/issue")
                .header("X-USER-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("사용자 쿠폰 발급 실패 - 유효하지 않은 요청(ID 누락)")
    void issueCouponFailureInvalidRequest() throws Exception {
        UserCouponIssueRequestDto requestDto = new UserCouponIssueRequestDto(null);

        mockMvc.perform(post("/coupons/issue")
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 성공")
    void getCouponByUserIdSuccess() throws Exception {
        Long userId = 1L;

        MemberCouponResponseDto responseDto = MemberCouponResponseDto.builder()
                .couponName("테스트 쿠폰")
                .build();

        Page<MemberCouponResponseDto> pageResponse = new PageImpl<>(List.of(responseDto));

        when(memberCouponService.findCouponByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/coupons/members/{memberId}", userId)
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].couponName").value("테스트 쿠폰"));
    }

    @Test
    @DisplayName("발급 가능한 쿠폰 템플릿 목록 조회 성공")
    void getIssuableCouponsSuccess() throws Exception {
        CouponResponseDto limitedCoupon = CouponResponseDto.builder()
                .id(1L)
                .couponName("선착순 쿠폰")
                .remainingCount(90) // 잔여 수량 설정
                .build();

        CouponResponseDto unlimitedCoupon = CouponResponseDto.builder()
                .id(2L)
                .couponName("무제한 쿠폰")
                .remainingCount(null)
                .build();

        Page<CouponResponseDto> mockPage = new PageImpl<>(List.of(limitedCoupon, unlimitedCoupon));

        when(couponService.findIssuableCoupons(any(Pageable.class)))
                .thenReturn(mockPage);

        mockMvc.perform(get("/coupons/templates")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].couponName").value("선착순 쿠폰"))
                .andExpect(jsonPath("$.content[0].remainingCount").value(90))
                .andExpect(jsonPath("$.content[1].couponName").value("무제한 쿠폰"))
                .andExpect(jsonPath("$.content[1].remainingCount").doesNotExist())
                .andDo(print());
    }
}
