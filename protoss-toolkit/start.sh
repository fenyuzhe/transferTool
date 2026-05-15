#!/bin/bash

# 设置内存参数
JAVA_OPTS="-Xms512m -Xmx4096m -Dfile.encoding=UTF-8"

# 尝试使用同目录下的 JDK (如果存在)
if [ -d "./jdk-17.0.12" ]; then
    JAVA_CMD="./jdk-17.0.12/bin/java"
else
    # 否则使用系统环境变量中的 java
    JAVA_CMD="java"
fi

echo "Starting protoss-toolkit with: $JAVA_CMD"

# 启动程序
$JAVA_CMD $JAVA_OPTS -jar protoss-toolkit-1.2.jar
