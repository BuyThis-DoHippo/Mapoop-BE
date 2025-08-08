package BuyThisDoHippo.Mapoop.domain.user.repository;

import BuyThisDoHippo.Mapoop.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 카카오 ID로 사용자 조회
    Optional<User> findByKakaoId(String kakaoId);

    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);

    // 카카오 ID 존재 여부 확인
    boolean existsByKakaoId(String kakaoId);

    // 이메일 존재 여부 확인
    boolean existsByEmail(String email);
}
