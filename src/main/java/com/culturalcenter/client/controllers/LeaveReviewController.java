package com.culturalcenter.client.controllers;

import com.culturalcenter.client.models.ReviewBookingItem;
import com.culturalcenter.client.network.NetworkClient;
import com.culturalcenter.client.utils.StarRatingControl;
import com.culturalcenter.server.network.Response;
import com.culturalcenter.server.serializer.Serializer;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaveReviewController extends BaseController {

    @FXML private Label userLabel;
    @FXML private ComboBox<ReviewBookingItem> bookingCombo;
    @FXML private HBox starContainer;
    @FXML private TextArea commentArea;

    private StarRatingControl starRating;
    private int currentUserId;
    private String currentLogin;

    @FXML
    public void initialize() {
        updateUserLabel(userLabel);

        starRating = new StarRatingControl();
        starContainer.getChildren().add(starRating);
    }

    public void initData(String login, int userId, String fullName) {
        this.currentLogin = login;
        this.currentUserId = userId;
        if (userLabel != null) {
            userLabel.setText(fullName);
        }
        loadAvailableBookings();
    }

    private void loadAvailableBookings() {
        Response response = NetworkClient.sendRequest("GET_MY_BOOKINGS", String.valueOf(currentUserId));
        if (response.isSuccess()) {
            List<Map<String, Object>> bookings = Serializer.fromJson(response.getData(), List.class);
            bookingCombo.setItems(FXCollections.observableArrayList());

            for (Map<String, Object> booking : bookings) {
                String status = (String) booking.get("status");

                // Только COMPLETED (дата уже прошла, статус гарантирует)
                if ("COMPLETED".equals(status)) {
                    bookingCombo.getItems().add(new ReviewBookingItem(
                            ((Number) booking.get("bookingId")).intValue(),
                            (String) booking.get("centerName"),
                            (String) booking.get("roomName"),
                            (String) booking.get("eventDate")
                    ));
                }
            }
        }
    }

    @FXML
    private void onSubmit() {
        ReviewBookingItem selected = bookingCombo.getValue();
        if (selected == null) {
            showError("Ошибка", "Выберите мероприятие");
            return;
        }

        int rating = starRating.getRating();
        if (rating == 0) {
            showError("Ошибка", "Поставьте оценку (нажмите на звёзды)");
            return;
        }

        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("bookingId", selected.getBookingId());
        reviewData.put("rating", rating);
        reviewData.put("comment", commentArea.getText().trim());

        Response response = NetworkClient.sendRequest("CREATE_REVIEW", Serializer.toJson(reviewData));
        if (response.isSuccess()) {
            showInfo("Успех", response.getMessage());
            onBack();
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    @FXML
    private void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/organizer.fxml"));
            Parent root = loader.load();
            OrganizerController controller = loader.getController();

            Map<String, Object> userData = new HashMap<>();
            userData.put("login", currentLogin);
            userData.put("userId", currentUserId);
            userData.put("role", "ORGANIZER");
            controller.initUserData(currentLogin, userData);

            Scene scene = new Scene(root, 750, 450);
            Stage stage = (Stage) bookingCombo.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Панель управления - ORGANIZER");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось вернуться назад");
        }
    }
}