package com.example.demo.services;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    public String generateAnswer(String context, String userQuestion, List<Message> history) {
        String systemInstruction = """
            Bạn là trợ lý ảo ChronoLux chuyên tư vấn đồng hồ cao cấp.

            QUY TẮC TRẢ LỜI:
            1. Bắt buộc sử dụng dữ liệu trong phần 'Ngữ cảnh sản phẩm' để trả lời.
            2. Nếu ngữ cảnh có phần 'DỮ LIỆU TỒN KHO TOÀN CỬA HÀNG' thì ưu tiên phần này khi khách hỏi:
               - Đang bán gì
               - Có những sản phẩm nào
               - Số lượng tồn kho / còn hàng bao nhiêu
            3. Khi ngữ cảnh đã có dữ liệu tồn kho, không được nói 'không có thông tin'.
            4. Nếu khách hỏi thông số kỹ thuật (máy, kính, kháng nước...), liệt kê theo ngữ cảnh.
            5. Nếu ngữ cảnh thật sự không có thông tin liên quan, mới thông báo chưa có thông tin.
            6. Trả lời bằng tiếng Việt lịch sự, chuyên nghiệp, thuần văn bản (không markdown).

            Ngữ cảnh sản phẩm:
            {context}
            """;

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemInstruction);
        var systemMessage = systemPromptTemplate.createMessage(Map.of("context", context));
        List<Message> messages = new ArrayList<>();
        messages.add(systemMessage);
        if (history != null) {
            messages.addAll(history);
        }
        messages.add(new UserMessage(userQuestion));

        Prompt prompt = new Prompt(messages);

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