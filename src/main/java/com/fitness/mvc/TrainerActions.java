package com.fitness.mvc;

import com.fitness.model.UserProfile;

public interface TrainerActions {
    void refreshOverview();
    void showSelectedTrainee(int row);
    void refreshExerciseLibrary();
    void createExercise(ExerciseForm form);
    void deleteSelectedExercise(int row);
    void traineeSelectionChanged(UserProfile trainee);
    void assignSelectedExercise(int exerciseRow);
    void removeSelectedAssignment(int row);
    void saveDailyGoal(int goal);
}
