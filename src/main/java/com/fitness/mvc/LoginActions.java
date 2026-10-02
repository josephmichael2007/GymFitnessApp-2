package com.fitness.mvc;

public interface LoginActions {
    void loadTrainersForView();
    void submit(LoginRequest request);
    void sendPasswordReset(String email);
}
