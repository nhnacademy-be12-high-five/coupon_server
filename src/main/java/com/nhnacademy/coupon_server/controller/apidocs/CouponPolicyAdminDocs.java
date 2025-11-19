package com.nhnacademy.coupon_server.controller.apidocs;

import com.nhnacademy.coupon_server.dto.CouponPolicyRequestDto;
import com.nhnacademy.coupon_server.dto.CouponPolicyResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@Tag(name = "CouponPolicyAdmin", description = "쿠폰 서비스 CRUD API")
public interface CouponPolicyAdminDocs {

    @Operation(summary = "쿠폰 정책 생성", description = "새 쿠폰 정책을 생성합니다.")
    @ApiResponse(responseCode = "201", description = "쿠폰 정책 생성 성공")
    @PostMapping
    ResponseEntity<CouponPolicyResponseDto> createPolicy(
            @Valid @RequestBody(description = "쿠폰 정책 생성 요청 정보 (name)", required = true, content = @Content(schema = @Schema(implementation = CouponPolicyRequestDto.class)))
            @org.springframework.web.bind.annotation.RequestBody CouponPolicyRequestDto couponPolicyRequestDto
    );

    @Operation(summary = "쿠폰 정책 전체 조회", description = "생성된 쿠폰 정책리스트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "쿠폰 정책리스트 조회 성공")
    @GetMapping
    ResponseEntity<List<CouponPolicyResponseDto>> getAllPolicies();
}
