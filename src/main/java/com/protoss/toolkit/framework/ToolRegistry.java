package com.protoss.toolkit.framework;

import com.protoss.toolkit.tools.image.ImageTransferToolModule;

import java.util.List;

public final class ToolRegistry {
    private ToolRegistry() {
    }

    public static List<ToolModule> defaultTools() {
        return List.of(
                new ImageTransferToolModule());
    }
}
