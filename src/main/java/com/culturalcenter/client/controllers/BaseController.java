package com.culturalcenter.client.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class BaseController {

    // статическая переменная для хранения ФИО текущего пользователя
    private static String currentFullName = "";

    // данные для редактирования профиля (заполняются в наследниках)
    protected String currentLogin;
    protected String currentRole;
    protected String currentFxmlPath;
    protected Button profileButton;  // будет установлен в наследниках

    // сохранить ФИО (вызвать один раз при входе)
    public static void setCurrentFullName(String fullName) {
        currentFullName = fullName;
    }

    // получить ФИО (если нужно где-то отдельно)
    public static String getCurrentFullName() {
        return currentFullName;
    }

    // обновить Label с ФИО
    protected void updateUserLabel(Label userLabel) {
        if (userLabel != null && currentFullName != null && !currentFullName.isEmpty()) {
            userLabel.setText(currentFullName);
        }
    }

    // инициализация данных для редактирования профиля (вызывать в наследниках)
    protected void initProfileData(String login, String role, String fxmlPath, Button profileButton) {
        this.currentLogin = login;
        this.currentRole = role;
        this.currentFxmlPath = fxmlPath;
        this.profileButton = profileButton;
    }

    // единый метод редактирования профиля для всех ролей
    @FXML
    protected void onEditProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/edit_profile.fxml"));
            Parent root = loader.load();

            EditProfileController controller = loader.getController();
            controller.initData(currentLogin, currentRole, currentFxmlPath);

            Scene scene = new Scene(root, 550, 650);
            Stage stage = (Stage) profileButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Редактирование профиля");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось открыть редактирование профиля");
        }
    }

    protected void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    protected void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    protected void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}