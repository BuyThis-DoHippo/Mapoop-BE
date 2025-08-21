package BuyThisDoHippo.Mapoop.global.config;

import BuyThisDoHippo.Mapoop.domain.tag.entity.Tag;
import BuyThisDoHippo.Mapoop.domain.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final TagRepository tagRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeTags();
    }

    private void initializeTags() {
        try {
            // 기존 태그 개수 확인
            long tagCount = tagRepository.count();
            log.info("현재 태그 개수: {}", tagCount);

            if (tagCount == 0) {
                log.info("태그 데이터가 없습니다. 초기 태그 데이터를 생성합니다.");
                
                List<String> tagNames = Arrays.asList(
                    "남녀 분리", "가게 안 화장실", "24시간",
                    "비데 있음", "위생용품 제공", "깨끗함", "칸많음",
                    "장애인화장실", "기저귀교환대", "휴지있음", "향기좋음"
                );

                for (String name : tagNames) {
                    Tag tag = Tag.builder()
                            .name(name)
                            .build();
                    tagRepository.save(tag);
                    log.info("태그 생성됨: {}", name);
                }
                
                log.info("총 {}개의 태그가 생성되었습니다.", tagNames.size());
            } else {
                log.info("태그 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            }
        } catch (Exception e) {
            log.error("태그 초기화 중 오류 발생: ", e);
        }
    }
}
