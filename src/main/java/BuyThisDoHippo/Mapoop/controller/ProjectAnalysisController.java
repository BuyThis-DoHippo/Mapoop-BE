package BuyThisDoHippo.Mapoop.controller;

import BuyThisDoHippo.Mapoop.service.ProjectAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 프로젝트 구조 분석 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class ProjectAnalysisController {

    private final ProjectAnalysisService projectAnalysisService;

    /**
     * 프로젝트 전체 구조를 분석합니다.
     */
    @GetMapping("/structure")
    public ResponseEntity<Map<String, Object>> analyzeProjectStructure() {
        try {
            log.info("프로젝트 구조 분석 시작");
            Map<String, Object> analysis = projectAnalysisService.analyzeProjectStructure();
            log.info("프로젝트 구조 분석 완료");
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("프로젝트 구조 분석 실패", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "프로젝트 구조 분석 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 도메인별 상세 분석을 제공합니다.
     */
    @GetMapping("/domains")
    public ResponseEntity<Map<String, Object>> analyzeDomains() {
        try {
            Map<String, Object> analysis = projectAnalysisService.analyzeProjectStructure();
            Map<String, Object> domainAnalysis = (Map<String, Object>) analysis.get("domainStructure");
            return ResponseEntity.ok(domainAnalysis);
        } catch (Exception e) {
            log.error("도메인 분석 실패", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "도메인 분석 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * API 엔드포인트 분석을 제공합니다.
     */
    @GetMapping("/apis")
    public ResponseEntity<Map<String, Object>> analyzeApis() {
        try {
            Map<String, Object> analysis = projectAnalysisService.analyzeProjectStructure();
            Map<String, Object> apiAnalysis = (Map<String, Object>) analysis.get("apiEndpoints");
            return ResponseEntity.ok(apiAnalysis);
        } catch (Exception e) {
            log.error("API 분석 실패", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "API 분석 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
