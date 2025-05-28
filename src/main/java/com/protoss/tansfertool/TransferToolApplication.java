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
            // 加载FXML
            FXMLLoader fxmlLoader = new FXMLLoader(TransferToolApplication.class
                    .getResource("/com/protoss/tansfertool/fxml/transfer-view.fxml"));
            Parent root = fxmlLoader.load();

            // 配置控制器
            TransferToolController controller = fxmlLoader.getController();
            controller.setPrimaryStage(stage);

            // 创建场景并加载样式
            Scene scene = new Scene(root);
            loadStyles(scene);

            // 配置窗口
            stage.setScene(scene);
            stage.setTitle(WINDOW_TITLE);

            // 加载图标
            var iconUrl = getClass().getResource("/com/protoss/tansfertool/images/logo2.png");
            if (iconUrl != null) {
                stage.getIcons().add(new Image(iconUrl.openStream()));
            }

            // 配置窗口属性
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

    private void showErrorAndExit(Exception e) {
        System.err.println("Application failed to start: " + e.getMessage());
        e.printStackTrace();
        System.exit(1);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
