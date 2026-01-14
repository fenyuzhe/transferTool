package com.protoss.tansfertool;

import com.protoss.tansfertool.controller.TransferToolController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferToolApplication extends Application {
    private static final Logger log = LoggerFactory.getLogger(TransferToolApplication.class);
    private static final String WINDOW_TITLE = "图像转移工具";

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(TransferToolApplication.class
                    .getResource("/com/protoss/tansfertool/fxml/transfer-view.fxml"));
            Parent root = fxmlLoader.load();

            TransferToolController controller = fxmlLoader.getController();
            controller.setPrimaryStage(stage);

            Scene scene = new Scene(root);
            loadFonts();
            loadStyles(scene);

            stage.setScene(scene);
            stage.setTitle(WINDOW_TITLE);

            var iconUrl = getClass().getResource("/com/protoss/tansfertool/images/logo2.png");
            if (iconUrl != null) {
                stage.getIcons().add(new Image(iconUrl.openStream()));
            }

            stage.initStyle(StageStyle.DECORATED);
            stage.setResizable(false);
            stage.show();

            log.info("Application started successfully");
        } catch (Exception e) {
            log.error("Failed to start application", e);
            showErrorAndExit(e);
        }
    }

    private void loadStyles(Scene scene) {
        try {
            String cssPath = "/com/protoss/tansfertool/styles/modern-styles.css";
            var cssUrl = getClass().getResource(cssPath);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                log.debug("Styles loaded successfully");
            } else {
                log.warn("Style file not found: {}", cssPath);
            }
        } catch (Exception e) {
            log.error("Failed to load styles", e);
        }
    }

    private void loadFonts() {
        try {
            String fontPath = "/com/protoss/tansfertool/fonts/SimSun.ttf";
            var fontUrl = getClass().getResource(fontPath);
            if (fontUrl != null) {
                javafx.scene.text.Font.loadFont(fontUrl.toExternalForm(), 12);
                log.info("Loaded custom font: SimSun");
            } else {
                log.warn("Custom font not found: {}", fontPath);
            }
        } catch (Exception e) {
            log.error("Failed to load custom fonts", e);
        }
    }

    private void showErrorAndExit(Exception e) {
        System.err.println("Application failed to start: " + e.getMessage());
        e.printStackTrace();
        System.exit(1);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
