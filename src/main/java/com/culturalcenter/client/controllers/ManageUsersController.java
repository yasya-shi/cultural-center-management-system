package com.culturalcenter.client.controllers;

import com.culturalcenter.client.models.UserItem;
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

public class ManageUsersController extends BaseController {

    @FXML private TableView<UserItem> usersTable;
    @FXML private TableColumn<UserItem, Integer> colId;
    @FXML private TableColumn<UserItem, String> colLogin;
    @FXML private TableColumn<UserItem, String> colFullName;
    @FXML private TableColumn<UserItem, String> colEmail;
    @FXML private TableColumn<UserItem, String> colPhone;
    @FXML private TableColumn<UserItem, String> colRole;
    @FXML private TableColumn<UserItem, String> colStatus;

    private ObservableList<UserItem> userList = FXCollections.observableArrayList();

    private String currentLogin;
    private String fullName;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colLogin.setCellValueFactory(new PropertyValueFactory<>("login"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        usersTable.setItems(userList);
        usersTable.setStyle("-fx-border-color: #8b3a3a; -fx-border-width: 2; -fx-selection-bar: #c97a7a; -fx-selection-bar-non-focused: #d4a0a0;");

        loadUsers();
    }

    public void initData(String login, String fullName) {
        this.currentLogin = login;
        this.fullName = fullName;
    }

    private void loadUsers() {
        Response response = NetworkClient.sendRequest("GET_ALL_USERS", "");
        if (response.isSuccess()) {
            List<Map<String, Object>> users = Serializer.fromJson(response.getData(), List.class);
            userList.clear();
            for (Map<String, Object> user : users) {
                userList.add(new UserItem(
                        ((Number) user.get("userId")).intValue(),
                        (String) user.get("login"),
                        (String) user.get("fullName"),
                        (String) user.get("email"),
                        (String) user.get("phone"),
                        (String) user.get("role"),
                        (String) user.get("status")
                ));
            }
            usersTable.refresh();
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    @FXML
    private void onAdd() {
        openUserForm(null);
    }

    @FXML
    private void onEdit() {
        UserItem selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Предупреждение", "Выберите пользователя для редактирования");
            return;
        }
        openUserForm(selected);
    }

    @FXML
    private void onBlock() {
        UserItem selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Предупреждение", "Выберите пользователя");
            return;
        }
        if ("BLOCKED".equals(selected.getStatus())) {
            showWarning("Предупреждение", "Пользователь уже заблокирован");
            return;
        }
        Response response = NetworkClient.sendRequest("BLOCK_USER", Serializer.toJson(String.valueOf(selected.getUserId())));
        if (response.isSuccess()) {
            showInfo("Успех", response.getMessage());
            loadUsers();
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    @FXML
    private void onUnblock() {
        UserItem selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Предупреждение", "Выберите пользователя");
            return;
        }
        if ("ACTIVE".equals(selected.getStatus())) {
            showWarning("Предупреждение", "Пользователь уже активен");
            return;
        }
        Response response = NetworkClient.sendRequest("UNBLOCK_USER", Serializer.toJson(String.valueOf(selected.getUserId())));
        if (response.isSuccess()) {
            showInfo("Успех", response.getMessage());
            loadUsers();
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        UserItem selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Предупреждение", "Выберите пользователя");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Удалить пользователя " + selected.getLogin() + "?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Button yesButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.YES);
        Button noButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.NO);
        yesButton.setText("ДА");
        noButton.setText("НЕТ");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            Response response = NetworkClient.sendRequest("DELETE_USER", Serializer.toJson(String.valueOf(selected.getUserId())));
            if (response.isSuccess()) {
                showInfo("Успех", response.getMessage());
                loadUsers();
            } else {
                showError("Ошибка", response.getMessage());
            }
        }
    }

    @FXML
    private void onAssignDirector() {
        UserItem selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Предупреждение", "Выберите пользователя");
            return;
        }
        if (!"DIRECTOR".equals(selected.getRole())) {
            showWarning("Предупреждение", "Пользователь должен иметь роль DIRECTOR");
            return;
        }
        openAssignDirectorDialog(selected);
    }

    private void openAssignDirectorDialog(UserItem user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/assign_director.fxml"));
            Parent root = loader.load();
            AssignDirectorController controller = loader.getController();
            controller.initData(user, this::loadUsers);
            Scene scene = new Scene(root, 500, 350);
            Stage stage = new Stage();
            stage.setTitle("Назначить директором");
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось открыть окно назначения");
        }
    }

    private void openUserForm(UserItem user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user_form.fxml"));
            Parent root = loader.load();
            UserFormController controller = loader.getController();
            controller.initData(user, this::loadUsers);
            Scene scene = new Scene(root, 600, 550);
            Stage stage = new Stage();
            stage.setTitle(user == null ? "Добавление пользователя" : "Редактирование пользователя");
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось открыть форму");
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
            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Панель управления - ADMIN");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось вернуться назад");
        }
    }
}