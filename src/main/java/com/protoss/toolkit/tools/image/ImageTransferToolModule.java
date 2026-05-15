package com.protoss.toolkit.tools.image;

import com.protoss.toolkit.framework.FxmlToolModule;
import com.protoss.toolkit.framework.ToolDescriptor;

public class ImageTransferToolModule extends FxmlToolModule {
    public ImageTransferToolModule() {
        super(
                new ToolDescriptor(
                        "image-transfer",
                        "图像转移",
                        "支持手动转移、定时转移、压缩转码、日期筛选与运行监控。",
                        "将现有影像目录按规则复制或移动到目标目录，可选 JPEG2000 无损压缩。",
                        "fas-exchange-alt",
                        "图像工具"),
                "/com/protoss/toolkit/fxml/transfer-view.fxml");
    }
}
