package BuyThisDoHippo.Mapoop.domain.user.service;

import BuyThisDoHippo.Mapoop.domain.user.dto.*;
import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import BuyThisDoHippo.Mapoop.domain.user.repository.UserRepository;
import BuyThisDoHippo.Mapoop.global.auth.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import BuyThisDoHippo.Mapoop.domain.user.dto.LoginResponse;
import BuyThisDoHippo.Mapoop.domain.user.dto.TokenResponse;
import BuyThisDoHippo.Mapoop.domain.user.dto.UserResponse;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final WebClient webClient;

    // 카카오 로그인/회원가입
    public LoginResponse kakaoLogin(LoginRequest request) {
        // 1. 카카오 API로 사용자 정보 조회
        KakaoUserInfo kakaoUserInfo = getKakaoUserInfo(request.getKakaoAccessToken());

        // 2. 기존 사용자 조회 또는 새 사용자 생성
        User user = findOrCreateUser(kakaoUserInfo, request);

        // 3. JWT 토큰 생성
        String accessToken = jwtUtils.generateAccessToken(user.getId());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserResponse.from(user))
                .build();
    }

    // 카카오 API 호출
    private KakaoUserInfo getKakaoUserInfo(String kakaoAccessToken) {
        try {
            log.info("카카오 API 호출 시작: {}", kakaoAccessToken);

            KakaoUserInfo result = webClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header("Authorization", "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .bodyToMono(KakaoUserInfo.class)
                    .block();

            log.info("카카오 API 응답: {}", result);
            return result;

        } catch (Exception e) {
            log.error("카카오 API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("카카오 사용자 정보 조회 실패: " + e.getMessage());
        }
    }

    // 사용자 조회 또는 생성
    private User findOrCreateUser(KakaoUserInfo kakaoUserInfo, LoginRequest request) {
        Optional<User> existingUser = userRepository.findByKakaoId(kakaoUserInfo.getKakaoId());

        if (existingUser.isPresent()) {
            // 기존 사용자 - 위치정보 동의 정보 업데이트
            User user = existingUser.get();
            if (request.getLocationConsent() != null) {
                user.updateLocationConsent(request.getLocationConsent(), request.getLocationConsentVersion());
            }
            return user;
        } else {
            // 신규 사용자 생성
            User newUser = User.builder()
                    .name(kakaoUserInfo.getNickname())
                    .email(kakaoUserInfo.getEmail())
                    .kakaoId(kakaoUserInfo.getKakaoId())
                    .isLocationConsent(request.getLocationConsent() != null ? request.getLocationConsent() : false)
                    .locationConsentDate(request.getLocationConsent() != null ? LocalDateTime.now() : null)
                    .build();

            return userRepository.save(newUser);
        }
    }

    // 토큰 갱신
    public TokenResponse refreshToken(String refreshToken) {
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        Long userId = jwtUtils.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String newAccessToken = jwtUtils.generateAccessToken(userId);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }
}