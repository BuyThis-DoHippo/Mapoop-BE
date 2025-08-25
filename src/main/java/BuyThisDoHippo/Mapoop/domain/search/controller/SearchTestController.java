package BuyThisDoHippo.Mapoop.domain.search.controller;

import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.DefaultedRedisConnection;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/test")
public class SearchTestController {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis 캐시 상태 확인 (테스트용)
     * GET /api/test/cache/status
     */
    @GetMapping("/cache/status")
    public ResponseEntity<CommonResponse<Map<String, Object>>> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();

        // Redis 연결 확인
        try {
            // 간단한 Redis 작업으로 연결 확인
            redisTemplate.opsForValue().set("health:check", "ping", Duration.ofSeconds(10));

            Object resultObj = redisTemplate.opsForValue().get("health:check");
            String result = resultObj != null ? resultObj.toString() : null;

            if ("ping".equals(result)) {
                status.put("redis_connection", "OK");
                // 테스트 키 삭제
                redisTemplate.delete("health:check");
            } else {
                status.put("redis_connection", "FAIL");
                status.put("connection_error", "Ping test failed - expected 'ping', got: " + result);
            }
        } catch (Exception e) {
            status.put("redis_connection", "FAIL");
            status.put("connection_error", e.getMessage());
            log.warn("Redis connection test failed: {}", e.getMessage());
        }

        // 현재 시간 추가 (캐시 상태 확인 시점)
        status.put("check_timestamp", System.currentTimeMillis());
        status.put("check_datetime", new Date().toString());

        return ResponseEntity.ok(CommonResponse.onSuccess(status, "캐시 상태 조회 성공"));
    }
}
