package com.example.demo.services;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;

    public float[] embedText(String text) {
        return embeddingModel.embed(text);
    }

    public String generateAnswer(String context, String userQuestion) {
        String systemInstruction = """
                Bạn là trợ lý ảo ChronoLux chuyên nghiệp.
                Sử dụng các thông tin sau để trả lời câu hỏi của khách hàng:
                {context}
                Nếu thông tin không có trong ngữ cảnh, hãy lịch sự từ chối và đề nghị kết nối với nhân viên.
                Luôn trả lời bằng tiếng Việt có dấu.
                Trả lời thuần văn bản, không dùng Markdown (không dùng **, __, #, -).
                """;

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemInstruction);
        var systemMessage = systemPromptTemplate.createMessage(Map.of("context", context));
        var userMessage = new UserMessage(userQuestion);

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        try {
            String rawAnswer = chatModel.call(prompt).getResult().getOutput().getText();
            return sanitizePlainText(rawAnswer);
        } catch (Exception e) {
            return "Xin lỗi, tôi đang gặp khó khăn khi kết nối. Bạn vui lòng thử lại sau.";
        }
    }

    private String sanitizePlainText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .trim();
    }
}
