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

    public LoginResponse googleLogin(GoogleLoginRequest request) {
        GoogleUserInfo googleUserInfo = getGoogleUserInfo(request.getGoogleAccessToken());

        User user = findOrCreateUser(googleUserInfo, request);

        String accessToken = jwtUtils.generateAccessToken(user.getId());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserResponse.from(user))
                .build();
    }

    private GoogleUserInfo getGoogleUserInfo(String googleAccessToken) {
        try {
            log.info("구글 UserInfo API 호출 시작");

            GoogleUserInfo result = webClient.get()
                    .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                    .header("Authorization", "Bearer " + googleAccessToken)
                    .retrieve()
                    .bodyToMono(GoogleUserInfo.class)
                    .block();

            log.info("구글 UserInfo 응답: {}", result);
            if (result == null || result.getSub() == null) {
                throw new IllegalStateException("구글 사용자 정보 조회 실패: sub(googleId) 없음");
            }

            return result;
        } catch (Exception e) {
            log.error("구글 API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("구글 사용자 정보 조회 실패: " + e.getMessage());
        }
    }

    private User findOrCreateUser(GoogleUserInfo googleUserInfo, GoogleLoginRequest request) {

        Optional<User> existing = userRepository.findByGoogleId(googleUserInfo.getSub());
        if (existing.isPresent()) {
            User user = existing.get();
            if (request.getLocationConsent() != null) {
                user.updateLocationConsent(request.getLocationConsent(), request.getLocationConsentVersion());
            }
            return user;
        }

        // 카카오로 가입된 동일 유저가 있다면 googleId만 추가로 매핑
        if (googleUserInfo.getEmail() != null && !googleUserInfo.getEmail().isBlank()) {
            Optional<User> byEmail = userRepository.findByEmail(googleUserInfo.getEmail());
            if (byEmail.isPresent()) {
                User user = byEmail.get();
                user.linkGoogle(googleUserInfo.getSub());
                if (request.getLocationConsent() != null) {
                    user.updateLocationConsent(request.getLocationConsent(), request.getLocationConsentVersion());
                }
                return user;
            }
        }

         // 없다면 신규 생성
        User newUser = User.builder()
                .name(googleUserInfo.getName())
                .email(googleUserInfo.getEmail())
                .googleId(googleUserInfo.getSub())
                .isLocationConsent(Boolean.TRUE.equals(request.getLocationConsent()))
                .locationConsentDate(Boolean.TRUE.equals(request.getLocationConsent()) ? LocalDateTime.now() : null)
                .build();

        return userRepository.save(newUser);

    }
}