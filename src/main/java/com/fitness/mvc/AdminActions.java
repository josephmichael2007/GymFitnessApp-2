package com.fitness.mvc;

public interface AdminActions {
    void refreshUsers();
    void changeSelectedRole();
    void showSelectedAccountStatus();
    void deleteSelectedUser();
    void refreshGymCodes();
    void createGymCodeRequested(String gymName, String customCode);
    void toggleSelectedGymCode();
    void deleteSelectedGymCode();
}
