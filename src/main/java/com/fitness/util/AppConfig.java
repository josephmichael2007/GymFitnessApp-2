package com.fitness.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Reads config.properties from the classpath (src/main/resources). */
public final class AppConfig {
    private static final Properties P = new Properties();

    static {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) P.load(in);
        } catch (IOException ignored) { }
    }

    private AppConfig() { }

    public static String apiKey() { return P.getProperty("firebase.apiKey", "").trim(); }

    /** Only needed to register the very first Admin account; gyms/trainers use codes created in the app. */
    public static String adminCode() { return P.getProperty("admin.invite.code", "").trim(); }

    /** Fallback goal used until a trainer sets a per-trainee goal. */
    public static int defaultDailyGoal() {
        try { return Integer.parseInt(P.getProperty("daily.calorie.goal", "1000").trim()); }
        catch (NumberFormatException e) { return 1000; }
    }
}
