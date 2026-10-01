package com.fitness.model;

/**
 * A signed-in account.
 *  - dailyGoal:  per-trainee, set by their trainer; null until set (falls back to config default).
 *  - trainerUid: for a Trainee, the uid of the trainer they picked at registration.
 *  - gymName:    for a Trainer, the gym their invite code belonged to.
 */
public record UserProfile(String uid, String email, String name, String role, Integer dailyGoal,
                          String trainerUid, String gymName) {
    public boolean isTrainer() { return "Trainer".equals(role); }
    public boolean isAdmin() { return "Admin".equals(role); }
    public boolean isTrainee() { return "Trainee".equals(role); }
}
