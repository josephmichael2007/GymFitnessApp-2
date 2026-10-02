package com.fitness.controller;

import com.fitness.model.GymCode;
import com.fitness.model.AdminAccountStatus;
import com.fitness.model.AdminTraineeStatus;
import com.fitness.model.UserProfile;
import com.fitness.model.Workout;
import com.fitness.service.FirebaseService;
import com.fitness.util.Async;
import com.fitness.mvc.AdminActions;
import com.fitness.mvc.AdminScreen;

import java.util.List;
import javax.swing.JOptionPane;

/** Account management, plus creating/managing the per-gym trainer invite codes. */
public class AdminController implements AdminActions {

    private AdminScreen view;
    private List<UserProfile> users = List.of();
    private List<GymCode> gymCodes = List.of();

    public List<UserProfile> loadAllUsers() throws Exception {
        return FirebaseService.getAllUsers();
    }

    public AdminAccountStatus loadAccountStatus(UserProfile user) throws Exception {
        if (user.isTrainer()) {
            List<AdminTraineeStatus> trainees = new java.util.ArrayList<>();
            for (UserProfile trainee : FirebaseService.getTraineesOf(user.uid())) {
                trainees.add(new AdminTraineeStatus(trainee, FirebaseService.getWorkouts(trainee.uid())));
            }
            return new AdminAccountStatus(user, List.of(), trainees);
        }
        List<Workout> workouts = user.isTrainee() ? FirebaseService.getWorkouts(user.uid()) : List.of();
        return new AdminAccountStatus(user, workouts, List.of());
    }

    public void changeRole(String uid, String newRole, String assignmentId) throws Exception {
        FirebaseService.updateRole(uid, newRole, assignmentId);
    }

    public void deleteUser(String uid) throws Exception {
        FirebaseService.deleteUser(uid);
    }

    public List<GymCode> loadGymCodes() throws Exception {
        return FirebaseService.getGymCodes();
    }

    /** Returns the code that was created (useful when one was auto-generated). */
    public String createGymCode(String gymName, String customCode) throws Exception {
        if (gymName == null || gymName.isBlank()) throw new IllegalArgumentException("Enter a gym name.");
        return FirebaseService.createGymCode(gymName.trim(), customCode);
    }

    public void setGymCodeActive(String code, boolean active) throws Exception {
        FirebaseService.setGymCodeActive(code, active);
    }

    public void deleteGymCode(String code) throws Exception {
        FirebaseService.deleteGymCode(code);
    }

    public void attach(AdminScreen view) {
        this.view = view;
        view.setActions(this);
        refreshUsers();
        refreshGymCodes();
    }

    @Override public void refreshUsers() {
        Async.run(this::loadAllUsers, result -> {
            users = result;
            long trainers = users.stream().filter(UserProfile::isTrainer).count();
            long trainees = users.stream().filter(UserProfile::isTrainee).count();
            long admins = users.stream().filter(UserProfile::isAdmin).count();
            view.renderUsers(users, users.size() + " accounts  (" + trainees + " trainees, " + trainers
                    + " trainers, " + admins + " admins)");
        }, view::showError);
    }

    @Override public void refreshGymCodes() {
        Async.run(this::loadGymCodes, result -> {
            gymCodes = result;
            view.renderGymCodes(result);
        }, view::showError);
    }

    @Override public void changeSelectedRole() {
        int row = view.selectedUserRow();
        if (row < 0 || row >= users.size()) {
            view.showMessage("Select an account first.", "", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        UserProfile user = users.get(row);
        String newRole = view.selectedRole();
        if (newRole == null || newRole.equals(user.role())) return;
        String assignmentId = view.chooseRoleAssignment(user, newRole, users, gymCodes);
        if (assignmentId == null) return;
        if (!view.confirmRoleChange(user, newRole)) return;
        Async.run(() -> { changeRole(user.uid(), newRole, assignmentId); return null; },
                result -> refreshUsers(), view::showError);
    }

    @Override public void deleteSelectedUser() {
        int row = view.selectedUserRow();
        if (row < 0 || row >= users.size()) {
            view.showMessage("Select an account first.", "", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        UserProfile user = users.get(row);
        if (!view.confirmDeleteUser(user)) return;
        Async.run(() -> { deleteUser(user.uid()); return null; }, result -> refreshUsers(), view::showError);
    }

    @Override public void showSelectedAccountStatus() {
        int row = view.selectedUserRow();
        if (row < 0 || row >= users.size()) {
            view.showMessage("Select an account first.", "", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Async.run(() -> loadAccountStatus(users.get(row)), view::showAccountStatus, view::showError);
    }

    @Override public void createGymCodeRequested(String gymName, String customCode) {
        Async.run(() -> createGymCode(gymName, customCode), code -> {
            view.clearGymCodeForm();
            refreshGymCodes();
            view.showMessage("Gym code created: " + code
                    + "\nGive this to trainers at that gym so they can register.", "Gym code created",
                    JOptionPane.INFORMATION_MESSAGE);
        }, view::showError);
    }

    @Override public void toggleSelectedGymCode() {
        int row = view.selectedGymCodeRow();
        if (row < 0 || row >= gymCodes.size()) {
            view.showMessage("Select a gym code first.", "", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        GymCode code = gymCodes.get(row);
        Async.run(() -> { setGymCodeActive(code.code(), !code.active()); return null; },
                result -> refreshGymCodes(), view::showError);
    }

    @Override public void deleteSelectedGymCode() {
        int row = view.selectedGymCodeRow();
        if (row < 0 || row >= gymCodes.size()) {
            view.showMessage("Select a gym code first.", "", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        GymCode code = gymCodes.get(row);
        if (!view.confirmDeleteGymCode(code)) return;
        Async.run(() -> { deleteGymCode(code.code()); return null; }, result -> refreshGymCodes(), view::showError);
    }
}
