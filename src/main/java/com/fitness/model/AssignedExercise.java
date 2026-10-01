package com.fitness.model;

/** An exercise a trainer has prescribed to one trainee. */
public record AssignedExercise(String id, String exerciseName, String category, int sets, int reps,
                               String notes, String assignedBy, String assignedDate) {
    public String setsReps() { return sets + " sets x " + reps + " reps"; }
}
