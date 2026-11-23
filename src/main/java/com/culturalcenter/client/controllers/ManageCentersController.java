package com.culturalcenter.client.controllers;

import com.culturalcenter.client.models.CenterItem;  // ← изменён импорт
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

public class ManageCentersController extends BaseController {

    @FXML private TableView<CenterItem> centersTable;  // ← CenterItem
    @FXML private TableColumn<CenterItem, Integer> colId;
    @FXML private TableColumn<CenterItem, String> colName;
    @FXML private TableColumn<CenterItem, String> colAddress;
    @FXML private TableColumn<CenterItem, String> colPhone;
    @FXML private TableColumn<CenterItem, String> colEmail;
    @FXML private TableColumn<CenterItem, String> colDirector;

    private ObservableList<CenterItem> centerList = FXCollections.observableArrayList();

    private String currentLogin;
    private String fullName;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("centerId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colDirector.setCellValueFactory(new PropertyValueFactory<>("directorName"));
        centersTable.setItems(centerList);
        centersTable.setStyle("-fx-border-color: #8b3a3a; -fx-border-width: 2; -fx-selection-bar: #c97a7a; -fx-selection-bar-non-focused: #d4a0a0;");
        loadCenters();
    }

    public void initData(String login, String fullName) {
        this.currentLogin = login;
        this.fullName = fullName;
    }

    private void loadCenters() {
        Response response = NetworkClient.sendRequest("GET_ALL_CENTERS", "");
        if (response.isSuccess()) {
            List<Map<String, Object>> centers = Serializer.fromJson(response.getData(), List.class);
            centerList.clear();
            for (Map<String, Object> center : centers) {
                centerList.add(new CenterItem(
                        ((Number) center.get("centerId")).intValue(),
                        (String) center.get("name"),
                        (String) center.get("address"),
                        (String) center.get("phone"),
                        (String) center.get("email"),
                        (String) center.get("directorName")
                ));
            }
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    @FXML
    private void onAdd() {
        openCenterForm(null);
    }

    @FXML
    private void onEdit() {
        CenterItem selected = centersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Предупреждение", "Выберите центр для редактирования");
            return;
        }
        openCenterForm(selected);
    }

    @FXML
    private void onDelete() {
        CenterItem selected = centersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Предупреждение", "Выберите центр");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить центр \"" + selected.getName() + "\"?\nВсе залы центра будут удалены.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Button yesButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.YES);
        Button noButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.NO);
        yesButton.setText("ДА");
        noButton.setText("НЕТ");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            Response response = NetworkClient.sendRequest("DELETE_CENTER", Serializer.toJson(String.valueOf(selected.getCenterId())));
            if (response.isSuccess()) {
                showInfo("Успех", response.getMessage());
                loadCenters();
            } else {
                showError("Ошибка", response.getMessage());
            }
        }
    }

    private void openCenterForm(CenterItem center) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/center_form.fxml"));
            Parent root = loader.load();
            CenterFormController controller = loader.getController();
            controller.initData(center, this::loadCenters);
            Scene scene = new Scene(root, 950, 750);
            Stage stage = new Stage();
            stage.setTitle(center == null ? "Добавление культурного центра" : "Редактирование центра");
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось открыть форму центра");
        }
    }

    @FXML
    private void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin.fxml"));
            Parent root = loader.load();

            AdminController controller = loader.getController();

            // передаём данные в AdminController
            Map<String, Object> userData = new HashMap<>();
            userData.put("login", currentLogin);
            userData.put("role", "ADMIN");
            userData.put("fullName", fullName);
            controller.initUserData(currentLogin, userData);

            Scene scene = new Scene(root, 750, 450);
            Stage stage = (Stage) centersTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Панель управления - ADMIN");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось вернуться назад");
        }
    }
}