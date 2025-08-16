package BuyThisDoHippo.Mapoop.domain.chat_log.repository;

import BuyThisDoHippo.Mapoop.domain.chat_log.entity.ChatLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {

    Page<ChatLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<ChatLog> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);

    boolean existsByIdAndUserId(Long chatId, Long userId);

    boolean existsByIdAndSessionId(Long chatId, String sessionId);
}
