package com.claudacious.tilth.sim.calendar;

/** Meteorological seasons for the northern hemisphere. */
public enum Season {
    WINTER("Winter"),
    SPRING("Spring"),
    SUMMER("Summer"),
    AUTUMN("Autumn");

    private final String displayName;

    Season(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static Season ofMonth(int month) {
        return switch (month) {
            case 12, 1, 2 -> WINTER;
            case 3, 4, 5 -> SPRING;
            case 6, 7, 8 -> SUMMER;
            default -> AUTUMN;
        };
    }
}
