package com.fitness.controller;

import com.fitness.model.AssignedExercise;
import com.fitness.model.TraineeDashboardData;
import com.fitness.model.UserProfile;
import com.fitness.model.Workout;
import com.fitness.mvc.TraineeActions;
import com.fitness.mvc.TraineeScreen;
import com.fitness.mvc.WorkoutForm;
import com.fitness.util.Async;

import javax.swing.JOptionPane;
import java.time.LocalDate;
import com.fitness.service.FirebaseService;

import java.util.List;

/** Everything a trainee's dashboard needs. */
public class TraineeController implements TraineeActions {

    private TraineeScreen view;
    private String uid;
    private TraineeDashboardData currentView;

    public TraineeDashboardData load(String uid) throws Exception {
        UserProfile profile = FirebaseService.getProfile(uid);
        List<Workout> workouts = FirebaseService.getWorkouts(uid);
        List<AssignedExercise> assigned = FirebaseService.getAssignedExercises(uid);
        return new TraineeDashboardData(profile, workouts, assigned);
    }

    public TraineeDashboardData addWorkout(String uid, Workout w) throws Exception {
        FirebaseService.addWorkout(uid, w);
        return load(uid);
    }

    public TraineeDashboardData deleteWorkout(String uid, String workoutId) throws Exception {
        FirebaseService.deleteWorkout(uid, workoutId);
        return load(uid);
    }

    public void attach(TraineeScreen view, String uid) {
        this.view = view;
        this.uid = uid;
        view.setActions(this);
        refresh();
    }

    @Override
    public void refresh() {
        Async.run(() -> load(uid), result -> {
            currentView = result;
            view.render(result);
        }, view::showError);
    }

    @Override
    public void addWorkout(WorkoutForm form) {
        if (form.exercise().isBlank()) {
            view.showMessage("Please enter an exercise name.", "Input error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int calories;
        try {
            calories = Integer.parseInt(form.calories().trim());
            if (calories < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            view.showMessage("Calories must be a positive whole number.", "Input error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Workout workout = new Workout(null, form.exercise().trim(), form.category(), form.details().trim(),
                form.intensity(), calories, LocalDate.now().toString(), form.completed());
        view.setAddEnabled(false);
        Async.run(() -> addWorkout(uid, workout), result -> {
            view.setAddEnabled(true);
            view.clearExercise();
            currentView = result;
            view.render(result);
        }, error -> {
            view.setAddEnabled(true);
            view.showError(error);
        });
    }

    @Override
    public void deleteWorkout(int row) {
        if (currentView == null || row < 0 || row >= currentView.workouts().size()) {
            view.showMessage("Select a workout in the table first.", "", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Workout workout = currentView.workouts().get(row);
        if (!view.confirmDelete(workout)) return;
        Async.run(() -> deleteWorkout(uid, workout.id()), result -> {
            currentView = result;
            view.render(result);
        }, view::showError);
    }

    @Override
    public void logAssignedExercise(int row) {
        if (currentView == null || row < 0 || row >= currentView.assigned().size()) {
            view.showMessage("Select an assigned exercise first.", "", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        var exercise = currentView.assigned().get(row);
        view.prefillAssignedExercise(exercise);
        view.showMessage("Filled the log form with \"" + exercise.exerciseName()
                + "\". Set the calories burned and click Add workout.", "Ready to log", JOptionPane.INFORMATION_MESSAGE);
    }
}
