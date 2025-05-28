package com.protoss.tansfertool.controller;

import cn.hutool.core.convert.Convert;
import com.protoss.tansfertool.TransferToolApplication;
import com.protoss.tansfertool.thread.CountDirFilesTask;
import com.protoss.tansfertool.entity.DirEntry;
import com.protoss.tansfertool.thread.TransferTask;
import com.protoss.tansfertool.util.TransferFileUtil;
import javafx.collections.FXCollections;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.UnaryOperator;

public class TransferToolController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(TransferToolController.class);
    private static final String IMAGEIO_MODE = "imageio";
    private static final String OPENCV_MODE = "opencv";

    @FXML private BorderPane rootPane;
    @FXML private BorderPane rootPane1;

    // 基础布局组件
    @FXML private HBox hbox_1;
    @FXML private HBox hbox_2;
    @FXML private HBox hbox_3;
    @FXML private VBox hbox_4;
    @FXML private VBox hbox_5;
    @FXML private HBox hbox_6;
    @FXML private HBox hbox_7;

    // 文本输入框
    @FXML private TextField txt_sourceDir;
    @FXML private TextField txt_desDir;
    @FXML private TextField txt_filtersize;
    @FXML private TextField txt_threads;

    // 组合框
    @FXML private ComboBox<String> box_strategy;

    // 进度相关
    @FXML private ProgressBar progressBar;
    @FXML private Label lb_fileslength;
    @FXML private Label lb_filescount;
    @FXML private Label lb_percent;

    // 按钮
    @FXML private Button btn_start;
    @FXML private Button btn_pause;
    @FXML private Button btn_resume;
    @FXML private Button btn_sourceDir;
    @FXML private Button btn_desDir;

    // 复选框和单选按钮
    @FXML private CheckBox cb_compressed;
    @FXML private RadioButton radio_timefilter;
    @FXML private RadioButton radio_all;
    @FXML private RadioButton radio_imageio;
    @FXML private RadioButton radio_opencv;

    // 日期选择器
    @FXML private DatePicker dp_start;
    @FXML private DatePicker dp_end;

    // ToggleGroups
    @FXML private ToggleGroup filterGroup;
    @FXML private ToggleGroup compressionGroup;

    private Stage primaryStage;
    private final String transcode = "1.2.840.10008.1.2.4.90";
    private final String pattern = "(([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})(((0[13578]|1[02])(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)(0[1-9]|[12][0-9]|30))|(02(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))0229)";
    private List<File> fileList;
    private TransferTask task;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeUI();
        initializeControls();
        initializeEventHandlers();
    }

    private void initializeUI() {
        // 设置组合框选项
        box_strategy.setItems(FXCollections.observableArrayList("复制", "移动"));
        box_strategy.getSelectionModel().select(0);

        // 禁用按钮
        btn_pause.setDisable(true);
        btn_resume.setDisable(true);

        // 设置文本框提示
        txt_sourceDir.setPromptText("请选择源文件夹");
        txt_desDir.setPromptText("请选择目标文件夹");

        // 初始化文本格式化器
        setupTextFormatters();
    }

    private void setupTextFormatters() {
        UnaryOperator<TextFormatter.Change> numericFilter = change -> {
            String text = change.getControlNewText();
            if (text.matches("[0-9]*")) {
                return change;
            }
            return null;
        };

        txt_filtersize.setTextFormatter(new TextFormatter<>(numericFilter));
        txt_threads.setTextFormatter(new TextFormatter<>(numericFilter));
        txt_threads.setText("8");
    }

    private void initializeControls() {
        if (filterGroup == null) {
            filterGroup = new ToggleGroup();
        }
        if (compressionGroup == null) {
            compressionGroup = new ToggleGroup();
        }

        radio_timefilter.setToggleGroup(filterGroup);
        radio_all.setToggleGroup(filterGroup);
        radio_imageio.setToggleGroup(compressionGroup);
        radio_opencv.setToggleGroup(compressionGroup);

        radio_timefilter.setSelected(true);
        hbox_4.setDisable(true);
        hbox_5.setDisable(true);
        hbox_6.setDisable(true);
        hbox_7.setDisable(true);
    }

    private void initializeEventHandlers() {
        btn_start.setOnAction(event -> startTransfer());
        btn_pause.setOnAction(event -> pauseTransfer());
        btn_resume.setOnAction(event -> resumeTransfer());
        cb_compressed.setOnAction(event -> handleCompressionToggle());
        radio_timefilter.setOnAction(event -> handleTimeFilterToggle());
        radio_all.setOnAction(event -> handleAllFilesToggle());
        dp_start.setOnAction(event -> handleDateSelection());
        dp_end.setOnAction(event -> handleDateSelection());
    }

    private void startTransfer() {
        if (Objects.nonNull(fileList)) {
            String compressMode = radio_imageio.isSelected() ? IMAGEIO_MODE :
                    radio_opencv.isSelected() ? OPENCV_MODE : "";

            task = new TransferTask(
                    compressMode,
                    Convert.toLong(lb_filescount.getText().replace("个", "")),
                    fileList,
                    txt_sourceDir.getText(),
                    txt_desDir.getText(),
                    box_strategy.getSelectionModel().getSelectedItem(),
                    cb_compressed.isSelected(),
                    transcode,
                    Convert.toLong(txt_filtersize.getText().trim().isEmpty() ? "0" : txt_filtersize.getText().trim()),
                    Convert.toInt(txt_threads.getText().trim().isEmpty() ? "8" : txt_threads.getText().trim())
            );

            setupProgressBar();
            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
            task.setOnSucceeded(this::handleTaskCompletion);
        }
    }

    private void pauseTransfer() {
        if (task != null) {
            task.pause();
            btn_pause.setDisable(true);
            btn_resume.setDisable(false);
        }
    }

    private void resumeTransfer() {
        if (task != null) {
            task.resume();
            btn_pause.setDisable(false);
            btn_resume.setDisable(true);
        }
    }

    private void handleCompressionToggle() {
        boolean isSelected = cb_compressed.isSelected();
        txt_filtersize.setDisable(!isSelected);
        hbox_7.setDisable(!isSelected);
        if (isSelected) {
            radio_imageio.setSelected(true);
        } else {
            radio_imageio.setSelected(false);
            radio_opencv.setSelected(false);
            txt_filtersize.clear();
        }
    }

    private void handleTimeFilterToggle() {
        lb_filescount.setText("");
        lb_fileslength.setText("");
        dp_start.setDisable(false);
        dp_end.setDisable(false);
        hbox_6.setDisable(true);
    }

    private void handleAllFilesToggle() {
        clearDateFilters();
        loadWaitingController();
    }

    private void handleDateSelection() {
        if (txt_sourceDir.getText() != null && dp_start.getValue() != null && dp_end.getValue() != null) {
            if (dp_start.getValue().isBefore(dp_end.getValue()) || dp_start.getValue().isEqual(dp_end.getValue())) {
                List<DirEntry> list = new ArrayList<>();
                TransferFileUtil.getDir(new File(txt_sourceDir.getText()), list, pattern, dp_start.getValue(), dp_end.getValue());
                if (Objects.nonNull(list)) {
                    Collections.sort(list);
                    fileList = new ArrayList<>();
                    for (DirEntry entry : list) {
                        fileList.add(new File(entry.getDirPath()));
                    }
                    loadWaitingController();
                }
            }
        }
    }

    private void clearDateFilters() {
        dp_start.setValue(null);
        dp_end.setValue(null);
        lb_filescount.setText("");
        lb_fileslength.setText("");
        dp_start.setDisable(true);
        dp_end.setDisable(true);
        hbox_6.setDisable(true);
    }

    private void loadWaitingController() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/protoss/tansfertool/fxml/wating-view.fxml"));
            fxmlLoader.load();
            WaitingController controller = fxmlLoader.getController();
            CountDirFilesTask task = new CountDirFilesTask(fileList);
            controller.setPrimaryStage(task, primaryStage, lb_fileslength, lb_filescount, hbox_6);
            controller.activateWating();
        } catch (IOException e) {
            log.error("Error loading waiting view", e);
        }
    }

    private void setupProgressBar() {
        btn_start.setDisable(true);
        btn_pause.setDisable(false);
        btn_resume.setDisable(true);
        progressBar.progressProperty().unbind();
        lb_percent.textProperty().unbind();
        progressBar.setProgress(0.0f);
        progressBar.progressProperty().bind(task.progressProperty());
        lb_percent.textProperty().bind(task.messageProperty());
        setDisabled(true);
    }

    private void handleTaskCompletion(WorkerStateEvent event) {
        try {
            if (task.get() == 1) {
                resetUIAfterCompletion();
            }
        } catch (Exception ex) {
            log.error("Error on task completion", ex);
        }
    }

    private void resetUIAfterCompletion() {
        btn_start.setDisable(false);
        btn_pause.setDisable(true);
        btn_resume.setDisable(true);
        setDisabled(false);
    }

    @FXML
    public void handlerButtonAction(javafx.event.ActionEvent event) {
        if (!(event.getSource() instanceof Button btn)) {
            return;
        }

        TextField targetField = btn.getId().equals("btn_sourceDir") ? txt_sourceDir : txt_desDir;

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择文件夹");
        File selectedDirectory = directoryChooser.showDialog(primaryStage);

        if (selectedDirectory != null) {
            targetField.setText(selectedDirectory.getAbsolutePath());
            if (btn.getId().equals("btn_sourceDir")) {
                hbox_4.setDisable(false);
                hbox_5.setDisable(false);
            }
        }
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    private void setDisabled(boolean flag) {
        hbox_1.setDisable(flag);
        hbox_2.setDisable(flag);
        hbox_3.setDisable(flag);
        hbox_4.setDisable(flag);
        hbox_5.setDisable(flag);
    }
}
