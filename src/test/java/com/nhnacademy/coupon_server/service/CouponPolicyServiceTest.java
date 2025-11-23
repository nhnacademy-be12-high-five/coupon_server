package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.CouponPolicyRequestDto;
import com.nhnacademy.coupon_server.dto.CouponPolicyResponseDto;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.exception.CouponPolicyNotFoundException;
import com.nhnacademy.coupon_server.repository.CouponPolicyBookRepository;
import com.nhnacademy.coupon_server.repository.CouponPolicyCategoryRepository;
import com.nhnacademy.coupon_server.repository.CouponPolicyRepository;
import com.nhnacademy.coupon_server.service.impl.CouponPolicyServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CouponPolicyServiceTest {
    @Mock
    private CouponPolicyRepository couponPolicyRepository;
    @Mock
    private CouponPolicyBookRepository couponPolicyBookRepository;
    @Mock
    private CouponPolicyCategoryRepository couponPolicyCategoryRepository;

    @InjectMocks
    private CouponPolicyServiceImpl couponPolicyService;

    private CouponPolicyRequestDto couponPolicyRequestDto;
    private CouponPolicy mockPolicy;

    @BeforeEach
    void setUp() {
        couponPolicyRequestDto = new CouponPolicyRequestDto();
        couponPolicyRequestDto.setName("신규 정책 테스트");
        couponPolicyRequestDto.setComment(Comment.WELCOME);
        couponPolicyRequestDto.setDiscountType(DiscountType.PERCENTAGE);
        couponPolicyRequestDto.setDiscountValue(10L);
        couponPolicyRequestDto.setMinOrderValue(10000L);
        couponPolicyRequestDto.setMaxDiscountValue(5000L);
        couponPolicyRequestDto.setTargetBookIds(List.of(1L, 2L));
        couponPolicyRequestDto.setTargetCategoryIds(List.of(10L));

        mockPolicy = CouponPolicy.builder()
                .id(1L)
                .name(couponPolicyRequestDto.getName())
                .comment(couponPolicyRequestDto.getComment())
                .discountType(couponPolicyRequestDto.getDiscountType())
                .discountValue(couponPolicyRequestDto.getDiscountValue())
                .minOrderValue(couponPolicyRequestDto.getMinOrderValue())
                .maxDiscountValue(couponPolicyRequestDto.getDiscountValue())
                .build();
    }

    @Test
    @DisplayName("새로운 쿠폰 정책 생성 성공 - 특정 도서 및 카테고리 지정 포함")
    void testCreatePolicyWithTarget() {
        when(couponPolicyRepository.save(any(CouponPolicy.class))).thenReturn(mockPolicy);

        CouponPolicyResponseDto responseDto = couponPolicyService.create(couponPolicyRequestDto);

        verify(couponPolicyRepository, times(1)).save(any(CouponPolicy.class));

        verify(couponPolicyBookRepository, times(1)).saveAll(any());
        verify(couponPolicyCategoryRepository, times(1)).saveAll(any());

        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(1L, responseDto.getId());
        Assertions.assertEquals("신규 정책 테스트", responseDto.getName());
        Assertions.assertEquals(Comment.WELCOME, responseDto.getComment());
    }

    @Test
    @DisplayName("새로운 쿠폰 정책 생성 성공 - 적용 대상 없이 정책만 생성")
    void testCreatePolicyWithoutTarget() {
        couponPolicyRequestDto.setTargetBookIds(null);
        couponPolicyRequestDto.setTargetCategoryIds(List.of());

        when(couponPolicyRepository.save(any(CouponPolicy.class))).thenReturn(mockPolicy);

        CouponPolicyResponseDto responseDto = couponPolicyService.create(couponPolicyRequestDto);
        verify(couponPolicyRepository, times(1)).save(any(CouponPolicy.class));

        verify(couponPolicyBookRepository, never()).saveAll(any());
        verify(couponPolicyCategoryRepository, never()).saveAll(any());

        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(1L, responseDto.getId());
    }

    @Test
    @DisplayName("쿠폰 정책 전체 조회 성공")
    void testFindAllSuccess(){
        CouponPolicy policy1 = CouponPolicy.builder()
                .id(1L)
                .name("정책1")
                .comment(Comment.WELCOME)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .build();
        CouponPolicy policy2 = CouponPolicy.builder()
                .id(2L)
                .name("정책2")
                .comment(Comment.EVENT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10L)
                .build();

        when(couponPolicyRepository.findAll()).thenReturn(List.of(policy1, policy2));
        List<CouponPolicyResponseDto> responseDtoList = couponPolicyService.findAll();
        Assertions.assertNotNull(responseDtoList);
        Assertions.assertEquals(2, responseDtoList.size());

        Assertions.assertEquals("정책1", responseDtoList.get(0).getName());
        Assertions.assertEquals("정책2", responseDtoList.get(1).getName());

        verify(couponPolicyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("쿠폰 정책 단건 조회 성공")
    void testFindByIdSuccess(){
        Long id = 1L;
        when(couponPolicyRepository.findById(id)).thenReturn(Optional.of(mockPolicy));

        CouponPolicyResponseDto responseDto = couponPolicyService.findById(id);

        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(id, responseDto.getId());
        Assertions.assertEquals(mockPolicy.getName(), responseDto.getName());

        verify(couponPolicyRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("쿠폰 정책 단건 조회 실패")
    void testFindByIdFailure(){
        Long id = 999L;
        when(couponPolicyRepository.findById(id)).thenReturn(Optional.empty());

        Assertions.assertThrows(CouponPolicyNotFoundException.class, () -> couponPolicyService.findById(id));

        verify(couponPolicyRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("쿠폰 정책 삭제 성공")
    void testDeleteSuccess(){
        Long id = 1L;
        when(couponPolicyRepository.existsById(id)).thenReturn(true);
        doNothing().when(couponPolicyRepository).deleteById(id);

        couponPolicyService.deleteById(id);

        verify(couponPolicyRepository, times(1)).existsById(id);
        verify(couponPolicyRepository, times(1)).deleteById(id);
    }

    @Test
    @DisplayName("쿠폰 정책 삭제 실패 - 존재하지 않는 ID")
    void testDeleteFailureNotFound(){
        Long id = 999L;
        when(couponPolicyRepository.existsById(id)).thenReturn(false);

        Assertions.assertThrows(CouponPolicyNotFoundException.class, () -> couponPolicyService.deleteById(id));
        verify(couponPolicyRepository, never()).deleteById(id);
    }
}
