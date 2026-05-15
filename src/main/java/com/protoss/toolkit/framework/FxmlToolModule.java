package com.protoss.toolkit.framework;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.io.IOException;
import java.net.URL;

public abstract class FxmlToolModule implements ToolModule {
    private final ToolDescriptor descriptor;
    private final String fxmlPath;

    protected FxmlToolModule(ToolDescriptor descriptor, String fxmlPath) {
        this.descriptor = descriptor;
        this.fxmlPath = fxmlPath;
    }

    @Override
    public ToolDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public Node createView(ToolContext context) throws IOException {
        URL fxmlUrl = getClass().getResource(fxmlPath);
        if (fxmlUrl == null) {
            throw new IOException("Tool FXML not found: " + fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Node view = loader.load();
        Object controller = loader.getController();
        if (controller instanceof StageAware stageAware) {
            stageAware.setPrimaryStage(context.primaryStage());
        }
        return view;
    }
}
