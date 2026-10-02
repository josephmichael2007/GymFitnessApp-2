package com.fitness.mvc;

import com.fitness.model.AdminAccountStatus;
import com.fitness.model.GymCode;
import com.fitness.model.UserProfile;
import java.util.List;

public interface AdminScreen {
    void setActions(AdminActions actions);
    int selectedUserRow();
    String selectedRole();
    int selectedGymCodeRow();
    void renderUsers(List<UserProfile> users, String summary);
    String chooseRoleAssignment(UserProfile user, String newRole, List<UserProfile> users, List<GymCode> codes);
    boolean confirmRoleChange(UserProfile user, String newRole);
    boolean confirmDeleteUser(UserProfile user);
    void showAccountStatus(AdminAccountStatus status);
    void renderGymCodes(List<GymCode> codes);
    void clearGymCodeForm();
    boolean confirmDeleteGymCode(GymCode code);
    void showError(Throwable error);
    void showMessage(String message, String title, int messageType);
}
