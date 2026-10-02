package com.fitness.view;

import com.fitness.model.AssignedExercise;
import com.fitness.model.Exercise;
import com.fitness.model.Stats;
import com.fitness.model.TrainerTraineeSummary;
import com.fitness.model.UserProfile;
import com.fitness.model.Workout;
import com.fitness.util.AppConfig;
import com.fitness.mvc.ExerciseForm;
import com.fitness.mvc.TrainerActions;
import com.fitness.mvc.TrainerScreen;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Trainer view: trainee progress overview, plus an exercise library, assignment and goal-setting tab. */
public class TrainerDashboard extends JPanel implements TrainerScreen {
    private TrainerActions actions;

    // ---- Tab 1: overview ----
    private final DefaultTableModel overviewModel = new DefaultTableModel(
            new String[]{"Trainee", "Workouts", "Total kcal", "Today kcal", "Goal %"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable overview = new JTable(overviewModel);
    private final DefaultTableModel detailModel = new DefaultTableModel(
            new String[]{"Date", "Exercise", "Category", "Details", "Intensity", "Calories", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable detailTable = new JTable(detailModel);
    private final CircularProgressPanel ring = new CircularProgressPanel();
    private final BarChartPanel bars = new BarChartPanel("Last 7 days (kcal)");
    private final DonutChartPanel donut = new DonutChartPanel("By category");
    private final JLabel lblSelected = new JLabel("Select a trainee");
    private final JLabel lblSummary = new JLabel(" ");
    private final JButton btnRefresh = Theme.button("Refresh", Theme.PRIMARY);

    // ---- Tab 2: exercises & goals ----
    private final JTextField txtExName = new JTextField();
    private final JComboBox<String> cbExCategory =
            new JComboBox<>(new String[]{"Cardio", "Strength", "HIIT", "Yoga/Flexibility", "Other"});
    private final JSpinner spSets = new JSpinner(new SpinnerNumberModel(3, 1, 20, 1));
    private final JSpinner spReps = new JSpinner(new SpinnerNumberModel(12, 1, 100, 1));
    private final JTextField txtExNotes = new JTextField();
    private final DefaultTableModel libraryModel = new DefaultTableModel(
            new String[]{"Exercise", "Category", "Sets x Reps", "Notes"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable libraryTable = new JTable(libraryModel);

    private final JComboBox<UserProfile> cbAssignTrainee = new JComboBox<>();
    private final JSpinner spGoal = new JSpinner(new SpinnerNumberModel(AppConfig.defaultDailyGoal(), 100, 10000, 50));
    private final DefaultTableModel traineeAssignedModel = new DefaultTableModel(
            new String[]{"Exercise", "Category", "Prescription"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable traineeAssignedTable = new JTable(traineeAssignedModel);


    public TrainerDashboard(UserProfile trainer, Runnable onLogout) {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        String subtitle = "Signed in as " + trainer.name()
                + (trainer.gymName() != null && !trainer.gymName().isBlank() ? "   |   " + trainer.gymName() : "");
        add(Theme.header("Trainer Dashboard", subtitle, onLogout), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Trainee Progress", buildOverviewTab());
        tabs.addTab("Exercises & Goals", buildExercisesTab());
        add(tabs, BorderLayout.CENTER);

    }

    @Override public void setActions(TrainerActions actions) { this.actions = actions; }

    // ================================================================= tab 1

    private JPanel buildOverviewTab() {
        JPanel body = new JPanel(new BorderLayout(14, 14));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 14, 14, 14));

        Theme.styleTable(overview);
        JPanel left = Theme.card("All trainees", new BorderLayout(0, 8));
        lblSummary.setForeground(Theme.MUTED);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblSummary, BorderLayout.CENTER);
        top.add(btnRefresh, BorderLayout.EAST);
        left.add(top, BorderLayout.NORTH);
        left.add(new JScrollPane(overview), BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(470, 0));
        body.add(left, BorderLayout.WEST);

        lblSelected.setFont(Theme.font(Font.BOLD, 17));
        lblSelected.setForeground(Theme.TEXT);

        JPanel viz = new JPanel(new GridLayout(1, 3, 12, 0));
        viz.setOpaque(false);
        JPanel r = Theme.card(null, new BorderLayout());
        r.add(ring);
        JPanel b = Theme.card(null, new BorderLayout());
        b.add(bars);
        JPanel d = Theme.card(null, new BorderLayout());
        d.add(donut);
        viz.add(r); viz.add(b); viz.add(d);
        viz.setPreferredSize(new Dimension(0, 230));

        Theme.styleTable(detailTable);
        JPanel history = Theme.card("Workout history", new BorderLayout());
        history.add(new JScrollPane(detailTable), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(0, 12));
        right.setOpaque(false);
        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);
        north.add(lblSelected, BorderLayout.NORTH);
        north.add(viz, BorderLayout.CENTER);
        right.add(north, BorderLayout.NORTH);
        right.add(history, BorderLayout.CENTER);
        body.add(right, BorderLayout.CENTER);

        overview.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && actions != null) actions.showSelectedTrainee(overview.getSelectedRow());
        });
        btnRefresh.addActionListener(e -> { if (actions != null) actions.refreshOverview(); });
        return body;
    }

    @Override public void renderOverview(List<TrainerTraineeSummary> data) {
        int goal = AppConfig.defaultDailyGoal();
        int active = 0;
        overviewModel.setRowCount(0);
        for (var t : data) {
            int g = t.profile().dailyGoal() != null ? t.profile().dailyGoal() : goal;
            int today = Stats.today(t.workouts());
            if (today > 0) active++;
            overviewModel.addRow(new Object[]{t.profile().name(), t.workouts().size(),
                    Stats.total(t.workouts()), today, (today * 100 / Math.max(g, 1)) + "%"});
        }
        lblSummary.setText(data.size() + " trainees  |  " + active + " active today");
        populateAssignDropdown(data);
        if (!data.isEmpty()) overview.setRowSelectionInterval(0, 0);
        else renderSelectedTrainee(null);
    }

    @Override public void renderSelectedTrainee(TrainerTraineeSummary t) {
        detailModel.setRowCount(0);
        int goal = AppConfig.defaultDailyGoal();
        if (t == null) {
            lblSelected.setText("Select a trainee");
            ring.setProgress(0, goal);
            bars.setData(new java.util.LinkedHashMap<>(), goal);
            donut.setData(new java.util.LinkedHashMap<>());
            return;
        }
        int g = t.profile().dailyGoal() != null ? t.profile().dailyGoal() : goal;
        lblSelected.setText(t.profile().name() + "  -  " + t.profile().email() + "   (goal: " + g + " kcal/day)");
        ring.setProgress(Stats.today(t.workouts()), g);
        bars.setData(Stats.last7Days(t.workouts()), g);
        donut.setData(Stats.byCategory(t.workouts()));
        for (Workout w : t.workouts()) {
            detailModel.addRow(new Object[]{w.date(), w.exercise(), w.category(), w.details(), w.intensity(),
                    w.calories(), w.completed() ? "Completed" : "In progress"});
        }
    }

    // ================================================================= tab 2

    private JPanel buildExercisesTab() {
        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 14, 14, 14));
        body.add(buildLibraryCard());
        body.add(buildAssignCard());
        return body;
    }

    private JPanel buildLibraryCard() {
        JPanel card = Theme.card("Exercise library", new BorderLayout(0, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        String[] labels = {"Name", "Category", "Sets", "Reps", "Notes"};
        JComponent[] fields = {txtExName, cbExCategory, spSets, spReps, txtExNotes};
        for (int i = 0; i < labels.length; i++) {
            c.gridy = i; c.gridx = 0; c.weightx = 0;
            form.add(new JLabel(labels[i]), c);
            c.gridx = 1; c.weightx = 1;
            form.add(fields[i], c);
        }
        JButton btnCreate = Theme.button("Add to library", Theme.SUCCESS);
        c.gridy = labels.length; c.gridx = 0; c.gridwidth = 2;
        form.add(btnCreate, c);

        Theme.styleTable(libraryTable);
        JButton btnDelete = Theme.button("Remove selected", Theme.DANGER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(btnDelete);

        card.add(form, BorderLayout.NORTH);
        card.add(new JScrollPane(libraryTable), BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        btnCreate.addActionListener(e -> {
            if (actions != null) actions.createExercise(new ExerciseForm(txtExName.getText(),
                    (String) cbExCategory.getSelectedItem(), (Integer) spSets.getValue(), (Integer) spReps.getValue(),
                    txtExNotes.getText()));
        });
        btnDelete.addActionListener(e -> {
            if (actions != null) actions.deleteSelectedExercise(libraryTable.getSelectedRow());
        });
        return card;
    }

    private JPanel buildAssignCard() {
        JPanel card = Theme.card("Assign & set goals", new BorderLayout(0, 10));

        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0; c.gridx = 0; c.weightx = 0;
        top.add(new JLabel("Trainee"), c);
        c.gridx = 1; c.weightx = 1;
        cbAssignTrainee.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof UserProfile p) setText(p.name());
                return this;
            }
        });
        top.add(cbAssignTrainee, c);

        JButton btnAssign = Theme.button("Assign selected exercise", Theme.PRIMARY);
        c.gridy = 1; c.gridx = 0; c.gridwidth = 2; c.weightx = 1;
        top.add(btnAssign, c);

        c.gridy = 2; c.gridwidth = 1; c.gridx = 0; c.weightx = 0;
        top.add(new JLabel("Daily goal (kcal)"), c);
        c.gridx = 1; c.weightx = 1;
        top.add(spGoal, c);
        JButton btnSetGoal = Theme.button("Save goal", Theme.SUCCESS);
        c.gridy = 3; c.gridx = 0; c.gridwidth = 2;
        top.add(btnSetGoal, c);

        Theme.styleTable(traineeAssignedTable);
        JButton btnUnassign = Theme.button("Remove assignment", Theme.DANGER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(btnUnassign);

        JPanel assignedCard = Theme.card("This trainee's assigned exercises", new BorderLayout(0, 8));
        assignedCard.add(new JScrollPane(traineeAssignedTable), BorderLayout.CENTER);
        assignedCard.add(bottom, BorderLayout.SOUTH);

        card.add(top, BorderLayout.NORTH);
        card.add(assignedCard, BorderLayout.CENTER);

        cbAssignTrainee.addActionListener(e -> {
            if (actions != null) actions.traineeSelectionChanged((UserProfile) cbAssignTrainee.getSelectedItem());
        });
        btnAssign.addActionListener(e -> {
            if (actions != null) actions.assignSelectedExercise(libraryTable.getSelectedRow());
        });
        btnSetGoal.addActionListener(e -> {
            if (actions != null) actions.saveDailyGoal((Integer) spGoal.getValue());
        });
        btnUnassign.addActionListener(e -> {
            if (actions != null) actions.removeSelectedAssignment(traineeAssignedTable.getSelectedRow());
        });
        return card;
    }

    @Override public void renderExerciseLibrary(List<Exercise> library) {
        libraryModel.setRowCount(0);
        for (Exercise exercise : library) {
            libraryModel.addRow(new Object[]{exercise.name(), exercise.category(), exercise.setsReps(), exercise.notes()});
        }
        if (!library.isEmpty()) libraryTable.setRowSelectionInterval(0, 0);
    }

    @Override public void renderAssignedExercises(List<AssignedExercise> assigned) {
        traineeAssignedModel.setRowCount(0);
        for (AssignedExercise exercise : assigned) {
            traineeAssignedModel.addRow(new Object[]{exercise.exerciseName(), exercise.category(), exercise.setsReps()});
        }
    }

    private void populateAssignDropdown(List<TrainerTraineeSummary> data) {
        UserProfile previouslySelected = (UserProfile) cbAssignTrainee.getSelectedItem();
        cbAssignTrainee.removeAllItems();
        for (var trainee : data) cbAssignTrainee.addItem(trainee.profile());
        if (data.isEmpty()) return;
        if (previouslySelected != null) {
            for (int i = 0; i < cbAssignTrainee.getItemCount(); i++) {
                if (((UserProfile) cbAssignTrainee.getItemAt(i)).uid().equals(previouslySelected.uid())) {
                    cbAssignTrainee.setSelectedIndex(i);
                    return;
                }
            }
        }
        cbAssignTrainee.setSelectedIndex(0);
    }

    @Override public void setDailyGoalValue(Integer goal) {
        spGoal.setValue(goal != null ? goal : AppConfig.defaultDailyGoal());
    }

    @Override public void clearExerciseForm() { txtExName.setText(""); txtExNotes.setText(""); }
    @Override public void showMessage(String message) { JOptionPane.showMessageDialog(this, message); }
    @Override public void showError(Throwable error) { Theme.error(this, error); }

}
