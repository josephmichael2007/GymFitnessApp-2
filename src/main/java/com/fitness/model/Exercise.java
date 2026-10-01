package com.fitness.model;

/** A reusable exercise template a trainer creates, e.g. "Push-ups, 3x15". */
public record Exercise(String id, String name, String category, int sets, int reps, String notes) {
    public String setsReps() { return sets + " sets x " + reps + " reps"; }
}
