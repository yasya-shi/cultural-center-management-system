package com.culturalcenter.client.controllers;

import com.culturalcenter.client.models.BookingItem;
import com.culturalcenter.client.network.NetworkClient;
import com.culturalcenter.server.network.Response;
import com.culturalcenter.server.serializer.Serializer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyBookingsController extends BaseController {

    @FXML private Label userLabel;
    @FXML private TableView<BookingItem> bookingsTable;
    @FXML private TableColumn<BookingItem, Integer> colId;
    @FXML private TableColumn<BookingItem, String> colCenter;
    @FXML private TableColumn<BookingItem, String> colRoom;
    @FXML private TableColumn<BookingItem, String> colDate;
    @FXML private TableColumn<BookingItem, String> colTime;
    @FXML private TableColumn<BookingItem, String> colPurpose;
    @FXML private TableColumn<BookingItem, String> colStatus;
    @FXML private TableColumn<BookingItem, String> colReason;

    private ObservableList<BookingItem> bookingList = FXCollections.observableArrayList();
    private int currentUserId;
    private String currentLogin;

    @FXML
    public void initialize() {
        updateUserLabel(userLabel);

        colId.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        colCenter.setCellValueFactory(new PropertyValueFactory<>("centerName"));
        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("eventDate"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timeRange"));
        colPurpose.setCellValueFactory(new PropertyValueFactory<>("purpose"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("rejectionReason"));

        bookingsTable.setItems(bookingList);
        bookingsTable.setStyle("-fx-border-color: #8b3a3a; -fx-border-width: 2; -fx-selection-bar: #c97a7a; -fx-selection-bar-non-focused: #d4a0a0;");

        colDate.setSortable(true);
        colDate.setSortType(TableColumn.SortType.DESCENDING);
        bookingsTable.getSortOrder().add(colDate);
        bookingsTable.sort();
    }

    public void initData(String login, int userId, String fullName) {
        this.currentLogin = login;
        this.currentUserId = userId;
        // ФИО уже отобразится в initialize() через updateUserLabel, но можно и так
        if (userLabel != null) {
            userLabel.setText(fullName);
        }
        loadBookings();
    }

    private void loadBookings() {
        NetworkClient.sendRequest("UPDATE_COMPLETED_BOOKINGS", "");

        Response response = NetworkClient.sendRequest("GET_MY_BOOKINGS", String.valueOf(currentUserId));
        if (response.isSuccess()) {
            List<Map<String, Object>> bookings = Serializer.fromJson(response.getData(), List.class);
            bookingList.clear();
            for (Map<String, Object> booking : bookings) {
                bookingList.add(new BookingItem(
                        ((Number) booking.get("bookingId")).intValue(),
                        (String) booking.get("centerName"),
                        (String) booking.get("roomName"),
                        (String) booking.get("eventDate"),
                        (String) booking.get("startTime"),
                        (String) booking.get("endTime"),
                        (String) booking.get("purpose"),
                        (String) booking.get("status"),
                        (String) booking.get("rejectionReason")
                ));
            }
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        BookingItem selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Предупреждение", "Выберите заявку");
            return;
        }
        if (!"PENDING".equals(selected.getRawStatus())) {
            showWarning("Предупреждение", "Можно отменить только заявки со статусом 'На рассмотрении'");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Отменить заявку №" + selected.getBookingId() + "?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Button yesButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.YES);
        Button noButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.NO);
        yesButton.setText("ДА");
        noButton.setText("НЕТ");

        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            Response response = NetworkClient.sendRequest("CANCEL_BOOKING", String.valueOf(selected.getBookingId()));
            if (response.isSuccess()) {
                showInfo("Успех", response.getMessage());
                loadBookings();
            } else {
                showError("Ошибка", response.getMessage());
            }
        }
    }

    @FXML
    private void onRefresh() {
        loadBookings();
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
            Stage stage = (Stage) bookingsTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Панель управления - ORGANIZER");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось вернуться назад");
        }
    }
}