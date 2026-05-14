package com.protoss.tansfertool.controller;

import cn.hutool.core.convert.Convert;

import com.protoss.tansfertool.thread.CountDirFilesTask;
import com.protoss.tansfertool.entity.DirEntry;
import com.protoss.tansfertool.thread.TransferTask;
import com.protoss.tansfertool.util.TransferFileUtil;
import javafx.collections.FXCollections;
import javafx.application.Platform;
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
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
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

        // 监听 DatePicker 的 disabled 状态变化，用 Platform.runLater 延迟应用样式，
        // 确保在 JavaFX 渲染帧结束后才覆盖 Modena 默认的 opacity:0.4 效果
        dp_start.disabledProperty().addListener((obs, oldVal, nowDisabled) -> javafx.application.Platform
                .runLater(() -> setDatePickerEditorStyle(dp_start, nowDisabled)));
        dp_end.disabledProperty().addListener((obs, oldVal, nowDisabled) -> javafx.application.Platform
                .runLater(() -> setDatePickerEditorStyle(dp_end, nowDisabled)));

        // 设置初始状态样式（hbox_5 初始化为 disable，监听器不会触发初始值）
        javafx.application.Platform.runLater(() -> {
            setDatePickerEditorStyle(dp_start, true);
            setDatePickerEditorStyle(dp_end, true);
        });
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
            if (!validateTransferPaths(txt_sourceDir.getText(), txt_desDir.getText(), true)) {
                return;
            }

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
            task.setOnSucceeded(this::handleTaskCompletion);
            task.setOnFailed(event -> {
                log.error("Transfer task failed", task.getException());
                resetUIAfterCompletion();
            });
            task.setOnCancelled(event -> resetUIAfterCompletion());
            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        }
    }

    private boolean validateTransferPaths(String sourcePath, String targetPath, boolean showAlert) {
        String error = getTransferPathError(sourcePath, targetPath);
        if (error == null) {
            return true;
        }
        if (showAlert) {
            showValidationError(error);
        } else {
            logToSched("错误: " + error);
        }
        return false;
    }

    private String getTransferPathError(String sourcePath, String targetPath) {
        if (sourcePath == null || sourcePath.trim().isEmpty() || targetPath == null || targetPath.trim().isEmpty()) {
            return "请先选择源目录和目标目录";
        }
        try {
            Path sourceRoot = toComparablePath(Path.of(sourcePath));
            Path targetRoot = toComparablePath(Path.of(targetPath));
            if (sourceRoot.equals(targetRoot)) {
                return "源目录和目标目录不能相同";
            }
            if (targetRoot.startsWith(sourceRoot)) {
                return "目标目录不能位于源目录内部";
            }
            return null;
        } catch (InvalidPathException e) {
            return "目录路径格式不正确: " + e.getInput();
        }
    }

    private Path toComparablePath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        try {
            return normalized.toRealPath();
        } catch (IOException e) {
            Path parent = normalized.getParent();
            if (parent != null && Files.exists(parent)) {
                try {
                    return parent.toRealPath().resolve(normalized.getFileName()).normalize();
                } catch (IOException ignored) {
                    return normalized;
                }
            }
            return normalized;
        }
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("路径错误");
        alert.setHeaderText(null);
        if (primaryStage != null) {
            alert.initOwner(primaryStage);
        }
        alert.showAndWait();
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
        setDatePickerEditorStyle(dp_start, false);
        setDatePickerEditorStyle(dp_end, false);
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
        setDatePickerEditorStyle(dp_start, true);
        setDatePickerEditorStyle(dp_end, true);
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
    private volatile ScheduledTransferConfig scheduledConfig;
    private final AtomicBoolean scheduledTransferRunning = new AtomicBoolean(false);

    private record ScheduledTransferConfig(
            String sourcePath,
            String targetPath,
            String strategy,
            String compressMode,
            boolean isCompress,
            int daysAgo) {
    }

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
        if (!validateTransferPaths(txt_schedSource.getText(), txt_schedTarget.getText(), false)) {
            return;
        }
        String compressSelection = box_schedCompress.getSelectionModel().getSelectedItem();
        String compressMode = "ImageIO".equals(compressSelection) ? IMAGEIO_MODE
                : "OpenCV".equals(compressSelection) ? OPENCV_MODE : "";
        scheduledConfig = new ScheduledTransferConfig(
                txt_schedSource.getText(),
                txt_schedTarget.getText(),
                box_schedStrategy.getSelectionModel().getSelectedItem(),
                compressMode,
                !compressMode.isEmpty(),
                Convert.toInt(txt_daysAgo.getText(), 0));

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

        // 使用 scheduleAtFixedRate 确保每次任务的【开始时间】严格按照间隔触发，
        // 而非 scheduleWithFixedDelay（上次完成后再等间隔）
        scheduledTask = scheduler.scheduleAtFixedRate(() -> runScheduledTransferIfIdle(scheduledConfig), initialDelay, interval * 60 * 1000,
                java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void stopSchedule() {
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        scheduledTransferRunning.set(false);
        logToSched("定时任务已停止");
        lb_schedStatus.setText("已停止");
        lb_schedStatus.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold;");
        btn_startSched.setDisable(false);
        btn_stopSched.setDisable(true);
        setSchedInputsDisable(false);
    }

    private void runScheduledTransferIfIdle(ScheduledTransferConfig config) {
        if (!scheduledTransferRunning.compareAndSet(false, true)) {
            logToSched("上一次定时任务仍在执行，本次跳过");
            return;
        }
        try {
            runScheduledTransfer(config);
        } finally {
            scheduledTransferRunning.set(false);
        }
    }

    private void runScheduledTransfer(ScheduledTransferConfig config) {
        if (config == null) {
            logToSched("Schedule config is empty");
            return;
        }
        Platform.runLater(() -> {
            lb_lastRun.setText(cn.hutool.core.date.DateUtil.now());
            logToSched("开始执行定时转移...");
        });

        try {
            String sourcePath = config.sourcePath();
            String targetPath = config.targetPath();
            String strategy = config.strategy();
            String compressMode = config.compressMode();
            int daysAgo = config.daysAgo();
            boolean isCompress = config.isCompress();

            File root = new File(sourcePath);
            if (!root.exists()) {
                logToSched("错误: 源路径不存在 " + sourcePath);
                return;
            }

            List<File> roots = new ArrayList<>();
            if (daysAgo > 0) {
                Date targetDate = cn.hutool.core.date.DateUtil.offsetDay(new Date(), -daysAgo);
                String targetDateStr = cn.hutool.core.date.DateUtil.format(targetDate, "yyyyMMdd"); // 假设文件夹格式包含日期
                logToSched("筛选日期早于或等于 " + targetDateStr + " 的文件夹 (N=" + daysAgo + ")");

                List<DirEntry> list = new ArrayList<>();
                java.time.LocalDate endDate = java.time.LocalDate.now().minusDays(daysAgo);
                java.time.LocalDate startDate = java.time.LocalDate.of(1900, 1, 1);
                TransferFileUtil.getDir(root, list, pattern, startDate, endDate);

                for (DirEntry entry : list) {
                    roots.add(new File(entry.getDirPath()));
                }

                if (roots.isEmpty()) {
                    logToSched("未找到 " + endDate + " 及更早的对应文件夹，跳过本次执行");
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
            awaitScheduledTask(schedTask);

            logToSched("定时转移执行完成");

        } catch (Exception e) {
            log.error("Scheduled transfer failed", e);
            logToSched("执行失败: " + e.getMessage());
        }
    }

    private void awaitScheduledTask(TransferTask schedTask) {
        try {
            schedTask.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Scheduled transfer interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private void logToSched(String msg) {
        Runnable appendLog = () -> txt_schedLog.appendText("[" + cn.hutool.core.date.DateUtil.now() + "] " + msg + "\n");
        if (Platform.isFxApplicationThread()) {
            appendLog.run();
        } else {
            Platform.runLater(appendLog);
        }
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
        setDatePickerEditorStyle(dp_start, flag);
        setDatePickerEditorStyle(dp_end, flag);
    }

    /**
     * 直接设置 DatePicker 编辑器的样式，绕过 JavaFX CSS 伪类传播不稳定的问题。
     * 禁用时：深灰背景 + 近黑文字 + opacity:1.0；启用时：恢复正常浅灰背景 + 深色文字。
     */
    private void setDatePickerEditorStyle(DatePicker dp, boolean disabled) {
        if (dp == null || dp.getEditor() == null)
            return;
        if (disabled) {
            dp.getEditor().setStyle(
                    "-fx-background-color: #b8bfc9;" +
                            "-fx-text-fill: #111827;" +
                            "-fx-border-color: #a0a8b4;" +
                            "-fx-border-radius: 4;" +
                            "-fx-background-radius: 4;" +
                            "-fx-opacity: 1.0;");
        } else {
            dp.getEditor().setStyle(
                    "-fx-background-color: #f9fafb;" +
                            "-fx-text-fill: #111827;" +
                            "-fx-border-color: #d1d5db;" +
                            "-fx-border-radius: 4;" +
                            "-fx-background-radius: 4;" +
                            "-fx-opacity: 1.0;");
        }
    }
}
