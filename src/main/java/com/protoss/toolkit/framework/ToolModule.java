package com.protoss.toolkit.framework;

import javafx.scene.Node;

import java.io.IOException;

public interface ToolModule {
    ToolDescriptor descriptor();

    Node createView(ToolContext context) throws IOException;
}
