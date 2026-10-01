package com.fitness.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Turns a workout list into numbers for the charts. */
public final class Stats {
    private Stats() { }

    public static int total(List<Workout> list) {
        return list.stream().mapToInt(Workout::calories).sum();
    }

    public static int today(List<Workout> list) {
        String today = LocalDate.now().toString();
        return list.stream().filter(w -> today.equals(w.date())).mapToInt(Workout::calories).sum();
    }

    /** Calories per day for the last 7 days, oldest first. */
    public static Map<String, Integer> last7Days(List<Workout> list) {
        Map<String, Integer> out = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE d");
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            String iso = d.toString();
            int sum = list.stream().filter(w -> iso.equals(w.date())).mapToInt(Workout::calories).sum();
            out.put(d.format(fmt), sum);
        }
        return out;
    }

    public static Map<String, Integer> byCategory(List<Workout> list) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Workout w : list) out.merge(w.category(), w.calories(), Integer::sum);
        return out;
    }
}
