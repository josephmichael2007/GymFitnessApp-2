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
            TrainerDashboard dashboard = new TrainerDashboard(user, this::logout);
            new TrainerController().attach(dashboard, user);
            view.showDashboard(dashboard);
        } else if (user.isAdmin()) {
            AdminDashboard dashboard = new AdminDashboard(user, this::logout);
            new AdminController().attach(dashboard);
            view.showDashboard(dashboard);
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