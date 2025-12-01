package com.nhnacademy.coupon_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.coupon_server.dto.coupon.CouponRequestDto;
import com.nhnacademy.coupon_server.dto.coupon.CouponResponseDto;
import com.nhnacademy.coupon_server.exception.CouponNotFoundException;
import com.nhnacademy.coupon_server.service.CouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CouponAdminController.class,
        properties = {"spring.cloud.config.enabled=false"}
)
@DisplayName("CouponAdminController 컨트롤러 테스트")
public class CouponAdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponService couponService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("쿠폰 템플릿 생성 성공")
    void createCouponSuccess() throws Exception {
        CouponRequestDto couponRequestDto = CouponRequestDto.builder()
                .id(1L)
                .couponName("신규 쿠폰")
                .issueCount(100)
                .issueStartAt(LocalDateTime.now().plusDays(1))
                .issueEndAt(LocalDateTime.now().plusDays(2))
                .validPeriodDate(30)
                .build();

        CouponResponseDto responseDto = CouponResponseDto.builder()
                .id(100L)
                .couponPolicyId(1L)
                .couponName("신규 쿠폰")
                .issueCount(100)
                .build();

        when(couponService.create(any(CouponRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/admin/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(couponRequestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.couponName").value("신규 쿠폰"));
    }

    @Test
    @DisplayName("모든 쿠폰 템플릿 조회 성공")
    void findAllCouponSuccess() throws Exception {
        CouponResponseDto coupon1 = CouponResponseDto.builder()
                .id(101L)
                .couponName("여름 세일 쿠폰")
                .build();

        CouponResponseDto coupon2 = CouponResponseDto.builder()
                .id(102L)
                .couponName("겨울 세일 쿠폰")
                .build();

        when(couponService.findAll()).thenReturn(List.of(coupon1, coupon2));

        mockMvc.perform(get("/api/admin/coupons")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].couponName").value("여름 세일 쿠폰"))
                .andExpect(jsonPath("$[1].couponName").value("겨울 세일 쿠폰"));
    }

    @Test
    @DisplayName("쿠폰 템플릿 수정 성공")
    void updateCouponSuccess() throws Exception {
        Long couponId = 100L;
        CouponRequestDto requestDto = CouponRequestDto.builder()
                .id(1L)
                .couponName("수정된 쿠폰 이름")
                .issueCount(500)
                .issueStartAt(LocalDateTime.now())
                .issueEndAt(LocalDateTime.now().plusDays(7))
                .build();

        CouponResponseDto responseDto = CouponResponseDto.builder()
                .id(couponId)
                .couponPolicyId(1L)
                .couponName("수정된 쿠폰 이름")
                .issueCount(500)
                .issueStartAt(requestDto.getIssueStartAt())
                .issueEndAt(requestDto.getIssueEndAt())
                .build();

        when(couponService.update(eq(couponId), any(CouponRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/admin/coupons/{couponId}", couponId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couponName").value("수정된 쿠폰 이름"));
    }

    @Test
    @DisplayName("쿠폰 템플릿 수정 실패 - 존재하지 않는 쿠폰 ID")
    void updateCouponNotFound() throws Exception {
        Long couponId = 999L;
        CouponRequestDto requestDto = CouponRequestDto.builder()
                .id(1L)
                .couponName("수정 시도")
                .issueStartAt(LocalDateTime.now())
                .issueEndAt(LocalDateTime.now().plusDays(7))
                .validPeriodDate(30)
                .issueCount(100)
                .build();

        doThrow(new CouponNotFoundException("쿠폰을 찾을 수 없습니다."))
                .when(couponService).update(eq(couponId), any(CouponRequestDto.class));

        mockMvc.perform(put("/api/admin/coupons/{couponId}", couponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().string("쿠폰을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("쿠폰 템플릿 삭제 성공")
    void deleteCouponSuccess() throws Exception {
        Long couponId = 100L;
        doNothing().when(couponService).delete(couponId);

        mockMvc.perform(delete("/api/admin/coupons/{couponId}", couponId))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("쿠폰 템플릿 삭제 실패 - 존재하지 않는 쿠폰 ID")
    void deleteCouponNotFound() throws Exception {
        Long couponId = 999L;

        doThrow(new CouponNotFoundException("쿠폰을 찾을 수 없습니다."))
                .when(couponService).delete(couponId);

        mockMvc.perform(delete("/api/admin/coupons/{couponId}", couponId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

}
