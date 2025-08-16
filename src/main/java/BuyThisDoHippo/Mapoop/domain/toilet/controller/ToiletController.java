package BuyThisDoHippo.Mapoop.domain.toilet.controller;

import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletRegisterRequest;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletRegisterResponse;
import BuyThisDoHippo.Mapoop.domain.toilet.service.ToiletService;
import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import BuyThisDoHippo.Mapoop.global.error.ApplicationException;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/toilet")
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
}
