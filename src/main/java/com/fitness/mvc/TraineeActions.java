package com.fitness.mvc;

public interface TraineeActions {
    void refresh();
    void addWorkout(WorkoutForm form);
    void deleteWorkout(int row);
    void logAssignedExercise(int row);
    void loadTrainersForSelection();
    void updateAssignment(String gymName, String trainerUid);
}
