package BuyThisDoHippo.Mapoop.global.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Slf4j
public class S3Config {


    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${aws.enabled:false}") boolean awsEnabled;
    @PostConstruct
    void logFlag() { log.info("aws.enabled = {}", awsEnabled);}

    @Bean
    public S3Client s3Client() {
        log.info("S3Client 빈을 생성합니다. Region: {}", region);
        
        try {
            // AWS 자격 증명이 없으면 기본 자격 증명 사용
            if (accessKey == null || accessKey.trim().isEmpty() || 
                secretKey == null || secretKey.trim().isEmpty()) {
                log.warn("AWS 자격 증명이 설정되지 않았습니다. 기본 자격 증명을 사용합니다.");
                return S3Client.builder()
                        .region(Region.of(region))
                        .build();
            }

            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            
            return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build();

        } catch (Exception e) {
            log.error("S3Client 생성 중 오류 발생: {}", e.getMessage());
            // 빈 생성 실패를 방지하기 위해 기본 클라이언트 반환
            return S3Client.builder()
                    .region(Region.of(region))
                    .build();
        }
    }
}
