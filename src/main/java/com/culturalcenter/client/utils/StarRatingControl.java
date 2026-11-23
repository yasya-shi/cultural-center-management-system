package com.culturalcenter.client.utils;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public class StarRatingControl extends HBox {
// Текущая оценка (0 - ничего не выбрано, 1-5 - количество звёзд)
    private int rating = 0;
    // Массив из 5 кнопок (каждая кнопка - одна звезда)
    private final Button[] stars = new Button[5];
// Конструктор - создаёт 5 звёзд и настраивает их внешний вид

    public StarRatingControl() {
        setSpacing(5);
        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            // Создаём кнопку с пустой звездой
            Button star = new Button("☆");
            star.setStyle("-fx-font-size: 22; -fx-text-fill: #d4a0a0; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
            star.setPrefWidth(35);
            star.setPrefHeight(35);
            star.setOnAction(e -> setRating(starIndex));
            stars[i] = star;
            getChildren().add(star);
        }
    }

    public void setRating(int rating) {
        // Сохраняем новую оценку
        this.rating = rating;
        // Обновляем каждую звезду
        for (int i = 0; i < 5; i++) {
            if (i < rating) {
                stars[i].setText("★");
                stars[i].setStyle("-fx-font-size: 22; -fx-text-fill: #c97a7a; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
            } else {
                stars[i].setText("☆");
                stars[i].setStyle("-fx-font-size: 22; -fx-text-fill: #d4a0a0; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
            }
        }
    }

    public int getRating() {
        return rating;
    }

    public void clear() {
        setRating(0);
    }
}