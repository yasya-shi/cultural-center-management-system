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
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class DirectorController extends BaseController {

    @FXML private Label userLabel;
    @FXML private Button logoutButton;
    @FXML private Button profileButton;

    private String role;
    private Map<String, Object> userData;
    private int currentCenterId = -1;

    public void initUserData(String login, Map<String, Object> data) {
        this.currentLogin = login;
        this.userData = data;
        this.currentRole = (String) data.get("role");

        initProfileData(login, currentRole, "/views/director.fxml", profileButton);

        updateUserLabel(userLabel);

        Response response = NetworkClient.sendRequest("GET_MY_CENTER", login);
        if (response.isSuccess()) {
            Map<String, Object> center = Serializer.fromJson(response.getData(), Map.class);
            currentCenterId = ((Number) center.get("centerId")).intValue();
            userData.put("centerId", currentCenterId);
        }

        System.out.println("✅ DirectorController инициализирован для: " + login + ", центр: " + currentCenterId);

        checkNotifications();
    }

    @FXML
    private void onBookings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/manage_bookings.fxml"));
            Parent root = loader.load();
            ManageBookingsController controller = loader.getController();

            System.out.println("Передаём centerId: " + currentCenterId);
            controller.initData(currentLogin, currentCenterId);

            Scene scene = new Scene(root, 950, 600);
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Управление заявками");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось открыть заявки");
        }
    }

    @FXML
    private void onCenters() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/my_center.fxml"));
            Parent root = loader.load();
            MyCenterController controller = loader.getController();
            controller.initData(currentLogin);
            Scene scene = new Scene(root, 950, 750);
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Мой культурный центр");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось открыть информацию о центре");
        }
    }

    @FXML
    private void onReports() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/reports.fxml"));
            Parent root = loader.load();
            ReportsController controller = loader.getController();
            controller.initData(currentLogin);
            Scene scene = new Scene(root, 950, 600);
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Аналитические отчёты");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось открыть отчёты");
        }
    }

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

    private void checkNotifications() {
        try {
            Object userIdObj = userData.get("userId");
            if (userIdObj == null) {
                System.out.println("userId не найден в userData, пропускаем проверку уведомлений");
                return;
            }
            int userId = ((Number) userIdObj).intValue();
            Response response = NetworkClient.sendRequest("GET_UNREAD_NOTIFICATIONS", String.valueOf(userId));

            if (response.isSuccess() && response.getData() != null && !response.getData().isEmpty()) {
                List<Map<String, Object>> notifications = Serializer.fromJson(response.getData(), List.class);
                if (notifications != null && !notifications.isEmpty()) {
                    StringBuilder message = new StringBuilder("У вас новые уведомления:\n\n");
                    for (Map<String, Object> n : notifications) {
                        message.append("- ").append(n.get("message")).append("\n");
                    }
                    message.append("\nПроверьте раздел 'Управление заявками'.");

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Уведомления");
                    alert.setHeaderText(null);
                    alert.setContentText(message.toString());
                    alert.showAndWait();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}