package com.fitness.controller;

import com.fitness.model.AssignedExercise;
import com.fitness.model.Exercise;
import com.fitness.model.TrainerTraineeSummary;
import com.fitness.model.UserProfile;
import com.fitness.service.FirebaseService;
import com.fitness.util.Async;
import com.fitness.mvc.ExerciseForm;
import com.fitness.mvc.TrainerActions;
import com.fitness.mvc.TrainerScreen;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

/**
 * Everything a trainer's dashboard needs: their OWN trainees' overview, the shared exercise
 * library, assignment, and goals. Every trainee-facing method is scoped to this trainer's uid,
 * so one trainer can never see another trainer's (or gym's) trainees.
 */
public class TrainerController implements TrainerActions {

    private TrainerScreen view;
    private UserProfile trainer;
    private List<TrainerTraineeSummary> trainees = List.of();
    private List<Exercise> exercises = List.of();
    private List<AssignedExercise> assignedExercises = List.of();
    private UserProfile selectedTrainee;

    public List<TrainerTraineeSummary> loadTraineeOverview(String trainerUid) throws Exception {
        List<TrainerTraineeSummary> out = new ArrayList<>();
        for (UserProfile p : FirebaseService.getTraineesOf(trainerUid)) {
            out.add(new TrainerTraineeSummary(p, FirebaseService.getWorkouts(p.uid())));
        }
        return out;
    }

    public List<Exercise> loadExerciseLibrary() throws Exception {
        return FirebaseService.getExerciseLibrary();
    }

    public Exercise createExercise(Exercise e) throws Exception {
        return FirebaseService.createExercise(e);
    }

    public void deleteExercise(String exerciseId) throws Exception {
        FirebaseService.deleteExercise(exerciseId);
    }

    public void assignExercise(String traineeUid, AssignedExercise a) throws Exception {
        FirebaseService.assignExercise(traineeUid, a);
    }

    public List<AssignedExercise> getAssignedExercises(String traineeUid) throws Exception {
        return FirebaseService.getAssignedExercises(traineeUid);
    }

    public void removeAssignedExercise(String traineeUid, String assignedId) throws Exception {
        FirebaseService.removeAssignedExercise(traineeUid, assignedId);
    }

    public void setDailyGoal(String traineeUid, int goal) throws Exception {
        FirebaseService.setDailyGoal(traineeUid, goal);
    }

    public void attach(TrainerScreen view, UserProfile trainer) {
        this.view = view;
        this.trainer = trainer;
        view.setActions(this);
        refreshOverview();
        refreshExerciseLibrary();
    }

    @Override public void refreshOverview() {
        Async.run(() -> loadTraineeOverview(trainer.uid()), result -> {
            trainees = result;
            view.renderOverview(result);
        }, view::showError);
    }

    @Override public void showSelectedTrainee(int row) {
        if (row < 0 || row >= trainees.size()) return;
        view.renderSelectedTrainee(trainees.get(row));
    }

    @Override public void refreshExerciseLibrary() {
        Async.run(this::loadExerciseLibrary, result -> {
            exercises = result;
            view.renderExerciseLibrary(result);
        }, view::showError);
    }

    @Override public void createExercise(ExerciseForm form) {
        if (form.name().isBlank()) {
            view.showMessage("Enter an exercise name.");
            return;
        }
        Exercise exercise = new Exercise(null, form.name().trim(), form.category(), form.sets(), form.reps(),
                form.notes().trim());
        Async.run(() -> createExercise(exercise), result -> {
            view.clearExerciseForm();
            refreshExerciseLibrary();
        }, view::showError);
    }

    @Override public void deleteSelectedExercise(int row) {
        if (row < 0 || row >= exercises.size()) {
            view.showMessage("Select an exercise first.");
            return;
        }
        Async.run(() -> { deleteExercise(exercises.get(row).id()); return null; },
                result -> refreshExerciseLibrary(), view::showError);
    }

    @Override public void traineeSelectionChanged(UserProfile trainee) {
        selectedTrainee = trainee;
        assignedExercises = List.of();
        view.renderAssignedExercises(List.of());
        if (trainee == null) return;
        view.setDailyGoalValue(trainee.dailyGoal());
        Async.run(() -> getAssignedExercises(trainee.uid()), result -> {
            assignedExercises = result;
            view.renderAssignedExercises(result);
        }, view::showError);
    }

    @Override public void assignSelectedExercise(int exerciseRow) {
        if (selectedTrainee == null) {
            view.showMessage("No trainees to assign to yet.");
            return;
        }
        if (exerciseRow < 0 || exerciseRow >= exercises.size()) {
            view.showMessage("Select an exercise from the library first.");
            return;
        }
        Exercise exercise = exercises.get(exerciseRow);
        AssignedExercise assigned = new AssignedExercise(null, exercise.name(), exercise.category(), exercise.sets(),
                exercise.reps(), exercise.notes(), trainer.name(), LocalDate.now().toString());
        Async.run(() -> { assignExercise(selectedTrainee.uid(), assigned); return null; }, result -> {
            traineeSelectionChanged(selectedTrainee);
            view.showMessage(exercise.name() + " assigned to " + selectedTrainee.name() + ".");
        }, view::showError);
    }

    @Override public void removeSelectedAssignment(int row) {
        if (selectedTrainee == null || row < 0 || row >= assignedExercises.size()) {
            view.showMessage("Select an assignment to remove.");
            return;
        }
        String assignmentId = assignedExercises.get(row).id();
        Async.run(() -> { removeAssignedExercise(selectedTrainee.uid(), assignmentId); return null; },
                result -> traineeSelectionChanged(selectedTrainee), view::showError);
    }

    @Override public void saveDailyGoal(int goal) {
        if (selectedTrainee == null) return;
        UserProfile trainee = selectedTrainee;
        Async.run(() -> { setDailyGoal(trainee.uid(), goal); return null; }, result -> {
            view.showMessage("Daily goal for " + trainee.name() + " set to " + goal + " kcal.");
            refreshOverview();
        }, view::showError);
    }
}
