# English Learning AI Agent (Java)

支持两种模式：

- CLI 命令行模式
- HTTP 可视化页面模式

## 运行 CLI

```bash
javac src/Main.java
java -cp src Main
```

## 运行可视化 HTTP 页面

```bash
javac src/Main.java
java -cp src Main web
```

默认访问地址：

- http://localhost:8080

也可自定义端口：

```bash
java -cp src Main web 18080
```

## 可选：接入真实大模型

设置环境变量后，程序会自动调用 OpenAI Responses API：

```bash
export OPENAI_API_KEY="your_api_key"
export OPENAI_MODEL="gpt-4.1-mini"
```

未设置 API Key 时会使用本地 fallback（可离线演示完整流程）。
