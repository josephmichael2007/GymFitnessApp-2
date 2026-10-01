package com.fitness.controller;

import com.fitness.model.UserProfile;
import com.fitness.view.AdminDashboard;
import com.fitness.view.MainFrame;
import com.fitness.view.TraineeDashboard;
import com.fitness.view.TrainerDashboard;

import javax.swing.SwingUtilities;

public final class MainFrameController {
    private final MainFrame view;

    public MainFrameController() {
        view = new MainFrame(this::onLogin);
        new AuthController().attach(view.loginPanel());
    }

    public void show() {
        SwingUtilities.invokeLater(() -> view.setVisible(true));
    }

    private void onLogin(UserProfile user) {
        if (user.isTrainer()) {
            view.showDashboard(new TrainerDashboard(user, this::logout));
        } else if (user.isAdmin()) {
            view.showDashboard(new AdminDashboard(user, this::logout));
        } else {
            TraineeDashboard dashboard = new TraineeDashboard(user, this::logout);
            new TraineeController().attach(dashboard, user.uid());
            view.showDashboard(dashboard);
        }
    }

    private void logout() {
        view.showLogin();
    }
}