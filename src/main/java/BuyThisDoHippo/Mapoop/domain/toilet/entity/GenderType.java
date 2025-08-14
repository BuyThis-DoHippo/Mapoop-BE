package BuyThisDoHippo.Mapoop.domain.toilet.entity;

import java.util.Optional;

public enum GenderType {
    UNISEX,
    SEPARATE;

    public static Optional<GenderType> fromString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(GenderType.valueOf(str.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
