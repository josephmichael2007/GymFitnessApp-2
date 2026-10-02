package com.fitness.view;

import com.fitness.model.AssignedExercise;
import com.fitness.model.Stats;
import com.fitness.model.TraineeDashboardData;
import com.fitness.model.UserProfile;
import com.fitness.model.Workout;
import com.fitness.mvc.TraineeActions;
import com.fitness.mvc.TraineeScreen;
import com.fitness.mvc.WorkoutForm;
import com.fitness.util.AppConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** What a trainee sees: exercises their trainer assigned, a workout log form, progress ring, charts, history. */
public class TraineeDashboard extends JPanel implements TraineeScreen {
    private TraineeActions actions;

    private final JTextField txtExercise = new JTextField();
    private final JTextField txtDetails = new JTextField("3 sets x 12 reps");
    private final JTextField txtCalories = new JTextField("150");
    private final JComboBox<String> cbCategory =
            new JComboBox<>(new String[]{"Cardio", "Strength", "HIIT", "Yoga/Flexibility", "Other"});
    private final JComboBox<String> cbIntensity = new JComboBox<>(new String[]{"Low", "Medium", "High"});
    private final JCheckBox chkDone = new JCheckBox("Completed", true);
    private final JButton btnAdd = Theme.button("Add workout", Theme.SUCCESS);

    private final DefaultTableModel assignedModel = new DefaultTableModel(
            new String[]{"Exercise", "Category", "Prescription", "Set by"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable assignedTable = new JTable(assignedModel);
    private final JButton btnLogAssigned = Theme.button("Log selected", Theme.PRIMARY);

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Date", "Exercise", "Category", "Details", "Intensity", "Calories", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final CircularProgressPanel ring = new CircularProgressPanel();
    private final BarChartPanel bars = new BarChartPanel("Last 7 days (kcal)");
    private final DonutChartPanel donut = new DonutChartPanel("By category");
    private final JButton btnChangeAssignment = Theme.button("Change gym / trainer", Theme.PRIMARY);

    private int goal = AppConfig.defaultDailyGoal();
    private TraineeDashboardData currentData;

    public TraineeDashboard(UserProfile user, Runnable onLogout) {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        add(Theme.header("My Workouts", "Signed in as " + user.name() + "  (" + user.email() + ")", onLogout),
                BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(14, 14));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel assignmentBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        assignmentBar.setOpaque(false);
        assignmentBar.add(btnChangeAssignment);
        body.add(assignmentBar, BorderLayout.NORTH);

        JPanel workspace = new JPanel(new BorderLayout(14, 14));
        workspace.setOpaque(false);

        // Left column: assigned exercises, log form, ring
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(buildAssignedCard());
        left.add(Box.createVerticalStrut(14));
        left.add(buildForm());
        left.add(Box.createVerticalStrut(14));
        JPanel ringCard = Theme.card("Today's goal", new BorderLayout());
        ring.setPreferredSize(new Dimension(220, 200));
        ringCard.add(ring, BorderLayout.CENTER);
        left.add(ringCard);
        JScrollPane leftScroll = new JScrollPane(left);
        leftScroll.setBorder(null);
        leftScroll.getVerticalScrollBar().setUnitIncrement(14);
        leftScroll.setPreferredSize(new Dimension(360, 0));
        workspace.add(leftScroll, BorderLayout.WEST);

        // Right column: charts + table
        JPanel charts = new JPanel(new GridLayout(1, 2, 14, 0));
        charts.setOpaque(false);
        JPanel c1 = Theme.card(null, new BorderLayout());
        c1.add(bars);
        JPanel c2 = Theme.card(null, new BorderLayout());
        c2.add(donut);
        charts.add(c1);
        charts.add(c2);
        charts.setPreferredSize(new Dimension(0, 240));

        Theme.styleTable(table);
        JPanel tableCard = Theme.card("History", new BorderLayout(0, 8));
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton btnDelete = Theme.button("Delete selected", Theme.DANGER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(btnDelete);
        tableCard.add(bottom, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(0, 14));
        right.setOpaque(false);
        right.add(charts, BorderLayout.NORTH);
        right.add(tableCard, BorderLayout.CENTER);
        workspace.add(right, BorderLayout.CENTER);
        body.add(workspace, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        btnAdd.addActionListener(e -> {
            if (actions != null) actions.addWorkout(new WorkoutForm(txtExercise.getText(),
                    (String) cbCategory.getSelectedItem(), txtDetails.getText(), (String) cbIntensity.getSelectedItem(),
                    txtCalories.getText(), chkDone.isSelected()));
        });
        btnDelete.addActionListener(e -> { if (actions != null) actions.deleteWorkout(table.getSelectedRow()); });
        btnLogAssigned.addActionListener(e -> {
            if (actions != null) actions.logAssignedExercise(assignedTable.getSelectedRow());
        });
        btnChangeAssignment.addActionListener(e -> {
            if (actions != null) actions.loadTrainersForSelection();
        });
    }

    @Override public void setActions(TraineeActions actions) { this.actions = actions; }

    private JPanel buildAssignedCard() {
        JPanel card = Theme.card("Assigned by your trainer", new BorderLayout(0, 8));
        Theme.styleTable(assignedTable);
        assignedTable.setPreferredScrollableViewportSize(new Dimension(300, 90));
        card.add(new JScrollPane(assignedTable), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(btnLogAssigned);
        card.add(bottom, BorderLayout.SOUTH);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        return card;
    }

    private JPanel buildForm() {
        JPanel card = Theme.card("Log a workout", new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 4, 5, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        String[] labels = {"Exercise", "Category", "Details", "Intensity", "Calories burned"};
        JComponent[] fields = {txtExercise, cbCategory, txtDetails, cbIntensity, txtCalories};
        for (int i = 0; i < labels.length; i++) {
            c.gridy = i; c.gridx = 0; c.weightx = 0;
            card.add(new JLabel(labels[i]), c);
            c.gridx = 1; c.weightx = 1;
            card.add(fields[i], c);
        }
        c.gridy = labels.length; c.gridx = 0; c.gridwidth = 2;
        card.add(chkDone, c);
        c.gridy++;
        card.add(btnAdd, c);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        return card;
    }

    @Override public void render(TraineeDashboardData view) {
        currentData = view;
        List<Workout> workouts = view.workouts();
        List<AssignedExercise> assigned = view.assigned();
        goal = view.profile().dailyGoal() != null ? view.profile().dailyGoal() : AppConfig.defaultDailyGoal();

        model.setRowCount(0);
        for (Workout w : workouts) {
            model.addRow(new Object[]{w.date(), w.exercise(), w.category(), w.details(), w.intensity(),
                    w.calories(), w.completed() ? "Completed" : "In progress"});
        }
        assignedModel.setRowCount(0);
        for (AssignedExercise a : assigned) {
            assignedModel.addRow(new Object[]{a.exerciseName(), a.category(), a.setsReps(), a.assignedBy()});
        }
        ring.setProgress(Stats.today(workouts), goal);
        bars.setData(Stats.last7Days(workouts), goal);
        donut.setData(Stats.byCategory(workouts));
    }

    @Override public void prefillAssignedExercise(AssignedExercise a) {
        txtExercise.setText(a.exerciseName());
        cbCategory.setSelectedItem(a.category());
        txtDetails.setText(a.setsReps());
    }

    @Override public boolean confirmDelete(Workout workout) {
        return JOptionPane.showConfirmDialog(this, "Delete this workout?", "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    @Override public void setAddEnabled(boolean enabled) { btnAdd.setEnabled(enabled); }
    @Override public void clearExercise() { txtExercise.setText(""); }
    @Override public void showError(Throwable error) { Theme.error(this, error); }
    @Override public void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    @Override public void showTrainerSelection(List<UserProfile> trainers) {
        if (currentData == null) return;
        List<String> gyms = trainers.stream().map(UserProfile::gymName)
                .filter(name -> name != null && !name.isBlank()).distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        if (gyms.isEmpty()) {
            showMessage("No trainers are available yet.", "Change gym / trainer", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JComboBox<String> gymChoice = new JComboBox<>(gyms.toArray(String[]::new));
        JComboBox<UserProfile> trainerChoice = new JComboBox<>();
        trainerChoice.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof UserProfile trainer) setText(trainer.name() + "  -  " + trainer.email());
                return this;
            }
        });
        Runnable updateTrainers = () -> {
            String gym = (String) gymChoice.getSelectedItem();
            trainerChoice.removeAllItems();
            trainers.stream().filter(trainer -> gym != null && gym.equals(trainer.gymName()))
                    .sorted(java.util.Comparator.comparing(UserProfile::name, String.CASE_INSENSITIVE_ORDER))
                    .forEach(trainerChoice::addItem);
        };
        gymChoice.addActionListener(e -> updateTrainers.run());

        String currentGym = currentData.profile().gymName();
        if (currentGym == null && currentData.profile().trainerUid() != null) {
            currentGym = trainers.stream().filter(t -> t.uid().equals(currentData.profile().trainerUid()))
                    .map(UserProfile::gymName).findFirst().orElse(null);
        }
        if (currentGym != null) gymChoice.setSelectedItem(currentGym);
        updateTrainers.run();
        for (int i = 0; i < trainerChoice.getItemCount(); i++) {
            if (trainerChoice.getItemAt(i).uid().equals(currentData.profile().trainerUid())) {
                trainerChoice.setSelectedIndex(i);
                break;
            }
        }

        JPanel choices = new JPanel(new GridLayout(2, 2, 8, 8));
        choices.add(new JLabel("Gym"));
        choices.add(gymChoice);
        choices.add(new JLabel("Trainer"));
        choices.add(trainerChoice);
        int result = JOptionPane.showConfirmDialog(this, choices, "Change gym / trainer",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION && trainerChoice.getSelectedItem() instanceof UserProfile trainer) {
            actions.updateAssignment((String) gymChoice.getSelectedItem(), trainer.uid());
        }
    }
}
