package com.fitness.view;

import com.fitness.model.UserProfile;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final LoginPanel login;
    private JPanel dashboard;

    public MainFrame(Consumer<UserProfile> onLogin) {
        login = new LoginPanel(onLogin);
        setTitle("Gym Fitness Tracker");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1150, 760);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);
        root.add(login, "LOGIN");
        add(root);
        cards.show(root, "LOGIN");
    }

    public LoginPanel loginPanel() { return login; }

    public void showDashboard(JPanel dashboard) {
        if (this.dashboard != null) root.remove(this.dashboard);
        this.dashboard = dashboard;
        root.add(dashboard, "DASH");
        cards.show(root, "DASH");
    }

    public void showLogin() {
        cards.show(root, "LOGIN");
        if (dashboard != null) { root.remove(dashboard); dashboard = null; }
        login.reset();
    }
}
