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
    Double getDistance();
}
