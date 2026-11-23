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

public class ManageBookingsController extends BaseController {

    @FXML private TableView<BookingItem> bookingsTable;
    @FXML private TableColumn<BookingItem, Integer> colId;
    @FXML private TableColumn<BookingItem, String> colRoom;
    @FXML private TableColumn<BookingItem, String> colOrganizer;
    @FXML private TableColumn<BookingItem, String> colDate;
    @FXML private TableColumn<BookingItem, String> colTime;
    @FXML private TableColumn<BookingItem, String> colPurpose;
    @FXML private TableColumn<BookingItem, String> colStatus;
    @FXML private ComboBox<String> statusFilter;

    private ObservableList<BookingItem> bookingList = FXCollections.observableArrayList();
    private ObservableList<BookingItem> filteredList = FXCollections.observableArrayList();
    private int currentCenterId = -1;
    private String currentLogin;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomName"));
        colOrganizer.setCellValueFactory(new PropertyValueFactory<>("organizerName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("eventDate"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timeRange"));
        colPurpose.setCellValueFactory(new PropertyValueFactory<>("purpose"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        bookingsTable.setItems(filteredList);
        bookingsTable.setStyle("-fx-border-color: #8b3a3a; -fx-border-width: 2; -fx-selection-bar: #c97a7a; -fx-selection-bar-non-focused: #d4a0a0;");

        statusFilter.setItems(FXCollections.observableArrayList("Все", "PENDING", "APPROVED", "REJECTED", "COMPLETED"));
        statusFilter.setValue("Все");
    }

    public void initData(String login, int centerId) {
        this.currentLogin = login;
        this.currentCenterId = centerId;
        loadBookings();
    }

    private void loadBookings() {
        NetworkClient.sendRequest("UPDATE_COMPLETED_BOOKINGS", "");

        System.out.println("Загружаем заявки для centerId: " + currentCenterId);

        Response response = NetworkClient.sendRequest("GET_BOOKINGS_BY_CENTER", String.valueOf(currentCenterId));

        System.out.println("Ответ: " + response.isSuccess());
        System.out.println("Данные: " + response.getData());

        if (response.isSuccess()) {
            List<Map<String, Object>> bookings = Serializer.fromJson(response.getData(), List.class);
            System.out.println("Найдено заявок: " + bookings.size());
            bookingList.clear();
            for (Map<String, Object> booking : bookings) {
                bookingList.add(new BookingItem(
                        ((Number) booking.get("bookingId")).intValue(),
                        (String) booking.get("roomName"),
                        (String) booking.get("organizerName"),
                        (String) booking.get("eventDate"),
                        (String) booking.get("startTime"),
                        (String) booking.get("endTime"),
                        (String) booking.get("purpose"),
                        (String) booking.get("status")
                ));
            }
            applyFilter();
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    private void applyFilter() {
        String filter = statusFilter.getValue();
        filteredList.clear();
        for (BookingItem item : bookingList) {
            if ("Все".equals(filter) || item.getRawStatus().equals(filter)) {
                filteredList.add(item);
            }
        }
    }

    @FXML
    private void onApplyFilter() {
        applyFilter();
    }

    @FXML
    private void onRefresh() {
        loadBookings();
    }

    @FXML
    private void onApprove() {
        BookingItem selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Предупреждение", "Выберите заявку");
            return;
        }
        if (!"PENDING".equals(selected.getRawStatus())) {
            showWarning("Предупреждение", "Можно подтвердить только заявки со статусом PENDING");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("bookingId", selected.getBookingId());

        Response response = NetworkClient.sendRequest("APPROVE_BOOKING", Serializer.toJson(data));
        if (response.isSuccess()) {
            showInfo("Успех", response.getMessage());
            loadBookings();
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    @FXML
    private void onReject() {
        BookingItem selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Предупреждение", "Выберите заявку");
            return;
        }
        if (!"PENDING".equals(selected.getRawStatus())) {
            showWarning("Предупреждение", "Можно отклонить только заявки со статусом PENDING");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Отклонение заявки");
        dialog.setHeaderText("Укажите причину отказа:");
        dialog.setContentText("Причина:");
        dialog.getEditor().setPrefWidth(400);


//если пользователь ввел функция вызывается
        dialog.showAndWait().ifPresent(reason -> {
            if (reason.trim().isEmpty()) {
                showError("Ошибка", "Причина не может быть пустой");
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("bookingId", selected.getBookingId());
            data.put("reason", reason);

            Response response = NetworkClient.sendRequest("REJECT_BOOKING", Serializer.toJson(data));
            if (response.isSuccess()) {
                showInfo("Успех", response.getMessage());
                loadBookings();
            } else {
                showError("Ошибка", response.getMessage());
            }
        });
    }

    @FXML
    private void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/director.fxml"));
            Parent root = loader.load();
            DirectorController controller = loader.getController();

            Map<String, Object> userData = new HashMap<>();
            userData.put("login", currentLogin);
            userData.put("role", "DIRECTOR");
            userData.put("centerId", currentCenterId);

            controller.initUserData(currentLogin, userData);

            Scene scene = new Scene(root, 750, 450);
            Stage stage = (Stage) bookingsTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Панель управления - DIRECTOR");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось вернуться назад");
        }
    }
}