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
import javafx.scene.control.Alert;
import java.util.List;
import java.util.Map;

public class OrganizerController extends BaseController {

    @FXML private Label userLabel;
    @FXML private Button logoutButton;
    @FXML private Button profileButton;

    private Map<String, Object> userData;

    public void initUserData(String login, Map<String, Object> data) {
        this.currentLogin = login;
        this.userData = data;
        this.currentRole = (String) data.get("role");

        // инициализируем данные для редактирования профиля
        initProfileData(login, currentRole, "/views/organizer.fxml", profileButton);

        updateUserLabel(userLabel);

        System.out.println("✅ OrganizerController инициализирован для: " + login);

        checkNotifications();
    }

    @FXML
    private void onNewBooking() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/new_booking.fxml"));
            Parent root = loader.load();
            NewBookingController controller = loader.getController();

            int userId = ((Number) userData.get("userId")).intValue();
            String fullName = BaseController.getCurrentFullName();
            controller.initData(currentLogin, userId, fullName);

            Scene scene = new Scene(root, 850, 600);
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Новая заявка");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось открыть форму создания заявки");
        }
    }

    @FXML
    private void onMyBookings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/my_bookings.fxml"));
            Parent root = loader.load();
            MyBookingsController controller = loader.getController();

            int userId = ((Number) userData.get("userId")).intValue();
            String fullName = BaseController.getCurrentFullName();
            controller.initData(currentLogin, userId, fullName);

            Scene scene = new Scene(root, 1100, 700);
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Мои заявки");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось открыть список заявок");
        }
    }

    @FXML
    private void onLeaveReview() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/leave_review.fxml"));
            Parent root = loader.load();
            LeaveReviewController controller = loader.getController();
            int userId = ((Number) userData.get("userId")).intValue();

            String fullName = BaseController.getCurrentFullName();
            controller.initData(currentLogin, userId, fullName);

            Scene scene = new Scene(root, 700, 550);
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Оставить отзыв");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось открыть форму отзыва");
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

    private void checkNotifications() {
        try {
            Object userIdObj = userData.get("userId");
            if (userIdObj == null) {
                System.out.println("userId не найден в userData, пропускаем проверку уведомлений");
                return;
            }
            int userId = ((Number) userIdObj).intValue();
            Response response = NetworkClient.sendRequest("GET_UNREAD_NOTIFICATIONS", String.valueOf(userId));

            if (response.isSuccess()) {
                List<Map<String, Object>> notifications = Serializer.fromJson(response.getData(), List.class);
                if (!notifications.isEmpty()) {
                    StringBuilder message = new StringBuilder("📬 У вас новые уведомления:\n\n");
                    for (Map<String, Object> n : notifications) {
                        message.append("• ").append(n.get("message")).append("\n");
                    }
                    message.append("\nОни отображаются в разделе 'Мои заявки'.");

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