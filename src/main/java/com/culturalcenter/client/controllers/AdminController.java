package com.culturalcenter.client.controllers;

import com.culturalcenter.client.network.NetworkClient;
import com.culturalcenter.server.network.Response;
import com.culturalcenter.server.serializer.Serializer;
import javafx.scene.control.Label;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.Map;

public class AdminController extends BaseController {

    @FXML private Label userLabel;
    @FXML private Button logoutButton;
    @FXML private Button profileButton;

    private Map<String, Object> userData;

    public void initUserData(String login, Map<String, Object> data) {
        this.currentLogin = login;
        this.userData = data != null ? data : new HashMap<>();
        this.currentRole = "ADMIN";

        // инициализируем данные для редактирования профиля
        initProfileData(login, currentRole, "/views/admin.fxml", profileButton);

        updateUserLabel(userLabel);

        System.out.println("✅ AdminController инициализирован для: " + login);
    }

    @FXML
    private void onManageUsers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/manage_users.fxml"));
            Parent root = loader.load();

            ManageUsersController controller = loader.getController();
            controller.initData(currentLogin, BaseController.getCurrentFullName());

            Scene scene = new Scene(root, 1100, 650);
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Управление пользователями");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось открыть управление пользователями");
        }
    }

    @FXML
    private void onManageCenters() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/manage_centers.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1100, 650);
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Управление культурными центрами");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось открыть управление центрами");
        }
    }

    // удалили onEditProfile() - теперь он в BaseController

    @FXML
    private void onLogout() {
        BaseController.setCurrentFullName("");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 600, 700);
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Вход в систему");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось выйти из системы");
        }
    }
}