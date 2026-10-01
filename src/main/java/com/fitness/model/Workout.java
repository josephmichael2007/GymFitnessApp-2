package com.fitness.model;

public record Workout(String id, String exercise, String category, String details,
                      String intensity, int calories, String date, boolean completed) { }
