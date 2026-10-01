package com.fitness.view;

import com.fitness.controller.AdminController;
import com.fitness.model.GymCode;
import com.fitness.model.Stats;
import com.fitness.model.UserProfile;
import com.fitness.model.Workout;
import com.fitness.util.AppConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Admin view: every account across every gym, role changes, account removal, and gym invite codes. */
public class AdminDashboard extends JPanel {
    public interface Actions {
        void refreshUsers();
        void changeSelectedRole();
        void showSelectedAccountStatus();
        void deleteSelectedUser();
        void refreshGymCodes();
        void createGymCodeRequested(String gymName, String customCode);
        void toggleSelectedGymCode();
        void deleteSelectedGymCode();
    }

    private Actions actions;

    // ---- Tab 1: accounts ----
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Name", "Email", "Role", "Daily goal", "Trainer / Gym"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JComboBox<String> cbNewRole = new JComboBox<>(new String[]{"Trainee", "Trainer"});
    private final JLabel lblSummary = new JLabel(" ");
    private List<UserProfile> users = List.of();

    // ---- Tab 2: gym codes ----
    private final JTextField txtGymName = new JTextField();
    private final JTextField txtCustomCode = new JTextField();
    private final DefaultTableModel codeModel = new DefaultTableModel(
            new String[]{"Gym", "Code", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable codeTable = new JTable(codeModel);
    private List<GymCode> codes = List.of();

    public AdminDashboard(UserProfile admin, Runnable onLogout) {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        add(Theme.header("Admin Dashboard", "Signed in as " + admin.name(), onLogout), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Accounts", buildAccountsTab());
        tabs.addTab("Gym Codes", buildGymCodesTab());
        add(tabs, BorderLayout.CENTER);

    }

    public void setActions(Actions actions) { this.actions = actions; }

    // ================================================================= tab 1

    private JPanel buildAccountsTab() {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 14, 14, 14));

        Theme.styleTable(table);
        JPanel card = Theme.card("All accounts", new BorderLayout(0, 10));
        lblSummary.setForeground(Theme.MUTED);
        JButton btnRefresh = Theme.button("Refresh", Theme.PRIMARY);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblSummary, BorderLayout.CENTER);
        top.add(btnRefresh, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);
        card.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.add(new JLabel("Change role to:"));
        actions.add(cbNewRole);
        JButton btnChangeRole = Theme.button("Update role", Theme.SUCCESS);
        JButton btnViewStatus = Theme.button("View status", Theme.PRIMARY);
        JButton btnDelete = Theme.button("Delete account", Theme.DANGER);
        actions.add(btnChangeRole);
        actions.add(btnViewStatus);
        actions.add(btnDelete);
        card.add(actions, BorderLayout.SOUTH);

        body.add(card, BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> { if (this.actions != null) this.actions.refreshUsers(); });
        btnChangeRole.addActionListener(e -> { if (this.actions != null) this.actions.changeSelectedRole(); });
        btnViewStatus.addActionListener(e -> { if (this.actions != null) this.actions.showSelectedAccountStatus(); });
        btnDelete.addActionListener(e -> { if (this.actions != null) this.actions.deleteSelectedUser(); });
        return body;
    }

    public int selectedUserRow() { return table.getSelectedRow(); }
    public String selectedRole() { return (String) cbNewRole.getSelectedItem(); }
    public int selectedGymCodeRow() { return codeTable.getSelectedRow(); }

    public void renderUsers(List<UserProfile> list, String summary) {
        users = list;
            Map<String, String> nameByUid = new HashMap<>();
            for (UserProfile u : list) nameByUid.put(u.uid(), u.name());

            model.setRowCount(0);
            lblSummary.setText(summary);
            for (UserProfile u : list) {
                String linked = u.isTrainee() ? nameByUid.getOrDefault(u.trainerUid(), "-")
                        : u.isTrainer() ? (u.gymName() == null ? "-" : u.gymName()) : "-";
                model.addRow(new Object[]{u.name(), u.email(), u.role(),
                        u.dailyGoal() == null ? "-" : u.dailyGoal(), linked});
            }
    }

    public String chooseRoleAssignment(UserProfile user, String newRole, List<UserProfile> users, List<GymCode> codes) {
        if ("Trainer".equals(newRole)) {
            JComboBox<GymCode> choice = new JComboBox<>(codes.toArray(GymCode[]::new));
            choice.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof GymCode gym) {
                        setText(gym.gymName() + " (" + gym.code() + ")" + (gym.active() ? "" : " - inactive"));
                    }
                    return this;
                }
            });
            if (codes.isEmpty()) {
                showMessage("Create a gym code before assigning a trainer.", "", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
            if (JOptionPane.showConfirmDialog(this, choice, "Select gym for " + user.name(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return null;
            return ((GymCode) choice.getSelectedItem()).code();
        } else {
            List<UserProfile> trainers = users.stream()
                    .filter(UserProfile::isTrainer)
                    .filter(trainer -> !trainer.uid().equals(user.uid()))
                    .toList();
            if (trainers.isEmpty()) {
                showMessage("Register or assign a trainer before assigning a trainee.", "", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
            JComboBox<UserProfile> choice = new JComboBox<>(trainers.toArray(UserProfile[]::new));
            choice.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof UserProfile trainer) {
                        setText(trainer.name() + (trainer.gymName() == null || trainer.gymName().isBlank()
                                ? "" : " - " + trainer.gymName()));
                    }
                    return this;
                }
            });
                if (JOptionPane.showConfirmDialog(this, choice, "Select trainer for " + user.name(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return null;
                return ((UserProfile) choice.getSelectedItem()).uid();
        }
            }

            public boolean confirmRoleChange(UserProfile user, String newRole) {
            return JOptionPane.showConfirmDialog(this, "Change " + user.name() + "'s role to " + newRole + "?", "Confirm",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
            }

            public boolean confirmDeleteUser(UserProfile user) {
            return JOptionPane.showConfirmDialog(this,
                "Permanently delete " + user.name() + " (" + user.email() + ")? This removes their login and all data.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

            public void showAccountStatus(AdminController.AccountStatus status) {
        UserProfile user = status.profile();
        StringBuilder text = new StringBuilder();
        text.append(user.name()).append(" (").append(user.email()).append(")\n")
                .append("Role: ").append(user.role()).append('\n');

        if (user.isTrainee()) {
            int goal = user.dailyGoal() == null ? AppConfig.defaultDailyGoal() : user.dailyGoal();
            int today = Stats.today(status.workouts());
            long completed = status.workouts().stream().filter(Workout::completed).count();
            text.append("Trainer: ").append(nameByUid(user.trainerUid())).append('\n')
                    .append("Daily goal: ").append(goal).append(" kcal\n")
                    .append("Today's activity: ").append(today).append(" / ").append(goal).append(" kcal (")
                    .append(today * 100 / Math.max(goal, 1)).append("%)\n")
                    .append("Workouts: ").append(status.workouts().size()).append(" total, ")
                    .append(completed).append(" completed, ")
                    .append(status.workouts().size() - completed).append(" in progress\n\n")
                    .append("Workout history:\n");
            if (status.workouts().isEmpty()) text.append("No workouts logged.");
            for (Workout workout : status.workouts()) {
                text.append(workout.date()).append(" | ").append(workout.exercise()).append(" | ")
                        .append(workout.calories()).append(" kcal | ")
                        .append(workout.completed() ? "Completed" : "In progress").append('\n');
            }
        } else if (user.isTrainer()) {
            long active = status.trainees().stream().filter(trainee -> Stats.today(trainee.workouts()) > 0).count();
            text.append("Gym: ").append(user.gymName() == null ? "-" : user.gymName()).append('\n')
                    .append("Trainees: ").append(status.trainees().size()).append(" total, ")
                    .append(active).append(" active today\n\n")
                    .append("Trainee status:\n");
            if (status.trainees().isEmpty()) text.append("No trainees assigned.");
            for (AdminController.TraineeStatus trainee : status.trainees()) {
                int goal = trainee.profile().dailyGoal() == null
                        ? AppConfig.defaultDailyGoal() : trainee.profile().dailyGoal();
                int today = Stats.today(trainee.workouts());
                text.append(trainee.profile().name()).append(" | ")
                        .append(today > 0 ? "Active today" : "No activity today").append(" | ")
                        .append(today).append(" / ").append(goal).append(" kcal | ")
                        .append(trainee.workouts().size()).append(" workouts\n");
            }
        } else {
            text.append("No trainer or trainee activity status for this account.");
        }

        JTextArea details = new JTextArea(text.toString(), 18, 58);
        details.setEditable(false);
        details.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(details), "Account status", JOptionPane.INFORMATION_MESSAGE);
    }

    private String nameByUid(String uid) {
        return users.stream().filter(user -> user.uid().equals(uid)).map(UserProfile::name).findFirst().orElse("-");
    }

    // ================================================================= tab 2

    private JPanel buildGymCodesTab() {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        txtCustomCode.putClientProperty("JTextField.placeholderText", "Leave blank to auto-generate");

        c.gridy = 0; c.gridx = 0; c.weightx = 0;
        form.add(new JLabel("Gym name"), c);
        c.gridx = 1; c.weightx = 1;
        form.add(txtGymName, c);
        c.gridy = 1; c.gridx = 0; c.weightx = 0;
        form.add(new JLabel("Custom code"), c);
        c.gridx = 1; c.weightx = 1;
        form.add(txtCustomCode, c);
        JButton btnCreate = Theme.button("Create gym code", Theme.SUCCESS);
        c.gridy = 2; c.gridx = 0; c.gridwidth = 2;
        form.add(btnCreate, c);

        JPanel formCard = Theme.card("New gym", form.getLayout());
        formCard.setLayout(new BorderLayout());
        formCard.add(form, BorderLayout.CENTER);
        formCard.setPreferredSize(new Dimension(0, 150));

        Theme.styleTable(codeTable);
        JPanel listCard = Theme.card("Existing gym codes", new BorderLayout(0, 8));
        listCard.add(new JScrollPane(codeTable), BorderLayout.CENTER);
        JButton btnToggle = Theme.button("Activate / Deactivate", Theme.PRIMARY);
        JButton btnDelete = Theme.button("Delete code", Theme.DANGER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);
        bottom.add(btnToggle);
        bottom.add(btnDelete);
        listCard.add(bottom, BorderLayout.SOUTH);

        body.add(formCard, BorderLayout.NORTH);
        body.add(listCard, BorderLayout.CENTER);

        btnCreate.addActionListener(e -> {
            if (this.actions != null) this.actions.createGymCodeRequested(txtGymName.getText().trim(), txtCustomCode.getText().trim());
        });
        btnToggle.addActionListener(e -> { if (this.actions != null) this.actions.toggleSelectedGymCode(); });
        btnDelete.addActionListener(e -> { if (this.actions != null) this.actions.deleteSelectedGymCode(); });
        return body;
    }

    public void renderGymCodes(List<GymCode> list) {
        codes = list;
        codeModel.setRowCount(0);
        for (GymCode code : list) {
            codeModel.addRow(new Object[]{code.gymName(), code.code(), code.active() ? "Active" : "Deactivated"});
        }
    }

    public void clearGymCodeForm() { txtGymName.setText(""); txtCustomCode.setText(""); }
    public boolean confirmDeleteGymCode(GymCode code) {
        return JOptionPane.showConfirmDialog(this, "Delete the gym code for " + code.gymName() + "? Existing trainers keep "
                + "their access; this just stops new trainers from using this code.", "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
    public void showError(Throwable error) { Theme.error(this, error); }
    public void showMessage(String message, String title, int type) {
        JOptionPane.showMessageDialog(this, message, title, type);
    }
}
