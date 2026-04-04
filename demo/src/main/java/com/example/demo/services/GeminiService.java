package com.example.demo.services;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
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
            1. Sử dụng thông tin trong phần 'Ngữ cảnh' dưới đây để trả lời.
            2. Kiểm tra kỹ 'Số lượng còn lại trong kho' trước khi trả lời về tình trạng hàng. 
               - Nếu số lượng > 0: Xác nhận còn hàng.
               - Nếu số lượng = 0: Thông báo hết hàng và gợi ý mẫu tương tự.
            3. Nếu khách hỏi về thông số (mặt kính, loại máy, kháng nước...), hãy liệt kê chi tiết từ ngữ cảnh.
            4. Trả lời bằng tiếng Việt lịch sự, chuyên nghiệp.
            5. Nếu thông tin hoàn toàn không có trong ngữ cảnh, hãy báo khách hàng rằng bạn chưa có thông tin về mẫu này và đề nghị kết nối nhân viên.
            6. Trả lời thuần văn bản, không dùng ký tự định dạng Markdown như **, #, -, __.

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
