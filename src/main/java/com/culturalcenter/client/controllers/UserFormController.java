package com.culturalcenter.client.controllers;

import javafx.scene.text.Text;
import com.culturalcenter.client.network.NetworkClient;
import com.culturalcenter.server.network.Response;
import com.culturalcenter.server.serializer.Serializer;
import com.culturalcenter.client.models.UserItem;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class UserFormController extends BaseController {

    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Text titleText;

    private UserItem editingUser;  // ← ИСПРАВЛЕНО
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("ORGANIZER", "DIRECTOR"));
    }

    public void initData(UserItem user, Runnable onSaveCallback) {  // ← ИСПРАВЛЕНО
        this.editingUser = user;
        this.onSaveCallback = onSaveCallback;

        if (user != null) {
            titleText.setText("Редактирование пользователя");
            loginField.setText(user.getLogin());
            loginField.setDisable(true);
            fullNameField.setText(user.getFullName());
            emailField.setText(user.getEmail());
            phoneField.setText(user.getPhone());
            roleCombo.setValue(user.getRole());
            passwordField.setVisible(false);
        } else {
            titleText.setText("Добавление пользователя");
        }
    }

    @FXML
    private void onSave() {
        String login = loginField.getText().trim();
        String password = passwordField.getText();
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String role = roleCombo.getValue();

        if (fullName.isEmpty()) {
            showError("Ошибка", "Введите ФИО");
            return;
        }
        if (email.isEmpty() || !email.contains("@")) {
            showError("Ошибка", "Введите корректный Email");
            return;
        }

        if (editingUser == null) {
            if (login.isEmpty() || login.length() < 4) {
                showError("Ошибка", "Логин не менее 4 символов");
                return;
            }
            if (password.isEmpty() || password.length() < 6) {
                showError("Ошибка", "Пароль не менее 6 символов");
                return;
            }
            if (role == null) {
                showError("Ошибка", "Выберите роль");
                return;
            }

            Map<String, String> userData = new HashMap<>();
            userData.put("login", login);
            userData.put("password", password);
            userData.put("fullName", fullName);
            userData.put("email", email);
            userData.put("phone", phone);
            userData.put("role", role);

            Response response = NetworkClient.sendRequest("CREATE_USER", Serializer.toJson(userData));
            if (response.isSuccess()) {
                showInfo("Успех", response.getMessage());
                closeWindow();
                if (onSaveCallback != null) onSaveCallback.run();
            } else {
                showError("Ошибка", response.getMessage());
            }
        } else {
            Map<String, String> userData = new HashMap<>();
            userData.put("userId", String.valueOf(editingUser.getUserId()));
            userData.put("fullName", fullName);
            userData.put("email", email);
            userData.put("phone", phone);
            userData.put("role", role);

            Response response = NetworkClient.sendRequest("UPDATE_USER_BY_ADMIN", Serializer.toJson(userData));
            if (response.isSuccess()) {
                showInfo("Успех", response.getMessage());
                closeWindow();
                if (onSaveCallback != null) onSaveCallback.run();
            } else {
                showError("Ошибка", response.getMessage());
            }
        }
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) fullNameField.getScene().getWindow();
        stage.close();
    }
}