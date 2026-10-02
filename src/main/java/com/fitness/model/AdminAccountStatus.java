package com.fitness.model;

import java.util.List;

public record AdminAccountStatus(UserProfile profile, List<Workout> workouts, List<AdminTraineeStatus> trainees) { }
