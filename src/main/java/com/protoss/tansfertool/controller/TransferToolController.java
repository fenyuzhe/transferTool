package com.protoss.tansfertool.controller;

import cn.hutool.core.convert.Convert;

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

    @FXML
    private BorderPane rootPane;
    @FXML
    private BorderPane rootPane1;

    // 基础布局组件
    @FXML
    private VBox vbox_paths;
    @FXML
    private HBox hbox_3;
    @FXML
    private VBox hbox_4;
    @FXML
    private VBox hbox_5;
    @FXML
    private HBox hbox_6;
    @FXML
    private HBox hbox_7;

    @FXML
    private TextField txt_sourceDir;
    @FXML
    private TextField txt_desDir;
    @FXML
    private TextField txt_filtersize;
    @FXML
    private TextField txt_threads;

    @FXML
    private ComboBox<String> box_strategy;

    // 进度相关
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label lb_fileslength;
    @FXML
    private Label lb_filescount;
    @FXML
    private Label lb_percent;

    @FXML
    private Button btn_start;
    @FXML
    private Button btn_pause;
    @FXML
    private Button btn_resume;
    @FXML
    private Button btn_sourceDir;
    @FXML
    private Button btn_desDir;

    @FXML
    private CheckBox cb_compressed;
    @FXML
    private RadioButton radio_timefilter;
    @FXML
    private RadioButton radio_all;
    @FXML
    private RadioButton radio_imageio;
    @FXML
    private RadioButton radio_opencv;

    @FXML
    private DatePicker dp_start;
    @FXML
    private DatePicker dp_end;

    @FXML
    private ToggleGroup filterGroup;
    @FXML
    private ToggleGroup compressionGroup;

    private Stage primaryStage;
    private final String transcode = "1.2.840.10008.1.2.4.90";
    private final String pattern = "(([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})(((0[13578]|1[02])(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)(0[1-9]|[12][0-9]|30))|(02(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))0229)";
    private List<File> fileList;
    private TransferTask task;

    private void initializeUI() {
        box_strategy.setItems(FXCollections.observableArrayList("复制", "移动"));
        box_strategy.getSelectionModel().select(0);

        btn_pause.setDisable(true);
        btn_resume.setDisable(true);

        txt_sourceDir.setPromptText("请选择源文件夹");
        txt_desDir.setPromptText("请选择目标文件夹");

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
            String compressMode = radio_imageio.isSelected() ? IMAGEIO_MODE
                    : radio_opencv.isSelected() ? OPENCV_MODE : "";

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
                    Convert.toInt(txt_threads.getText().trim().isEmpty() ? "8" : txt_threads.getText().trim()));

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
        String sourcePath = txt_sourceDir.getText();
        if (sourcePath != null && !sourcePath.isEmpty()) {
            fileList = new ArrayList<>();
            fileList.add(new File(sourcePath));
            loadWaitingController();
        }
    }

    private void handleDateSelection() {
        if (txt_sourceDir.getText() != null && dp_start.getValue() != null && dp_end.getValue() != null) {
            if (dp_start.getValue().isBefore(dp_end.getValue()) || dp_start.getValue().isEqual(dp_end.getValue())) {
                List<DirEntry> list = new ArrayList<>();
                TransferFileUtil.getDir(new File(txt_sourceDir.getText()), list, pattern, dp_start.getValue(),
                        dp_end.getValue());
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
        if (fileList == null || fileList.isEmpty()) {
            return;
        }
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("/com/protoss/tansfertool/fxml/wating-view.fxml"));
            fxmlLoader.load();
            WaitingController controller = fxmlLoader.getController();
            CountDirFilesTask task = new CountDirFilesTask(fileList);
            task.setFilterPattern(pattern);
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

    // 定时转移相关组件
    @FXML
    private TextField txt_schedSource;
    @FXML
    private TextField txt_schedTarget;
    @FXML
    private Button btn_schedSource;
    @FXML
    private Button btn_schedTarget;
    @FXML
    private TextField txt_interval;
    @FXML
    private ComboBox<String> box_schedStrategy;
    @FXML
    private ComboBox<String> box_schedCompress;
    @FXML
    private Label lb_schedStatus;
    @FXML
    private Label lb_lastRun;
    @FXML
    private TextField txt_startTime;
    @FXML
    private TextField txt_daysAgo;
    @FXML
    private TextArea txt_schedLog;
    @FXML
    private Button btn_startSched;
    @FXML
    private Button btn_stopSched;

    private java.util.concurrent.ScheduledExecutorService scheduler;
    private java.util.concurrent.ScheduledFuture<?> scheduledTask;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeUI();
        initializeControls();
        initializeEventHandlers();
        initializeSchedControls();
    }

    private void initializeSchedControls() {
        box_schedStrategy.setItems(FXCollections.observableArrayList("复制", "移动"));
        box_schedStrategy.getSelectionModel().select(0);

        box_schedCompress.setItems(FXCollections.observableArrayList("不压缩", "ImageIO", "OpenCV"));
        box_schedCompress.getSelectionModel().select(0);

        txt_interval.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getControlNewText();
            if (text.matches("[0-9]*"))
                return change;
            return null;
        }));

        txt_daysAgo.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getControlNewText();
            if (text.matches("[0-9]*"))
                return change;
            return null;
        }));

        btn_startSched.setOnAction(e -> startSchedule());
        btn_stopSched.setOnAction(e -> stopSchedule());
    }

    private void startSchedule() {
        if (txt_schedSource.getText() == null || txt_schedSource.getText().isEmpty() ||
                txt_schedTarget.getText() == null || txt_schedTarget.getText().isEmpty()) {
            logToSched("错误: 请先选择源路径和目标路径");
            return;
        }

        long interval = Convert.toLong(txt_interval.getText(), 60L);
        if (interval <= 0) {
            logToSched("错误: 间隔时间必须大于0");
            return;
        }

        long initialDelay = 0;
        String startTimeStr = txt_startTime.getText();
        if (startTimeStr != null && !startTimeStr.trim().isEmpty()) {
            try {
                String[] parts = startTimeStr.trim().split("[:：]");
                if (parts.length == 2) {
                    int hour = Integer.parseInt(parts[0]);
                    int minute = Integer.parseInt(parts[1]);

                    if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                        logToSched("错误: 时间格式无效 (00:00 - 23:59)");
                        return;
                    }

                    Calendar now = Calendar.getInstance();
                    Calendar target = Calendar.getInstance();
                    target.set(Calendar.HOUR_OF_DAY, hour);
                    target.set(Calendar.MINUTE, minute);
                    target.set(Calendar.SECOND, 0);
                    target.set(Calendar.MILLISECOND, 0);

                    if (target.before(now)) {
                        target.add(Calendar.DAY_OF_MONTH, 1);
                    }

                    initialDelay = target.getTimeInMillis() - now.getTimeInMillis();
                    logToSched("任务将在 " + cn.hutool.core.date.DateUtil.format(target.getTime(), "yyyy-MM-dd HH:mm:ss")
                            + " 启动");
                } else {
                    logToSched("错误: 时间格式错误，请使用 HH:mm");
                    return;
                }
            } catch (NumberFormatException e) {
                logToSched("错误: 时间格式错误，请使用 HH:mm");
                return;
            }
        } else {
            logToSched("任务立即启动");
        }

        scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        logToSched("定时任务已启动，执行间隔: " + interval + " 分钟");
        lb_schedStatus.setText("运行中");
        lb_schedStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        btn_startSched.setDisable(true);
        btn_stopSched.setDisable(false);
        setSchedInputsDisable(true);

        // 使用 MILLISECONDS 确保精度和单位正确
        scheduledTask = scheduler.scheduleWithFixedDelay(this::runScheduledTransfer, initialDelay, interval * 60 * 1000,
                java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void stopSchedule() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        logToSched("定时任务已停止");
        lb_schedStatus.setText("已停止");
        lb_schedStatus.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold;");
        btn_startSched.setDisable(false);
        btn_stopSched.setDisable(true);
        setSchedInputsDisable(false);
    }

    private void runScheduledTransfer() {
        javafx.application.Platform.runLater(() -> {
            lb_lastRun.setText(cn.hutool.core.date.DateUtil.now());
            logToSched("开始执行定时转移...");
        });

        try {
            String sourcePath = txt_schedSource.getText();
            String targetPath = txt_schedTarget.getText();
            String strategy = box_schedStrategy.getSelectionModel().getSelectedItem();
            String compressSelection = box_schedCompress.getSelectionModel().getSelectedItem();
            boolean isCompress = !"不压缩".equals(compressSelection);
            String compressMode = "ImageIO".equals(compressSelection) ? IMAGEIO_MODE
                    : "OpenCV".equals(compressSelection) ? OPENCV_MODE : "";
            int daysAgo = Convert.toInt(txt_daysAgo.getText(), 0);

            File root = new File(sourcePath);
            if (!root.exists()) {
                javafx.application.Platform.runLater(() -> logToSched("错误: 源路径不存在 " + sourcePath));
                return;
            }

            List<File> roots = new ArrayList<>();
            if (daysAgo > 0) {
                Date targetDate = cn.hutool.core.date.DateUtil.offsetDay(new Date(), -daysAgo);
                String targetDateStr = cn.hutool.core.date.DateUtil.format(targetDate, "yyyyMMdd"); // 假设文件夹格式包含日期
                logToSched("筛选包含日期 " + targetDateStr + " 的文件夹 (N=" + daysAgo + ")");

                List<DirEntry> list = new ArrayList<>();
                java.time.LocalDate localTargetDate = java.time.LocalDate.now().minusDays(daysAgo);
                TransferFileUtil.getDir(root, list, pattern, localTargetDate, localTargetDate);

                for (DirEntry entry : list) {
                    roots.add(new File(entry.getDirPath()));
                }

                if (roots.isEmpty()) {
                    logToSched("未找到 " + localTargetDate + " 的对应文件夹，跳过本次执行");
                    return;
                }
            } else {
                // 如果 0，扫描所有
                roots.add(root);
            }

            TransferTask schedTask = new TransferTask(
                    compressMode,
                    0L,
                    roots,
                    sourcePath,
                    targetPath,
                    strategy,
                    isCompress,
                    transcode,
                    0,
                    4);

            schedTask.run();

            javafx.application.Platform.runLater(() -> logToSched("定时转移执行完成"));

        } catch (Exception e) {
            log.error("Scheduled transfer failed", e);
            javafx.application.Platform.runLater(() -> logToSched("执行失败: " + e.getMessage()));
        }
    }

    private void logToSched(String msg) {
        txt_schedLog.appendText("[" + cn.hutool.core.date.DateUtil.now() + "] " + msg + "\n");
    }

    private void setSchedInputsDisable(boolean disable) {
        txt_schedSource.setDisable(disable);
        txt_schedTarget.setDisable(disable);
        txt_startTime.setDisable(disable);
        txt_daysAgo.setDisable(disable);
        btn_schedSource.setDisable(disable);
        btn_schedTarget.setDisable(disable);
        txt_interval.setDisable(disable);
        box_schedStrategy.setDisable(disable);
        box_schedCompress.setDisable(disable);
    }

    @FXML
    public void handlerButtonAction(javafx.event.ActionEvent event) {
        if (!(event.getSource() instanceof Button btn)) {
            return;
        }

        TextField targetField;
        if (btn.getId().equals("btn_sourceDir"))
            targetField = txt_sourceDir;
        else if (btn.getId().equals("btn_desDir"))
            targetField = txt_desDir;
        else if (btn.getId().equals("btn_schedSource"))
            targetField = txt_schedSource;
        else if (btn.getId().equals("btn_schedTarget"))
            targetField = txt_schedTarget;
        else
            return;

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择文件夹");
        File selectedDirectory = directoryChooser.showDialog(primaryStage);

        if (selectedDirectory != null) {
            targetField.setText(selectedDirectory.getAbsolutePath());
            if (btn.getId().equals("btn_sourceDir")) {
                hbox_4.setDisable(false);
                hbox_5.setDisable(false);
                if (radio_all.isSelected()) {
                    handleAllFilesToggle();
                }
            }
        }
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    private void setDisabled(boolean flag) {
        vbox_paths.setDisable(flag);
        hbox_3.setDisable(flag);
        hbox_4.setDisable(flag);
        hbox_5.setDisable(flag);
    }
}
