package BuyThisDoHippo.Mapoop.domain.user.service;

import BuyThisDoHippo.Mapoop.domain.user.dto.LocationConsentRequest;
import BuyThisDoHippo.Mapoop.domain.user.dto.UserResponse;
import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import BuyThisDoHippo.Mapoop.domain.user.repository.UserRepository;
import BuyThisDoHippo.Mapoop.global.auth.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    // 내 정보 조회
    @Transactional(readOnly = true)
    public UserResponse getUserInfo(String token) {
        Long userId = jwtUtils.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return UserResponse.from(user);
    }

    // 위치정보 동의 업데이트
    public void updateLocationConsent(String token, LocationConsentRequest request) {
        Long userId = jwtUtils.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.updateLocationConsent(request.getLocationConsent(), request.getLocationConsentVersion());
        userRepository.save(user);
    }

    // 회원 탈퇴 (하드 삭제)
    public void deleteAccount(String token) {
        Long userId = jwtUtils.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 하드 삭제 - 데이터베이스에서 완전히 삭제
        userRepository.delete(user);
    }

}