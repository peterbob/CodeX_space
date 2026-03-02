# English Learning AI Agent (Java)

一个命令行版英语学习 Agent，支持：

- 自由对话练习（返回更自然表达）
- 英文语法纠错（含中文解释）
- 词汇卡片生成
- 7天学习计划

## 运行

```bash
javac src/Main.java
java -cp src Main
```

## 可选：接入真实大模型

设置环境变量后，程序会自动调用 OpenAI Responses API：

```bash
export OPENAI_API_KEY="your_api_key"
export OPENAI_MODEL="gpt-4.1-mini"
```

未设置 API Key 时会使用本地 fallback，用于离线演示流程。

## 后续可扩展

- 保存学习记录（SQLite / JSON）
- 加入每日复习（SRS）
- 增加口语评分（发音维度）
