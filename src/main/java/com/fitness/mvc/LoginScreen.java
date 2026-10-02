package com.fitness.mvc;

import com.fitness.model.UserProfile;
import java.util.List;

public interface LoginScreen {
    void setActions(LoginActions actions);
    void showTrainers(List<UserProfile> trainers);
    void showError(Throwable error);
    void setSubmitEnabled(boolean enabled);
    void setResetEnabled(boolean enabled);
    void focusEmail();
    void loginSucceeded(UserProfile user);
    void showMessage(String message, String title, int messageType);
}
