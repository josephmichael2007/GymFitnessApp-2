package com.fitness.model;

/** A gym's trainer invite code, created by an admin. Trainers who register with this code join this gym. */
public record GymCode(String code, String gymName, boolean active) { }
