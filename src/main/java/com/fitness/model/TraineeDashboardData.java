package com.fitness.model;

import java.util.List;

public record TraineeDashboardData(UserProfile profile, List<Workout> workouts, List<AssignedExercise> assigned) { }
