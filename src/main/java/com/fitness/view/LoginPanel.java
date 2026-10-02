package com.fitness.view;

import com.fitness.model.UserProfile;
import com.fitness.mvc.LoginActions;
import com.fitness.mvc.LoginRequest;
import com.fitness.mvc.LoginScreen;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Login / registration screen. Defaults to the Trainee portal; the gear icon
 * (top-right of the card) switches between Trainee, Trainer and Admin login.
 *  - Trainee registration: pick a trainer from a dropdown (no code needed).
 *  - Trainer registration: enter the gym code an admin gave them.
 *  - Admin registration: enter the admin bootstrap code from config.properties.
 */
public class LoginPanel extends JPanel implements LoginScreen {
    private LoginActions actions;
    private final Consumer<UserProfile> onSuccess;

    private final JTextField txtName = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JPasswordField txtPass = new JPasswordField();
    private final JPasswordField txtCode = new JPasswordField();
    private final JComboBox<Object> cbTrainer = new JComboBox<>();

    private final JPanel nameBlock, codeBlock, trainerBlock;
    private final JLabel lblCodeCaption = new JLabel("Invite code");
    private final JButton btnSubmit = Theme.button("Log in", Theme.PRIMARY);
    private final JButton btnToggle = new JButton("New here? Create an account");
    private final JButton btnForgot = new JButton("Forgot password?");
    private final JLabel lblPortal = new JLabel();
    private final JButton btnGear = new JButton("\u2699");

    private String portal = "Trainee"; // Trainee | Trainer | Admin
    private boolean registerMode = false;
    private boolean trainersLoading = false;
    private static final String LOADING = "Loading trainers\u2026";
    private static final String NONE_YET = "No trainers registered yet";

    public LoginPanel(Consumer<UserProfile> onSuccess) {
        this.onSuccess = onSuccess;
        setLayout(new GridBagLayout());
        setBackground(Theme.BG);

        JPanel card = Theme.card(null, new BorderLayout());
        card.setPreferredSize(new Dimension(380, 520));

        btnGear.setFocusPainted(false);
        btnGear.setFont(Theme.font(Font.PLAIN, 16));
        btnGear.setToolTipText("Switch portal (Trainee / Trainer / Admin)");
        btnGear.putClientProperty("JButton.buttonType", "roundRect");
        JPanel gearBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        gearBar.setOpaque(false);
        gearBar.add(btnGear);
        card.add(gearBar, BorderLayout.NORTH);
        btnGear.addActionListener(e -> showPortalMenu());

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(0, 14, 10, 14));

        JLabel brand = new JLabel("Gym Fitness Tracker", SwingConstants.CENTER);
        brand.setFont(Theme.font(Font.BOLD, 22));
        brand.setForeground(Theme.PRIMARY);
        brand.setAlignmentX(CENTER_ALIGNMENT);
        lblPortal.setFont(Theme.font(Font.BOLD, 13));
        lblPortal.setAlignmentX(CENTER_ALIGNMENT);

        txtName.putClientProperty("JTextField.placeholderText", "Your full name");
        txtEmail.putClientProperty("JTextField.placeholderText", "you@example.com");
        txtPass.putClientProperty("JTextField.placeholderText", "At least 6 characters");
        txtPass.putClientProperty("JPasswordField.showRevealButton", true);
        txtCode.putClientProperty("JTextField.placeholderText", "Ask your gym admin");

        nameBlock = block("Name", txtName);
        codeBlock = block(lblCodeCaption, txtCode);
        trainerBlock = block("Your trainer", cbTrainer);

        form.add(brand);
        form.add(Box.createVerticalStrut(4));
        form.add(lblPortal);
        form.add(Box.createVerticalStrut(16));
        form.add(nameBlock);
        form.add(block("Email", txtEmail));
        form.add(block("Password", txtPass));
        form.add(trainerBlock);
        form.add(codeBlock);
        form.add(Box.createVerticalStrut(8));

        btnSubmit.setAlignmentX(CENTER_ALIGNMENT);
        btnSubmit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        form.add(btnSubmit);
        form.add(Box.createVerticalStrut(6));

        btnToggle.setBorderPainted(false);
        btnToggle.setContentAreaFilled(false);
        btnToggle.setForeground(Theme.PRIMARY);
        btnToggle.setFocusPainted(false);
        btnToggle.setAlignmentX(CENTER_ALIGNMENT);
        btnForgot.setBorderPainted(false);
        btnForgot.setContentAreaFilled(false);
        btnForgot.setForeground(Theme.PRIMARY);
        btnForgot.setFocusPainted(false);
        JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        linkRow.setOpaque(false);
        linkRow.add(btnForgot);
        linkRow.add(btnToggle);
        form.add(linkRow);

        card.add(form, BorderLayout.CENTER);
        add(card);

        btnToggle.addActionListener(e -> { registerMode = !registerMode; updateVisibility(); });
        btnForgot.addActionListener(e -> {
            if (actions != null) actions.sendPasswordReset(txtEmail.getText().trim());
        });
        btnSubmit.addActionListener(e -> submit());
        txtPass.addActionListener(e -> submit());
        updateVisibility();
    }

    @Override public void setActions(LoginActions actions) { this.actions = actions; }

    private void showPortalMenu() {
        JPopupMenu menu = new JPopupMenu();
        for (String p : new String[]{"Trainee", "Trainer", "Admin"}) {
            JMenuItem item = new JMenuItem((p.equals(portal) ? "\u2713 " : "   ") + p + " login");
            item.addActionListener(e -> { portal = p; registerMode = false; updateVisibility(); });
            menu.add(item);
        }
        menu.show(btnGear, 0, btnGear.getHeight());
    }

    private static JPanel block(String label, JComponent field) { return block(new JLabel(label), field); }

    private static JPanel block(JLabel label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        label.setFont(Theme.font(Font.BOLD, 12));
        label.setForeground(Theme.TEXT);
        p.add(label, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        p.setBorder(new EmptyBorder(0, 0, 10, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        p.setAlignmentX(CENTER_ALIGNMENT);
        return p;
    }

    private void updateVisibility() {
        boolean isTraineeReg = registerMode && "Trainee".equals(portal);
        boolean isStaffReg = registerMode && !"Trainee".equals(portal);

        nameBlock.setVisible(registerMode);
        trainerBlock.setVisible(isTraineeReg);
        codeBlock.setVisible(isStaffReg);
        lblCodeCaption.setText("Admin".equals(portal) ? "Admin invite code" : "Gym code");

        btnSubmit.setText(registerMode ? "Create account" : "Log in");
        btnForgot.setVisible(!registerMode);
        btnToggle.setText(registerMode ? "Already registered? Log in" : "New here? Create an account");
        lblPortal.setText(portal + " Portal" + (registerMode ? " \u2013 Create account" : ""));
        lblPortal.setForeground("Trainee".equals(portal) ? Theme.PRIMARY
                : "Trainer".equals(portal) ? Theme.SUCCESS : Theme.WARNING);

        if (isTraineeReg) loadTrainersIfNeeded();
        revalidate();
        repaint();
    }

    private void loadTrainersIfNeeded() {
        if (trainersLoading || actions == null) return;
        trainersLoading = true;
        cbTrainer.removeAllItems();
        cbTrainer.addItem(LOADING);
        actions.loadTrainersForView();
        cbTrainer.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof UserProfile p) {
                    setText(p.name() + (p.gymName() != null && !p.gymName().isBlank() ? "  \u2014  " + p.gymName() : ""));
                }
                return this;
            }
        });
    }

    public void reset() {
        txtPass.setText("");
        txtCode.setText("");
        registerMode = false;
        portal = "Trainee";
        updateVisibility();
    }

    @Override public void showTrainers(List<UserProfile> trainers) {
        trainersLoading = false;
        cbTrainer.removeAllItems();
        if (trainers.isEmpty()) cbTrainer.addItem(NONE_YET);
        else for (UserProfile trainer : trainers) cbTrainer.addItem(trainer);
    }

    @Override public void showError(Throwable error) {
        trainersLoading = false;
        Theme.error(this, error);
    }

    @Override public void setSubmitEnabled(boolean enabled) { btnSubmit.setEnabled(enabled); }
    @Override public void setResetEnabled(boolean enabled) { btnForgot.setEnabled(enabled); }
    @Override public void focusEmail() { txtEmail.requestFocusInWindow(); }
    @Override public void loginSucceeded(UserProfile user) { onSuccess.accept(user); }
    @Override public void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    private void submit() {
        Object selection = cbTrainer.getSelectedItem();
        UserProfile trainer = selection instanceof UserProfile profile ? profile : null;
        if (actions != null) actions.submit(new LoginRequest(txtName.getText().trim(), txtEmail.getText().trim(),
                new String(txtPass.getPassword()), portal, new String(txtCode.getPassword()).trim(), registerMode,
                trainer));
    }
}
