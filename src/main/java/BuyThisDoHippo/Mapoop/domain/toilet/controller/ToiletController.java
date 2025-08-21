package BuyThisDoHippo.Mapoop.domain.toilet.controller;

import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletDetailResponse;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletRegisterRequest;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletRegisterResponse;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletUpdateRequest;
import BuyThisDoHippo.Mapoop.domain.toilet.service.ToiletService;
import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import BuyThisDoHippo.Mapoop.global.error.ApplicationException;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/toilets")
public class ToiletController {
    private final ToiletService toiletService;

    @PostMapping("")
    public CommonResponse<ToiletRegisterResponse> registerToilet(@Valid @RequestBody ToiletRegisterRequest request) {

        // 사용자 정보 가져오기
        Long userId;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApplicationException(CustomErrorCode.AUTHENTICATION_REQUIRED);
        }
        // userId 가져오기
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
           userId = (Long) principal;
        } else {
            throw new ApplicationException(CustomErrorCode.INVALID_TOKEN);
        }

        ToiletRegisterResponse response = toiletService.createToilet(request, userId);
        return CommonResponse.onSuccess(response, "화장실 등록 성공");
    }

    @GetMapping("/{toiletId}")
    public CommonResponse<ToiletDetailResponse> getDetailToilet(@PathVariable Long toiletId) {
        ToiletDetailResponse response = toiletService.getToiletDetail(toiletId, LocalTime.now());
        return CommonResponse.onSuccess(response, "화장실 상세 조회 성공");
    }

    @PutMapping("/{toiletId}")
    public CommonResponse<Void> updateToilet(
            @PathVariable Long toiletId,
            @Valid @RequestBody ToiletUpdateRequest request,
            Authentication authentication
    ) {
        // 로그인 유저 정보 가져오기
        Long userId = Long.valueOf(authentication.getName());
        toiletService.updateToilet(toiletId, userId, request);
        return CommonResponse.onSuccess(null, "화장실 정보 수정 성공");
    }
}
