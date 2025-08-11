package BuyThisDoHippo.Mapoop.domain.toilet.repository.projection;

public interface ToiletWithDistance {
    Long getId();
    String getName();
    Boolean getIsPartnership();
    Double getLatitude();
    Double getLongitude();
    String getAddress();
    Integer getFloor();
    Double getAvgRating();
    Integer getTotalReviews();

    // 위치 허용 시 채워짐
    Double getDistance();
}
