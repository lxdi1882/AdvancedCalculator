package com.calculator;

import com.calculator.ui.CalculatorView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        CalculatorView view = new CalculatorView();
        Scene scene = new Scene(view.getRoot(), 560, 820);
        scene.getStylesheets().add(getClass().getResource("/com/calculator/style.css").toExternalForm());

        stage.setTitle("Advanced Calculator");
        stage.setMinWidth(480);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}