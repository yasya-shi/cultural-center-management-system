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
import java.util.regex.Pattern;

public class RegisterController extends BaseController {

    @FXML
    private TextField loginField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private void onRegister() {
        String login = loginField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        // ========== ВАЛИДАЦИЯ (на клиенте) ==========
        if (login.isEmpty() || login.length() < 4) {
            showError("Ошибка", "Логин должен содержать не менее 4 символов!");
            return;
        }
        if (!Pattern.matches("[a-zA-Zа-яА-ЯёЁ0-9]+", login)) {
            showError("Ошибка", "Логин может содержать только буквы и цифры!");
            return;
        }
        if (password.isEmpty() || password.length() < 6) {
            showError("Ошибка", "Пароль должен содержать не менее 6 символов!");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Ошибка", "Пароли не совпадают!");
            clearFields();
            return;
        }
        if (fullName.isEmpty()) {
            showError("Ошибка", "Введите ФИО!");
            return;
        }
        if (email.isEmpty() || !Pattern.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", email)) {
            showError("Ошибка", "Введите корректный Email!");
            return;
        }
        if (!phone.isEmpty() && !Pattern.matches("\\+?[0-9\\s\\-]{7,20}", phone)) {
            showError("Ошибка", "Введите корректный номер телефона!");
            return;
        }

        // Формируем JSON-запрос
        Map<String, String> userData = new HashMap<>();
        userData.put("login", login);
        userData.put("password", password);
        userData.put("fullName", fullName);
        userData.put("email", email);
        userData.put("phone", phone);
        String jsonData = Serializer.toJson(userData);

        // Отправляем на сервер
        Response response = NetworkClient.sendRequest("REGISTER", jsonData);

        if (response.isSuccess()) {
            showInfo("Успех", response.getMessage());
            clearFields();
            onBackToLogin();
        } else {
            showError("Ошибка", response.getMessage());
            clearFields();
        }
    }

    @FXML
    private void onBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 600, 700);
            Stage stage = (Stage) loginField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Вход в систему");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось вернуться к окну входа");
        }
    }

    @FXML
    private void onExit() {
        javafx.application.Platform.exit();
    }

    private void clearFields() {
        loginField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        fullNameField.clear();
        emailField.clear();
        phoneField.clear();
    }
}