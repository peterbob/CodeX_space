import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
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

    public static void main(String[] args) {
        EnglishLearningAgent agent = EnglishLearningAgent.fromEnvironment();
        agent.run();
    }

    static class EnglishLearningAgent {
        private final LlmClient llmClient;
        private final Scanner scanner = new Scanner(System.in, "UTF-8");

        private EnglishLearningAgent(LlmClient llmClient) {
            this.llmClient = llmClient;
        }

        static EnglishLearningAgent fromEnvironment() {
            String apiKey = System.getenv("OPENAI_API_KEY");
            String model = envOrDefault("OPENAI_MODEL", "gpt-4.1-mini");

            if (apiKey != null && !apiKey.trim().isEmpty()) {
                return new EnglishLearningAgent(new OpenAiClient(apiKey, model));
            }
            return new EnglishLearningAgent(new LocalFallbackClient());
        }

        void run() {
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

                String prompt = "You are an English tutor. Reply with JSON keys: reply, betterVersion, tip. " +
                        "User sentence: " + userInput;
                String raw = llmClient.chat(prompt);

                System.out.println("Agent: " + extractJsonField(raw, "reply", "Good attempt! Keep going."));
                System.out.println("Better: " + extractJsonField(raw, "betterVersion", userInput));
                System.out.println("Tip: " + extractJsonField(raw, "tip", "Try adding one more detail next time."));
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

            String prompt = "Correct the grammar and style. Return JSON keys: corrected, chineseExplanation, keyMistakes. Text: " + text;
            String raw = llmClient.chat(prompt);

            System.out.println("纠错结果: " + extractJsonField(raw, "corrected", text));
            System.out.println("中文解释: " + extractJsonField(raw, "chineseExplanation", "整体可理解，建议优化时态和搭配。"));
            System.out.println("关键问题: " + extractJsonField(raw, "keyMistakes", "注意主谓一致和冠词。"));
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

            String prompt = "Extract up to 8 useful vocabulary words from the text for Chinese learner. " +
                    "Return JSON with cards (word, meaningZh, example). Text: " + text;
            String raw = llmClient.chat(prompt);

            List<String> fallbackWords = extractTopWords(text, 8);
            if (raw.contains("cards")) {
                System.out.println("词汇卡片(JSON): " + raw);
            } else {
                System.out.println("推荐词汇:");
                for (String word : fallbackWords) {
                    System.out.println("- " + word + " | 中文: 待补充 | 例句: I use '" + word + "' in a sentence.");
                }
            }
            System.out.println();
        }

        private void dailyPlan() {
            System.out.println("\n[学习计划] 我会根据你的目标生成 7 天练习建议。\n");
            System.out.print("你的目标（如 雅思6.5 / 职场口语 / 旅行英语）: ");
            String goal = scanner.nextLine().trim();
            if (goal.isEmpty()) {
                goal = "提升日常沟通能力";
            }

            String prompt = "Create a 7-day English study plan for this goal: " + goal +
                    ". Return JSON key plan in Chinese.";
            String raw = llmClient.chat(prompt);

            String plan = extractJsonField(raw, "plan", "第1-7天: 每天30分钟，10分钟输入+10分钟复述+10分钟纠错。重点: " + goal);
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
        }

        private void printMenu() {
            System.out.println("1. 自由对话练习");
            System.out.println("2. 英文语法纠错");
            System.out.println("3. 词汇卡片生成");
            System.out.println("4. 7天学习计划");
            System.out.println("5. 退出");
        }

        private static String envOrDefault(String name, String fallback) {
            String value = System.getenv(name);
            return (value == null || value.trim().isEmpty()) ? fallback : value;
        }
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
        List<String> stopwords = Arrays.asList("the", "a", "an", "is", "are", "am", "to", "in", "on", "at", "for", "of", "and", "or", "it", "i", "you", "he", "she", "they", "we", "this", "that", "with", "be", "as", "was", "were", "my", "your");

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
