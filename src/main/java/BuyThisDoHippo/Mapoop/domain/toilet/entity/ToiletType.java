package BuyThisDoHippo.Mapoop.domain.toilet.entity;

import java.util.Optional;

public enum ToiletType {
    STORE,
    PUBLIC;

    public static Optional<ToiletType> fromString(String str) {
        if(str == null || str.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(ToiletType.valueOf(str.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
