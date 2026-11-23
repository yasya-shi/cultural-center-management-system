package com.culturalcenter.client.controllers;

import com.culturalcenter.client.models.RoomItem;
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

public class MyCenterController extends BaseController {

    @FXML private Label centerName;
    @FXML private Label centerAddress;
    @FXML private Label centerPhone;
    @FXML private Label centerEmail;

    @FXML private TableView<RoomItem> roomsTable;
    @FXML private TableColumn<RoomItem, String> colName;
    @FXML private TableColumn<RoomItem, Integer> colCapacity;
    @FXML private TableColumn<RoomItem, String> colEquipment;
    @FXML private TableColumn<RoomItem, Double> colPrice;
    @FXML private TableColumn<RoomItem, String> colStatus;

    private ObservableList<RoomItem> roomList = FXCollections.observableArrayList();
    private int currentCenterId = -1;
    private String currentLogin;

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colEquipment.setCellValueFactory(new PropertyValueFactory<>("equipment"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("pricePerHour"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        roomsTable.setItems(roomList);
        roomsTable.setStyle("-fx-border-color: #8b3a3a; -fx-border-width: 2; -fx-selection-bar: #c97a7a; -fx-selection-bar-non-focused: #d4a0a0;");
    }

    public void initData(String login) {
        this.currentLogin = login;
        loadCenterInfo();
    }

    private void loadCenterInfo() {
        Response response = NetworkClient.sendRequest("GET_MY_CENTER", currentLogin);

        if (response.isSuccess()) {
            Map<String, Object> center = Serializer.fromJson(response.getData(), Map.class);
            currentCenterId = ((Number) center.get("centerId")).intValue();
            centerName.setText((String) center.get("name"));
            centerAddress.setText((String) center.get("address"));
            centerPhone.setText((String) center.get("phone"));
            centerEmail.setText((String) center.get("email"));

            // Небольшая задержка перед загрузкой залов
            javafx.application.Platform.runLater(() -> loadRooms());
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    private void loadRooms() {
        if (currentCenterId <= 0) {
            System.out.println("centerId не получен, пропускаем загрузку залов");
            return;
        }

        System.out.println("Загружаем залы для centerId: " + currentCenterId);

        Response response = NetworkClient.sendRequest("GET_MY_ROOMS", String.valueOf(currentCenterId));

        System.out.println("GET_MY_ROOMS ответ: " + response.isSuccess());
        System.out.println("GET_MY_ROOMS данные: " + response.getData());

        if (response.isSuccess()) {
            List<Map<String, Object>> rooms = Serializer.fromJson(response.getData(), List.class);
            roomList.clear();
            for (Map<String, Object> room : rooms) {
                roomList.add(new RoomItem(
                        ((Number) room.get("roomId")).intValue(),
                        (String) room.get("name"),
                        ((Number) room.get("capacity")).intValue(),
                        (String) room.get("equipment"),
                        ((Number) room.get("pricePerHour")).doubleValue(),
                        (String) room.getOrDefault("status", "OPEN")
                ));
            }
            System.out.println("Загружено залов: " + roomList.size());
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    @FXML
    private void onCloseRoom() {
        RoomItem selected = roomsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Предупреждение", "Выберите зал");
            return;
        }
        if ("ЗАКРЫТ".equals(selected.getStatus())) {
            showWarning("Предупреждение", "Зал уже закрыт");
            return;
        }

        Response response = NetworkClient.sendRequest("CLOSE_ROOM", String.valueOf(selected.getRoomId()));
        if (response.isSuccess()) {
            showInfo("Успех", response.getMessage());
            loadRooms();
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    @FXML
    private void onOpenRoom() {
        RoomItem selected = roomsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Предупреждение", "Выберите зал");
            return;
        }
        if ("ОТКРЫТ".equals(selected.getStatus())) {
            showWarning("Предупреждение", "Зал уже открыт");
            return;
        }

        Response response = NetworkClient.sendRequest("OPEN_ROOM", String.valueOf(selected.getRoomId()));
        if (response.isSuccess()) {
            showInfo("Успех", response.getMessage());
            loadRooms();
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    @FXML
    private void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/director.fxml"));
            Parent root = loader.load();
            DirectorController controller = loader.getController();

            // Передаём НЕ null, а данные
            Map<String, Object> userData = new HashMap<>();
            userData.put("login", currentLogin);
            userData.put("role", "DIRECTOR");

            controller.initUserData(currentLogin, userData);

            Scene scene = new Scene(root, 750, 450);
            Stage stage = (Stage) centerName.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Панель управления - DIRECTOR");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось вернуться назад");
        }
    }
}