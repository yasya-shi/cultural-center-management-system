package com.culturalcenter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
//start() — точка входа JavaFX-приложения
    @Override
    public void start(Stage primaryStage) throws Exception { // ← точка входа JavaFX
        Parent root = FXMLLoader.load(getClass().getResource("/views/login.fxml"));
        primaryStage.setTitle("Культурный центр - Вход в систему");
        primaryStage.setScene(new Scene(root, 600, 700));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args); // ← запускает JavaFX (	launch() запускает JavaFX runtime)
    }
}