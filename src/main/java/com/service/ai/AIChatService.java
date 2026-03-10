package com.service.ai;

import com.dto.ChatRequest;
import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AIChatService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    public AIChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }
    
    public String generateAnswer(ChatRequest chatRequest) throws InterruptedException {
        String question = chatRequest != null ? chatRequest.getQuestion() : null;
        if (question == null || question.trim().isEmpty()) {
            return "Ban vui long nhap cau hoi cu the de minh tu van chinh xac hon.";
        }

        List<Document> similarDocument = vectorStore.similaritySearch(
            SearchRequest.builder().query(question)
                        .topK(4)
                        .build()
        );
        String context = similarDocument.stream()
                .map(docs -> {
                    String id = docs.getMetadata().get("id").toString();
                    String text = docs.getText();
                    return text + "\nLink: http://localhost:8080/product_detail?id=" + id;
                })
                .collect(Collectors.joining("\n"));
        String promptTemplate = String.format("""
            Ban la tro ly ao cho shop CarHub.
            Cau hoi cua khach:
            %s
            "QUAN TRỌNG: Không dùng markdown, không dùng **, không dùng *, " +
            "xuống dòng bằng \\n thông thường.";
            Duoi day la cac thong tin tham khao (context):
                %s
            Hay tu van dung trong pham vi cau hoi, ngan gon, ro rang.
            Neu context khong du thong tin, hay noi ro dieu do va dat 1-2 cau hoi lam ro nhu cau.
            """, question, context);
        try {
            return chatClient.prompt().user(promptTemplate).call().content();
        }
        catch (Exception e) {
            if (e.getMessage().contains("429")) {
                Thread.sleep(15000); // chờ 15 giây
                return chatClient.prompt().user(promptTemplate).call().content();
            }
            throw e;
        }
    }
}
