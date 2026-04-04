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
            Ban la tro ly ao ChronoLux chuyen tu van dong ho cao cap.

            QUY TAC TRA LOI:
            1. Bat buoc su dung du lieu trong phan 'Ngu canh san pham' de tra loi.
            2. Neu ngu canh co phan 'DU LIEU TON KHO TOAN CUA HANG' thi uu tien phan nay khi khach hoi:
               - Dang ban gi
               - Co nhung san pham nao
               - So luong ton kho / con hang bao nhieu
            3. Khi ngu canh da co du lieu ton kho, khong duoc noi 'khong co thong tin'.
            4. Neu khach hoi thong so ky thuat (may, kinh, khang nuoc...), liet ke theo ngu canh.
            5. Neu ngu canh that su khong co thong tin lien quan, moi thong bao chua co thong tin.
            6. Tra loi bang tieng Viet lich su, chuyen nghiep, thuan van ban (khong markdown).

            Ngu canh san pham:
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
            return "Xin loi, toi dang gap kho khan khi ket noi. Ban vui long thu lai sau.";
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