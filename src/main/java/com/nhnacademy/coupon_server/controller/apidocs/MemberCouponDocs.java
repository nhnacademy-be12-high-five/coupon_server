package com.nhnacademy.coupon_server.controller.apidocs;

import com.nhnacademy.coupon_server.dto.coupon.CouponResponseDto;
import com.nhnacademy.coupon_server.dto.memberCoupon.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.dto.memberCoupon.UserCouponIssueRequestDto;
import com.nhnacademy.coupon_server.exception.DuplicateCouponException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MemberCoupon", description = "사용자 전용 회원 쿠폰 관리 API")
public interface MemberCouponDocs {

    @Operation(summary = "쿠폰 발급 신청", description = "로그인한 사용자가 선착순 또는 일반 쿠폰 발급을 요청합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "쿠폰 발급 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (발급 기간 위반, 유효하지 않은 쿠폰 등)"),
            @ApiResponse(responseCode = "404", description = "대상 쿠폰을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 발급된 쿠폰이거나 재고가 소진됨")
    })
    @PostMapping("/issue")
    ResponseEntity<Void> issueCoupon(
            @Parameter(description = "사용자 ID (HTTP Header)", required = true, in = ParameterIn.HEADER, example = "1")
            @RequestHeader("X-USER-ID") Long userId,

            @Valid @RequestBody UserCouponIssueRequestDto requestDto
    );

    @Operation(summary = "사용자 쿠폰 목록 조회", description = "사용자가 보유한 쿠폰 목록을 조회합니다. (페이징 적용)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "쿠폰 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = MemberCouponResponseDto.class)))
    })
    @GetMapping("/members/{memberId}")
    ResponseEntity<Page<MemberCouponResponseDto>> getCouponsByUserId(
            @Parameter(name = "memberId", description = "조회할 사용자 ID", required = true, in = ParameterIn.PATH, example = "1")
            @PathVariable Long memberId,
            @Parameter(hidden = true) Pageable pageable
    );

    @Operation(summary = "발급 가능한 쿠폰 목록 조회", description = "현재 발급 기간에 해당하여 사용자가 다운로드할 수 있는 쿠폰 템플릿 목록을 조회합니다.")
    @GetMapping("/templates")
    ResponseEntity<Page<CouponResponseDto>> getIssuableCoupons(
            @Parameter(hidden = true) Pageable pageable
    );
}
