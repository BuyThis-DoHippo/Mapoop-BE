package BuyThisDoHippo.Mapoop.domain.user.controller;

import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletSimpleInfo;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.service.ToiletService;
import BuyThisDoHippo.Mapoop.domain.user.dto.LocationConsentRequest;
import BuyThisDoHippo.Mapoop.domain.user.dto.MyToiletResponse;
import BuyThisDoHippo.Mapoop.domain.user.service.UserService;
import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ToiletService toiletService;

    @GetMapping("/me")
    public ResponseEntity<CommonResponse<?>> getMyInfo(@RequestHeader("Authorization") String token) {
        try {
            // "Bearer " 제거
            String jwtToken = token.substring(7);
            var userResponse = userService.getUserInfo(jwtToken);

            return ResponseEntity.ok(
                    CommonResponse.onSuccess(userResponse, "내 정보 조회 성공")
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    CommonResponse.onFailure(null, CustomErrorCode.INVALID_REQUEST_DTO)
            );
        }
    }

    @PatchMapping("/me/location-consent")
    public ResponseEntity<CommonResponse<?>> updateLocationConsent(
            @RequestHeader("Authorization") String token,
            @RequestBody LocationConsentRequest request) {
        try {
            String jwtToken = token.substring(7);
            userService.updateLocationConsent(jwtToken, request);

            return ResponseEntity.ok(
                    CommonResponse.onSuccess(null, "위치정보 동의가 업데이트되었습니다.")
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    CommonResponse.onFailure(null, CustomErrorCode.INVALID_REQUEST_DTO)
            );
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<CommonResponse<?>> deleteAccount(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7);
            userService.deleteAccount(jwtToken);

            return ResponseEntity.ok(
                    CommonResponse.onSuccess(null, "회원 탈퇴가 완료되었습니다.")
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    CommonResponse.onFailure(null, CustomErrorCode.INVALID_REQUEST_DTO)
            );
        }
    }

    @GetMapping("/me/toilets")
    public CommonResponse<MyToiletResponse> getMyToilet(Authentication authentication) {
        // 로그인 유저 정보 가져오기
        Long userId = Long.valueOf(authentication.getName());
        List<ToiletSimpleInfo> toilets = toiletService.getToiletsByUserId(userId);
        MyToiletResponse response = MyToiletResponse.builder()
                .totalCount(toilets.size())
                .toilets(toilets)
                .build();
        return CommonResponse.onSuccess(response, "내가 등록한 화장실 목록 조회 성공");
    }
}