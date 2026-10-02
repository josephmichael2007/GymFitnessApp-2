package com.fitness.model;

import java.util.List;

public record AdminTraineeStatus(UserProfile profile, List<Workout> workouts) { }
