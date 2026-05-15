package com.protoss.toolkit.controller;

import com.protoss.toolkit.thread.CountDirFilesTask;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class WaitingController implements Initializable {
    private static Logger log = LoggerFactory.getLogger(WaitingController.class);
    @FXML
    private VBox vboxPane;
    @FXML
    private Label lb_message;

    private Stage dialogStage;
    private Stage primaryStage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        vboxPane.setBackground(Background.EMPTY);
    }

    public void setPrimaryStage(CountDirFilesTask task, Stage stage, Label labelsize, Label labelcount, HBox hBox) {
        prepare(stage, "正在计算大小，请稍候...");

        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                dialogStage.close();
                try {
                    Map<String, Long> value = task.get();
                    labelsize.setText(value.get("size") / 1024 / 1024 + "MB");
                    labelcount.setText(value.get("count") + "个");
                    hBox.setDisable(false);
                } catch (Exception e) {
                    log.error("Error retrieving task results", e);
                }
            }
        });
        task.setOnFailed(workerStateEvent -> {
            dialogStage.close();
            hBox.setDisable(false);
            log.error("Count task failed", task.getException());
        });
        task.setOnCancelled(workerStateEvent -> {
            dialogStage.close();
            hBox.setDisable(false);
            log.warn("Count task cancelled");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public void prepare(Stage stage, String message) {
        if (this.primaryStage == null) {
            this.primaryStage = stage;
        }
        if (lb_message != null && message != null && !message.isEmpty()) {
            lb_message.setText(message);
        }
        Scene scene = new Scene(vboxPane);
        scene.setFill(null);
        var cssUrl = getClass().getResource("/com/protoss/toolkit/styles/modern-styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        dialogStage = new Stage();
        dialogStage.setScene(scene);
        dialogStage.initOwner(primaryStage);
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initModality(Modality.WINDOW_MODAL);
    }

    public Stage getDialogStage() {
        return dialogStage;
    }

    public void activateWating() {
        this.dialogStage.show();
    }

    public void cancelWating() {
        this.dialogStage.close();
    }
}