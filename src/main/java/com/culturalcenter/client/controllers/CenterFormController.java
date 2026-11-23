package com.culturalcenter.client.controllers;

import com.culturalcenter.client.models.CenterItem;  // ← изменён импорт
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
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CenterFormController extends BaseController {

    @FXML private Text titleText;
    @FXML private TextField nameField;
    @FXML private TextField addressField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TableView<RoomItem> roomsTable;
    @FXML private TableColumn<RoomItem, String> colRoomName;
    @FXML private TableColumn<RoomItem, Integer> colCapacity;
    @FXML private TableColumn<RoomItem, String> colEquipment;
    @FXML private TableColumn<RoomItem, Double> colPrice;

    private ObservableList<RoomItem> roomList = FXCollections.observableArrayList();
    private CenterItem editingCenter;  // ← CenterItem
    private Runnable onSaveCallback;
    private int currentCenterId = -1;

    private List<Map<String, Object>> roomsToSave = new ArrayList<>();
    private List<Integer> roomsToDelete = new ArrayList<>();

    @FXML
    public void initialize() {
        colRoomName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colEquipment.setCellValueFactory(new PropertyValueFactory<>("equipment"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("pricePerHour"));

        roomsTable.setItems(roomList);
        roomsTable.setStyle("-fx-border-color: #8b3a3a; -fx-border-width: 2; -fx-selection-bar: #c97a7a; -fx-selection-bar-non-focused: #d4a0a0;");
    }

    public void initData(CenterItem center, Runnable onSaveCallback) {  // ← CenterItem
        this.editingCenter = center;
        this.onSaveCallback = onSaveCallback;

        if (center != null) {
            titleText.setText("Редактирование культурного центра");
            currentCenterId = center.getCenterId();
            nameField.setText(center.getName());
            addressField.setText(center.getAddress());
            phoneField.setText(center.getPhone());
            emailField.setText(center.getEmail());
            loadRooms();
        } else {
            titleText.setText("Добавление культурного центра");
        }
    }

    private void loadRooms() {
        if (currentCenterId <= 0) return;
        Response response = NetworkClient.sendRequest("GET_ROOMS_BY_CENTER", String.valueOf(currentCenterId));
        if (response.isSuccess()) {
            List<Map<String, Object>> rooms = Serializer.fromJson(response.getData(), List.class);
            roomList.clear();
            for (Map<String, Object> room : rooms) {
                RoomItem item = new RoomItem(
                        ((Number) room.get("roomId")).intValue(),
                        (String) room.get("name"),
                        ((Number) room.get("capacity")).intValue(),
                        (String) room.get("equipment"),
                        ((Number) room.get("pricePerHour")).doubleValue(),
                        (String) room.getOrDefault("status", "OPEN")
                );
                roomList.add(item);
            }
        }
    }

    @FXML
    private void onAddRoom() {
        openRoomForm(null);
    }

    @FXML
    private void onEditRoom() {
        RoomItem selected = roomsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Предупреждение", "Выберите зал для редактирования");
            return;
        }
        openRoomForm(selected);
    }

    @FXML
    private void onDeleteRoom() {
        RoomItem selected = roomsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Предупреждение", "Выберите зал");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Удалить зал \"" + selected.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Button yesButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.YES);
        Button noButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.NO);
        yesButton.setText("ДА");
        noButton.setText("НЕТ");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            if (selected.getRoomId() > 0) {
                roomsToDelete.add(selected.getRoomId());
            }
            roomList.remove(selected);
        }
    }

    private void openRoomForm(RoomItem room) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/room_form.fxml"));
            Parent root = loader.load();
            RoomFormController controller = loader.getController();
            //Лямбда передаётся как callback. Когда форма зала закрывается, вызывается этот код с данными из формы
            controller.initData(room, (newRoomData, isEditMode) -> {
                if (isEditMode && room != null && room.getRoomId() > 0) {
                    room.setName((String) newRoomData.get("name"));
                    room.setCapacity(((Number) newRoomData.get("capacity")).intValue());
                    room.setEquipment((String) newRoomData.get("equipment"));
                    room.setPricePerHour(((Number) newRoomData.get("pricePerHour")).doubleValue());
                    roomsTable.refresh();
                    newRoomData.put("roomId", room.getRoomId());
                    roomsToSave.add(newRoomData);
                } else if (!isEditMode) {
                    int tempId = -(roomList.size() + 1);
                    RoomItem newRoom = new RoomItem(
                            tempId,
                            (String) newRoomData.get("name"),
                            ((Number) newRoomData.get("capacity")).intValue(),
                            (String) newRoomData.get("equipment"),
                            ((Number) newRoomData.get("pricePerHour")).doubleValue(),
                            "OPEN"
                    );
                    roomList.add(newRoom);
                    newRoomData.put("tempId", tempId);
                    roomsToSave.add(newRoomData);
                }
            });
            Scene scene = new Scene(root, 600, 500);
            Stage stage = new Stage();
            stage.setTitle(room == null ? "Добавление зала" : "Редактирование зала");
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось открыть форму зала");
        }
    }

    @FXML
    private void onSave() {
        String name = nameField.getText().trim();
        String address = addressField.getText().trim();

        if (name.isEmpty()) {
            showError("Ошибка", "Введите название центра");
            return;
        }
        if (address.isEmpty()) {
            showError("Ошибка", "Введите адрес");
            return;
        }

        Map<String, Object> centerData = new HashMap<>();
        centerData.put("name", name);
        centerData.put("address", address);
        centerData.put("phone", phoneField.getText().trim());
        centerData.put("email", emailField.getText().trim());

        Response response;
        boolean isNewCenter = (editingCenter == null);

        if (isNewCenter) {
            response = NetworkClient.sendRequest("CREATE_CENTER", Serializer.toJson(centerData));
            if (response.isSuccess()) {
                Map<String, Integer> result = Serializer.fromJson(response.getData(), Map.class);
                currentCenterId = result.get("centerId");
                response = new Response(true, "Центр создан", null);
            }
        } else {
            centerData.put("centerId", currentCenterId);
            response = NetworkClient.sendRequest("UPDATE_CENTER", Serializer.toJson(centerData));
        }

        if (!response.isSuccess()) {
            showError("Ошибка", response.getMessage());
            return;
        }

        for (Map<String, Object> roomData : roomsToSave) {
            if (roomData.containsKey("roomId") && ((Number) roomData.get("roomId")).intValue() > 0) {
                roomData.put("centerId", currentCenterId);
                NetworkClient.sendRequest("UPDATE_ROOM", Serializer.toJson(roomData));
            } else {
                roomData.put("centerId", currentCenterId);
                roomData.remove("tempId");
                NetworkClient.sendRequest("CREATE_ROOM", Serializer.toJson(roomData));
            }
        }

        for (int roomId : roomsToDelete) {
            NetworkClient.sendRequest("DELETE_ROOM", Serializer.toJson(String.valueOf(roomId)));
        }

        showInfo("Успех", "Центр успешно сохранён");
        if (onSaveCallback != null) onSaveCallback.run();
        closeWindow();
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}