# English Learning AI Agent (Java + Maven)

支持两种模式：

- CLI 命令行模式
- HTTP 可视化页面模式

## 使用 Maven 运行

先编译：

```bash
mvn clean compile
```

运行 CLI：

```bash
mvn exec:java
```

运行可视化 HTTP 页面：

```bash
mvn exec:java -Dexec.args="web"
```

默认访问地址：

- http://localhost:8080

自定义端口：

```bash
mvn exec:java -Dexec.args="web 18080"
```

## 打包

```bash
mvn clean package
java -jar target/english-learning-agent-1.0.0-SNAPSHOT.jar
```

Web 模式（jar）：

```bash
java -jar target/english-learning-agent-1.0.0-SNAPSHOT.jar web 18080
```

## 可选：接入真实大模型

设置环境变量后，程序会自动调用 OpenAI Responses API：

```bash
export OPENAI_API_KEY="your_api_key"
export OPENAI_MODEL="gpt-4.1-mini"
```

未设置 API Key 时会使用本地 fallback（可离线演示完整流程）。
