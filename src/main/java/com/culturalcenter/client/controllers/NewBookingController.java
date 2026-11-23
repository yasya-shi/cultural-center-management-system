package com.culturalcenter.client.controllers;

import com.culturalcenter.client.models.CenterItem;
import com.culturalcenter.client.models.RoomItem;
import com.culturalcenter.client.network.NetworkClient;
import com.culturalcenter.server.network.Response;
import com.culturalcenter.server.serializer.Serializer;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewBookingController extends BaseController {

    @FXML private Label userLabel;
    @FXML private ComboBox<CenterItem> centerCombo;
    @FXML private ComboBox<RoomItem> roomCombo;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> startTimeCombo;
    @FXML private ComboBox<Integer> durationCombo;
    @FXML private TextField purposeField;

    private int currentUserId;
    private String currentLogin;

    public void initData(String login, int userId, String fullName) {
        this.currentLogin = login;
        this.currentUserId = userId;

        if (userLabel != null) {
            userLabel.setText(fullName);
        }

        loadCenters();
        initTimeOptions();
        initDurationOptions();

        //datacell  это встроенный класс JavaFX, который отвечает за одну ячейку в календаре
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);           // вызываем родительский метод
                setDisable(empty || date.isBefore(LocalDate.now())); // добавляем свою логику
            }
        });
    }

    private void loadCenters() {
        Response response = NetworkClient.sendRequest("GET_ALL_CENTERS", "");
        if (response.isSuccess()) {
            List<Map<String, Object>> centers = Serializer.fromJson(response.getData(), List.class);
            centerCombo.setItems(FXCollections.observableArrayList());
            for (Map<String, Object> center : centers) {
                int centerId = ((Number) center.get("centerId")).intValue();
                String name = (String) center.get("name");
                centerCombo.getItems().add(new CenterItem(centerId, name));
            }
        } else {
            showError("Ошибка", "Не удалось загрузить центры");
        }
    }

    @FXML
    private void onCenterSelected() {
        CenterItem selected = centerCombo.getValue();
        if (selected == null) return;
        loadRooms(selected.getCenterId());
    }

    private void loadRooms(int centerId) {
        Response response = NetworkClient.sendRequest("GET_ROOMS_BY_CENTER", String.valueOf(centerId));
        if (response.isSuccess()) {
            List<Map<String, Object>> rooms = Serializer.fromJson(response.getData(), List.class);
            roomCombo.setItems(FXCollections.observableArrayList());
            for (Map<String, Object> room : rooms) {
                int roomId = ((Number) room.get("roomId")).intValue();
                String name = (String) room.get("name");
                String status = (String) room.getOrDefault("status", "OPEN");
                if ("OPEN".equals(status)) {
                    roomCombo.getItems().add(new RoomItem(roomId, name));
                }
            }
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    private void initTimeOptions() {
        startTimeCombo.setItems(FXCollections.observableArrayList(
                "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00",
                "16:00", "17:00", "18:00", "19:00", "20:00", "21:00"
        ));
    }

    private void initDurationOptions() {
        durationCombo.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        durationCombo.setValue(2);
    }

    @FXML
    private void onSubmit() {
        if (centerCombo.getValue() == null) {
            showError("Ошибка", "Выберите культурный центр");
            return;
        }
        if (roomCombo.getValue() == null) {
            showError("Ошибка", "Выберите зал");
            return;
        }
        if (datePicker.getValue() == null) {
            showError("Ошибка", "Выберите дату");
            return;
        }
        if (startTimeCombo.getValue() == null) {
            showError("Ошибка", "Выберите время начала");
            return;
        }
        if (purposeField.getText().trim().isEmpty()) {
            showError("Ошибка", "Укажите цель мероприятия");
            return;
        }

        LocalDate date = datePicker.getValue();
        String startTime = startTimeCombo.getValue();
        int duration = durationCombo.getValue();
        String endTime = LocalTime.parse(startTime).plusHours(duration).toString();

        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("roomId", roomCombo.getValue().getRoomId());
        bookingData.put("organizerId", currentUserId);
        bookingData.put("eventDate", date.toString());
        bookingData.put("startTime", startTime + ":00");
        bookingData.put("endTime", endTime + ":00");
        bookingData.put("purpose", purposeField.getText().trim());

        Response response = NetworkClient.sendRequest("CREATE_BOOKING", Serializer.toJson(bookingData));
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
            userData.put("fullName", BaseController.getCurrentFullName());
            controller.initUserData(currentLogin, userData);

            Scene scene = new Scene(root, 750, 450);
            Stage stage = (Stage) centerCombo.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Панель управления - ORGANIZER");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось вернуться назад");
        }
    }
}