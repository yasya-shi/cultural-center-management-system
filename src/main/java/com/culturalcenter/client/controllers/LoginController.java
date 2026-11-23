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

public class LoginController extends BaseController {

    @FXML
    private TextField loginField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void onLogin() {
        String login = loginField.getText().trim();
        String password = passwordField.getText();

        if (login.isEmpty() || password.isEmpty()) {
            showError("Ошибка", "Введите логин и пароль!");
            clearFields();
            return;
        }

        Map<String, String> credentials = new HashMap<>();
        credentials.put("login", login);
        credentials.put("password", password);
        String jsonData = Serializer.toJson(credentials);

        Response response = NetworkClient.sendRequest("LOGIN", jsonData);

        if (response.isSuccess()) {
            showInfo("Успех", response.getMessage());
            Map<String, Object> userData = Serializer.fromJson(response.getData(), Map.class);
            String role = (String) userData.get("role");


            BaseController.setCurrentFullName((String) userData.get("fullName"));

            openMenuByRole(role, login, userData);
        } else {
            showError("Ошибка", response.getMessage());
            clearFields();
        }
    }

    // фабричный метод
    private void openMenuByRole(String role, String login, Map<String, Object> userData) {
        try {
            FXMLLoader loader;
            String fxmlPath;

            switch (role) {
                case "ADMIN":
                    fxmlPath = "/views/admin.fxml";
                    break;
                case "DIRECTOR":
                    fxmlPath = "/views/director.fxml";
                    break;
                default:
                    fxmlPath = "/views/organizer.fxml";
                    break;
            }

            loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller instanceof OrganizerController) {
                ((OrganizerController) controller).initUserData(login, userData);
            } else if (controller instanceof DirectorController) {
                ((DirectorController) controller).initUserData(login, userData);
            } else if (controller instanceof AdminController) {
                ((AdminController) controller).initUserData(login, userData);
            }

            Scene scene = new Scene(root, 750, 450);
            Stage stage = (Stage) loginField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Панель управления - " + role);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось открыть главное меню");
        }
    }

    @FXML
    private void onRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/register.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 600, 700);
            Stage stage = (Stage) loginField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Регистрация");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось открыть окно регистрации");
        }
    }

    @FXML
    private void onExit() {
        javafx.application.Platform.exit();
    }

    private void clearFields() {
        loginField.clear();
        passwordField.clear();
    }
}