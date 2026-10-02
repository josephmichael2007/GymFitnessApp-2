package com.fitness.controller;

import com.fitness.model.UserProfile;
import com.fitness.service.FirebaseService;
import com.fitness.util.Async;
import com.fitness.mvc.LoginActions;
import com.fitness.mvc.LoginRequest;
import com.fitness.mvc.LoginScreen;

import javax.swing.JOptionPane;
import java.util.List;

/** Handles login and registration, including checking the account's role matches the chosen portal. */
public class AuthController implements LoginActions {

    private LoginScreen view;

    public void attach(LoginScreen view) {
        this.view = view;
        view.setActions(this);
    }

    public UserProfile login(String email, String password, String expectedRole) throws Exception {
        UserProfile profile = FirebaseService.login(email, password);
        if (!profile.role().equals(expectedRole)) {
            throw new IllegalStateException(
                    "This account is registered as " + profile.role() + ", not " + expectedRole
                            + ". Use the " + profile.role() + " login instead.");
        }
        return profile;
    }

    public void sendPasswordResetEmail(String email) throws Exception {
        FirebaseService.sendPasswordResetEmail(email);
    }

    public UserProfile register(String name, String email, String password, String role, String inviteCode,
                                 String gymName, String trainerUid) throws Exception {
        return FirebaseService.register(name, email, password, role, inviteCode, gymName, trainerUid);
    }

    /** For the Trainee registration screen's "pick your trainer" dropdown. */
    public List<UserProfile> loadTrainers() throws Exception {
        return FirebaseService.getTrainersForSelection();
    }

    @Override
    public void loadTrainersForView() {
        Async.run(this::loadTrainers, view::showTrainers, view::showError);
    }

    @Override
    public void submit(LoginRequest request) {
        if (request.email().isBlank() || request.password().isEmpty()
                || (request.registerMode() && request.name().isBlank())) {
            view.showMessage("Please fill in all fields.", "Missing info", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String trainerUid = null;
        String gymName = null;
        if (request.registerMode() && "Trainee".equals(request.portal())) {
            if (request.gymName() == null || request.gymName().isBlank() || request.trainer() == null) {
                view.showMessage("Please select your gym and trainer.", "Missing info", JOptionPane.WARNING_MESSAGE);
                return;
            }
            gymName = request.gymName();
            trainerUid = request.trainer().uid();
        }
        String selectedGymName = gymName;
        String selectedTrainerUid = trainerUid;
        view.setSubmitEnabled(false);
        Async.run(() -> request.registerMode()
                        ? register(request.name(), request.email(), request.password(), request.portal(),
                            request.code(), selectedGymName, selectedTrainerUid)
                        : login(request.email(), request.password(), request.portal()),
                user -> {
                    view.setSubmitEnabled(true);
                    view.loginSucceeded(user);
                }, error -> {
                    view.setSubmitEnabled(true);
                    view.showError(error);
                });
    }

    @Override
    public void sendPasswordReset(String email) {
        if (email.isBlank()) {
            view.showMessage("Enter your email address first.", "Password reset", JOptionPane.INFORMATION_MESSAGE);
            view.focusEmail();
            return;
        }
        view.setResetEnabled(false);
        Async.run(() -> {
            sendPasswordResetEmail(email.trim());
            return null;
        }, result -> {
            view.setResetEnabled(true);
            view.showMessage("If an account exists for this email, a password reset email has been sent.",
                    "Password reset", JOptionPane.INFORMATION_MESSAGE);
        }, error -> {
            view.setResetEnabled(true);
            view.showError(error);
        });
    }
}
