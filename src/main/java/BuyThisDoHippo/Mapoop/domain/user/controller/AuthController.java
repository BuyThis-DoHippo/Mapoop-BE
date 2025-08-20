package BuyThisDoHippo.Mapoop.domain.user.controller;

import BuyThisDoHippo.Mapoop.domain.user.dto.GoogleLoginRequest;
import BuyThisDoHippo.Mapoop.domain.user.dto.LoginRequest;
import BuyThisDoHippo.Mapoop.domain.user.dto.LoginResponse;
import BuyThisDoHippo.Mapoop.domain.user.dto.RefreshTokenRequest;
import BuyThisDoHippo.Mapoop.domain.user.service.AuthService;
import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/kakao/login")
    public ResponseEntity<CommonResponse<?>> kakaoLogin(@RequestBody LoginRequest request) {
        try {
            var loginResponse = authService.kakaoLogin(request);
            return ResponseEntity.ok(
                    CommonResponse.onSuccess(loginResponse, "로그인 성공")
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    CommonResponse.onFailure(null, CustomErrorCode.INVALID_REQUEST_DTO)
            );
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<CommonResponse<?>> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            var tokenResponse = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(
                    CommonResponse.onSuccess(tokenResponse, "토큰 갱신 성공")
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    CommonResponse.onFailure(null, CustomErrorCode.INVALID_REQUEST_DTO)
            );
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<?>> logout() {
        // JWT는 stateless이므로 서버에서 특별한 처리 없음
        // 클라이언트에서 토큰 삭제하면 됨
        return ResponseEntity.ok(
                CommonResponse.onSuccess(null, "로그아웃되었습니다.")
        );
    }

    @PostMapping("/google/login")
    public ResponseEntity<CommonResponse<?>> googleLogin(@RequestBody GoogleLoginRequest request) {
        try {
            LoginResponse loginResponse = authService.googleLogin(request);
            return ResponseEntity.ok(CommonResponse.onSuccess(loginResponse, "구글 로그인 성공"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(CommonResponse.onFailure(null, CustomErrorCode.INVALID_REQUEST_DTO));
        }
    }
}