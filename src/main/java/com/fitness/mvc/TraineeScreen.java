package com.fitness.mvc;

import com.fitness.model.AssignedExercise;
import com.fitness.model.TraineeDashboardData;
import com.fitness.model.Workout;

public interface TraineeScreen {
    void setActions(TraineeActions actions);
    void render(TraineeDashboardData data);
    void prefillAssignedExercise(AssignedExercise exercise);
    boolean confirmDelete(Workout workout);
    void setAddEnabled(boolean enabled);
    void clearExercise();
    void showError(Throwable error);
    void showMessage(String message, String title, int messageType);
}
