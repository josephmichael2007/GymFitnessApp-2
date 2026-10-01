package com.fitness.controller;

import com.fitness.model.AssignedExercise;
import com.fitness.model.Exercise;
import com.fitness.model.UserProfile;
import com.fitness.model.Workout;
import com.fitness.service.FirebaseService;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything a trainer's dashboard needs: their OWN trainees' overview, the shared exercise
 * library, assignment, and goals. Every trainee-facing method is scoped to this trainer's uid,
 * so one trainer can never see another trainer's (or gym's) trainees.
 */
public class TrainerController {

    public record TraineeSummary(UserProfile profile, List<Workout> workouts) { }

    public List<TraineeSummary> loadTraineeOverview(String trainerUid) throws Exception {
        List<TraineeSummary> out = new ArrayList<>();
        for (UserProfile p : FirebaseService.getTraineesOf(trainerUid)) {
            out.add(new TraineeSummary(p, FirebaseService.getWorkouts(p.uid())));
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
}
