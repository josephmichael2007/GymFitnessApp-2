package com.fitness.mvc;

import com.fitness.model.UserProfile;

public record LoginRequest(String name, String email, String password, String portal, String code,
                           boolean registerMode, UserProfile trainer) { }
