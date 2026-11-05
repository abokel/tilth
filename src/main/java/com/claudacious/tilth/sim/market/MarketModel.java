package com.claudacious.tilth.sim.market;

import java.util.List;
import java.util.Random;

/**
 * Pure price model for the farm market. Each commodity group has an index around 1.0 that drifts toward a seasonal
 * level (grain cheap after harvest and dear before the next, hay dearest at the end of winter, milk cheap in the
 * spring flush) with day-to-day noise. Events (droughts, gluts, fairs) multiply a group's price for a few days.
 *
 * <p>Groups are indexed in the order of {@code Commodity}: grain, vegetables, meat, dairy, eggs, wool, hay, fibre.
 * Months are 1..12.
 */
public final class MarketModel {
    public static final int GROUPS = 8;
    public static final int GRAIN = 0, VEG = 1, MEAT = 2, DAIRY = 3, EGGS = 4, WOOL = 5, HAY = 6, FIBRE = 7;

    private MarketModel() {
    }

    /** The level each group's price tends to in a given month. */
    public static double seasonal(int group, int month) {
        double phase = (month - 1) / 12.0 * 2 * Math.PI;
        return switch (group) {
            // Dearest in May-June before harvest, cheapest in September after it.
            case GRAIN -> 1.0 + 0.2 * Math.cos(phase - 2 * Math.PI * 4.5 / 12);
            // Fresh vegetables are dear in late winter and spring, cheap at the end of summer.
            case VEG -> 1.0 + 0.2 * Math.cos(phase - 2 * Math.PI * 2.5 / 12);
            case MEAT -> 1.0 + 0.1 * Math.cos(phase - 2 * Math.PI * 0.5 / 12);
            // The spring flush: milk is plentiful from April to June.
            case DAIRY -> 1.0 - 0.15 * Math.cos(phase - 2 * Math.PI * 4.0 / 12);
            case EGGS -> 1.0 - 0.15 * Math.cos(phase - 2 * Math.PI * 3.5 / 12);
            // Shearing in late spring floods the market; cloth sells into winter.
            case WOOL -> 1.0 - 0.1 * Math.cos(phase - 2 * Math.PI * 5.0 / 12);
            // Hay is dearest at the end of winter (February-March), cheap at haymaking.
            case HAY -> 1.0 + 0.35 * Math.cos(phase - 2 * Math.PI * 1.5 / 12);
            default -> 1.0;
        };
    }

    /** One day's move of an index: toward its seasonal level, plus a little noise. */
    public static double step(double index, int group, int month, Random random) {
        double target = seasonal(group, month);
        double next = index + 0.15 * (target - index) + random.nextGaussian() * 0.03;
        return Math.max(0.4, Math.min(2.5, next));
    }

    /** A market event: a headline, the groups it moves, by how much, for how many days, and in which months. */
    public record Event(String id, String headline, int[] groups, double multiplier, int days, int[] months) {
        public boolean affects(int group) {
            for (int g : groups) {
                if (g == group) {
                    return true;
                }
            }
            return false;
        }

        public boolean possibleIn(int month) {
            if (months.length == 0) {
                return true;
            }
            for (int m : months) {
                if (m == month) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final int[] ANY = {};
    private static final int[] ALL = {GRAIN, VEG, MEAT, DAIRY, EGGS, WOOL, HAY, FIBRE};

    public static final List<Event> EVENTS = List.of(
            new Event("drought", "Drought in the grain country: grain prices climb", new int[]{GRAIN, HAY}, 1.45, 6, new int[]{5, 6, 7, 8}),
            new Event("bumper_harvest", "A bumper harvest floods the market with grain", new int[]{GRAIN}, 0.65, 6, new int[]{8, 9, 10}),
            new Event("hard_winter", "A hard winter: hay is scarce and dear", new int[]{HAY}, 1.8, 5, new int[]{12, 1, 2, 3}),
            new Event("murrain", "Murrain among the herds: meat and dairy are dear", new int[]{MEAT, DAIRY}, 1.35, 5, ANY),
            new Event("cloth_fair", "The cloth fair is in town: wool is wanted", new int[]{WOOL}, 1.5, 4, new int[]{9, 10, 11, 12}),
            new Event("spring_flush", "The spring flush: milk is plentiful and cheap", new int[]{DAIRY}, 0.7, 5, new int[]{4, 5, 6}),
            new Event("harvest_festival", "Harvest festival: everything sells well", ALL, 1.2, 3, new int[]{9, 10}),
            new Event("blight", "Blight in the potato fields: vegetables are dear", new int[]{VEG}, 1.6, 6, new int[]{6, 7, 8, 9}),
            new Event("egg_glut", "Hens laying hard everywhere: an egg glut", new int[]{EGGS}, 0.6, 4, new int[]{3, 4, 5}),
            new Event("garrison", "Soldiers quartered nearby are buying meat and grain", new int[]{MEAT, GRAIN}, 1.3, 4, ANY),
            new Event("mill_fire", "Fire at the town mill: little grain is wanted this week", new int[]{GRAIN}, 0.75, 4, ANY),
            new Event("weavers", "The weavers are short of fibre", new int[]{FIBRE, WOOL}, 1.35, 4, ANY));

    /** Picks today's new event, if any (about one day in four), among those possible this month. */
    public static Event roll(int month, Random random) {
        if (random.nextDouble() > 0.25) {
            return null;
        }
        List<Event> possible = EVENTS.stream().filter(e -> e.possibleIn(month)).toList();
        return possible.isEmpty() ? null : possible.get(random.nextInt(possible.size()));
    }

    /** Price for one item: base value times the group's index, event and the item's quality (grade 0 = ungraded). */
    public static double price(double base, double index, double eventMultiplier, int grade) {
        double quality = switch (grade) {
            case 1 -> 0.6;
            case 2 -> 0.8;
            case 4 -> 1.25;
            case 5 -> 1.6;
            default -> 1.0;
        };
        return base * index * eventMultiplier * quality;
    }
}
