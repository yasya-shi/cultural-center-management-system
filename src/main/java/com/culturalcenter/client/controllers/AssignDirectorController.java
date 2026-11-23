package com.culturalcenter.client.controllers;

import com.culturalcenter.client.models.CenterItem;  // ← изменён импорт
import com.culturalcenter.client.models.UserItem;
import com.culturalcenter.client.network.NetworkClient;
import com.culturalcenter.server.network.Response;
import com.culturalcenter.server.serializer.Serializer;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignDirectorController extends BaseController {

    @FXML private Label userLabel;
    @FXML private ComboBox<CenterItem> centerCombo;  // ← CenterItem

    private UserItem user;
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        loadCenters();
    }

    public void initData(UserItem user, Runnable onSaveCallback) {
        this.user = user;
        this.onSaveCallback = onSaveCallback;
        userLabel.setText("Назначить директором: " + user.getFullName());
    }

    private void loadCenters() {
        Response response = NetworkClient.sendRequest("GET_CENTERS_FOR_DROPDOWN", "");
        if (response.isSuccess()) {
            List<Map<String, Object>> centers = Serializer.fromJson(response.getData(), List.class);
            centerCombo.setItems(FXCollections.observableArrayList());
            for (Map<String, Object> center : centers) {
                centerCombo.getItems().add(new CenterItem(
                        ((Number) center.get("centerId")).intValue(),
                        (String) center.get("name")
                ));
            }
        } else {
            showError("Ошибка", "Не удалось загрузить список центров");
        }
    }

    @FXML
    private void onAssign() {
        CenterItem selected = centerCombo.getValue();
        if (selected == null) {
            showWarning("Предупреждение", "Выберите культурный центр");
            return;
        }

        Map<String, Number> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("centerId", selected.getCenterId());

        Response response = NetworkClient.sendRequest("ASSIGN_DIRECTOR", Serializer.toJson(data));
        if (response.isSuccess()) {
            showInfo("Успех", response.getMessage());
            closeWindow();
            if (onSaveCallback != null) onSaveCallback.run();
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) userLabel.getScene().getWindow();
        stage.close();
    }
}