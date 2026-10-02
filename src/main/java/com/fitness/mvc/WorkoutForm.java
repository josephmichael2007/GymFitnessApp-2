package com.fitness.mvc;

public record WorkoutForm(String exercise, String category, String details, String intensity,
                          String calories, boolean completed) { }
