package ua.bonny.infoplayer.data;

public enum PrivacyOption {
    ONLINE_STATUS(0),
    LAST_ACTIVITY(1),
    EXPERIENCE(2),
    ADVANCEMENTS(3),
    PLAY_TIME(4),
    HEALTH(5),
    FOOD(6),
    GAME_MODE(7),
    DIMENSION(8),
    COORDINATES(9),
    FIRST_SEEN(10),
    INVENTORY(11),
    CURIOS(12);

    public static final long ALL_VISIBLE = (1L << values().length) - 1;

    private final long bit;

    PrivacyOption(int bitIndex) {
        this.bit = 1L << bitIndex;
    }

    public boolean visible(long mask) {
        return (mask & bit) != 0;
    }

    public long setVisible(long mask, boolean visible) {
        return visible ? mask | bit : mask & ~bit;
    }

    public static long sanitize(long mask) {
        return mask & ALL_VISIBLE;
    }
}
