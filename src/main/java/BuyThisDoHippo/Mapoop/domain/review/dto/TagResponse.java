/**
 * 태그 응답 DTO
 * 리뷰에 포함된 태그 정보를 클라이언트에 전달
 */
package BuyThisDoHippo.Mapoop.domain.review.dto;

import BuyThisDoHippo.Mapoop.domain.tag.entity.Tag;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder // 빌더 패턴 자동 생성 (객체 생성을 깔끔하게)
public class TagResponse {
    
    /**
     * 태그 고유 ID
     */
    private Long tagId;
    
    /**
     * 태그 이름 (예: "현재이용가능", "깨끗함", "24시간")
     */
    private String tagName;
    
    /**
     * Tag Entity → TagResponse DTO 변환 메서드
     * static 메서드: 클래스 인스턴스 없이 호출 가능한 유틸성 메서드
     * 팩토리 패턴: 객체 생성 로직을 캡슐화
     * 
     * @param tag Tag 엔티티
     * @return TagResponse DTO
     */
    public static TagResponse from(Tag tag) {
        return TagResponse.builder()
                .tagId(tag.getId())
                .tagName(tag.getName())
                .build();
    }
}
