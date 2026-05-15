package com.protoss.toolkit.controller;

import com.protoss.toolkit.framework.StageAware;
import com.protoss.toolkit.framework.ToolContext;
import com.protoss.toolkit.framework.ToolDescriptor;
import com.protoss.toolkit.framework.ToolModule;
import com.protoss.toolkit.framework.ToolRegistry;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ToolHostController implements Initializable, StageAware {
    private static final Logger log = LoggerFactory.getLogger(ToolHostController.class);

    @FXML
    private VBox toolNavigation;
    @FXML
    private Label toolName;
    @FXML
    private Label toolSubtitle;
    @FXML
    private Label toolCategory;
    @FXML
    private StackPane toolContent;

    private final ToggleGroup navigationGroup = new ToggleGroup();
    private final List<ToolModule> tools = ToolRegistry.defaultTools();
    private final Map<String, Node> toolViewCache = new HashMap<>();
    private Stage primaryStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        renderToolNavigation();
    }

    @Override
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        if (!tools.isEmpty() && toolContent.getChildren().isEmpty()) {
            openTool(tools.get(0));
        }
    }

    private void renderToolNavigation() {
        toolNavigation.getChildren().clear();
        for (ToolModule tool : tools) {
            ToggleButton item = createNavigationItem(tool);
            toolNavigation.getChildren().add(item);
        }
        if (!toolNavigation.getChildren().isEmpty()) {
            ToggleButton first = (ToggleButton) toolNavigation.getChildren().get(0);
            first.setSelected(true);
        }
    }

    private ToggleButton createNavigationItem(ToolModule tool) {
        ToolDescriptor descriptor = tool.descriptor();
        ToggleButton item = new ToggleButton(descriptor.name());
        item.setMaxWidth(Double.MAX_VALUE);
        item.setToggleGroup(navigationGroup);
        item.getStyleClass().add("tool-nav-item");
        item.setGraphic(new FontIcon(descriptor.iconLiteral()));
        item.setOnAction(event -> openTool(tool));
        return item;
    }

    private void openTool(ToolModule tool) {
        try {
            ToolDescriptor descriptor = tool.descriptor();
            toolName.setText(descriptor.name());
            toolSubtitle.setText(descriptor.subtitle());
            toolCategory.setText(descriptor.category());

            Node view = toolViewCache.computeIfAbsent(descriptor.id(), id -> createToolView(tool));
            toolContent.getChildren().setAll(view);
        } catch (RuntimeException e) {
            log.error("Failed to open tool: {}", tool.descriptor().id(), e);
            toolName.setText("工具加载失败");
            toolSubtitle.setText(e.getMessage());
            toolCategory.setText("错误");
            toolContent.getChildren().setAll(new Label("无法加载工具：" + tool.descriptor().name()));
        }
    }

    private Node createToolView(ToolModule tool) {
        try {
            return tool.createView(new ToolContext(primaryStage));
        } catch (IOException e) {
            throw new ToolLoadException("无法加载工具：" + tool.descriptor().name(), e);
        }
    }

    private static class ToolLoadException extends RuntimeException {
        private ToolLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
