package com.fitness.view;

import com.fitness.controller.TrainerController;
import com.fitness.model.AssignedExercise;
import com.fitness.model.Exercise;
import com.fitness.model.Stats;
import com.fitness.model.UserProfile;
import com.fitness.model.Workout;
import com.fitness.util.AppConfig;
import com.fitness.util.Async;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/** Trainer view: trainee progress overview, plus an exercise library, assignment and goal-setting tab. */
public class TrainerDashboard extends JPanel {
    private final TrainerController controller = new TrainerController();
    private final UserProfile trainer;

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
    private List<TrainerController.TraineeSummary> data = List.of();

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

    private List<Exercise> library = List.of();
    private List<AssignedExercise> traineeAssigned = List.of();

    public TrainerDashboard(UserProfile trainer, Runnable onLogout) {
        this.trainer = trainer;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        String subtitle = "Signed in as " + trainer.name()
                + (trainer.gymName() != null && !trainer.gymName().isBlank() ? "   |   " + trainer.gymName() : "");
        add(Theme.header("Trainer Dashboard", subtitle, onLogout), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Trainee Progress", buildOverviewTab());
        tabs.addTab("Exercises & Goals", buildExercisesTab());
        add(tabs, BorderLayout.CENTER);

        loadOverview();
        loadExerciseLibrary();
    }

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
            if (!e.getValueIsAdjusting()) showDetail(overview.getSelectedRow());
        });
        btnRefresh.addActionListener(e -> loadOverview());
        return body;
    }

    private void loadOverview() {
        btnRefresh.setEnabled(false);
        Async.run(() -> controller.loadTraineeOverview(trainer.uid()), list -> {
            btnRefresh.setEnabled(true);
            data = list;
            fillOverview();
            populateAssignDropdown();
        }, err -> { btnRefresh.setEnabled(true); Theme.error(this, err); });
    }

    private void fillOverview() {
        int goal = AppConfig.defaultDailyGoal();
        int active = 0;
        overviewModel.setRowCount(0);
        for (TrainerController.TraineeSummary t : data) {
            int g = t.profile().dailyGoal() != null ? t.profile().dailyGoal() : goal;
            int today = Stats.today(t.workouts());
            if (today > 0) active++;
            overviewModel.addRow(new Object[]{t.profile().name(), t.workouts().size(),
                    Stats.total(t.workouts()), today, (today * 100 / Math.max(g, 1)) + "%"});
        }
        lblSummary.setText(data.size() + " trainees  |  " + active + " active today");
        if (!data.isEmpty()) overview.setRowSelectionInterval(0, 0);
        else showDetail(-1);
    }

    private void showDetail(int row) {
        detailModel.setRowCount(0);
        int goal = AppConfig.defaultDailyGoal();
        if (row < 0 || row >= data.size()) {
            lblSelected.setText("Select a trainee");
            ring.setProgress(0, goal);
            bars.setData(new java.util.LinkedHashMap<>(), goal);
            donut.setData(new java.util.LinkedHashMap<>());
            return;
        }
        TrainerController.TraineeSummary t = data.get(row);
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

        btnCreate.addActionListener(e -> createExercise());
        btnDelete.addActionListener(e -> deleteExercise());
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

        cbAssignTrainee.addActionListener(e -> loadTraineeAssigned());
        btnAssign.addActionListener(e -> assignExercise());
        btnSetGoal.addActionListener(e -> saveGoal());
        btnUnassign.addActionListener(e -> removeAssignment());
        return card;
    }

    private void loadExerciseLibrary() {
        Async.run(controller::loadExerciseLibrary, list -> {
            library = list;
            libraryModel.setRowCount(0);
            for (Exercise e : list) {
                libraryModel.addRow(new Object[]{e.name(), e.category(), e.setsReps(), e.notes()});
            }
            if (!list.isEmpty()) libraryTable.setRowSelectionInterval(0, 0);
        }, err -> Theme.error(this, err));
    }

    private void createExercise() {
        String name = txtExName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter an exercise name.");
            return;
        }
        Exercise e = new Exercise(null, name, (String) cbExCategory.getSelectedItem(),
                (Integer) spSets.getValue(), (Integer) spReps.getValue(), txtExNotes.getText().trim());
        Async.run(() -> controller.createExercise(e), ex -> { txtExName.setText(""); txtExNotes.setText(""); loadExerciseLibrary(); },
                err -> Theme.error(this, err));
    }

    private void deleteExercise() {
        int row = libraryTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an exercise first.");
            return;
        }
        String id = library.get(row).id();
        Async.run(() -> { controller.deleteExercise(id); return null; }, v -> loadExerciseLibrary(),
                err -> Theme.error(this, err));
    }

    /** Only ever this trainer's own trainees - the data-isolation boundary lives in the query, not here. */
    private void populateAssignDropdown() {
        UserProfile previouslySelected = selectedTrainee();
        cbAssignTrainee.removeAllItems();
        for (TrainerController.TraineeSummary t : data) cbAssignTrainee.addItem(t.profile());
        if (data.isEmpty()) return;
        if (previouslySelected != null) {
            for (int i = 0; i < cbAssignTrainee.getItemCount(); i++) {
                if (((UserProfile) cbAssignTrainee.getItemAt(i)).uid().equals(previouslySelected.uid())) {
                    cbAssignTrainee.setSelectedIndex(i);
                    return;
                }
            }
        }
        cbAssignTrainee.setSelectedIndex(0); // fires the combo's listener, which loads this trainee's assignments
    }

    private UserProfile selectedTrainee() {
        return (UserProfile) cbAssignTrainee.getSelectedItem();
    }

    private void loadTraineeAssigned() {
        UserProfile t = selectedTrainee();
        traineeAssignedModel.setRowCount(0);
        if (t == null) return;
        spGoal.setValue(t.dailyGoal() != null ? t.dailyGoal() : AppConfig.defaultDailyGoal());
        Async.run(() -> controller.getAssignedExercises(t.uid()), list -> {
            traineeAssigned = list;
            for (AssignedExercise a : list) {
                traineeAssignedModel.addRow(new Object[]{a.exerciseName(), a.category(), a.setsReps()});
            }
        }, err -> Theme.error(this, err));
    }

    private void assignExercise() {
        UserProfile t = selectedTrainee();
        int row = libraryTable.getSelectedRow();
        if (t == null) {
            JOptionPane.showMessageDialog(this, "No trainees to assign to yet.");
            return;
        }
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an exercise from the library first.");
            return;
        }
        Exercise ex = library.get(row);
        AssignedExercise a = new AssignedExercise(null, ex.name(), ex.category(), ex.sets(), ex.reps(),
                ex.notes(), trainer.name(), LocalDate.now().toString());
        Async.run(() -> { controller.assignExercise(t.uid(), a); return null; }, v -> {
            loadTraineeAssigned();
            JOptionPane.showMessageDialog(this, ex.name() + " assigned to " + t.name() + ".");
        }, err -> Theme.error(this, err));
    }

    private void removeAssignment() {
        UserProfile t = selectedTrainee();
        int row = traineeAssignedTable.getSelectedRow();
        if (t == null || row < 0) {
            JOptionPane.showMessageDialog(this, "Select an assignment to remove.");
            return;
        }
        String id = traineeAssigned.get(row).id();
        Async.run(() -> { controller.removeAssignedExercise(t.uid(), id); return null; }, v -> loadTraineeAssigned(),
                err -> Theme.error(this, err));
    }

    private void saveGoal() {
        UserProfile t = selectedTrainee();
        if (t == null) return;
        int goal = (Integer) spGoal.getValue();
        Async.run(() -> { controller.setDailyGoal(t.uid(), goal); return null; }, v -> {
            JOptionPane.showMessageDialog(this, "Daily goal for " + t.name() + " set to " + goal + " kcal.");
            loadOverview();
        }, err -> Theme.error(this, err));
    }
}
