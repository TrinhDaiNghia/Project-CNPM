package com.example.demo.services;

import lombok.RequiredArgsConstructor;
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

    /**
     * Chuyển đổi văn bản thành mảng vector (Embedding)
     */
    public float[] embedText(String text) {
        return embeddingModel.embed(text);
    }

    /**
     * Tạo câu trả lời dựa trên ngữ cảnh và câu hỏi của khách hàng
     */
    public String generateAnswer(String context, String userQuestion) {
        String systemInstruction = """
                Bạn là trợ lý ảo ChronoLux chuyên nghiệp. 
                Sử dụng các thông tin sau để trả lời câu hỏi của khách hàng: 
                {context}
                Nếu thông tin không có trong ngữ cảnh, hãy lịch sự từ chối và đề nghị kết nối với nhân viên.
                """;

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemInstruction);
        var systemMessage = systemPromptTemplate.createMessage(Map.of("context", context));
        var userMessage = new org.springframework.ai.chat.messages.UserMessage(userQuestion);

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        
        try {
            return chatModel.call(prompt).getResult().getOutput().getText();
        } catch (Exception e) {
            return "Xin lỗi, tôi đang gặp khó khăn khi kết nối. Bạn vui lòng thử lại sau.";
        }
    }
}
