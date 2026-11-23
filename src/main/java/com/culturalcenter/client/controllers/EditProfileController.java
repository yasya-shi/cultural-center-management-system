package com.culturalcenter.client.controllers;

import com.culturalcenter.client.network.NetworkClient;
import com.culturalcenter.server.network.Response;
import com.culturalcenter.server.serializer.Serializer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class EditProfileController extends BaseController {

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    private String currentLogin;
    private String previousRole;
    private String previousFxmlPath;

    // упрощённый метод initData
    public void initData(String login, String role, String previousFxmlPath) {
        this.currentLogin = login;
        this.previousRole = role;
        this.previousFxmlPath = previousFxmlPath;
        loadUserData();
    }

    private void loadUserData() {
        Response response = NetworkClient.sendRequest("GET_USER", currentLogin);

        if (response.isSuccess()) {
            Map<String, Object> userData = Serializer.fromJson(response.getData(), Map.class);

            System.out.println("Получены данные: " + userData);

            fullNameField.setText((String) userData.get("fullName"));
            emailField.setText((String) userData.get("email"));

            Object phone = userData.get("phone");
            if (phone != null && !"null".equals(phone)) {
                phoneField.setText(phone.toString());
            }
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    @FXML
    private void onSave() {
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (fullName.isEmpty()) {
            showError("Ошибка", "ФИО не может быть пустым!");
            return;
        }
        if (email.isEmpty() || !email.contains("@")) {
            showError("Ошибка", "Введите корректный Email!");
            return;
        }
        if (!newPassword.isEmpty() && !newPassword.equals(confirmPassword)) {
            showError("Ошибка", "Пароли не совпадают!");
            return;
        }
        if (!newPassword.isEmpty() && newPassword.length() < 6) {
            showError("Ошибка", "Пароль должен быть не менее 6 символов!");
            return;
        }

        Map<String, String> updateData = new HashMap<>();
        updateData.put("login", currentLogin);
        updateData.put("fullName", fullName);
        updateData.put("email", email);
        updateData.put("phone", phone);
        if (!newPassword.isEmpty()) {
            updateData.put("newPassword", newPassword);
        }

        String jsonData = Serializer.toJson(updateData);

        Response response = NetworkClient.sendRequest("UPDATE_USER", jsonData);

        if (response.isSuccess()) {
            showInfo("Успех", response.getMessage());
            BaseController.setCurrentFullName(fullName);
            goBack();
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        goBack();
    }

    private void goBack() {
        try {
            Response response = NetworkClient.sendRequest("GET_USER", currentLogin);

            Map<String, Object> userData;
            if (response.isSuccess()) {
                userData = Serializer.fromJson(response.getData(), Map.class);
            } else {
                userData = new HashMap<>();
                userData.put("role", previousRole);
                userData.put("login", currentLogin);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(previousFxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller instanceof OrganizerController) {
                ((OrganizerController) controller).initUserData(currentLogin, userData);
            } else if (controller instanceof DirectorController) {
                ((DirectorController) controller).initUserData(currentLogin, userData);
            } else if (controller instanceof AdminController) {
                ((AdminController) controller).initUserData(currentLogin, userData);
            }

            Scene scene = new Scene(root, 750, 450);
            Stage stage = (Stage) fullNameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Панель управления - " + previousRole);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось вернуться назад: " + e.getMessage());
        }
    }
}