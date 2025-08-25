package BuyThisDoHippo.Mapoop.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1 * 1024 * 1024)) // 1MB
                .build();
    }

    @Bean
    public WebClient googleWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://maps.googleapis.com")
                .build();
    }
}
