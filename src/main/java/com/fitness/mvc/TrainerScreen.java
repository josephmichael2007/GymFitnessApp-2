package com.fitness.mvc;

import com.fitness.model.AssignedExercise;
import com.fitness.model.Exercise;
import com.fitness.model.TrainerTraineeSummary;
import java.util.List;

public interface TrainerScreen {
    void setActions(TrainerActions actions);
    void renderOverview(List<TrainerTraineeSummary> trainees);
    void renderSelectedTrainee(TrainerTraineeSummary trainee);
    void renderExerciseLibrary(List<Exercise> exercises);
    void renderAssignedExercises(List<AssignedExercise> exercises);
    void setDailyGoalValue(Integer goal);
    void clearExerciseForm();
    void showMessage(String message);
    void showError(Throwable error);
}
