package BuyThisDoHippo.Mapoop.service;

import BuyThisDoHippo.Mapoop.dto.mcp.McpTool;
import BuyThisDoHippo.Mapoop.dto.mcp.McpToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MCP를 통한 프로젝트 구조 분석 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectAnalysisService {
    
    private final McpClientService mcpClientService;
    
    /**
     * 프로젝트 전체 구조를 분석합니다.
     */
    public Map<String, Object> analyzeProjectStructure() {
        Map<String, Object> analysis = new HashMap<>();
        
        try {
            // 1. 기본 프로젝트 정보
            analysis.put("projectInfo", getProjectInfo());
            
            // 2. 도메인 구조 분석
            analysis.put("domainStructure", analyzeDomainStructure());
            
            // 3. 엔티티 관계 분석
            analysis.put("entityRelations", analyzeEntityRelations());
            
            // 4. API 엔드포인트 분석
            analysis.put("apiEndpoints", analyzeApiEndpoints());
            
            // 5. 서비스 계층 분석
            analysis.put("serviceLayer", analyzeServiceLayer());
            
            // 6. 설정 파일 분석
            analysis.put("configuration", analyzeConfiguration());
            
            // 7. MCP 도구를 통한 추가 분석
            analysis.put("mcpAnalysis", runMcpAnalysis());
            
        } catch (Exception e) {
            log.error("프로젝트 구조 분석 중 오류 발생", e);
            analysis.put("error", e.getMessage());
        }
        
        return analysis;
    }
    
    private Map<String, Object> getProjectInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Mapoop-BE");
        info.put("type", "Spring Boot Application");
        info.put("description", "화장실 정보 및 리뷰 플랫폼 백엔드");
        info.put("mainPackage", "BuyThisDoHippo.Mapoop");
        return info;
    }
    
    private Map<String, Object> analyzeDomainStructure() {
        Map<String, Object> domains = new HashMap<>();
        
        try {
            Path domainPath = Paths.get("src/main/java/BuyThisDoHippo/Mapoop/domain");
            
            if (Files.exists(domainPath)) {
                try (Stream<Path> paths = Files.list(domainPath)) {
                    List<String> domainList = paths
                        .filter(Files::isDirectory)
                        .map(path -> path.getFileName().toString())
                        .sorted()
                        .collect(Collectors.toList());
                    
                    domains.put("domains", domainList);
                    domains.put("count", domainList.size());
                    
                    // 각 도메인별 상세 구조
                    Map<String, Object> domainDetails = new HashMap<>();
                    for (String domain : domainList) {
                        domainDetails.put(domain, analyzeSingleDomain(domain));
                    }
                    domains.put("details", domainDetails);
                }
            }
        } catch (IOException e) {
            log.error("도메인 구조 분석 실패", e);
        }
        
        return domains;
    }
    
    private Map<String, Object> analyzeSingleDomain(String domainName) {
        Map<String, Object> domainInfo = new HashMap<>();
        List<String> layers = new ArrayList<>();
        
        String domainBasePath = "src/main/java/BuyThisDoHippo/Mapoop/domain/" + domainName;
        
        // 각 계층별 파일 확인
        checkLayer(domainBasePath + "/entity", "entity", layers, domainInfo);
        checkLayer(domainBasePath + "/controller", "controller", layers, domainInfo);
        checkLayer(domainBasePath + "/service", "service", layers, domainInfo);
        checkLayer(domainBasePath + "/repository", "repository", layers, domainInfo);
        checkLayer(domainBasePath + "/dto", "dto", layers, domainInfo);
        
        domainInfo.put("layers", layers);
        return domainInfo;
    }
    
    private void checkLayer(String path, String layerName, List<String> layers, Map<String, Object> domainInfo) {
        Path layerPath = Paths.get(path);
        if (Files.exists(layerPath)) {
            layers.add(layerName);
            try (Stream<Path> files = Files.list(layerPath)) {
                List<String> fileNames = files
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java"))
                    .map(file -> file.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
                domainInfo.put(layerName + "Files", fileNames);
            } catch (IOException e) {
                log.warn("레이어 {} 분석 실패: {}", layerName, e.getMessage());
            }
        }
    }
    
    private Map<String, Object> analyzeEntityRelations() {
        Map<String, Object> relations = new HashMap<>();
        
        // 엔티티 파일들을 스캔해서 관계 분석
        List<String> entities = Arrays.asList(
            "User", "Toilet", "Review", "Tag", "ToiletTag", "ReviewTag", "Image", "ChatLog"
        );
        
        Map<String, List<String>> entityRelations = new HashMap<>();
        
        for (String entity : entities) {
            List<String> relatedEntities = findEntityRelations(entity);
            entityRelations.put(entity, relatedEntities);
        }
        
        relations.put("entities", entities);
        relations.put("relations", entityRelations);
        relations.put("totalEntities", entities.size());
        
        return relations;
    }
    
    private List<String> findEntityRelations(String entityName) {
        List<String> relations = new ArrayList<>();
        
        try {
            String entityPath = String.format("src/main/java/BuyThisDoHippo/Mapoop/domain/%s/entity/%s.java", 
                entityName.toLowerCase(), entityName);
            
            // 실제 파일이 있는 경로 찾기
            String[] possiblePaths = {
                "src/main/java/BuyThisDoHippo/Mapoop/domain/user/entity/User.java",
                "src/main/java/BuyThisDoHippo/Mapoop/domain/toilet/entity/Toilet.java",
                "src/main/java/BuyThisDoHippo/Mapoop/domain/review/entity/Review.java",
                "src/main/java/BuyThisDoHippo/Mapoop/domain/tag/entity/Tag.java",
                "src/main/java/BuyThisDoHippo/Mapoop/domain/tag/entity/ToiletTag.java",
                "src/main/java/BuyThisDoHippo/Mapoop/domain/tag/entity/ReviewTag.java",
                "src/main/java/BuyThisDoHippo/Mapoop/domain/image/entity/Image.java",
                "src/main/java/BuyThisDoHippo/Mapoop/domain/chat_log/entity/ChatLog.java"
            };
            
            for (String path : possiblePaths) {
                if (path.contains(entityName + ".java")) {
                    Path filePath = Paths.get(path);
                    if (Files.exists(filePath)) {
                        String content = Files.readString(filePath);
                        // JPA 관계 어노테이션 찾기
                        if (content.contains("@OneToMany")) relations.add("OneToMany");
                        if (content.contains("@ManyToOne")) relations.add("ManyToOne");
                        if (content.contains("@OneToOne")) relations.add("OneToOne");
                        if (content.contains("@ManyToMany")) relations.add("ManyToMany");
                        break;
                    }
                }
            }
        } catch (IOException e) {
            log.warn("엔티티 {} 관계 분석 실패: {}", entityName, e.getMessage());
        }
        
        return relations;
    }
    
    private Map<String, Object> analyzeApiEndpoints() {
        Map<String, Object> endpoints = new HashMap<>();
        List<String> controllers = new ArrayList<>();
        
        try {
            Path controllerPath = Paths.get("src/main/java/BuyThisDoHippo/Mapoop");
            
            Files.walk(controllerPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith("Controller.java"))
                .forEach(path -> {
                    String controllerName = path.getFileName().toString();
                    controllers.add(controllerName);
                });
                
        } catch (IOException e) {
            log.error("API 엔드포인트 분석 실패", e);
        }
        
        endpoints.put("controllers", controllers);
        endpoints.put("count", controllers.size());
        
        return endpoints;
    }
    
    private Map<String, Object> analyzeServiceLayer() {
        Map<String, Object> services = new HashMap<>();
        List<String> serviceFiles = new ArrayList<>();
        
        try {
            Path servicePath = Paths.get("src/main/java/BuyThisDoHippo/Mapoop");
            
            Files.walk(servicePath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith("Service.java"))
                .forEach(path -> {
                    String serviceName = path.getFileName().toString();
                    serviceFiles.add(serviceName);
                });
                
        } catch (IOException e) {
            log.error("서비스 계층 분석 실패", e);
        }
        
        services.put("services", serviceFiles);
        services.put("count", serviceFiles.size());
        
        return services;
    }
    
    private Map<String, Object> analyzeConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        try {
            // application.properties 분석
            Path appPropsPath = Paths.get("src/main/resources/application.properties");
            if (Files.exists(appPropsPath)) {
                String content = Files.readString(appPropsPath);
                config.put("applicationProperties", content.split("\n").length + " lines");
            }
            
            // application.yml 분석
            Path appYmlPath = Paths.get("src/main/resources/application.yml");
            if (Files.exists(appYmlPath)) {
                String content = Files.readString(appYmlPath);
                config.put("applicationYml", content.split("\n").length + " lines");
            }
            
            // build.gradle 분석
            Path buildGradlePath = Paths.get("build.gradle");
            if (Files.exists(buildGradlePath)) {
                String content = Files.readString(buildGradlePath);
                long dependencyCount = content.lines()
                    .filter(line -> line.trim().startsWith("implementation") || 
                                  line.trim().startsWith("compile") ||
                                  line.trim().startsWith("runtime"))
                    .count();
                config.put("dependencies", dependencyCount);
            }
            
        } catch (IOException e) {
            log.error("설정 파일 분석 실패", e);
        }
        
        return config;
    }
    
    private Map<String, Object> runMcpAnalysis() {
        Map<String, Object> mcpResult = new HashMap<>();
        
        try {
            // MCP 도구 목록 가져오기
            List<McpTool> availableTools = mcpClientService.getAvailableTools();
            mcpResult.put("availableTools", availableTools.size());
            mcpResult.put("toolNames", availableTools.stream()
                .map(McpTool::getName)
                .collect(Collectors.toList()));
            
            // 프로젝트 분석용 MCP 도구 호출
            if (!availableTools.isEmpty()) {
                for (McpTool tool : availableTools) {
                    if (tool.getName().contains("analyze") || tool.getName().contains("structure")) {
                        Map<String, Object> arguments = Map.of(
                            "projectPath", ".",
                            "language", "java",
                            "framework", "spring-boot"
                        );
                        
                        McpToolResult result = mcpClientService.callTool(tool.getName(), arguments);
                        mcpResult.put("toolResult_" + tool.getName(), result);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("MCP 분석 실행 실패", e);
            mcpResult.put("error", e.getMessage());
        }
        
        return mcpResult;
    }
}
