package com.fitness;

import com.fitness.service.FirebaseService;
import com.fitness.controller.MainFrameController;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        FlatLightLaf.setup();
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);

        try {
            FirebaseService.initialize();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Firebase is not configured yet.\n\n" +
                            e.getMessage() + "\n\n" +
                            "Add your Firebase service account JSON to src/main/resources/serviceAccountKey.json\n" +
                            "and fill in the values in src/main/resources/config.properties.\n\n" +
                            "The app will open in limited mode until the Firebase project is configured.",
                    "Firebase setup required", JOptionPane.WARNING_MESSAGE);
        }
        SwingUtilities.invokeLater(() -> new MainFrameController().show());
    }
}
