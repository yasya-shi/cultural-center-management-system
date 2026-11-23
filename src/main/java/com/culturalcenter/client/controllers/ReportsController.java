package com.culturalcenter.client.controllers;

import com.culturalcenter.client.models.ReportItem;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportsController extends BaseController {

    @FXML private ComboBox<String> periodCombo;
    @FXML private ComboBox<String> reportTypeCombo;
    @FXML private TableView<ReportItem> reportTable;
    @FXML private TableColumn<ReportItem, String> colRoomName;
    @FXML private TableColumn<ReportItem, Double> colHours;
    @FXML private TableColumn<ReportItem, Integer> colBookingCount;
    @FXML private TableColumn<ReportItem, Double> colRating;

    private ObservableList<ReportItem> reportList = FXCollections.observableArrayList();
    private int currentCenterId = -1;
    private String currentLogin;

    @FXML
    public void initialize() {
        colRoomName.setCellValueFactory(new PropertyValueFactory<>("roomName"));
        colHours.setCellValueFactory(new PropertyValueFactory<>("hours"));
        colBookingCount.setCellValueFactory(new PropertyValueFactory<>("bookingCount"));
        colRating.setCellValueFactory(new PropertyValueFactory<>("averageRating"));

        reportTable.setItems(reportList);
        reportTable.setStyle("-fx-border-color: #8b3a3a; -fx-border-width: 2; -fx-selection-bar: #c97a7a; -fx-selection-bar-non-focused: #d4a0a0;");

        periodCombo.setItems(FXCollections.observableArrayList("День", "Неделя", "Месяц", "Год"));
        periodCombo.setValue("Месяц");

        reportTypeCombo.setItems(FXCollections.observableArrayList("Загрузка залов", "Количество заявок", "Средняя оценка"));
        reportTypeCombo.setValue("Загрузка залов");
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
        }
    }

    private String getStartDate() {
        LocalDate now = LocalDate.now();
        String period = periodCombo.getValue();
        switch (period) {
            case "День": return now.toString();
            case "Неделя": return now.minusWeeks(1).toString();
            case "Месяц": return now.minusMonths(1).toString();
            case "Год": return now.minusYears(1).toString();
            default: return now.minusMonths(1).toString();
        }
    }

    @FXML
    private void onGenerateReport() {
        if (currentCenterId <= 0) {
            showError("Ошибка", "Центр не найден");
            return;
        }

        String startDate = getStartDate();
        String endDate = LocalDate.now().toString();
        String reportType = reportTypeCombo.getValue();

        Map<String, String> params = new HashMap<>();
        params.put("centerId", String.valueOf(currentCenterId));
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("reportType", reportType);

        Response response = NetworkClient.sendRequest("GENERATE_REPORT", Serializer.toJson(params));

        if (response.isSuccess()) {
            List<Map<String, Object>> data = Serializer.fromJson(response.getData(), List.class);
            reportList.clear();
            for (Map<String, Object> item : data) {
                reportList.add(new ReportItem(
                        (String) item.get("roomName"),
                        ((Number) item.getOrDefault("hours", 0)).doubleValue(),
                        ((Number) item.getOrDefault("bookingCount", 0)).intValue(),
                        ((Number) item.getOrDefault("averageRating", 0)).doubleValue()
                ));
            }
            updateColumnsVisibility(reportType);
        } else {
            showError("Ошибка", response.getMessage());
        }
    }

    private void updateColumnsVisibility(String reportType) {
        switch (reportType) {
            case "Загрузка залов":
                colHours.setVisible(true);
                colBookingCount.setVisible(false);
                colRating.setVisible(false);
                break;
            case "Количество заявок":
                colHours.setVisible(false);
                colBookingCount.setVisible(true);
                colRating.setVisible(false);
                break;
            case "Средняя оценка":
                colHours.setVisible(false);
                colBookingCount.setVisible(false);
                colRating.setVisible(true);
                break;
        }
    }

    @FXML
    private void onSaveToFile() {
        if (reportList.isEmpty()) {
            showWarning("Предупреждение", "Нет данных для сохранения");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить отчёт");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы", "*.csv"));
        File file = fileChooser.showSaveDialog(reportTable.getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("Название зала;Часы;Количество заявок;Средняя оценка");
                for (ReportItem item : reportList) {
                    writer.printf("%s;%.2f;%d;%.2f%n",
                            item.getRoomName(),
                            item.getHours(),
                            item.getBookingCount(),
                            item.getAverageRating());
                }
                showInfo("Успех", "Отчёт сохранён в " + file.getName());
            } catch (Exception e) {
                showError("Ошибка", "Не удалось сохранить файл");
                e.printStackTrace();
            }
        }
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

            controller.initUserData(currentLogin, userData);

            Scene scene = new Scene(root, 750, 450);
            Stage stage = (Stage) periodCombo.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Панель управления - DIRECTOR");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось вернуться назад");
        }
    }
}