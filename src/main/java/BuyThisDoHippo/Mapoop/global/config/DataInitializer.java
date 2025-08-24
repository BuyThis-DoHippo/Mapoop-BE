package BuyThisDoHippo.Mapoop.global.config;

import BuyThisDoHippo.Mapoop.domain.tag.entity.Tag;
import BuyThisDoHippo.Mapoop.domain.tag.repository.TagRepository;
import BuyThisDoHippo.Mapoop.domain.toilet.entity.Toilet;
import BuyThisDoHippo.Mapoop.domain.toilet.repository.ToiletRepository;
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
    private final ToiletRepository toiletRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeTags();
        initializeToilets();
    }

    private void initializeTags() {
        try {
            // 기존 태그 개수 확인
            long tagCount = tagRepository.count();
            log.info("현재 태그 개수: {}", tagCount);

            if (tagCount == 0) {
                log.info("태그 데이터가 없습니다. 초기 태그 데이터를 생성합니다.");
                
                List<String> tagNames = Arrays.asList(
                    "현재이용가능", "남녀분리", "가게 안 화장실", "24시간",
                    "비데있음", "위생용품제공", "깨끗함", "칸많음", 
                    "휴지있음", "향기좋음", "장애인화장실", "기저귀교환대"
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

    private void initializeToilets() {
        try {
            // 기존 화장실 개수 확인
            long toiletCount = toiletRepository.count();
            log.info("현재 화장실 개수: {}", toiletCount);

            if (toiletCount == 0) {
                log.info("화장실 데이터가 없습니다. 샘플 화장실 데이터를 생성합니다.");
                
                // 마포구 공공 화장실 샘플 데이터 (실제 좌표 기반)
                List<ToiletData> sampleToilets = Arrays.asList(
                    new ToiletData("마포구청 공공 화장실", 37.5660, 126.9019, "서울특별시 마포구 월드컵로 212", 1, 4.5),
                    new ToiletData("홍대입구역 공중화장실", 37.5572, 126.9240, "서울특별시 마포구 양화로 188", -1, 4.2),
                    new ToiletData("월드컵 공원 화장실", 37.5683, 126.8975, "서울특별시 마포구 월드컵로 240", 0, 4.8),
                    new ToiletData("망원시장 공중화장실", 37.5563, 126.9105, "서울특별시 마포구 포은로 164", 1, 4.1),
                    new ToiletData("상암동 주민센터 화장실", 37.5792, 126.8898, "서울특별시 마포구 증산로 183", 2, 4.6),
                    new ToiletData("합정역 공중화장실", 37.5495, 126.9137, "서울특별시 마포구 양화로 45", -1, 4.3),
                    new ToiletData("마포구민체육센터 화장실", 37.5389, 126.9058, "서울특별시 마포구 마포대로 188", 1, 4.4),
                    new ToiletData("한강공원 망원지구 화장실", 37.5520, 126.8965, "서울특별시 마포구 마포나루길 467", 0, 4.7)
                );

                for (ToiletData data : sampleToilets) {
                    Toilet toilet = Toilet.builder()
                            .name(data.name)
                            .latitude(data.latitude)
                            .longitude(data.longitude)
                            .address(data.address)
                            .floor(data.floor)
                            .avgRating(data.rating)
                            .isPartnership(false)
                            .build();
                    toiletRepository.save(toilet);
                    log.info("화장실 생성됨: {} (위치: {}, {})", data.name, data.latitude, data.longitude);
                }
                
                log.info("총 {}개의 샘플 화장실이 생성되었습니다.", sampleToilets.size());
            } else {
                log.info("화장실 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            }
        } catch (Exception e) {
            log.error("화장실 초기화 중 오류 발생: ", e);
        }
    }

    // 화장실 데이터 클래스
    private static class ToiletData {
        final String name;
        final Double latitude;
        final Double longitude;
        final String address;
        final Integer floor;
        final Double rating;

        ToiletData(String name, Double latitude, Double longitude, String address, Integer floor, Double rating) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
            this.floor = floor;
            this.rating = rating;
        }
    }
}
