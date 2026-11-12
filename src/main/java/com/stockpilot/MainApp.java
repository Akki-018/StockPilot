package com.stockpilot;

import com.stockpilot.controller.MainLayoutController;
import com.stockpilot.util.ThemeManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX entry point for the StockPilot Inventory Management System.
 * Coordinates window initialization and binds the centralized ThemeManager.
 */
public class MainApp extends Application {

    private static final String APP_TITLE = "StockPilot - Inventory Management System";
    private static final double INITIAL_WIDTH = 1240;
    private static final double INITIAL_HEIGHT = 780;
    private static final double MIN_WIDTH = 980;
    private static final double MIN_HEIGHT = 640;

    @Override
    public void start(Stage primaryStage) {
        MainLayoutController mainController = new MainLayoutController();
        Scene scene = new Scene(mainController.getRootLayout(), INITIAL_WIDTH, INITIAL_HEIGHT);

        // Bind ThemeManager to the application Scene (applies saved theme preference)
        ThemeManager.bindScene(scene);

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
