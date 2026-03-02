import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) throws Exception {
        LlmClient llmClient = createLlmClient();
        EnglishLearningAgent agent = new EnglishLearningAgent(llmClient);

        if (args.length > 0 && "web".equalsIgnoreCase(args[0])) {
            int port = args.length > 1 ? parsePort(args[1], 8080) : 8080;
            new WebServer(agent, port).start();
            return;
        }

        agent.runCli();
    }

    private static int parsePort(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static LlmClient createLlmClient() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        String model = envOrDefault("OPENAI_MODEL", "gpt-4.1-mini");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return new OpenAiClient(apiKey, model);
        }
        return new LocalFallbackClient();
    }

    static class EnglishLearningAgent {
        private final LlmClient llmClient;
        private final Scanner scanner = new Scanner(System.in, "UTF-8");

        private EnglishLearningAgent(LlmClient llmClient) {
            this.llmClient = llmClient;
        }

        void runCli() {
            printWelcome();
            while (true) {
                printMenu();
                System.out.print("请选择功能 (1-5): ");
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        conversationPractice();
                        break;
                    case "2":
                        grammarCorrection();
                        break;
                    case "3":
                        vocabularyCards();
                        break;
                    case "4":
                        dailyPlan();
                        break;
                    case "5":
                        System.out.println("再见，继续坚持每天输入英文。\n");
                        return;
                    default:
                        System.out.println("无效选择，请输入 1-5。\n");
                }
            }
        }

        ConversationResult analyzeConversation(String userInput) {
            String prompt = "You are an English tutor. Reply with JSON keys: reply, betterVersion, tip. " +
                    "User sentence: " + userInput;
            String raw = llmClient.chat(prompt);
            return new ConversationResult(
                    extractJsonField(raw, "reply", "Good attempt! Keep going."),
                    extractJsonField(raw, "betterVersion", userInput),
                    extractJsonField(raw, "tip", "Try adding one more detail next time.")
            );
        }

        CorrectionResult analyzeCorrection(String text) {
            String prompt = "Correct the grammar and style. Return JSON keys: corrected, chineseExplanation, keyMistakes. Text: " + text;
            String raw = llmClient.chat(prompt);
            return new CorrectionResult(
                    extractJsonField(raw, "corrected", text),
                    extractJsonField(raw, "chineseExplanation", "整体可理解，建议优化时态和搭配。"),
                    extractJsonField(raw, "keyMistakes", "注意主谓一致和冠词。")
            );
        }

        String analyzeVocabularyRaw(String text) {
            String prompt = "Extract up to 8 useful vocabulary words from the text for Chinese learner. " +
                    "Return JSON with cards (word, meaningZh, example). Text: " + text;
            String raw = llmClient.chat(prompt);
            if (raw.contains("cards")) {
                return raw;
            }

            List<String> fallbackWords = extractTopWords(text, 8);
            StringBuilder sb = new StringBuilder();
            sb.append("{\"cards\":[");
            for (int i = 0; i < fallbackWords.size(); i++) {
                String word = fallbackWords.get(i);
                if (i > 0) {
                    sb.append(",");
                }
                sb.append("{\"word\":\"").append(escapeJson(word)).append("\",")
                        .append("\"meaningZh\":\"待补充\",")
                        .append("\"example\":\"I use '").append(escapeJson(word)).append("' in a sentence.\"}");
            }
            sb.append("]}");
            return sb.toString();
        }

        String analyzePlan(String goal) {
            String usedGoal = (goal == null || goal.trim().isEmpty()) ? "提升日常沟通能力" : goal.trim();
            String prompt = "Create a 7-day English study plan for this goal: " + usedGoal +
                    ". Return JSON key plan in Chinese.";
            String raw = llmClient.chat(prompt);
            return extractJsonField(raw, "plan", "第1-7天: 每天30分钟，10分钟输入+10分钟复述+10分钟纠错。重点: " + usedGoal);
        }

        private void conversationPractice() {
            System.out.println("\n[自由对话模式] 输入英文，Agent 会回复并给你一个更自然表达建议。输入 exit 返回主菜单。");
            while (true) {
                System.out.print("You: ");
                String userInput = scanner.nextLine().trim();
                if ("exit".equalsIgnoreCase(userInput)) {
                    System.out.println();
                    return;
                }
                if (userInput.isEmpty()) {
                    System.out.println("请输入一句英文。\n");
                    continue;
                }
                ConversationResult result = analyzeConversation(userInput);
                System.out.println("Agent: " + result.reply);
                System.out.println("Better: " + result.betterVersion);
                System.out.println("Tip: " + result.tip);
                System.out.println();
            }
        }

        private void grammarCorrection() {
            System.out.println("\n[语法纠错] 输入一段英文，Agent 会给出纠错和中文解释。\n");
            System.out.print("请输入英文: ");
            String text = scanner.nextLine().trim();
            if (text.isEmpty()) {
                System.out.println("内容为空，已返回主菜单。\n");
                return;
            }

            CorrectionResult result = analyzeCorrection(text);
            System.out.println("纠错结果: " + result.corrected);
            System.out.println("中文解释: " + result.chineseExplanation);
            System.out.println("关键问题: " + result.keyMistakes);
            System.out.println();
        }

        private void vocabularyCards() {
            System.out.println("\n[词汇卡片] 输入一段英文，我会提取高频词并给出学习卡片。\n");
            System.out.print("请输入英文段落: ");
            String text = scanner.nextLine().trim();
            if (text.isEmpty()) {
                System.out.println("内容为空，已返回主菜单。\n");
                return;
            }
            System.out.println("词汇卡片(JSON): " + analyzeVocabularyRaw(text));
            System.out.println();
        }

        private void dailyPlan() {
            System.out.println("\n[学习计划] 我会根据你的目标生成 7 天练习建议。\n");
            System.out.print("你的目标（如 雅思6.5 / 职场口语 / 旅行英语）: ");
            String goal = scanner.nextLine().trim();
            String plan = analyzePlan(goal);
            System.out.println("你的 7 天计划:");
            System.out.println(plan);
            System.out.println();
        }

        private void printWelcome() {
            System.out.println("========================================");
            System.out.println(" English Learning AI Agent (Java CLI)");
            System.out.println("========================================");
            if (llmClient instanceof OpenAiClient) {
                System.out.println("已检测到 OPENAI_API_KEY，当前使用真实模型。\n");
            } else {
                System.out.println("未检测到 OPENAI_API_KEY，当前使用本地 fallback（可跑通流程）。\n");
            }
            System.out.println("提示: 运行 `mvn exec:java -Dexec.args=\"web\"` 可打开可视化页面。\n");
        }

        private void printMenu() {
            System.out.println("1. 自由对话练习");
            System.out.println("2. 英文语法纠错");
            System.out.println("3. 词汇卡片生成");
            System.out.println("4. 7天学习计划");
            System.out.println("5. 退出");
        }
    }

    static class WebServer {
        private final EnglishLearningAgent agent;
        private final int port;

        WebServer(EnglishLearningAgent agent, int port) {
            this.agent = agent;
            this.port = port;
        }

        void start() throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new IndexHandler());
            server.createContext("/api/chat", new ChatHandler(agent));
            server.createContext("/api/correct", new CorrectHandler(agent));
            server.createContext("/api/vocab", new VocabHandler(agent));
            server.createContext("/api/plan", new PlanHandler(agent));
            server.setExecutor(null);
            server.start();

            System.out.println("Web 服务已启动: http://localhost:" + port);
            System.out.println("按 Ctrl+C 停止服务。");
        }
    }

    static class IndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            byte[] html = loadWebFile();
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }

        private static byte[] loadWebFile() {
            InputStream is = Main.class.getResourceAsStream("/web/index.html");
            if (is == null) {
                return "<html><body><h1>index.html not found in resources</h1></body></html>"
                        .getBytes(StandardCharsets.UTF_8);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            try {
                while ((n = is.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
                is.close();
            } catch (IOException e) {
                return ("<html><body><h1>Read error: " + escapeJson(e.getMessage()) + "</h1></body></html>")
                        .getBytes(StandardCharsets.UTF_8);
            }
            return baos.toByteArray();
        }
    }

    static class ChatHandler implements HttpHandler {
        private final EnglishLearningAgent agent;

        ChatHandler(EnglishLearningAgent agent) {
            this.agent = agent;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            String body = readBody(exchange);
            String text = extractJsonField(body, "text", "");
            ConversationResult result = agent.analyzeConversation(text);
            String json = "{" +
                    "\"reply\":\"" + escapeJson(result.reply) + "\"," +
                    "\"betterVersion\":\"" + escapeJson(result.betterVersion) + "\"," +
                    "\"tip\":\"" + escapeJson(result.tip) + "\"" +
                    "}";
            sendJson(exchange, json);
        }
    }

    static class CorrectHandler implements HttpHandler {
        private final EnglishLearningAgent agent;

        CorrectHandler(EnglishLearningAgent agent) {
            this.agent = agent;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            String body = readBody(exchange);
            String text = extractJsonField(body, "text", "");
            CorrectionResult result = agent.analyzeCorrection(text);
            String json = "{" +
                    "\"corrected\":\"" + escapeJson(result.corrected) + "\"," +
                    "\"chineseExplanation\":\"" + escapeJson(result.chineseExplanation) + "\"," +
                    "\"keyMistakes\":\"" + escapeJson(result.keyMistakes) + "\"" +
                    "}";
            sendJson(exchange, json);
        }
    }

    static class VocabHandler implements HttpHandler {
        private final EnglishLearningAgent agent;

        VocabHandler(EnglishLearningAgent agent) {
            this.agent = agent;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            String body = readBody(exchange);
            String text = extractJsonField(body, "text", "");
            sendJson(exchange, agent.analyzeVocabularyRaw(text));
        }
    }

    static class PlanHandler implements HttpHandler {
        private final EnglishLearningAgent agent;

        PlanHandler(EnglishLearningAgent agent) {
            this.agent = agent;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            String body = readBody(exchange);
            String text = extractJsonField(body, "text", "");
            String plan = agent.analyzePlan(text);
            String json = "{\"plan\":\"" + escapeJson(plan) + "\"}";
            sendJson(exchange, json);
        }
    }

    private static void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 405, "text/plain; charset=UTF-8", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
    }

    private static void sendJson(HttpExchange exchange, String json) throws IOException {
        sendResponse(exchange, 200, "application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static void sendResponse(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        OutputStream os = exchange.getResponseBody();
        os.write(body);
        os.flush();
        os.close();
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    interface LlmClient {
        String chat(String prompt);
    }

    static class OpenAiClient implements LlmClient {
        private final String apiKey;
        private final String model;

        OpenAiClient(String apiKey, String model) {
            this.apiKey = apiKey;
            this.model = model;
        }

        @Override
        public String chat(String prompt) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://api.openai.com/v1/responses");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");

                String body = "{" +
                        "\"model\":\"" + escapeJson(model) + "\"," +
                        "\"input\":\"" + escapeJson(prompt) + "\"" +
                        "}";

                OutputStream os = conn.getOutputStream();
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int status = conn.getResponseCode();
                InputStream is = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
                String resp = readAll(is);
                if (status >= 200 && status < 300) {
                    return extractOutputText(resp);
                }
                return "{\"reply\":\"API error\",\"tip\":\"HTTP " + status + "\"}";
            } catch (Exception e) {
                return "{\"reply\":\"Network error\",\"tip\":\"" + escapeJson(e.getMessage()) + "\"}";
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        private static String readAll(InputStream is) throws IOException {
            if (is == null) {
                return "";
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            return sb.toString();
        }

        private static String extractOutputText(String json) {
            Pattern p = Pattern.compile("\\\"output_text\\\"\\s*:\\s*\\\"(.*?)\\\"", Pattern.DOTALL);
            Matcher m = p.matcher(json);
            if (m.find()) {
                return unescapeJson(m.group(1));
            }
            return json;
        }
    }

    static class LocalFallbackClient implements LlmClient {

        @Override
        public String chat(String prompt) {
            String lower = prompt.toLowerCase(Locale.ROOT);
            if (lower.contains("keys: reply")) {
                String userSentence = tailAfter(prompt, "User sentence:");
                return "{\"reply\":\"Nice sentence. Could you add one specific detail?\"," +
                        "\"betterVersion\":\"" + escapeJson(polishSentence(userSentence)) + "\"," +
                        "\"tip\":\"Use time markers like yesterday, recently, or every day.\"}";
            }
            if (lower.contains("keys: corrected")) {
                String text = tailAfter(prompt, "Text:");
                return "{\"corrected\":\"" + escapeJson(polishSentence(text)) + "\"," +
                        "\"chineseExplanation\":\"主要调整了大小写、句号和常见口语表达，使句子更自然。\"," +
                        "\"keyMistakes\":\"时态、冠词和固定搭配是最常见问题。\"}";
            }
            if (lower.contains("cards")) {
                return "{\"cards\":[{" +
                        "\"word\":\"improve\",\"meaningZh\":\"提升\",\"example\":\"I want to improve my English pronunciation.\"},{" +
                        "\"word\":\"confident\",\"meaningZh\":\"自信的\",\"example\":\"I feel more confident speaking in meetings.\"}]}";
            }
            if (lower.contains("7-day") || lower.contains("7 day")) {
                return "{\"plan\":\"Day1词汇输入, Day2听力跟读, Day3口语复述, Day4语法纠错, Day5写作100词, Day6场景对话, Day7复盘测试。\"}";
            }
            return "{\"reply\":\"Please provide your English sentence.\"}";
        }

        private static String polishSentence(String text) {
            String trimmed = text == null ? "" : text.trim();
            if (trimmed.isEmpty()) {
                return "Please share one sentence for practice.";
            }
            String first = trimmed.substring(0, 1).toUpperCase(Locale.ROOT);
            String rest = trimmed.length() > 1 ? trimmed.substring(1) : "";
            String s = first + rest;
            if (!s.endsWith(".") && !s.endsWith("!") && !s.endsWith("?")) {
                s = s + ".";
            }
            return s;
        }
    }

    static class ConversationResult {
        final String reply;
        final String betterVersion;
        final String tip;

        ConversationResult(String reply, String betterVersion, String tip) {
            this.reply = reply;
            this.betterVersion = betterVersion;
            this.tip = tip;
        }
    }

    static class CorrectionResult {
        final String corrected;
        final String chineseExplanation;
        final String keyMistakes;

        CorrectionResult(String corrected, String chineseExplanation, String keyMistakes) {
            this.corrected = corrected;
            this.chineseExplanation = chineseExplanation;
            this.keyMistakes = keyMistakes;
        }
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.trim().isEmpty()) ? fallback : value;
    }

    private static String extractJsonField(String json, String field, String fallback) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"(.*?)\\\"", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (m.find()) {
            return unescapeJson(m.group(1));
        }
        return fallback;
    }

    private static String tailAfter(String src, String marker) {
        int idx = src.indexOf(marker);
        if (idx < 0) {
            return src;
        }
        return src.substring(idx + marker.length()).trim();
    }

    private static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String unescapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static List<String> extractTopWords(String text, int limit) {
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z\\s]", " ");
        String[] tokens = normalized.split("\\s+");
        List<String> stopwords = Arrays.asList(
                "the", "a", "an", "is", "are", "am", "to", "in", "on", "at", "for", "of", "and", "or", "it", "i", "you",
                "he", "she", "they", "we", "this", "that", "with", "be", "as", "was", "were", "my", "your"
        );

        Set<String> unique = new LinkedHashSet<String>();
        for (String t : tokens) {
            if (t.length() < 4 || stopwords.contains(t)) {
                continue;
            }
            unique.add(t);
            if (unique.size() >= limit) {
                break;
            }
        }
        return new ArrayList<String>(unique);
    }
}
