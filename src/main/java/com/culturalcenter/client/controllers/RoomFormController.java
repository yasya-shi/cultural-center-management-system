package com.culturalcenter.client.controllers;

import com.culturalcenter.client.models.RoomItem;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class RoomFormController extends BaseController {

    @FXML private Text titleText;
    @FXML private TextField nameField;
    @FXML private TextField capacityField;
    @FXML private TextField equipmentField;
    @FXML private TextField priceField;

    private RoomItem editingRoom;  // ← ИСПРАВЛЕНО
    private BiConsumer<Map<String, Object>, Boolean> onSaveCallback;

    public void initData(RoomItem room, BiConsumer<Map<String, Object>, Boolean> onSaveCallback) {  // ← ИСПРАВЛЕНО
        this.editingRoom = room;
        this.onSaveCallback = onSaveCallback;

        if (room != null) {
            titleText.setText("Редактирование зала");
            nameField.setText(room.getName());
            capacityField.setText(String.valueOf(room.getCapacity()));
            equipmentField.setText(room.getEquipment());
            priceField.setText(String.valueOf(room.getPricePerHour()));
        } else {
            titleText.setText("Добавление зала");
        }
    }

    @FXML
    private void onSave() {
        String name = nameField.getText().trim();
        String capacityStr = capacityField.getText().trim();
        String equipment = equipmentField.getText().trim();
        String priceStr = priceField.getText().trim();

        if (name.isEmpty()) {
            showError("Ошибка", "Введите название зала");
            return;
        }

        int capacity;
        double price;
        try {
            capacity = Integer.parseInt(capacityStr);
            if (capacity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Ошибка", "Введите корректную вместимость (положительное число)");
            return;
        }

        try {
            price = Double.parseDouble(priceStr);
            if (price < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Ошибка", "Введите корректную цену");
            return;
        }

        Map<String, Object> roomData = new HashMap<>();
        roomData.put("name", name);
        roomData.put("capacity", capacity);
        roomData.put("equipment", equipment);
        roomData.put("pricePerHour", price);

        if (onSaveCallback != null) {
            onSaveCallback.accept(roomData, editingRoom != null);
        }
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